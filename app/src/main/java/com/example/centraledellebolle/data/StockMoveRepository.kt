package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService
import com.example.centraledellebolle.ui.stockmove.MoveType
import com.example.centraledellebolle.data.StockMoveErrorResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

class StockMoveRepository(private val apiService: ApiService) {

    suspend fun performStockMove(moveType: MoveType, numero: String, rawArticles: String): StockMoveResponse {
        val request = StockMoveRequest(
            type = moveType.name.lowercase().replaceFirstChar { it.uppercase() },
            numero = if (moveType == MoveType.CARICO) numero.ifBlank { null } else null,
            rawLines = rawArticles
        )

        val response = apiService.performStockMove(request)

        if (response.isSuccessful) {
            return response.body()!!
        } else {
            val errorBody = response.errorBody()?.string() ?: "Errore sconosciuto"
            val gson = Gson()
            var errorMessage = errorBody // Messaggio di default

            try {
                // Tentativo 1: Parsare la struttura {"errors": [...]}
                val errorResponse = gson.fromJson(errorBody, StockMoveErrorResponse::class.java)
                if (errorResponse?.errors != null) {
                    errorMessage = errorResponse.errors.joinToString("\n") { 
                        (if (it.line != null) "Riga ${it.line}: " else "") + it.message 
                    }
                }
            } catch (e: JsonSyntaxException) {
                // Tentativo 2: Se il primo fallisce, parsare la struttura {"detail": "..."}
                try {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val errorMap: Map<String, String> = gson.fromJson(errorBody, type)
                    if (errorMap.containsKey("detail")) {
                        errorMessage = errorMap["detail"]!!
                    }
                } catch (e2: JsonSyntaxException) {
                    // Se entrambi i tentativi falliscono, usiamo il body dell'errore come stringa grezza (già impostato)
                }
            }
            throw Exception(errorMessage)
        }
    }
}
