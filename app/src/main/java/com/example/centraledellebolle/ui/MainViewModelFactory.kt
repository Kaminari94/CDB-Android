package com.example.centraledellebolle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.centraledellebolle.data.TokenStore

class MainViewModelFactory(private val tokenStore: TokenStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(tokenStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}