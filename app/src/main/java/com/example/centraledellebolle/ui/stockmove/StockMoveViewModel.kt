package com.example.centraledellebolle.ui.stockmove

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.StockMoveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StockMoveViewModel(private val repository: StockMoveRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<StockMoveUiState>(StockMoveUiState.Idle)
    val uiState: StateFlow<StockMoveUiState> = _uiState

    fun performStockMove(moveType: MoveType, numero: String, articles: String) {
        viewModelScope.launch {
            _uiState.value = StockMoveUiState.Loading
            try {
                val response = repository.performStockMove(moveType, numero, articles)
                _uiState.value = StockMoveUiState.Success("Movimento registrato con successo", response.mov_id)
            } catch (e: Exception) {
                _uiState.value = StockMoveUiState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    fun resetState() {
        _uiState.value = StockMoveUiState.Idle
    }
}
