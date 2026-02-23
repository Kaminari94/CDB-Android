package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService

class HealthRepository(
    private val api: ApiService
) {
    suspend fun check(): Boolean {
        return api.health().ok
    }
}