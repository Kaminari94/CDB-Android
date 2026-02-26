package com.example.centraledellebolle.ui.bolle

import com.example.centraledellebolle.data.Bolla

sealed interface BolleUiState {
    data object Idle : BolleUiState
    data object Loading : BolleUiState
    data class Success(val bolle: List<Bolla>) : BolleUiState
    data object Empty : BolleUiState
    data class Error(val message: String) : BolleUiState
}

sealed interface PrintingUiState {
    data object Idle : PrintingUiState
    data object Printing : PrintingUiState
    data object Success : PrintingUiState
    data class Error(val message: String) : PrintingUiState
}

sealed interface DeleteUiState {
    data object Idle : DeleteUiState
    data class Request(val bollaId: Int) : DeleteUiState
    data object Deleting : DeleteUiState
    data object Success : DeleteUiState
    data class Error(val message: String) : DeleteUiState
}