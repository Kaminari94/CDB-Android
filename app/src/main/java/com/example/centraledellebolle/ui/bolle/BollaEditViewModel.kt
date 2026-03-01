package com.example.centraledellebolle.ui.bolle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.BollaDetail
import com.example.centraledellebolle.data.BolleRepository
import com.example.centraledellebolle.data.QuickLineError
import com.example.centraledellebolle.data.ValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditableRow(
    val codice: String,
    val descrizione: String,
    val lotto: String?,
    val qtyText: String
)

sealed interface BollaEditUiState {
    data object Loading : BollaEditUiState
    data class Ready(
        val bolla: BollaDetail,
        val rows: List<EditableRow>,
        val isSaving: Boolean = false
    ) : BollaEditUiState
    data class Error(val message: String) : BollaEditUiState
    data class ValidationError(val errors: List<QuickLineError>?, val message: String?) : BollaEditUiState
    data object Saved : BollaEditUiState
}

class BollaEditViewModel(private val repository: BolleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<BollaEditUiState>(BollaEditUiState.Loading)
    val uiState: StateFlow<BollaEditUiState> = _uiState.asStateFlow()

    fun load(bollaId: Int) {
        viewModelScope.launch {
            _uiState.value = BollaEditUiState.Loading
            repository.getBollaDetail(bollaId).onSuccess {
                val editableRows = it.righe.map { riga ->
                    EditableRow(riga.codice, riga.descrizione, riga.lotto, riga.quantita)
                }
                _uiState.value = BollaEditUiState.Ready(it, editableRows)
            }.onFailure {
                _uiState.value = BollaEditUiState.Error(it.message ?: "Errore sconosciuto")
            }
        }
    }

    fun onQtyChanged(index: Int, newText: String) {
        val currentState = _uiState.value
        if (currentState is BollaEditUiState.Ready) {
            val updatedRows = currentState.rows.toMutableList()
            updatedRows[index] = updatedRows[index].copy(qtyText = newText)
            _uiState.value = currentState.copy(rows = updatedRows)
        }
    }

    fun onCodiceChanged(index: Int, newText: String) {
        val currentState = _uiState.value
        if (currentState is BollaEditUiState.Ready) {
            val updatedRows = currentState.rows.toMutableList()
            updatedRows[index] = updatedRows[index].copy(codice = newText)
            _uiState.value = currentState.copy(rows = updatedRows)
        }
    }

    fun removeRow(index: Int) {
        val currentState = _uiState.value
        if (currentState is BollaEditUiState.Ready) {
            val updatedRows = currentState.rows.toMutableList()
            updatedRows.removeAt(index)
            _uiState.value = currentState.copy(rows = updatedRows)
        }
    }

    fun addRow() {
        val currentState = _uiState.value
        if (currentState is BollaEditUiState.Ready) {
            val updatedRows = currentState.rows.toMutableList()
            updatedRows.add(EditableRow("", "", "", ""))
            _uiState.value = currentState.copy(rows = updatedRows)
        }
    }

    fun save(bollaId: Int) {
        val currentState = _uiState.value
        if (currentState is BollaEditUiState.Ready) {
            val validationErrors = mutableListOf<QuickLineError>()
            currentState.rows.forEachIndexed { index, row ->
                if (row.codice.isBlank() && row.qtyText.isNotBlank() && row.qtyText.toIntOrNull() != 0) {
                    validationErrors.add(QuickLineError(index + 1, "Il codice non può essere vuoto"))
                }
                if (row.qtyText.isNotBlank() && row.qtyText.toIntOrNull() == null) {
                    validationErrors.add(QuickLineError(index + 1, "La quantità non è un numero valido"))
                }
            }

            if (validationErrors.isNotEmpty()) {
                _uiState.value = BollaEditUiState.ValidationError(validationErrors, "Sono presenti errori di validazione")
                return
            }

            val rawLines = currentState.rows.filter { it.qtyText.toIntOrNull() ?: 0 > 0 }
                .joinToString("\n") { "${it.codice} ${it.qtyText}" }

            viewModelScope.launch {
                _uiState.value = currentState.copy(isSaving = true)
                repository.updateBollaRighe(bollaId, rawLines).onSuccess {
                    _uiState.value = BollaEditUiState.Saved
                }.onFailure {
                    when (it) {
                        is ValidationException -> {
                            _uiState.value = BollaEditUiState.ValidationError(it.errors, "Errore di validazione dal server")
                        }
                        else -> {
                            _uiState.value = BollaEditUiState.Error(it.message ?: "Errore durante il salvataggio")
                        }
                    }
                }
            }
        }
    }
}