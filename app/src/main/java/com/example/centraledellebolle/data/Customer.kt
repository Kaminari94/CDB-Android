package com.example.centraledellebolle.data
/**
 * Rappresenta i clienti singoli nella lista ricevuta da api/customers CustomerDto
 */
data class Customer(
    val id: Int,
    val nome: String
)