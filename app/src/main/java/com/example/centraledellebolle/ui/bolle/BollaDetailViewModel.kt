package com.example.centraledellebolle.ui.bolle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.BolleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BollaDetailViewModel(private val bolleRepository: BolleRepository, private val bollaId: Int) : ViewModel() {

    private val _bollaDetailState = MutableStateFlow<BollaDetailUiState>(BollaDetailUiState.Loading)
    val bollaDetailState: StateFlow<BollaDetailUiState> = _bollaDetailState

    init {
        loadBollaDetail()
    }

    private fun loadBollaDetail() {
        viewModelScope.launch {
            _bollaDetailState.value = BollaDetailUiState.Loading
            val result = bolleRepository.getBollaDetail(bollaId)
            _bollaDetailState.value = result.fold(
                onSuccess = { BollaDetailUiState.Success(it) },
                onFailure = { BollaDetailUiState.Error(it.message ?: "Errore sconosciuto") }
            )
        }
    }
}

class BollaDetailViewModelFactory(private val bolleRepository: BolleRepository, private val bollaId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BollaDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BollaDetailViewModel(bolleRepository, bollaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}