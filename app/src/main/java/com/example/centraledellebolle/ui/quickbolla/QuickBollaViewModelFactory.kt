package com.example.centraledellebolle.ui.quickbolla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.centraledellebolle.data.OfflineQuickBollaRepository
import com.example.centraledellebolle.data.QuickBollaRepository
import com.example.centraledellebolle.data.UserPreferencesRepository

class QuickBollaViewModelFactory(
    private val offlineRepository: OfflineQuickBollaRepository,
    private val legacyRepository: QuickBollaRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickBollaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuickBollaViewModel(offlineRepository, legacyRepository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
