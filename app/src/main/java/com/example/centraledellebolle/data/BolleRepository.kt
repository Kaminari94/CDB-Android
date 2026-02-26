package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService
import com.example.centraledellebolle.printing.BluetoothPrinterService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BolleRepository(private val apiService: ApiService, private val printerService: BluetoothPrinterService) {

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

    suspend fun printBolla(id: Int, macAddress: String): Result<Unit> {
        return try {
            val receipt = apiService.getReceipt(id)
            printerService.printText(macAddress, receipt.text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBolla(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteBolla(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        val errorMap: Map<String, String> = Gson().fromJson(errorBody, type)
                        errorMap["detail"] ?: "Errore eliminazione (${response.code()})"
                    } catch (e: Exception) {
                        "Errore eliminazione (${response.code()})"
                    }
                } else {
                    "Errore eliminazione (${response.code()})"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}