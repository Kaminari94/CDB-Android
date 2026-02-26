package com.example.centraledellebolle.ui.stockmove

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StockMoveScreen(vm: StockMoveViewModel) {

    val uiState by vm.uiState.collectAsState()
    var articles by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var moveType by remember { mutableStateOf(MoveType.CARICO) }

    if (uiState is StockMoveUiState.Success) {
        articles = ""
        numero = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (uiState !is StockMoveUiState.Success) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { moveType = MoveType.CARICO },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (moveType == MoveType.CARICO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Carico")
                    }
                    Button(
                        onClick = { moveType = MoveType.RESO },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (moveType == MoveType.RESO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Reso")
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (moveType == MoveType.CARICO) {
                    OutlinedTextField(
                        value = numero,
                        onValueChange = { numero = it },
                        label = { Text("Numero") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is StockMoveUiState.Loading
                    )
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = articles,
                    onValueChange = { articles = it },
                    label = { Text("Articoli (CODICE QUANTITÀ)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("128 2\nPROVA 10\n...") },
                    enabled = uiState !is StockMoveUiState.Loading
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is StockMoveUiState.Loading -> CircularProgressIndicator()
                is StockMoveUiState.Success -> {
                    SuccessView(movId = state.movId) { vm.resetState() }
                }
                is StockMoveUiState.Error -> {
                    ErrorView(state.message) { vm.performStockMove(moveType, numero, articles) }
                }
                is StockMoveUiState.Idle -> {
                    Text("Pronto per un movimento di magazzino.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (uiState !is StockMoveUiState.Success) {
            Button(
                onClick = { vm.performStockMove(moveType, numero, articles) },
                modifier = Modifier.fillMaxWidth(),
                enabled = articles.isNotBlank() && uiState !is StockMoveUiState.Loading
            ) {
                Text("Invia")
            }
        }
    }
}

@Composable
private fun SuccessView(movId: Int?, onDismiss: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Successo!", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00C853))
        Text("Movimento di magazzino registrato.")
        movId?.let {
            Text("ID Movimento: $it")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDismiss) {
            Text("Esegui un altro movimento")
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Errore", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Riprova")
        }
    }
}

enum class MoveType {
    CARICO,
    RESO
}
