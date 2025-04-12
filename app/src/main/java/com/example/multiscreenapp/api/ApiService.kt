package com.example.multiscreenapp.api

// api/ApiService.kt
import android.annotation.SuppressLint
import android.app.appsearch.SearchResult
import androidx.work.Data
import com.example.multiscreenapp.model.LoginRequest
import com.example.multiscreenapp.model.ProfileUpdate
import com.example.multiscreenapp.model.responses.ProfileResponse
import com.google.firebase.firestore.auth.User
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // GET Request with Authentication
    @SuppressLint("RestrictedApi")
    @GET("users")
    suspend fun getUsers(
        @Header("Authorization") token: String? = null
    ): Response<List<User>>

    // POST Request with Request Body
    @POST("login")
    suspend fun <AuthResponse : Any?> login(
        @Body credentials: LoginRequest
    ): Response<AuthResponse>

    // GET Request with Path Parameter
    @SuppressLint("RestrictedApi")
    @GET("users/{userId}")
    suspend fun getUserById(
        @Path("userId") id: String
    ): Response<User>

    @GET("endpoint")
    suspend fun getData(): Response<List<Data>>

    // GET Request with Query Parameters
    @GET("search")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): Response<SearchResult>

    // PUT Request with Headers
    @PUT("profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body profile: ProfileUpdate
    ): Response<ProfileResponse>

    // DELETE Request
    @DELETE("users/{id}")
    suspend fun deleteUser(
        @Path("id") userId: String
    ): Response<Unit>
}