package com.example.centraledellebolle.ui.quickbolla

import com.example.centraledellebolle.data.QuickBollaErrorResponse
import com.example.centraledellebolle.data.QuickBollaSuccessResponse

/**
 * Rappresenta i possibili stati della schermata QuickBolla.
 */
sealed interface QuickBollaUiState {
    /** Stato iniziale, nessuna operazione in corso. */
    data class Idle(val previousInput: String = "") : QuickBollaUiState

    /** Creazione della bolla in corso. */
    data object Loading : QuickBollaUiState

    /** Creazione avvenuta con successo. */
    data class Success(val response: QuickBollaSuccessResponse) : QuickBollaUiState

    /** Errore durante la creazione. */
    data class Error(
        val rawInput: String, // Conserva l'input originale che ha causato l'errore
        val validationError: QuickBollaErrorResponse? = null, // Errore strutturato dal backend (400)
        val genericMessage: String? = null // Messaggio per altri errori (rete, 500, etc.)
    ) : QuickBollaUiState
}
