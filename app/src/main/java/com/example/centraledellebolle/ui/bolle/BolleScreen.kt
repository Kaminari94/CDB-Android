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
import androidx.compose.material3.Button
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
    val bolleState by vm.bolleState.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadBolle()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { /* Handle new bolla */ }) {
            Text("Crea nuova bolla")
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(text = "Cliente: ${bolla.clienteNome}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Data: ${formatDate(bolla.data)}")
            Text(text = "Numero: ${bolla.numero}")
            Row {
                TextButton(onClick = { /* Handle details */ }) {
                    Text("Dettagli", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { /* Handle edit */ }) {
                    Text("Modifica", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { /* Handle delete */ }) {
                    Text("Elimina", style = MaterialTheme.typography.titleMedium)
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