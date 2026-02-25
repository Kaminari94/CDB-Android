package com.example.centraledellebolle.ui.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.centraledellebolle.ui.health.HealthUiState

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit
) {
    val wifiBaseUrl by viewModel.wifiBaseUrl.collectAsState()
    val fallbackBaseUrl by viewModel.fallbackBaseUrl.collectAsState()
    val resolvedBaseUrl by viewModel.resolvedBaseUrl.collectAsState()
    val printerName by viewModel.printerName.collectAsState()
    val serverHealthState by viewModel.serverHealthState.collectAsState()
    val printingState by viewModel.printingState.collectAsState()

    var editedWifiUrl by remember(wifiBaseUrl) { mutableStateOf(wifiBaseUrl) }
    var editedFallbackUrl by remember(fallbackBaseUrl) { mutableStateOf(fallbackBaseUrl) }
    var showBluetoothDevices by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(printingState) {
        when (val state = printingState) {
            is PrintingUiState.Success -> {
                val result = snackbarHostState.showSnackbar("Stampa di prova inviata con successo", "OK")
                if (result == SnackbarResult.Dismissed || result == SnackbarResult.ActionPerformed) {
                    viewModel.resetPrintingState()
                }
            }

            is PrintingUiState.Error -> {
                val result = snackbarHostState.showSnackbar("Errore di stampa: ${state.message}", "OK")
                if (result == SnackbarResult.Dismissed || result == SnackbarResult.ActionPerformed) {
                    viewModel.resetPrintingState()
                }
            }

            else -> Unit
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showBluetoothDevices = true
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            ServerSettings(
                wifiUrl = editedWifiUrl ?: "",
                fallbackUrl = editedFallbackUrl ?: "",
                healthState = serverHealthState,
                resolvedUrl = resolvedBaseUrl,
                onWifiUrlChange = { editedWifiUrl = it },
                onFallbackUrlChange = { editedFallbackUrl = it },
                onSave = { viewModel.saveBaseUrls(editedWifiUrl, editedFallbackUrl) },
                onRetry = { viewModel.testServer() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            PrinterSettings(printerName, onChoosePrinter = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    showBluetoothDevices = true
                }
            }, onTestPrint = { viewModel.testPrint() })

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = onLogout) {
                Text("Logout")
            }

            if (showBluetoothDevices) {
                BluetoothDeviceList(onDismiss = { showBluetoothDevices = false }, onDeviceSelected = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.savePrinter(it.name ?: "Dispositivo sconosciuto", it.address)
                        showBluetoothDevices = false
                    }

                })
            }
        }
    }
}

@Composable
private fun ServerSettings(
    wifiUrl: String,
    fallbackUrl: String,
    healthState: HealthUiState,
    resolvedUrl: String?,
    onWifiUrlChange: (String) -> Unit,
    onFallbackUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = wifiUrl,
            onValueChange = onWifiUrlChange,
            label = { Text("Indirizzo server WiFi") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fallbackUrl,
            onValueChange = onFallbackUrlChange,
            label = { Text("Indirizzo server Fallback") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSave, enabled = wifiUrl.endsWith("/") && wifiUrl.isNotBlank() && fallbackUrl.endsWith("/") && fallbackUrl.isNotBlank()) {
            Text("Salva")
        }

        Text("URL in uso: ${resolvedUrl ?: "N/A"}")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stato server: ")
            when (healthState) {
                is HealthUiState.Loading -> CircularProgressIndicator()
                is HealthUiState.Ok -> Text("OK", color = MaterialTheme.colorScheme.primary)
                is HealthUiState.Error -> Text("Errore", color = MaterialTheme.colorScheme.error)
                is HealthUiState.Idle -> Text("N/A")
            }
            if (healthState is HealthUiState.Error) {
                TextButton(onClick = onRetry) {
                    Text("Riprova")
                }
            }
        }
    }
}

@Composable
private fun PrinterSettings(printerName: String?, onChoosePrinter: () -> Unit, onTestPrint: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Stampante Bluetooth: ${printerName ?: "Nessuna"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onChoosePrinter) {
                Text("Scegli stampante")
            }
            Button(onClick = onTestPrint, enabled = printerName != null) {
                Text("Test stampa")
            }
        }
    }
}

@Composable
private fun BluetoothDeviceList(onDismiss: () -> Unit, onDeviceSelected: (BluetoothDevice) -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    val bondedDevices: Set<BluetoothDevice>? =
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothAdapter?.bondedDevices
        } else {
            emptySet()
        }


    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Scegli un dispositivo", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            if (bondedDevices.isNullOrEmpty()) {
                Text("Nessun dispositivo associato")
            } else {
                LazyColumn {
                    items(bondedDevices.toList()) { device ->
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            Text(text = device.name ?: "Dispositivo sconosciuto", modifier = Modifier.clickable { onDeviceSelected(device) })
                        }

                    }
                }
            }
        }
    }
}