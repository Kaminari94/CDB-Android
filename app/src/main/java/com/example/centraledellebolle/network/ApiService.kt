package com.example.centraledellebolle.network

import com.example.centraledellebolle.data.BollaDetail
import com.example.centraledellebolle.data.BolletteResponse
import com.example.centraledellebolle.data.CustomersResponse
import com.example.centraledellebolle.data.QuickBollaRequest
import com.example.centraledellebolle.data.QuickBollaSuccessResponse
import com.example.centraledellebolle.data.QuickLineError
import com.example.centraledellebolle.data.Receipt
import com.example.centraledellebolle.data.StockMoveRequest
import com.example.centraledellebolle.data.StockMoveResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

data class UpdateBollaRigheRequest(val raw_lines: String)
data class UpdateBollaRigheResponse(val type: String, val bolla_id: Int, val rows_count: Int)
data class UpdateBollaRigheErrorResponse(val errors: List<QuickLineError>?, val detail: String?)

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

    @GET("api/bolle/{id}/receipt/")
    suspend fun getReceipt(@Path("id") id: Int): Receipt

    @POST("api/movim/")
    suspend fun performStockMove(@Body request: StockMoveRequest): Response<StockMoveResponse>

    @DELETE("api/bolle/{id}/elimina/")
    suspend fun deleteBolla(@Path("id") id: Int): Response<Unit>

    @POST("api/bolle/{id}/modifica/")
    suspend fun updateBollaRighe(
        @Path("id") id: Int,
        @Body body: UpdateBollaRigheRequest
    ): UpdateBollaRigheResponse
}