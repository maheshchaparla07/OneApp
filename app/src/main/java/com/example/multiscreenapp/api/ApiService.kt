package com.example.multiscreenapp.api

import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("users") // Replace with your actual endpoint
    suspend fun getUsers(): Response<List<User>>
}

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val avatar: String // URL to image
)