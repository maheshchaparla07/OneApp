package uk.ac.tees.mad.s3445191

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.ac.tees.mad.s3445191.api.ApiClient
import uk.ac.tees.mad.s3445191.api.TimeResponse
import uk.ac.tees.mad.s3445191.databinding.ActivityHomeBinding
import uk.ac.tees.mad.s3445191.databinding.ItemApiServiceBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity(), UrlInputDialog.OnUrlAddedListener, UrlInputDialog.QRScanListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var apiServiceAdapter: ApiServiceAdapter
    private lateinit var webAppAdapter: WebAppAdapter
    private val webApps = mutableListOf<WebApp>()
    private lateinit var timeFormat: SimpleDateFormat
    private var timeUpdateHandler: Handler? = null
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            fetchUkTime()
            timeUpdateHandler?.postDelayed(this, 60000) // Update every minute
        }
    }

    // QR Scanner launcher
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { scannedUrl ->
            if (scannedUrl.startsWith("http://") || scannedUrl.startsWith("https://")) {
                showUrlInputDialog(scannedUrl)
            } else {
                Toast.makeText(this, "Scanned content is not a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        timeFormat = SimpleDateFormat("HH:mm:ss", Locale.UK)
        binding.currentTimeText.text = "Loading UK time..."

        setupUI()
        setupRecyclerView()
        setupWebAppRecyclerView()

        // Set click listeners for both add buttons
        binding.addApiButton.setOnClickListener {
            showUrlInputDialog()
        }

        binding.fabAddApp.setOnClickListener {
            showUrlInputDialog()
        }

        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUkTime() // Initial fetch
        timeUpdateHandler = Handler(Looper.getMainLooper())
        timeUpdateHandler?.postDelayed(timeUpdateRunnable, 60000) // Update every minute
    }

    override fun onPause() {
        super.onPause()
        timeUpdateHandler?.removeCallbacks(timeUpdateRunnable)
        timeUpdateHandler = null
    }

    private fun fetchUkTime() {
        if (!isGooglePlayServicesAvailable()) {
            showLocalTimeFallback()
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.timeApiService.getUkTime()
                if (response.isSuccessful) {
                    response.body()?.let { timeResponse ->
                        val dateTime = timeResponse.datetime.substringBefore(".")
                        val parsedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK).parse(dateTime)

                        parsedTime?.let {
                            val formattedTime = timeFormat.format(it)
                            updateTimeDisplay(formattedTime)
                        }
                    }
                } else {
                    showLocalTimeFallback()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch UK time", e)
                showLocalTimeFallback()
            }
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun showLocalTimeFallback() {
        val currentTime = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("Europe/London")
        }.time
        val formattedTime = timeFormat.format(currentTime)
        updateTimeDisplay("Local UK Time: $formattedTime*")
    }

    private fun updateTimeDisplay(timeText: String) {
        runOnUiThread {
            binding.currentTimeText.text = timeText
        }
    }


    private fun setupWebAppRecyclerView() {
        webAppAdapter = WebAppAdapter(
            apps = webApps,
            onClick = { app ->
                val intent = Intent(this, WebViewActivity::class.java).apply {
                    putExtra("url", app.url)
                    putExtra("title", app.name)
                }
                startActivity(intent)
            },
            onDelete = { app ->
                showDeleteConfirmationDialog(app)
            }
        )

        binding.appsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 3)
            adapter = webAppAdapter
        }

        loadAppsFromFirestore()
    }

    private fun showDeleteConfirmationDialog(app: WebApp) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove App")
            .setMessage("Remove ${app.name} from your home screen?")
            .setPositiveButton("Remove") { _, _ ->
                val position = webApps.indexOf(app)
                if (position != -1) {
                    webApps.removeAt(position)
                    webAppAdapter.notifyItemRemoved(position)
                    saveApps()
                    Toast.makeText(this, "${app.name} removed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUrlInputDialog(prefilledUrl: String = "") {
        UrlInputDialog.newInstance(prefilledUrl).show(supportFragmentManager, "UrlInputDialog")
    }

    override fun onUrlAdded(url: String, name: String) {
        val userId = auth.currentUser?.uid ?: return

        val newApp = hashMapOf(
            "name" to name,
            "url" to url,
            "userId" to userId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        Firebase.firestore.collection("webApps")
            .add(newApp)
            .addOnSuccessListener { documentReference ->
                val savedApp = WebApp(
                    id = documentReference.id,
                    name = name,
                    url = url,
                    userId = userId,
                    createdAt = Date()
                )

                webApps.add(savedApp)
                webAppAdapter.notifyItemInserted(webApps.size - 1)
                Toast.makeText(this, "App saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving app", e)
                Toast.makeText(this, "Failed to save app", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onScanRequested() {
        val options = ScanOptions().apply {
            setPrompt("Scan a QR code")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(CaptureActivityPortrait::class.java)
        }
        qrScannerLauncher.launch(options)
    }

    private fun saveApps() {
        val user = auth.currentUser
        val userId = user?.email ?: return

        getSharedPreferences("WebAppsPref_$userId", Context.MODE_PRIVATE).edit().apply {
            putString("webApps", Gson().toJson(webApps))
            apply()
        }
    }

    private fun loadSavedApps() {
        val user = auth.currentUser
        val userId = user?.email ?: return

        val json = getSharedPreferences("WebAppsPref_$userId", Context.MODE_PRIVATE)
            .getString("webApps", null) ?: return

        val type = object : TypeToken<List<WebApp>>() {}.type
        val savedApps = Gson().fromJson<List<WebApp>>(json, type)
        webApps.clear()
        webApps.addAll(savedApps)
        webAppAdapter.notifyDataSetChanged()
    }

    private fun loadAppsFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        Firebase.firestore.collection("webApps")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val apps = documents.map { doc ->
                    doc.toObject(WebApp::class.java)
                }
                webApps.clear()
                webApps.addAll(apps)
                webAppAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading apps", e)
                Toast.makeText(this, "Failed to load apps", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupUI() {
        val user = auth.currentUser ?: return
        val sharedPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        Firebase.firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName")
                    ?: sharedPrefs.getString("firstName", null)

                binding.welcomeText.text = if (!firstName.isNullOrEmpty()) {
                    "Welcome, $firstName"
                } else {
                    "Welcome"
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Firestore fetch failed, using local data", e)
                val localFirstName = sharedPrefs.getString("firstName", null)
                binding.welcomeText.text = if (!localFirstName.isNullOrEmpty()) {
                    "Welcome, $localFirstName"
                } else {
                    "Welcome, ${user.displayName ?: user.email ?: "User"}"
                }
            }
    }

    private fun setupRecyclerView() {
        apiServiceAdapter = ApiServiceAdapter()
        binding.apiServicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = apiServiceAdapter
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> logoutUser() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logoutUser() {
        auth.signOut()
        val userId = auth.currentUser?.email
        if (userId != null) {
            getSharedPreferences("WebAppsPref_$userId", Context.MODE_PRIVATE).edit().clear().apply()
        }

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    data class ApiService(val name: String, val description: String)

    class ApiServiceAdapter : ListAdapter<ApiService, ApiServiceAdapter.ApiServiceViewHolder>(
        DiffCallback()
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiServiceViewHolder {
            val binding = ItemApiServiceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ApiServiceViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ApiServiceViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ApiServiceViewHolder(private val binding: ItemApiServiceBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(service: ApiService) {
                binding.serviceName.text = service.name
                binding.serviceDescription.text = service.description
            }
        }

        class DiffCallback : DiffUtil.ItemCallback<ApiService>() {
            override fun areItemsTheSame(oldItem: ApiService, newItem: ApiService) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: ApiService, newItem: ApiService) =
                oldItem == newItem
        }
    }
}