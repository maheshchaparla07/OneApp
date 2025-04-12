package com.example.multiscreenapp.model.responses

import com.google.gson.annotations.SerializedName

// model/responses/ProfileResponse.kt
data class ProfileResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("updated_at") val updatedAt: String
)