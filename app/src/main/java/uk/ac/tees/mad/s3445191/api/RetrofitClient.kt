package uk.ac.tees.mad.s3445191.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.ac.tees.mad.s3445191.HomeActivity

object RetrofitClient {
    private const val BASE_URL = "https://api.example.com/" // Replace with your API base URL

    val instance: HomeActivity.ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HomeActivity.ApiService::class.java)
    }
}