package com.example.centraledellebolle.ui.bolle

import com.example.centraledellebolle.data.Bolla

sealed interface BolleUiState {
    data object Idle : BolleUiState
    data object Loading : BolleUiState
    data class Success(val bolle: List<Bolla>) : BolleUiState
    data object Empty : BolleUiState
    data class Error(val message: String) : BolleUiState
}