package com.example.centraledellebolle.network

import retrofit2.Response
import retrofit2.http.GET

data class HealthResponse(
    val ok: Boolean
)

interface ApiService {
    @GET("api/health/")
    suspend fun health(): HealthResponse
}