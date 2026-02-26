package com.example.centraledellebolle.ui.stockmove

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.centraledellebolle.data.StockMoveRepository

class StockMoveViewModelFactory(private val repository: StockMoveRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockMoveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockMoveViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
