package com.example.centraledellebolle.data

/**
 * La risposta dell'endpoint api/customers, che contiene il conteggio dei clienti e la lista
 */
data class CustomerDto(
    val id: Int,
    val results: List<Customer>
)
