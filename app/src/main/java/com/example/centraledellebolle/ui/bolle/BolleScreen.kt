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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.centraledellebolle.data.Bolla
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun BolleScreen(vm: BolleViewModel) {
    BolleListScreen(vm = vm)
}

@Composable
fun BolleListScreen(vm: BolleViewModel) {
    val bolleState by vm.bolleState.collectAsState()

    // Carica i dati solo la prima volta
    LaunchedEffect(Unit) {
        if (bolleState is BolleUiState.Idle) {
            vm.loadBolle()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = bolleState) {
                is BolleUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is BolleUiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.bolle) { bolla ->
                            BollaItem(bolla = bolla)
                        }
                    }
                }

                is BolleUiState.Error -> {
                    Text(text = "Errore: ${state.message}")
                }

                is BolleUiState.Idle -> {
                    // Do nothing
                }
            }
        }
    }
}

@Composable
fun BollaItem(bolla: Bolla) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {

            Text(text = bolla.clienteNome, style = MaterialTheme.typography.titleLarge)
            Text(text = bolla.clienteVia, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(text = "Data: ${formatDate(bolla.data)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Documento: ${bolla.tipoDocumentoNome} n. ${bolla.numero}", style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { /* Handle details */ }) {
                    Text("Dettagli")
                }
                TextButton(onClick = { /* Handle print */ }) {
                    Text("Stampa")
                }
                TextButton(onClick = { /* Handle edit */ }) {
                    Text("Modifica")
                }
                TextButton(onClick = { /* Handle delete */ }) {
                    Text("Elimina")
                }
            }
        }
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val offsetDateTime = OffsetDateTime.parse(isoDate)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        offsetDateTime.format(formatter)
    } catch (e: Exception) {
        isoDate // Return original string if parsing fails
    }
}
