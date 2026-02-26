package com.example.centraledellebolle.ui.stockmove

sealed interface StockMoveUiState {
    object Idle : StockMoveUiState
    object Loading : StockMoveUiState
    data class Success(val message: String, val movId: Int?) : StockMoveUiState
    data class Error(val message: String) : StockMoveUiState
}
