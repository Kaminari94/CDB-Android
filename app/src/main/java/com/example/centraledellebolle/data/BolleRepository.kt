package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService

class BolleRepository(private val apiService: ApiService) {

    suspend fun getBolle(date: String): Result<BolletteResponse> {
        return try {
            val bolle = apiService.getBolle(date)
            Result.success(bolle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBollaDetail(id: Int): Result<BollaDetail> {
        return try {
            val bollaDetail = apiService.getBollaDetail(id)
            Result.success(bollaDetail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}