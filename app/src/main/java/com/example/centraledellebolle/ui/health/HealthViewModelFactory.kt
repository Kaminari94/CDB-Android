package com.example.centraledellebolle.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.centraledellebolle.data.HealthRepository

class HealthViewModelFactory(
    private val repo: HealthRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}