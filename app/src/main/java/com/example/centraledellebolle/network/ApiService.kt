package com.example.centraledellebolle.network

import com.example.centraledellebolle.data.BollaDetail
import com.example.centraledellebolle.data.BolletteResponse
import com.example.centraledellebolle.data.CustomersResponse
import com.example.centraledellebolle.data.QuickBollaRequest
import com.example.centraledellebolle.data.QuickBollaSuccessResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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
    suspend fun getBolle(@Query("date") date: String): BolletteResponse

    @GET("api/bolle/{id}/")
    suspend fun getBollaDetail(@Path("id") id: Int): BollaDetail

    @POST("api/bolle/quick/")
    suspend fun quickBolla(@Body request: QuickBollaRequest): Response<QuickBollaSuccessResponse>

    @GET("api/customers/")
    suspend fun getCustomers(): CustomersResponse
}