package com.example.multiscreenapp.model

import com.google.gson.annotations.SerializedName

// model/User.kt
data class User(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)


