package com.example.centraledellebolle.ui.bolle

import com.google.gson.JsonElement

sealed interface BolleUiState {
    data object Idle : BolleUiState
    data object Loading : BolleUiState
    data class Success(val bolle: JsonElement) : BolleUiState
    data class Error(val message: String) : BolleUiState
}