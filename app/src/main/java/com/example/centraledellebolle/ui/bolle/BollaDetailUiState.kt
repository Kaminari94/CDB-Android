package com.example.centraledellebolle.ui.bolle

import com.example.centraledellebolle.data.BollaDetail

sealed interface BollaDetailUiState {
    data object Loading : BollaDetailUiState
    data class Success(val bolla: BollaDetail) : BollaDetailUiState
    data class Error(val message: String) : BollaDetailUiState
}