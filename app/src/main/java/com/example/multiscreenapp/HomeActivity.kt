package com.example.multiscreenapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.multiscreenapp.ViewModel.HomeViewModel
import com.example.multiscreenapp.adaptar.ApiServiceAdapter
import com.example.multiscreenapp.api.RetrofitClient
import com.example.multiscreenapp.databinding.ActivityHomeBinding
import com.google.firebase.appdistribution.gradle.ApiService
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var apiServiceAdapter: ApiServiceAdapter
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: ApiServiceAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_home)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        fetchApiData()
    }

    private fun setupRecyclerView() {
        // Initialize the adapter once (remove duplicate initialization)
        adapter = ApiServiceAdapter()

        // Use view binding to access the RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter  // Use the single adapter instance

            // Optional: Add item decoration if needed
            addItemDecoration(
                DividerItemDecoration(
                    this@HomeActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }
    }

    private fun setupObservers() {
        viewModel.services.observe(this) { services -> apiServiceAdapter.submitList(services)
        }
    }

    private fun fetchApiData() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getData()
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        val services = data.map {
                            ApiService(it.title, it.description)
                        }
                        viewModel.loadServices(services)
                    }
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "API Error: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HomeActivity,
                    "Network Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}