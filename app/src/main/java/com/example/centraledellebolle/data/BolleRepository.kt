package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService

class BolleRepository(private val apiService: ApiService) {

    suspend fun getBolle(): Result<BolletteResponse> {
        return try {
            val bolle = apiService.getBolle()
            Result.success(bolle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}