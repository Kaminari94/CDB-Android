package com.example.centraledellebolle.data

// Defines the structure of a single validation error from the backend
data class StockMoveError(
    val line: Int?,
    val message: String
)

// Defines the top-level structure of a validation error response
data class StockMoveErrorResponse(
    val errors: List<StockMoveError>
)
