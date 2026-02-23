package com.example.centraledellebolle.ui.bolle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.BolleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BolleViewModel(private val bolleRepository: BolleRepository) : ViewModel() {

    private val _bolleState = MutableStateFlow<BolleUiState>(BolleUiState.Idle)
    val bolleState: StateFlow<BolleUiState> = _bolleState

    fun loadBolle() {
        viewModelScope.launch {
            _bolleState.value = BolleUiState.Loading
            val result = bolleRepository.getBolle()
            _bolleState.value = result.fold(
                onSuccess = { BolleUiState.Success(it) },
                onFailure = { BolleUiState.Error(it.message ?: "Errore sconosciuto") }
            )
        }
    }
}