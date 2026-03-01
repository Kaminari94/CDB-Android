package com.example.centraledellebolle.ui.bolle

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.centraledellebolle.data.BollaDetail
import com.example.centraledellebolle.data.Riga
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BollaDetailScreen(
    vm: BollaDetailViewModel,
    onNavigateBack: () -> Unit,
    navController: NavController
) {
    val bollaDetailState by vm.bollaDetailState.collectAsState()
    val printingState by vm.printingState.collectAsState()
    val deleteState by vm.deleteState.collectAsState()
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
                onNavigateBack()
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            IconButton(onClick = onNavigateBack, enabled = !isDeleting) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = bollaDetailState) {
                    is BollaDetailUiState.Loading -> CircularProgressIndicator()
                    is BollaDetailUiState.Success -> BollaDetailContent(
                        bolla = state.bolla,
                        onPrint = { vm.printBolla() },
                        onDelete = { vm.requestDelete() },
                        isDeleting = isDeleting,
                        onEdit = { navController.navigate("bolla_edit/${state.bolla.id}") }
                    )
                    is BollaDetailUiState.Error -> Text(text = "Errore: ${state.message}")
                }
            }
        }
    }
}

@Composable
fun BollaDetailContent(
    bolla: BollaDetail,
    onPrint: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean,
    onEdit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) { // Make the column fill the screen
        Text(text = bolla.clienteNome, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Data: ${formatDate(bolla.data)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${bolla.tipoDocumentoNome} n. ${bolla.numero}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Table Header
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Codice", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)
            Text("Descrizione", modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Bold)
            Text("Quantità", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)
        }

        // Table Rows - this will now be scrollable and take up the available space
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(bolla.righe) { riga ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(riga.codice, modifier = Modifier.weight(0.2f))
                    Column(modifier = Modifier.weight(0.6f)) {
                        Text(riga.descrizione)
                        riga.lotto?.let {
                            Text("Lotto: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(riga.quantita, modifier = Modifier.weight(0.2f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons - these will be pushed to the bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onEdit, enabled = !isDeleting) {
                Text("Modifica")
            }
            TextButton(onClick = onPrint, enabled = !isDeleting) {
                Text("Stampa")
            }
            TextButton(onClick = onDelete, enabled = !isDeleting) {
                Text("Elimina")
            }
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

private fun formatDate(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoDate // Return original string if parsing fails
    }
}
