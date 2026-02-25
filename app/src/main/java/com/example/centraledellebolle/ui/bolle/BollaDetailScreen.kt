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
import com.example.centraledellebolle.data.BollaDetail
import com.example.centraledellebolle.data.Riga
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BollaDetailScreen(vm: BollaDetailViewModel, onNavigateBack: () -> Unit) {
    val bollaDetailState by vm.bollaDetailState.collectAsState()
    val printingState by vm.printingState.collectAsState()
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        Column(modifier = Modifier.padding(it).padding(16.dp)) {
            IconButton(onClick = onNavigateBack) {
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
                        onPrint = { vm.printBolla() }
                    )
                    is BollaDetailUiState.Error -> Text(text = "Errore: ${state.message}")
                }
            }
        }
    }
}

@Composable
fun BollaDetailContent(bolla: BollaDetail, onPrint: () -> Unit) {
    Column {
        Text(text = bolla.clienteNome, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Data: ${formatDate(bolla.data)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${bolla.tipoDocumentoNome} n. ${bolla.numero}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        RigheTable(righe = bolla.righe)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { /* Handle edit */ }) {
                Text("Modifica")
            }
            TextButton(onClick = onPrint) {
                Text("Stampa")
            }
            TextButton(onClick = { /* Handle delete */ }) {
                Text("Elimina")
            }
        }
    }
}

@Composable
fun RigheTable(righe: List<Riga>) {
    Column {
        // Header della tabella
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Codice", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)
            Text("Descrizione", modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Bold)
            Text("Quantità", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)
        }
        // Righe della tabella
        LazyColumn {
            items(righe) { riga ->
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
