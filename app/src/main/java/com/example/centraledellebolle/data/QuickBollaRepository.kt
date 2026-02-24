package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService
import com.google.gson.Gson
import retrofit2.HttpException

/** Eccezione personalizzata per contenere gli errori di validazione specifici. */
class QuickBollaValidationException(val errorResponse: QuickBollaErrorResponse) : Exception("Validation failed")

class QuickBollaRepository(private val apiService: ApiService) {

    /**
     * Tenta di creare una bolla rapida.
     * Gestisce sia il caso di successo che quello di errore (validazione e rete).
     */
    suspend fun createQuickBolla(request: QuickBollaRequest): Result<QuickBollaSuccessResponse> {
        return runCatching {
            val response = apiService.quickBolla(request)

            if (response.isSuccessful) {
                response.body()!!
            } else {
                // Se il server risponde con un errore (es. 400), decodifico l'errorBody
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    val errorResponse = Gson().fromJson(errorBody, QuickBollaErrorResponse::class.java)
                    throw QuickBollaValidationException(errorResponse)
                } else {
                    throw HttpException(response) // Errore generico HTTP
                }
            }
        }
    }
}