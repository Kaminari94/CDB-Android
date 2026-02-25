package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService
import com.example.centraledellebolle.printing.BluetoothPrinterService

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
}