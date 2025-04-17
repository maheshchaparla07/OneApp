package com.example.multiscreenapp

import com.google.firebase.auth.FirebaseAuth
import java.util.Date


data class WebApp(
    val id: String = "",  // Important for document reference
    val name: String = "",
    val url: String = "",
    val userId: String = "",
    val createdAt: Date? = null,
    val iconUrl: String? = null,

)