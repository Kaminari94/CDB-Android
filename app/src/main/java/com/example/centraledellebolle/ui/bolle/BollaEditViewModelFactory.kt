package com.example.centraledellebolle.ui.bolle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.centraledellebolle.data.BolleRepository

class BollaEditViewModelFactory(private val repository: BolleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BollaEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BollaEditViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}