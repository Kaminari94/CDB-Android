package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.ApiService
import com.example.centraledellebolle.network.LoginRequest
import com.example.centraledellebolle.network.LoginResponse

class AuthRepository(private val apiService: ApiService) {

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(username, password)
            val response = apiService.login(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}