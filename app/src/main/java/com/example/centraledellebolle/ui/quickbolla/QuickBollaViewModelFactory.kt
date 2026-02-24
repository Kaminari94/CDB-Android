package com.example.centraledellebolle.ui.quickbolla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.centraledellebolle.data.CustomersRepository
import com.example.centraledellebolle.data.QuickBollaRepository

class QuickBollaViewModelFactory(
    private val quickBollaRepository: QuickBollaRepository,
    private val customersRepository: CustomersRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickBollaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuickBollaViewModel(quickBollaRepository, customersRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}