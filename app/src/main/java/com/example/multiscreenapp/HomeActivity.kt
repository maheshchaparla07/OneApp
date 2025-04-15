package com.example.multiscreenapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
    }

    private fun setupWebAppRecyclerView() {
        webAppAdapter = WebAppAdapter(webApps) { app ->
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra("url", app.url)
                putExtra("title", app.name)
            }
            startActivity(intent)
        }

        binding.appsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 3)
            adapter = webAppAdapter
        }

        loadSavedApps()
    }

    private fun showUrlInputDialog(prefilledUrl: String = "") {
        // Use the companion object to create new instance
        UrlInputDialog.newInstance(prefilledUrl).show(supportFragmentManager, "UrlInputDialog")
    }

    override fun onUrlAdded(url: String, name: String) {
        // Your existing implementation
        val newApp = WebApp(name, url)
        webApps.add(newApp)
        webAppAdapter.notifyItemInserted(webApps.size - 1)
        saveApps()
    }

    override fun onScanRequested() {
        // Your existing QR scan implementation
        val options = ScanOptions().apply {
            setPrompt("Scan a QR code")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(CaptureActivityPortrait::class.java)
        }
        qrScannerLauncher.launch(options)
    }

    private fun saveApps() {
        getSharedPreferences("WebAppsPref", Context.MODE_PRIVATE).edit().apply {
            putString("webApps", Gson().toJson(webApps))
            apply()
        }
    }

    private fun loadSavedApps() {
        val json = getSharedPreferences("WebAppsPref", Context.MODE_PRIVATE)
            .getString("webApps", null) ?: return

        val type = object : TypeToken<List<WebApp>>() {}.type
        val savedApps = Gson().fromJson<List<WebApp>>(json, type)
        webApps.clear()
        webApps.addAll(savedApps)
    }

    private fun setupUI() {
        val user = auth.currentUser
        binding.welcomeText.text = "Welcome, ${user?.displayName ?: user?.email ?: "User"}"
    }

    private fun setupRecyclerView() {
        apiServiceAdapter = ApiServiceAdapter()
        binding.apiServicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = apiServiceAdapter
        }

        // Sample data
        apiServiceAdapter.submitList(listOf(
            ApiService("Weather API", "Get current weather data"),
            ApiService("News API", "Latest news headlines"),
            ApiService("Currency API", "Real-time exchange rates")
        ))
    }

    data class WebApp(val name: String, val url: String, val iconUrl: String? = null)
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