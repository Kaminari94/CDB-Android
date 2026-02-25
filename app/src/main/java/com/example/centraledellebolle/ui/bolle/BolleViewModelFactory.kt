package com.example.centraledellebolle.ui.bolle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.centraledellebolle.data.BolleRepository
import com.example.centraledellebolle.data.UserPreferencesRepository

class BolleViewModelFactory(
    private val bolleRepository: BolleRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BolleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BolleViewModel(bolleRepository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}