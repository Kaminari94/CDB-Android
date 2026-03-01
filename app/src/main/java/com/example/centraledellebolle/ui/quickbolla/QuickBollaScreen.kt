package com.example.centraledellebolle.ui.quickbolla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.centraledellebolle.data.Customer
import com.example.centraledellebolle.data.QuickLineError

@Composable
fun QuickBollaScreen(vm: QuickBollaViewModel, onBollaCreated: () -> Unit) {

    val creationState by vm.creationState.collectAsState()
    var rawLines by remember { mutableStateOf("") }

    // Sincronizza il testo locale con lo stato del ViewModel
    LaunchedEffect(creationState) {
        when (val state = creationState) {
            is QuickBollaUiState.Idle -> {
                // Se c'è un input precedente, usalo
                if (state.previousInput.isNotEmpty()) {
                    rawLines = state.previousInput
                }
            }
            is QuickBollaUiState.Error -> {
                // In caso di errore, mantieni il testo che l'ha causato
                rawLines = state.rawInput
            }
            is QuickBollaUiState.Success -> {
                // Svuota il campo solo dopo un successo
                rawLines = ""
            }
            else -> Unit
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // -- Sezione Input --
            if (creationState !is QuickBollaUiState.Success) {
                Column(modifier = Modifier.weight(1f)) {

                    CustomerSelector(vm)

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = rawLines,
                        onValueChange = { rawLines = it },
                        label = { Text("Righe (CODICE QUANTITÀ)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = { Text("Esempio di testo da scrivere qui:\n128 2\n122 2\n125 50\n...") },
                        enabled = creationState !is QuickBollaUiState.Loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // -- Sezione Risultato/Stato --
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val state = creationState) {
                    is QuickBollaUiState.Loading -> CircularProgressIndicator()
                    is QuickBollaUiState.Success -> {
                        SuccessView(
                            vm = vm,
                            bollaId = state.response.bolla_id,
                            onNavigate = onBollaCreated
                        )
                    }
                    is QuickBollaUiState.Error -> {
                        ErrorView(state) { vm.reset() }
                    }
                    is QuickBollaUiState.Idle -> {
                        Text(
                            "Pronto per creare una nuova bolla.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // -- Bottone sticky in basso --
            if (creationState !is QuickBollaUiState.Success) {
                val selectedCustomer by vm.selectedCustomer.collectAsState()
                Button(
                    onClick = { vm.createBolla(rawLines) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedCustomer != null && rawLines.isNotBlank() && creationState !is QuickBollaUiState.Loading
                ) {
                    Text("CREA BOLLA")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerSelector(vm: QuickBollaViewModel) {
    val customersState by vm.customersUiState.collectAsState()
    val selectedCustomer by vm.selectedCustomer.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        when (val state = customersState) {
            is CustomersUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is CustomersUiState.Success -> {
                ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
                    TextField(
                        value = selectedCustomer?.nome ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cliente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        state.customers.forEach { customer ->
                            DropdownMenuItem(
                                text = { Text(customer.nome + " - " + customer.via) },
                                onClick = {
                                    vm.selectCustomer(customer)
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            is CustomersUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { vm.loadCustomers() }) {
                        Text("Riprova")
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessView(vm: QuickBollaViewModel, bollaId: Int, onNavigate: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "Successo!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF00C853)
        )
        Text("Creata bolla con ID: $bollaId")
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            vm.reset() // Resetta lo stato nel ViewModel
            onNavigate() // Torna alla schermata precedente
        }) {
            Text("Torna alla lista")
        }
    }
}

@Composable
private fun ErrorView(state: QuickBollaUiState.Error, onDismiss: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Errore",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))

        state.validationError?.errors?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dettagli errori:", style = MaterialTheme.typography.titleSmall)
                    it.forEach { error -> ErrorRow(error) }
                }
            }
        }

        state.genericMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onDismiss) {
            Text("OK")
        }
    }
}

@Composable
private fun ErrorRow(error: QuickLineError) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            "Riga ${error.line}:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(error.message, style = MaterialTheme.typography.bodyMedium)
    }
}
