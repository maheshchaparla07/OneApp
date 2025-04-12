package com.example.multiscreenapp.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// network/AuthenticatedHttpClient.kt
class AuthenticatedHttpClient private constructor(
    private val okHttpClient: OkHttpClient,
    private val retrofit: Retrofit
) {
    fun <T> create(service: Class<T>): T = retrofit.create(service)

    class Builder {
        private var baseUrl: String = ""
        private var authToken: String? = null
        private var timeout: Long = 30

        fun baseUrl(url: String) = apply { this.baseUrl = url }
        fun authToken(token: String?) = apply { this.authToken = token }
        fun timeout(seconds: Long) = apply { this.timeout = seconds }

        fun build(): AuthenticatedHttpClient {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder().apply {
                        authToken?.let { header("Authorization", "Bearer $it") }
                    }.build()
                    chain.proceed(request)
                }
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return AuthenticatedHttpClient(okHttpClient, retrofit)
        }
    }
}