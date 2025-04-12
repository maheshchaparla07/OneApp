package com.example.multiscreenapp.data.mock


import com.example.multiscreenapp.api.ApiResponse
import com.example.multiscreenapp.api.ApiService
import com.example.multiscreenapp.model.ServiceItem
import retrofit2.Response

class MockApiService : ApiService {

    // Implementation of the interface method
    override suspend fun getData(): Response<List<ApiResponse>> {
        return Response.success(createMockApiResponses())
    }

    // Method to get mock data as ApiResponse (for API compliance)
    private fun createMockApiResponses(): List<ApiResponse> {
        return listOf(
            ApiResponse("1", "Weather", "Weather service description"),
            ApiResponse("2", "Payments", "Payment processing")
        )
    }

    // Method to get mock data as ServiceItem (for app use)
    fun getMockServices(): List<ServiceItem> {
        return listOf(
            ServiceItem("1", "Weather", "Weather service description"),
            ServiceItem("2", "Payments", "Payment processing")
        )
    }
}