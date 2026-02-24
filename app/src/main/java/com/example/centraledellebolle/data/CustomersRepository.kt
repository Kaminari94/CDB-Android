package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService

class CustomersRepository(private val apiService: ApiService) {

    /**
     * Recupera la lista di tutti i clienti dal backend.
     */
    suspend fun getCustomers(): List<Customer> {
        return apiService.getCustomers().results
    }
}
