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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.centraledellebolle.data.Customer
import com.example.centraledellebolle.data.QuickLineError

val BOLLA_TEMPLATE = """
# Latte Fresco
600127 0
600128 0
600122 0
600101 0
600125 0
600114 0
600171 0
600025 0
600103 0
600104 0
# Latte UHT
600140 0
600141 0
600144 0
600145 0
600139 0
600142 0
600244 0
600119 0
600120 0
# Latte Bio
600146 0
600147 0
600148 0
# Latte ESL
004500 0
004501 0
# Derivati
600111 0
600112 0
# Uova
600026 0
600028 0
600030 0
# Casse
027110/S 0
027110/R 0
""".trimIndent()

@Composable
fun QuickBollaScreen(vm: QuickBollaViewModel, onBollaCreated: () -> Unit) {

    val creationState by vm.creationState.collectAsState()
    val rawLines by vm.rawLines.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (creationState !is QuickBollaUiState.Success) {
                Column(modifier = Modifier.weight(1f)) {

                    CustomerSelector(vm)

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Righe (CODICE QUANTITÀ)", style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = {
                            vm.onRawLinesChanged(rawLines.lines().filter { line ->
                                val trimmed = line.trim()
                                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@filter false
                                val parts = trimmed.split(Regex("\\s+"))
                                parts.size >= 2 && parts[1] != "0"
                            }.joinToString("\n"))
                        }) {
                            Text("Pulisci Zeri")
                        }
                    }

                    OutlinedTextField(
                        value = rawLines,
                        onValueChange = vm::onRawLinesChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = { Text("128 2\nPROVA 10\n...") },
                        enabled = creationState !is QuickBollaUiState.Loading
                    )
                }
            }

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

            if (creationState !is QuickBollaUiState.Success) {
                val selectedCustomer by vm.selectedCustomer.collectAsState()
                Button(
                    onClick = {
                        val validationErrors = vm.validateRawLines(rawLines)
                        if (validationErrors.isNotEmpty()) {
                            vm.showInputValidationErrors(rawLines, validationErrors)
                        } else {
                            val filteredLines = rawLines.lines()
                                .filter { line ->
                                    val trimmed = line.trim()
                                    if (trimmed.isBlank() || trimmed.startsWith("#")) return@filter false
                                    trimmed.split(Regex("\\s+"))[1].toIntOrNull()?.let { it != 0 } == true
                                }
                                .map { line ->
                                    line.trim().split(Regex("\\s+")).take(2).joinToString(" ")
                                }
                                .joinToString("\n")
                            vm.createBolla(filteredLines)
                        }
                    },
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
                                text = {
                                    Text(
                                            customer.codice
                                                ?.takeIf { it.isNotBlank() }
                                            ?.let { "$it - ${customer.nome} - ${customer.via}" }
                                            ?: "${customer.nome} - ${customer.via}"
                                    )
                                },
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
            vm.reset()
            onNavigate()
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
