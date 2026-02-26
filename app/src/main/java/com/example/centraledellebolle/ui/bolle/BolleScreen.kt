package com.example.centraledellebolle.ui.bolle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.centraledellebolle.data.Bolla
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BolleScreen(vm: BolleViewModel, onNavigateToDetail: (Int) -> Unit) {
    val bolleState by vm.bolleState.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val printingState by vm.printingState.collectAsState()
    val deleteState by vm.deleteState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(printingState) {
        when (val state = printingState) {
            is PrintingUiState.Success -> {
                snackbarHostState.showSnackbar("Stampa completata")
                vm.resetPrintingState()
            }
            is PrintingUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                vm.resetPrintingState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(deleteState) {
        when (val state = deleteState) {
            is DeleteUiState.Success -> {
                snackbarHostState.showSnackbar("Bolla eliminata con successo")
                vm.resetDeleteState()
            }
            is DeleteUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                vm.resetDeleteState()
            }
            else -> Unit
        }
    }

    val isDeleting = deleteState is DeleteUiState.Deleting

    if (deleteState is DeleteUiState.Request) {
        DeleteConfirmationDialog(
            onConfirm = { vm.confirmDelete() },
            onDismiss = { vm.cancelDelete() },
            isDeleting = isDeleting
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Column(modifier = Modifier.padding(it).padding(16.dp)) {
            DateSelector(date = selectedDate, onClick = { showDatePicker = true })

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = bolleState) {
                    is BolleUiState.Loading -> CircularProgressIndicator()
                    is BolleUiState.Success -> BolleList(
                        bolle = state.bolle,
                        onNavigateToDetail = onNavigateToDetail,
                        onPrint = { bollaId -> vm.printBolla(bollaId) },
                        onDelete = { bollaId -> vm.requestDelete(bollaId) },
                        isDeleting = isDeleting
                    )
                    is BolleUiState.Empty -> Text("Nessuna bolla per questa data.")
                    is BolleUiState.Error -> Text(text = "Errore: ${state.message}")
                    is BolleUiState.Idle -> { /* Do nothing */ }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        vm.onDateSelected(date)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDeleting: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminare la bolla?") },
        text = { Text("Questa azione non può essere annullata.") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                if(isDeleting) {
                    CircularProgressIndicator()
                } else {
                    Text("Elimina")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Annulla")
            }
        }
    )
}


@Composable
fun DateSelector(date: LocalDate, onClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Data: ${date.format(formatter)}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.DateRange, contentDescription = "Seleziona data")
    }
}

@Composable
fun BolleList(
    bolle: List<Bolla>,
    onNavigateToDetail: (Int) -> Unit,
    onPrint: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    isDeleting: Boolean
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(bolle) { bolla ->
            BollaItem(
                bolla = bolla,
                onNavigateToDetail = onNavigateToDetail,
                onPrint = onPrint,
                onDelete = onDelete,
                isDeleting = isDeleting
            )
        }
    }
}

@Composable
fun BollaItem(
    bolla: Bolla,
    onNavigateToDetail: (Int) -> Unit,
    onPrint: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    isDeleting: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {

            Text(text = bolla.clienteNome, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(text = "Data: ${formatDate(bolla.data)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Documento: ${bolla.tipoDocumentoNome} n. ${bolla.numero}", style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onNavigateToDetail(bolla.id.toInt()) }, enabled = !isDeleting) {
                    Text("Dettagli")
                }
                TextButton(onClick = { onPrint(bolla.id.toInt()) }, enabled = !isDeleting) {
                    Text("Stampa")
                }
                TextButton(onClick = { /* Handle edit */ }, enabled = !isDeleting) {
                    Text("Modifica")
                }
                TextButton(onClick = { onDelete(bolla.id.toInt()) }, enabled = !isDeleting) {
                    Text("Elimina")
                }
            }
        }
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoDate // Return original string if parsing fails
    }
}