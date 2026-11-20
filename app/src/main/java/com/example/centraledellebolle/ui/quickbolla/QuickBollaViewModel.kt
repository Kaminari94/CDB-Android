package com.example.centraledellebolle.ui.quickbolla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.LocalEmissionEntity
import com.example.centraledellebolle.data.OfflineIssueResult
import com.example.centraledellebolle.data.OfflineQuickBollaRepository
import com.example.centraledellebolle.data.QuickBollaRepository
import com.example.centraledellebolle.data.QuickBollaRequest
import com.example.centraledellebolle.data.QuickCustomerEntity
import com.example.centraledellebolle.data.QuickDocumentTypeEntity
import com.example.centraledellebolle.data.QuickLineError
import com.example.centraledellebolle.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface QuickCatalogUiState {
    data object Loading : QuickCatalogUiState
    data object Ready : QuickCatalogUiState
    data class Offline(val message: String) : QuickCatalogUiState
}

sealed interface OfflineQuickBollaUiState {
    data object Idle : OfflineQuickBollaUiState
    data object Issuing : OfflineQuickBollaUiState
    data class Success(val emission: LocalEmissionEntity, val printed: Boolean, val message: String) : OfflineQuickBollaUiState
    data class LegacySuccess(val serverBollaId: Int) : OfflineQuickBollaUiState
    data class ValidationError(val errors: List<QuickLineError>) : OfflineQuickBollaUiState
    data class Error(val message: String) : OfflineQuickBollaUiState
}

class QuickBollaViewModel(
    private val offlineRepository: OfflineQuickBollaRepository,
    private val legacyRepository: QuickBollaRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _rawLines = MutableStateFlow(BOLLA_TEMPLATE)
    val rawLines: StateFlow<String> = _rawLines.asStateFlow()

    private val _customers = MutableStateFlow<List<QuickCustomerEntity>>(emptyList())
    val customers: StateFlow<List<QuickCustomerEntity>> = _customers.asStateFlow()
    private val _types = MutableStateFlow<List<QuickDocumentTypeEntity>>(emptyList())
    val types: StateFlow<List<QuickDocumentTypeEntity>> = _types.asStateFlow()
    private val _selectedCustomer = MutableStateFlow<QuickCustomerEntity?>(null)
    val selectedCustomer: StateFlow<QuickCustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _catalogState = MutableStateFlow<QuickCatalogUiState>(QuickCatalogUiState.Loading)
    val catalogState: StateFlow<QuickCatalogUiState> = _catalogState.asStateFlow()
    private val _uiState = MutableStateFlow<OfflineQuickBollaUiState>(OfflineQuickBollaUiState.Idle)
    val uiState: StateFlow<OfflineQuickBollaUiState> = _uiState.asStateFlow()

    val pendingCount = offlineRepository.pendingCount
    val emissions = offlineRepository.emissions
    val conflicts = offlineRepository.conflicts

    init {
        offlineRepository.customers.onEach {
            _customers.value = it
            updateCatalogAvailability()
        }.launchIn(viewModelScope)
        offlineRepository.documentTypes.onEach {
            _types.value = it
            updateCatalogAvailability()
        }.launchIn(viewModelScope)
        offlineRepository.scheduleSync()
        refreshCatalog()
    }

    private fun updateCatalogAvailability() {
        if (_customers.value.isNotEmpty() && _types.value.isNotEmpty()) _catalogState.value = QuickCatalogUiState.Ready
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            if (_customers.value.isEmpty()) _catalogState.value = QuickCatalogUiState.Loading
            offlineRepository.refreshCatalog(LocalDate.now().year).onFailure {
                _catalogState.value = if (_customers.value.isNotEmpty() && _types.value.isNotEmpty()) {
                    QuickCatalogUiState.Offline("Catalogo locale in uso: ${it.message ?: "server non raggiungibile"}")
                } else {
                    QuickCatalogUiState.Offline("Nessun catalogo locale disponibile: collegati al server almeno una volta.")
                }
            }
        }
    }

    fun selectCustomer(customer: QuickCustomerEntity) {
        _selectedCustomer.value = customer
    }

    fun onRawLinesChanged(value: String) { _rawLines.value = value }

    fun validateRawLines(value: String): List<QuickLineError> = value.lines().mapIndexedNotNull { index, line ->
        val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.isEmpty() || line.trim().startsWith("#") -> null
            parts.size != 2 -> QuickLineError(index + 1, "Formato non valido: usa CODICE QUANTITÀ")
            parts[1].toIntOrNull() == null -> QuickLineError(index + 1, "Quantità non valida: usa un numero intero")
            else -> null
        }
    }

    fun createAndPrint() {
        val customer = _selectedCustomer.value ?: return
        // The document series is part of the customer catalogue; operators cannot override it.
        val typeId = customer.defaultDocumentTypeId
        if (_uiState.value is OfflineQuickBollaUiState.Issuing) return
        viewModelScope.launch {
            _uiState.value = OfflineQuickBollaUiState.Issuing
            val formatErrors = validateRawLines(_rawLines.value)
            if (formatErrors.isNotEmpty()) {
                _uiState.value = OfflineQuickBollaUiState.ValidationError(formatErrors)
                return@launch
            }
            val linesForIssue = _rawLines.value.lines().filter { line ->
                val trimmed = line.trim()
                val parts = trimmed.split(Regex("\\s+"))
                trimmed.isNotBlank() && !trimmed.startsWith("#") && parts.size >= 2 && parts[1].toIntOrNull()?.let { it != 0 } == true
            }.joinToString("\n")
            when (val issue = offlineRepository.issue(linesForIssue, customer.id, typeId)) {
                is OfflineIssueResult.ValidationError -> _uiState.value = OfflineQuickBollaUiState.ValidationError(issue.errors)
                is OfflineIssueResult.Error -> _uiState.value = OfflineQuickBollaUiState.Error(issue.message)
                is OfflineIssueResult.Success -> {
                    val mac = userPreferencesRepository.printerMac.first()
                    val print = if (mac.isNullOrBlank()) Result.failure(Exception("Nessuna stampante Bluetooth selezionata"))
                    else offlineRepository.print(issue.issued.emission.emissionId, mac)
                    offlineRepository.scheduleSync()
                    _uiState.value = OfflineQuickBollaUiState.Success(
                        issue.issued.emission, print.isSuccess,
                        print.exceptionOrNull()?.message ?: "Bolla salvata e stampata"
                    )
                }
            }
        }
    }

    fun reprint(emission: LocalEmissionEntity) {
        viewModelScope.launch {
            val mac = userPreferencesRepository.printerMac.first()
            if (mac.isNullOrBlank()) {
                _uiState.value = OfflineQuickBollaUiState.Error("Nessuna stampante Bluetooth selezionata")
                return@launch
            }
            val result = offlineRepository.print(emission.emissionId, mac)
            _uiState.value = if (result.isSuccess) OfflineQuickBollaUiState.Success(emission, true, "Ristampa completata")
            else OfflineQuickBollaUiState.Error(result.exceptionOrNull()?.message ?: "Errore di ristampa")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val summary = offlineRepository.syncPending()
            if (summary.hasRetryableFailure) offlineRepository.scheduleSync()
        }
    }

    /** Explicit test-only escape hatch; the normal button never uses this server-first path. */
    fun createWithLegacyServerForTest() {
        val customer = _selectedCustomer.value ?: return
        viewModelScope.launch {
            _uiState.value = OfflineQuickBollaUiState.Issuing
            val lines = _rawLines.value.lines().filter { line ->
                val parts = line.trim().split(Regex("\\s+"))
                line.isNotBlank() && !line.trim().startsWith("#") && parts.size >= 2 && parts[1].toIntOrNull()?.let { it != 0 } == true
            }.joinToString("\n")
            legacyRepository.createQuickBolla(QuickBollaRequest(customer.id, lines)).fold(
                onSuccess = { _uiState.value = OfflineQuickBollaUiState.LegacySuccess(it.bolla_id) },
                onFailure = { _uiState.value = OfflineQuickBollaUiState.Error(it.message ?: "Errore flusso legacy") }
            )
        }
    }

    fun reset() { _uiState.value = OfflineQuickBollaUiState.Idle }
}
