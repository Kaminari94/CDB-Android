package com.example.centraledellebolle.data

/**
 * Rappresenta la risposta completa dell'API per la lista dei clienti,
 * che include il conteggio e la lista di risultati.
 */
data class CustomersResponse(
    val count: Int,
    val results: List<Customer>
)
