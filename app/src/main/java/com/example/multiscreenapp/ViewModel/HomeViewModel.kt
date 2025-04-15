package com.example.multiscreenapp.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.multiscreenapp.model.ServiceItem



class HomeViewModel : ViewModel() {
    private val _services = MutableLiveData<List<ServiceItem>>()
    val services: LiveData<List<ServiceItem>> = _services

    fun loadServices(services: List<com.example.multiscreenapp.api.ApiService>) {
        // Replace with your actual data loading logic
        _services.value = listOf(
            ServiceItem("1", "Service 1", "Description 1"),
            ServiceItem("2", "Service 2", "Description 2")
        )
    }
}