package com.example.centraledellebolle.network

import com.example.centraledellebolle.data.BolletteResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class HealthResponse(
    val ok: Boolean
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

interface ApiService {
    @GET("api/health/")
    suspend fun health(): HealthResponse

    @POST("api/token/")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/bolle/")
    suspend fun getBolle(): BolletteResponse
}