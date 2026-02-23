package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService
import com.google.gson.JsonElement

class BolleRepository(private val apiService: ApiService) {

    suspend fun getBolle(): Result<JsonElement> {
        return try {
            val bolle = apiService.getBolle()
            Result.success(bolle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}