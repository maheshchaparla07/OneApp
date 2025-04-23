package uk.ac.tees.mad.s3445191.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface TimeApiService {
    @GET
    suspend fun getTimeFromEndpoint(@Url url: String): Response<Any>
}

private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    })
    .build()

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val avatar: String // URL to image
)