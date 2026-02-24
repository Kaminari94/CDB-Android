package com.example.centraledellebolle.ui.health

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HealthScreen(vm: HealthViewModel) {
    val health by vm.health.collectAsState()

    LaunchedEffect(Unit) {
        vm.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Connessione al Server:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        when (val s = health) {
            is HealthUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Controllo in corso...")
            }

            is HealthUiState.Ok -> {
                Text("OK ✅")
            }

            is HealthUiState.Error -> {
                Text("ERRORE ❌")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.retry() }) { Text("Riprova") }
            }

            is HealthUiState.Idle -> {
                Text("—") // oppure "Non ancora controllato"
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.load() }) { Text("Controlla") }
            }
        }
    }
}