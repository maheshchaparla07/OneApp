package com.example.multiscreenapp.model

import com.google.gson.annotations.SerializedName

// model/LoginRequest.kt
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)