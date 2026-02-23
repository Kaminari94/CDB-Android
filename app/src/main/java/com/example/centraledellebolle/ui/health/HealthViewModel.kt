package com.example.centraledellebolle.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthViewModel(
    private val repo: HealthRepository
) : ViewModel() {

    private val _health = MutableStateFlow<HealthUiState>(HealthUiState.Idle)
    val health: StateFlow<HealthUiState> = _health

    fun load() {
        // evita doppie chiamate se lo richiami per sbaglio
        if (_health.value is HealthUiState.Loading) return

        viewModelScope.launch {
            _health.value = HealthUiState.Loading
            try {
                val ok = repo.check()
                _health.value = if (ok) HealthUiState.Ok else HealthUiState.Error("Server ha risposto ok=false")
            } catch (e: Exception) {
                _health.value = HealthUiState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }

    fun retry() = load()
}