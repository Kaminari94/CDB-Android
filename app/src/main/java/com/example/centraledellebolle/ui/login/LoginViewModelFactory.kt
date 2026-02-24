package com.example.centraledellebolle.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.centraledellebolle.data.AuthRepository
import com.example.centraledellebolle.data.TokenStore

class LoginViewModelFactory(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authRepository, tokenStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}