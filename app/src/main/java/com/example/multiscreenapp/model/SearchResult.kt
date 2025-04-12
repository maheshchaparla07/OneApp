package com.example.multiscreenapp.model

import com.google.gson.annotations.SerializedName

// model/SearchResult.kt
data class SearchResult(
    @SerializedName("results") val results: List<User>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int
)