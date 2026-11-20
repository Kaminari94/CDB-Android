package com.example.centraledellebolle.ui.quickbolla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.centraledellebolle.data.LocalEmissionEntity
import com.example.centraledellebolle.data.QuickCustomerEntity

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
    val rawLines by vm.rawLines.collectAsState()
    val customers by vm.customers.collectAsState()
    val types by vm.types.collectAsState()
    val selectedCustomer by vm.selectedCustomer.collectAsState()
    val catalogState by vm.catalogState.collectAsState()
    val state by vm.uiState.collectAsState()
    val pendingCount by vm.pendingCount.collectAsState(initial = 0)
    val emissions by vm.emissions.collectAsState(initial = emptyList())

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (catalogState) {
                QuickCatalogUiState.Loading -> Text("Caricamento catalogo…")
                QuickCatalogUiState.Ready -> Text("Catalogo locale pronto", color = MaterialTheme.colorScheme.primary)
                is QuickCatalogUiState.Offline -> Text((catalogState as QuickCatalogUiState.Offline).message, color = MaterialTheme.colorScheme.tertiary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = vm::refreshCatalog) { Text("Aggiorna catalogo") }
                TextButton(onClick = vm::syncNow) { Text("Sincronizza ($pendingCount)") }
            }

            CustomerSelector(customers, selectedCustomer, vm::selectCustomer)
            selectedCustomer?.let { customer ->
                Text(
                    "Tipo documento: ${types.firstOrNull { it.id == customer.defaultDocumentTypeId }?.name ?: "non disponibile"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Righe (CODICE QUANTITÀ)", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { vm.onRawLinesChanged(removeZeroLines(rawLines)) }) { Text("Pulisci Zeri") }
            }
            OutlinedTextField(
                value = rawLines,
                onValueChange = vm::onRawLinesChanged,
                modifier = Modifier.fillMaxWidth().weight(1f),
                enabled = state !is OfflineQuickBollaUiState.Issuing,
                placeholder = { Text("600127 2") }
            )

            when (state) {
                OfflineQuickBollaUiState.Idle -> Unit
                OfflineQuickBollaUiState.Issuing -> CircularProgressIndicator()
                is OfflineQuickBollaUiState.Success -> {
                    val success = state as OfflineQuickBollaUiState.Success
                    Text("${success.emission.documentTypeName} n. ${success.emission.number}/${success.emission.year}")
                    Text(if (success.printed) "Stampata" else "Salvata: ristampa necessaria", color = if (success.printed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    Text(success.message)
                    Row {
                        TextButton(onClick = { vm.reprint(success.emission) }) { Text("Ristampa") }
                        TextButton(onClick = { vm.reset(); onBollaCreated() }) { Text("Torna alla lista") }
                    }
                }
                is OfflineQuickBollaUiState.LegacySuccess -> {
                    Text("Bolla legacy creata sul server: ID ${(state as OfflineQuickBollaUiState.LegacySuccess).serverBollaId}")
                    TextButton(onClick = { vm.reset(); onBollaCreated() }) { Text("Torna alla lista") }
                }
                is OfflineQuickBollaUiState.ValidationError -> {
                    val errors = (state as OfflineQuickBollaUiState.ValidationError).errors
                    Text("Correggi le righe:", color = MaterialTheme.colorScheme.error)
                    errors.forEach { Text("Riga ${it.line}: ${it.message}", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = vm::reset) { Text("OK") }
                }
                is OfflineQuickBollaUiState.Error -> {
                    Text((state as OfflineQuickBollaUiState.Error).message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = vm::reset) { Text("OK") }
                }
            }

            if (state !is OfflineQuickBollaUiState.Success && state !is OfflineQuickBollaUiState.LegacySuccess) {
                Button(
                    onClick = {
                        vm.createAndPrint()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedCustomer != null && rawLines.isNotBlank() && state !is OfflineQuickBollaUiState.Issuing
                ) { Text("CREA E STAMPA BOLLA") }
                TextButton(
                    onClick = vm::createWithLegacyServerForTest,
                    enabled = selectedCustomer != null && state !is OfflineQuickBollaUiState.Issuing
                ) { Text("Test: crea con flusso legacy server") }
            }
            PendingEmissions(emissions, vm::reprint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerSelector(customers: List<QuickCustomerEntity>, selected: QuickCustomerEntity?, onSelected: (QuickCustomerEntity) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = selected?.let { listOfNotNull(it.codice, it.nome, it.via).joinToString(" - ") } ?: "",
            onValueChange = {}, readOnly = true, label = { Text("Cliente") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            customers.forEach { customer ->
                DropdownMenuItem(text = { Text(listOfNotNull(customer.codice, customer.nome, customer.via).joinToString(" - ")) }, onClick = {
                    onSelected(customer); expanded = false
                })
            }
        }
    }
}

@Composable
private fun PendingEmissions(emissions: List<LocalEmissionEntity>, onReprint: (LocalEmissionEntity) -> Unit) {
    val actionable = emissions.filter { it.status != "SYNCED" }.take(4)
    if (actionable.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Archivio locale", style = MaterialTheme.typography.titleSmall)
        LazyColumn(Modifier.height(120.dp)) {
            items(actionable) { emission ->
                Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${emission.documentTypeName} ${emission.number}/${emission.year} — ${emission.status}")
                            emission.lastSyncError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        }
                        TextButton(onClick = { onReprint(emission) }) { Text("Ristampa") }
                    }
                }
            }
        }
    }
}

private fun removeZeroLines(input: String): String = input.lines().filter { line ->
    val parts = line.trim().split(Regex("\\s+"))
    line.isNotBlank() && !line.trim().startsWith("#") && parts.size >= 2 && parts[1].toIntOrNull()?.let { it != 0 } == true
}.joinToString("\n")
