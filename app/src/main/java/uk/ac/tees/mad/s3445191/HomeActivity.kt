package uk.ac.tees.mad.s3445191

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import uk.ac.tees.mad.s3445191.api.RetryInterceptor
import uk.ac.tees.mad.s3445191.databinding.ActivityHomeBinding
import uk.ac.tees.mad.s3445191.databinding.ItemApiServiceBinding
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

class HomeActivity : AppCompatActivity(), UrlInputDialog.OnUrlAddedListener, UrlInputDialog.QRScanListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var apiServiceAdapter: ApiServiceAdapter
    private lateinit var webAppAdapter: WebAppAdapter
    private val webApps = mutableListOf<WebApp>()

    // Time service configuration
    private val timeEndpoints = listOf(
        "https://worldtimeapi.org/api/timezone/Europe/London",
        "https://timeapi.io/api/Time/current/zone?timeZone=Europe/London"
    )
    private var currentEndpointIndex = 0
    private lateinit var timeService: WorldTimeService
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.UK)
    private var timeUpdateTimer: Timer? = null
    private var lastSuccessfulTime: String? = null
    private var lastUpdateTimestamp: Long = 0

    // QR Scanner launcher
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { scannedUrl ->
            if (scannedUrl.startsWith("http://") || scannedUrl.startsWith("https://")) {
                showUrlInputDialog(scannedUrl)
            } else {
                Toast.makeText(this, "Invalid URL format", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showUrlInputDialog(prefilledUrl: String = "") {
        // Use the companion object to create new instance
        UrlInputDialog.newInstance(prefilledUrl).show(supportFragmentManager, "UrlInputDialog")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupNetworkComponents()
        setupUI()
        setupRecyclerView()
        setupWebAppRecyclerView()
        startTimeUpdates()

        binding.apply {
            addApiButton.setOnClickListener { showUrlInputDialog() }
            fabAddApp.setOnClickListener { showUrlInputDialog() }
            logoutButton.setOnClickListener { showLogoutConfirmationDialog() }
        }
    }

    private fun setupNetworkComponents() {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(RetryInterceptor(
                maxRetries = 3,
                initialDelayMs = 1000L,  // 1 second initial delay
                maxDelayMs = 10000L      // 10 seconds maximum delay
            ))
            .protocols(listOf(Protocol.HTTP_1_1))
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return Dns.SYSTEM.lookup(hostname).sortedBy { address ->
                        if (address is Inet6Address) 1 else 0
                    }
                }
            })
            .connectionSpecs(listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS
            ))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://placeholder.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        timeService = retrofit.create(WorldTimeService::class.java)
    }

    private fun startTimeUpdates() {
        timeUpdateTimer?.cancel()
        timeUpdateTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (isNetworkAvailable()) fetchCurrentTime() else showCachedTimeIfAvailable()
                }
            }, 0, 30000) // 30 seconds interval
        }
    }

    private fun fetchCurrentTime() {
        lifecycleScope.launch {
            try {
                val response = timeService.getTime(timeEndpoints[currentEndpointIndex])
                if (response.isSuccessful) {
                    response.body()?.let { updateTimeDisplay(parseTimeFromResponse(it)) }
                } else {
                    // Replace this line
                    showTimeError("HTTP ${response.code()}")  // Changed from handleTimeError
                }
            } catch (e: Exception) {
                handleSpecificException(e)
            }
        }
    }

    private fun parseTimeFromResponse(response: WorldTimeResponse): String {
        return response.datetime ?: response.currentDateTime ?: ""
    }

    private fun updateTimeDisplay(isoTime: String) {
        runOnUiThread {
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.UK)
                val date = isoFormat.parse(isoTime)
                lastSuccessfulTime = timeFormatter.format(date).also {
                    binding.currentTimeText.text = "UK Time: $it"
                }
                lastUpdateTimestamp = System.currentTimeMillis()
            } catch (e: Exception) {
                showCachedTimeIfAvailable()
            }
        }
    }

    private fun handleSpecificException(e: Exception) {
        when (e) {
            is SocketTimeoutException -> {
                Log.w(TAG, "Timeout", e)
                rotateEndpoint()
                showTimeoutError()
            }
            is SSLHandshakeException -> {
                Log.w(TAG, "SSL Error", e)
                showSSLError()
            }
            is IOException -> {
                Log.w(TAG, "Network Error", e)
                showNetworkError()
            }
            else -> {
                Log.w(TAG, "Unexpected Error", e)
                showTimeError("Service unavailable")
            }
        }
    }

    private fun rotateEndpoint() {
        currentEndpointIndex = (currentEndpointIndex + 1) % timeEndpoints.size
        Log.d(TAG, "Switched to endpoint: ${timeEndpoints[currentEndpointIndex]}")
    }

    private fun showTimeoutError() = updateTimeStatus("Connection timeout", "Server response timed out")
    private fun showSSLError() = updateTimeStatus("Security error", "SSL handshake failed")
    private fun showNetworkError() = updateTimeStatus("No connection", "Network unavailable")
    private fun showTimeError(msg: String) = updateTimeStatus("Time error", msg)

    private fun updateTimeStatus(text: String, toast: String) {
        runOnUiThread {
            binding.currentTimeText.text = text
            Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCachedTimeIfAvailable() {
        lastSuccessfulTime?.takeIf {
            System.currentTimeMillis() - lastUpdateTimestamp < 3600000
        }?.let {
            binding.currentTimeText.text = "UK Time: $it"
        } ?: showTimeError("No cached data")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.activeNetwork?.let { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.let {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        } ?: false
    }

    // Web App Management
    private fun setupWebAppRecyclerView() {
        webAppAdapter = WebAppAdapter(
            apps = webApps,
            onClick = { app -> launchWebView(app) },
            onDelete = { app -> confirmDeleteApp(app) }
        )
        binding.appsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 3)
            adapter = webAppAdapter
        }
        loadAppsFromFirestore()
    }

    private fun launchWebView(app: WebApp) {
        if (isNetworkAvailable()) {
            startActivity(Intent(this, WebViewActivity::class.java).apply {
                putExtra("url", app.url)
                putExtra("title", app.name)
            })
        } else {
            showNoInternetDialog(app)
        }
    }

    // New dialog helper function
    private fun showNoInternetDialog(app: WebApp) {
        MaterialAlertDialogBuilder(this)
            .setTitle("No Internet Connection")
            .setMessage("Please enable internet to access")
            .setPositiveButton("Retry") { _, _ -> launchWebView(app) }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun confirmDeleteApp(app: WebApp) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove ${app.name}")
            .setMessage("Remove from home screen?")
            .setPositiveButton("Remove") { _, _ -> deleteApp(app) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteApp(app: WebApp) {
        val documentId = app.id
        if (documentId.isEmpty()) {
            Log.e(TAG, "Cannot delete app without document ID")
            return
        }

        Firebase.firestore.collection("webApps").document(documentId)
            .delete()
            .addOnSuccessListener {
                val position = webApps.indexOf(app)
                if (position != -1) {
                    webApps.removeAt(position)
                    webAppAdapter.notifyItemRemoved(position)
                    saveApps() // Optional: Update local cache if needed
                    Toast.makeText(this, "${app.name} removed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting app", e)
                Toast.makeText(this, "Failed to delete app", Toast.LENGTH_SHORT).show()
            }
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

    private fun saveApps() {
        val user = auth.currentUser
        val userId = user?.email ?: return
        getSharedPreferences("WebAppsPref_$userId", Context.MODE_PRIVATE).edit().apply {
            putString("webApps", Gson().toJson(webApps))
            apply()
        }
    }

    private fun loadAppsFromFirestore() {
        val userId = auth.currentUser?.uid ?: return
        Firebase.firestore.collection("webApps")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                webApps.clear()
                documents.map { doc ->
                    doc.toObject(WebApp::class.java)
                }.let(webApps::addAll)
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
                val firstName = document.getString("firstName") ?: sharedPrefs.getString("firstName", null)
                binding.welcomeText.text = if (!firstName.isNullOrEmpty()) "Welcome, $firstName" else "Welcome"
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Firestore fetch failed", e)
                val localFirstName = sharedPrefs.getString("firstName", null)
                binding.welcomeText.text = when {
                    !localFirstName.isNullOrEmpty() -> "Welcome, $localFirstName"
                    user.displayName != null -> "Welcome, ${user.displayName}"
                    user.email != null -> "Welcome, ${user.email}"
                    else -> "Welcome, User"
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

    data class ApiService(val name: String, val description: String)

    class ApiServiceAdapter : ListAdapter<ApiService, ApiServiceAdapter.ApiServiceViewHolder>(DiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiServiceViewHolder {
            val binding = ItemApiServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ApiServiceViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ApiServiceViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ApiServiceViewHolder(private val binding: ItemApiServiceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(service: ApiService) {
                binding.serviceName.text = service.name
                binding.serviceDescription.text = service.description
            }
        }

        class DiffCallback : DiffUtil.ItemCallback<ApiService>() {
            override fun areItemsTheSame(oldItem: ApiService, newItem: ApiService) = oldItem.name == newItem.name
            override fun areContentsTheSame(oldItem: ApiService, newItem: ApiService) = oldItem == newItem
        }
    }

    override fun onScanRequested() {
        ScanOptions().apply {
            setPrompt("Scan a QR code")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(CaptureActivityPortrait::class.java)
        }.let(qrScannerLauncher::launch)
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
        auth.currentUser?.email?.let { userId ->
            getSharedPreferences("WebAppsPref_$userId", Context.MODE_PRIVATE).edit().clear().apply()
        }
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }



    override fun onDestroy() {
        timeUpdateTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "HomeActivity"
    }


}



interface WorldTimeService {
    @GET
    suspend fun getTime(@Url url: String): Response<WorldTimeResponse> // Add generic type parameter
}

data class WorldTimeResponse(
    val datetime: String? = null,
    val currentDateTime: String? = null,
    val timezone: String? = null,
    val utc_offset: String? = null
)