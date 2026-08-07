package com.example.centraledellebolle.ui.stockmove

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val STOCK_MOVE_TEMPLATE = """
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
027110 0
027110/S 0
027110/R 0
""".trimIndent()

@Composable
fun StockMoveScreen(vm: StockMoveViewModel) {

    val uiState by vm.uiState.collectAsState()
    var articles by remember { mutableStateOf(STOCK_MOVE_TEMPLATE) }
    var numero by remember { mutableStateOf("") }
    var moveType by remember { mutableStateOf(MoveType.CARICO) }

    if (uiState is StockMoveUiState.Success) {
        articles = STOCK_MOVE_TEMPLATE
        numero = ""
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            label = { Text("Numero Documento") },
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
                        ErrorView(state.message) { 
                            vm.performStockMove(moveType, numero, filterLines(articles, moveType)) 
                        }
                    }
                    is StockMoveUiState.Idle -> {
                        Text("Pronto per un movimento di magazzino.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (uiState !is StockMoveUiState.Success) {
                Button(
                    onClick = { 
                        vm.performStockMove(moveType, numero, filterLines(articles, moveType)) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = articles.isNotBlank() && uiState !is StockMoveUiState.Loading
                ) {
                    Text("Invia")
                }
            }
        }
    }
}

/**
 * Filtra le righe inserite dall'utente:
 * 1. Toglie commenti (#) e righe vuote
 * 2. Toglie righe con quantità 0
 * 3. Se CARICO: Assicura 3 valori (Codice, Qty, Lotto). Se manca Lotto, mette oggi + 6gg (DDMMYY).
 * 4. Se RESO: Assicura 2 valori (Codice, Qty).
 */
private fun filterLines(input: String, moveType: MoveType): String {
    val autoLotto = LocalDate.now().plusDays(6).format(DateTimeFormatter.ofPattern("ddMMyy"))
    
    return input.lines()
        .map { it.trim() }
        .filter { line ->
            // Salta righe vuote o commenti
            if (line.isEmpty() || line.startsWith("#")) return@filter false
            
            val parts = line.split(Regex("\\s+"))
            if (parts.size >= 2) {
                val qty = parts[1].toIntOrNull()
                // Salta se quantità è 0 o non valida
                qty != null && qty != 0
            } else {
                false
            }
        }
        .map { line ->
            val parts = line.split(Regex("\\s+")).toMutableList()
            if (moveType == MoveType.CARICO) {
                // Carico vuole 3 parametri: CODICE QUANTITA LOTTO
                if (parts.size == 2) {
                    parts.add(autoLotto)
                } else if (parts.size >= 3 && (parts[2].isBlank() || parts[2] == "---")) {
                    parts[2] = autoLotto
                }
                parts.take(3).joinToString(" ")
            } else {
                // Reso vuole 2 parametri: CODICE QUANTITA
                parts.take(2).joinToString(" ")
            }
        }
        .joinToString("\n")
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
