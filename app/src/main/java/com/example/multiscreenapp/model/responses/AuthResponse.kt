package com.example.multiscreenapp.model.responses

import com.example.multiscreenapp.model.User
import com.google.gson.annotations.SerializedName

// model/responses/AuthResponse.kt
data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "Bearer",
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("user") val user: User? = null
)