package com.example.multiscreenapp.model

import com.google.gson.annotations.SerializedName

// model/ProfileUpdate.kt
data class ProfileUpdate(
    @SerializedName("name") val name: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)