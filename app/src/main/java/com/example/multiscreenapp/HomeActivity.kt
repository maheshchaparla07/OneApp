package com.example.multiscreenapp

import com.example.multiscreenapp.WebApp
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import com.example.multiscreenapp.databinding.ActivityHomeBinding
import com.example.multiscreenapp.databinding.ItemApiServiceBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import java.util.Date

class HomeActivity : AppCompatActivity(), UrlInputDialog.OnUrlAddedListener, UrlInputDialog.QRScanListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var apiServiceAdapter: ApiServiceAdapter
    private lateinit var webAppAdapter: WebAppAdapter
    private val webApps = mutableListOf<WebApp>()


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
        // Use the companion object to create new instance
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
                // Create proper WebApp object with the generated ID
                val savedApp = WebApp(
                    id = documentReference.id,
                    name = name,
                    url = url,
                    userId = userId,
                    createdAt = Date() // Or use server timestamp if needed
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
        // QR scan implementation
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
        val userId = user?.email ?: return // Use UID or email as key

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
            .whereEqualTo("userId", userId) // Critical: Filter by user
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

        // Try Firestore first
        Firebase.firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName")
                    ?: sharedPrefs.getString("firstName", null)

                binding.welcomeText.text = if (!firstName.isNullOrEmpty()) {
                    "Welcome, $firstName"
                } else {
                    "Welcome" // Fallback if no first name exists anywhere
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Firestore fetch failed, using local data", e)
                val localFirstName = sharedPrefs.getString("firstName", null)
                binding.welcomeText.text = if (!localFirstName.isNullOrEmpty()) {
                    "Welcome, $localFirstName"
                } else {
                    "Welcome, ${user.displayName ?: user.email ?: "User"}" // Ultimate fallback
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
    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> logoutUser()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun logoutUser() {
        auth.signOut()

        // Clear user-specific SharedPreferences
        val userId = auth.currentUser?.email
        if (userId != null) {
            getSharedPreferences("WebAppsPref_$userId", Context.MODE_PRIVATE).edit().clear().apply()
        }

        // Navigate to LoginActivity
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

}



