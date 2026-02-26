package com.example.centraledellebolle.data

import com.google.gson.annotations.SerializedName

// Request body to be sent to the backend
data class StockMoveRequest(
    val type: String,
    val numero: String?,
    @SerializedName("raw_lines")
    val rawLines: String
)

// Models the successful JSON response from the backend
data class StockMoveResponse(
    val type: String,
    val mov_id: Int
)
