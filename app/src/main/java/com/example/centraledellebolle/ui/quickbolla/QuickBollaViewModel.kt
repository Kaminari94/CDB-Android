package com.example.centraledellebolle.ui.quickbolla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.Customer
import com.example.centraledellebolle.data.CustomersRepository
import com.example.centraledellebolle.data.QuickBollaRepository
import com.example.centraledellebolle.data.QuickBollaRequest
import com.example.centraledellebolle.data.QuickBollaErrorResponse
import com.example.centraledellebolle.data.QuickBollaValidationException
import com.example.centraledellebolle.data.QuickLineError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuickBollaViewModel(
    private val quickBollaRepository: QuickBollaRepository,
    private val customersRepository: CustomersRepository
) : ViewModel() {

    // Dato operativo: deve sopravvivere alla ricreazione della composizione (es. rotazione).
    private val _rawLines = MutableStateFlow(BOLLA_TEMPLATE)
    val rawLines: StateFlow<String> = _rawLines.asStateFlow()

    // Stato per la creazione della bolla
    private val _creationState = MutableStateFlow<QuickBollaUiState>(QuickBollaUiState.Idle())
    val creationState: StateFlow<QuickBollaUiState> = _creationState.asStateFlow()

    // Stato per il caricamento dei clienti
    private val _customersUiState = MutableStateFlow<CustomersUiState>(CustomersUiState.Loading)
    val customersUiState: StateFlow<CustomersUiState> = _customersUiState.asStateFlow()

    // Cliente attualmente selezionato
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    init {
        loadCustomers()
    }

    fun loadCustomers() {
        viewModelScope.launch {
            _customersUiState.value = CustomersUiState.Loading
            try {
                val customers = customersRepository.getCustomers().sortedWith(
                    compareBy<Customer> { it.codice.isNullOrBlank() }
                        .thenBy { it.codice ?: "" }
                        .thenBy { it.nome }
                )
                _customersUiState.value = CustomersUiState.Success(customers)
            } catch (e: Exception) {
                _customersUiState.value = CustomersUiState.Error(e.message ?: "Impossibile caricare i clienti")
            }
        }
    }

    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
    }

    fun onRawLinesChanged(value: String) {
        _rawLines.value = value
    }

    fun validateRawLines(rawLines: String): List<QuickLineError> =
        rawLines.lines().mapIndexedNotNull { index, line ->
            val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            when {
                parts.isEmpty() || line.trim().startsWith("#") -> null
                parts.size != 2 -> QuickLineError(index + 1, "Formato non valido: usa CODICE QUANTITÀ")
                parts[1].toIntOrNull() == null -> QuickLineError(index + 1, "Quantità non valida: usa un numero intero")
                else -> null
            }
        }

    fun showInputValidationErrors(rawLines: String, errors: List<QuickLineError>) {
        _creationState.value = QuickBollaUiState.Error(
            rawInput = rawLines,
            validationError = QuickBollaErrorResponse(created = false, errors = errors)
        )
    }

    fun createBolla(rawLines: String) {
        val customer = _selectedCustomer.value ?: return

        if (_creationState.value is QuickBollaUiState.Loading) return

        viewModelScope.launch {
            _creationState.value = QuickBollaUiState.Loading

            val request = QuickBollaRequest(customer_id = customer.id, raw_lines = rawLines)
            val result = quickBollaRepository.createQuickBolla(request)

            _creationState.value = result.fold(
                onSuccess = { successResponse ->
                    QuickBollaUiState.Success(successResponse)
                },
                onFailure = { exception ->
                    when (exception) {
                        is QuickBollaValidationException -> {
                            QuickBollaUiState.Error(
                                rawInput = rawLines,
                                validationError = exception.errorResponse
                            )
                        }
                        else -> {
                            QuickBollaUiState.Error(
                                rawInput = rawLines,
                                genericMessage = exception.message ?: "Errore sconosciuto"
                            )
                        }
                    }
                }
            )
        }
    }

    /** Resetta lo stato della creazione per permettere di creare una nuova bolla. */
    fun reset() {
        val currentState = _creationState.value
        if (currentState is QuickBollaUiState.Error) {
            _creationState.value = QuickBollaUiState.Idle(previousInput = currentState.rawInput)
        } else {
            _creationState.value = QuickBollaUiState.Idle()
        }
    }
}

sealed interface CustomersUiState {
    data object Loading : CustomersUiState
    data class Success(val customers: List<Customer>) : CustomersUiState
    data class Error(val message: String) : CustomersUiState
}
