package com.example.centraledellebolle.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.HealthRepository
import com.example.centraledellebolle.data.UserPreferencesRepository
import com.example.centraledellebolle.network.BaseUrlResolver
import com.example.centraledellebolle.printing.BluetoothPrinterService
import com.example.centraledellebolle.ui.health.HealthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface PrintingUiState {
    object Idle : PrintingUiState
    object Printing : PrintingUiState
    object Success : PrintingUiState
    data class Error(val message: String) : PrintingUiState
}

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthRepository: HealthRepository,
    private val baseUrlResolver: BaseUrlResolver,
    private val bluetoothPrinterService: BluetoothPrinterService
) : ViewModel() {

    private val _wifiBaseUrl = MutableStateFlow("")
    val wifiBaseUrl = _wifiBaseUrl.asStateFlow()

    private val _fallbackBaseUrl = MutableStateFlow("")
    val fallbackBaseUrl = _fallbackBaseUrl.asStateFlow()

    private val _resolvedBaseUrl = MutableStateFlow<String?>("")
    val resolvedBaseUrl = _resolvedBaseUrl.asStateFlow()

    private val _printerName = MutableStateFlow<String?>("")
    val printerName = _printerName.asStateFlow()

    private val _printerMac = MutableStateFlow<String?>("")
    val printerMac = _printerMac.asStateFlow()

    private val _serverHealthState = MutableStateFlow<HealthUiState>(HealthUiState.Idle)
    val serverHealthState = _serverHealthState.asStateFlow()

    private val _printingState = MutableStateFlow<PrintingUiState>(PrintingUiState.Idle)
    val printingState = _printingState.asStateFlow()

    init {
        userPreferencesRepository.wifiBaseUrl.onEach {
            _wifiBaseUrl.value = it ?: ""
        }.launchIn(viewModelScope)

        userPreferencesRepository.fallbackBaseUrl.onEach {
            _fallbackBaseUrl.value = it ?: ""
        }.launchIn(viewModelScope)

        baseUrlResolver.resolvedBaseUrl.onEach {
            _resolvedBaseUrl.value = it
            testServer()
        }.launchIn(viewModelScope)

        userPreferencesRepository.printerName.onEach {
            _printerName.value = it
        }.launchIn(viewModelScope)

        userPreferencesRepository.printerMac.onEach {
            _printerMac.value = it
        }.launchIn(viewModelScope)
    }

    fun saveBaseUrls(wifiUrl: String, fallbackUrl: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveBaseUrls(wifiUrl, fallbackUrl)
        }
    }

    fun testServer() {
        viewModelScope.launch {
            _serverHealthState.value = HealthUiState.Loading
            try {
                val ok = healthRepository.check()
                _serverHealthState.value = if (ok) HealthUiState.Ok else HealthUiState.Error("Server ha risposto ok=false")
            } catch (e: Exception) {
                _serverHealthState.value = HealthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun savePrinter(name: String, macAddress: String) {
        viewModelScope.launch {
            userPreferencesRepository.savePrinter(name, macAddress)
        }
    }

    fun testPrint() {
        viewModelScope.launch {
            val macAddress = printerMac.first()
            if (macAddress == null) {
                _printingState.value = PrintingUiState.Error("Nessuna stampante selezionata")
                return@launch
            }

            _printingState.value = PrintingUiState.Printing
            val result = bluetoothPrinterService.printText(macAddress, "Test di stampa OK")
            result.fold(
                onSuccess = { _printingState.value = PrintingUiState.Success },
                onFailure = { _printingState.value = PrintingUiState.Error(it.message ?: "Errore di stampa sconosciuto") }
            )
        }
    }

    fun resetPrintingState() {
        _printingState.value = PrintingUiState.Idle
    }
}

class SettingsViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthRepository: HealthRepository,
    private val baseUrlResolver: BaseUrlResolver,
    private val bluetoothPrinterService: BluetoothPrinterService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userPreferencesRepository, healthRepository, baseUrlResolver, bluetoothPrinterService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
