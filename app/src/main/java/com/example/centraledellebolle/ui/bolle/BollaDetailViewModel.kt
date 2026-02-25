package com.example.centraledellebolle.ui.bolle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.BolleRepository
import com.example.centraledellebolle.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BollaDetailViewModel(
    private val bolleRepository: BolleRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val bollaId: Int
) : ViewModel() {

    private val _bollaDetailState = MutableStateFlow<BollaDetailUiState>(BollaDetailUiState.Loading)
    val bollaDetailState: StateFlow<BollaDetailUiState> = _bollaDetailState

    private val _printingState = MutableStateFlow<PrintingUiState>(PrintingUiState.Idle)
    val printingState: StateFlow<PrintingUiState> = _printingState

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

    fun printBolla() {
        viewModelScope.launch {
            _printingState.value = PrintingUiState.Printing
            val printerMac = userPreferencesRepository.printerMac.first()

            if (printerMac == null) {
                _printingState.value = PrintingUiState.Error("Nessuna stampante selezionata")
                return@launch
            }

            val result = bolleRepository.printBolla(bollaId, printerMac)
            _printingState.value = result.fold(
                onSuccess = { PrintingUiState.Success },
                onFailure = { PrintingUiState.Error(it.message ?: "Errore di stampa") }
            )
        }
    }

    fun resetPrintingState() {
        _printingState.value = PrintingUiState.Idle
    }
}

class BollaDetailViewModelFactory(
    private val bolleRepository: BolleRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val bollaId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BollaDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BollaDetailViewModel(bolleRepository, userPreferencesRepository, bollaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}