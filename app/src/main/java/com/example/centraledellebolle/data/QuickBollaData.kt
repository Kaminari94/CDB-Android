package com.example.centraledellebolle.data

// --- Data classes per la richiesta e risposta di QuickBolla ---

/** Richiesta inviata al backend per la creazione rapida. */
data class QuickBollaRequest(
    val customer_id: Int,
    val raw_lines: String
)

/** Risposta in caso di successo (HTTP 2xx). */
data class QuickBollaSuccessResponse(
    val bolla_id: Int,
    val created: Boolean,
    val warnings: List<String>
)

/** Risposta in caso di errore di validazione (HTTP 400). */
data class QuickBollaErrorResponse(
    val created: Boolean,
    val errors: List<QuickLineError>
)

/** Dettaglio dell'errore su una specifica linea. */
data class QuickLineError(
    val line: Int,
    val message: String
)
