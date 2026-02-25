package com.example.centraledellebolle.ui.bolle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.BolleRepository
import com.example.centraledellebolle.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BolleViewModel(
    private val bolleRepository: BolleRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _bolleState = MutableStateFlow<BolleUiState>(BolleUiState.Idle)
    val bolleState: StateFlow<BolleUiState> = _bolleState

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _printingState = MutableStateFlow<PrintingUiState>(PrintingUiState.Idle)
    val printingState: StateFlow<PrintingUiState> = _printingState

    init {
        loadBolleForDate(LocalDate.now())
    }

    fun loadBolleForDate(date: LocalDate) {
        viewModelScope.launch {
            _bolleState.value = BolleUiState.Loading
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE) // YYYY-MM-DD
            val result = bolleRepository.getBolle(dateString)
            _bolleState.value = result.fold(
                onSuccess = {
                    if (it.results.isEmpty()) {
                        BolleUiState.Empty
                    } else {
                        BolleUiState.Success(it.results)
                    }
                },
                onFailure = { BolleUiState.Error(it.message ?: "Errore sconosciuto") }
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
        loadBolleForDate(date)
    }

    fun printBolla(id: Int) {
        viewModelScope.launch {
            _printingState.value = PrintingUiState.Printing
            val printerMac = userPreferencesRepository.printerMac.first()

            if (printerMac == null) {
                _printingState.value = PrintingUiState.Error("Nessuna stampante selezionata")
                return@launch
            }

            val result = bolleRepository.printBolla(id, printerMac)
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