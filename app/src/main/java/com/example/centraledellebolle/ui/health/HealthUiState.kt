package com.example.centraledellebolle.ui.health

sealed interface HealthUiState {
    data object Idle : HealthUiState
    data object Loading : HealthUiState
    data object Ok : HealthUiState
    data class Error(val message: String) : HealthUiState
}