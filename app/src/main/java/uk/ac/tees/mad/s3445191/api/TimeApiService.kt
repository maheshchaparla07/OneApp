package uk.ac.tees.mad.s3445191.api

import retrofit2.Response
import retrofit2.http.GET

interface TimeApiService {
    @GET("Europe/London")
    suspend fun getUkTime(): Response<TimeResponse>
}