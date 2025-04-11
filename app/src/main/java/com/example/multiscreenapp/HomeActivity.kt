package com.example.multiscreenapp
//
//import android.os.Bundle
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.multiscreenapp.adaptar.UserAdapter
//import com.example.multiscreenapp.api.RetrofitClient
//import com.example.multiscreenapp.databinding.ActivityHomeBinding
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//
//class HomeActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityHomeBinding
//    private lateinit var userAdapter: UserAdapter
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityHomeBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setupRecyclerView()
//        loadData()
//    }
//
//    private fun setupRecyclerView() {
//        userAdapter = UserAdapter()
//        binding.recyclerView.apply {
//            layoutManager = LinearLayoutManager(this@HomeActivity)
//            adapter = userAdapter
//        }
//    }
//
//    private fun loadData() {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val response = RetrofitClient.instance.getUsers()
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        response.body()?.let { users -> userAdapter.submitList(users)
//                        }
//                    } else {
//                        showError("API Error: ${response.code()}")
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    showError("Network Error: ${e.message}")
//                }
//            }
//        }
//    }
//
//    private fun showError(message: String) {
//        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
//    }
//}

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.multiscreenapp.databinding.ActivityHomeBinding
import com.example.multiscreenapp.databinding.ItemApiServiceBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var apiServiceAdapter: ApiServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupUI()
        setupRecyclerView()
    }

    private fun setupUI() {
        val user = auth.currentUser
        binding.welcomeText.text = "Welcome, ${user?.displayName ?: user?.email ?: "User"}"

        binding.addApiButton.setOnClickListener {
            // Implement adding new API service
            Toast.makeText(this, "Add API Service clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        apiServiceAdapter = ApiServiceAdapter()
        binding.apiServicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = apiServiceAdapter
        }

        // Load sample data - in real app this would come from API
        val sampleServices = listOf(
            ApiService("Weather API", "Get current weather data"),
            ApiService("News API", "Latest news headlines"),
            ApiService("Currency API", "Real-time exchange rates")
        )
        apiServiceAdapter.submitList(sampleServices)
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