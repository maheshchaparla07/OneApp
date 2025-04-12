package com.example.multiscreenapp.repository

import android.annotation.SuppressLint
import com.example.multiscreenapp.api.ApiService
import com.example.multiscreenapp.data.mock.MockApiService
import com.example.multiscreenapp.model.LoginRequest
import com.example.multiscreenapp.model.ServiceItem
import com.example.multiscreenapp.model.User
import com.example.multiscreenapp.model.responses.AuthResponse
import com.example.multiscreenapp.network.RetrofitClient.apiService

class ApiRepository {
    private val mockService = MockApiService()

    suspend fun login(username: String, password: String): AuthResponse {
        val response = apiService.login<Any?>(LoginRequest(username, password))
        return response.body() ?: throw Exception("Login failed: ${response.code()}")
    }

    @SuppressLint("RestrictedApi")
    suspend fun getUsers(token: String): List<com.google.firebase.firestore.auth.User> {
        val response = apiService.getUsers("Bearer $token")
        return response.body() ?: emptyList()
    }
    // Option 2: Get through API interface
    suspend fun getMockServicesViaApi(): List<ServiceItem> {
        return mockService.getData().body()?.map { apiResponse ->
            ServiceItem(
                id = apiResponse.id,
                name = apiResponse.title,
                description = apiResponse.description
            )
        } ?: emptyList()
    }
}