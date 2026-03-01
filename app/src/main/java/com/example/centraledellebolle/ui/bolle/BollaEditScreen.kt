package com.example.centraledellebolle.ui.bolle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.centraledellebolle.data.QuickLineError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BollaEditScreen(
    vm: BollaEditViewModel,
    bollaId: Int,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(bollaId) {
        vm.load(bollaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Modifica Bolla") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            })
        },
        bottomBar = {
            val currentState = uiState
            if (currentState is BollaEditUiState.Ready) {
                Button(
                    onClick = { vm.save(bollaId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !currentState.isSaving
                ) {
                    if (currentState.isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text("SALVA MODIFICHE")
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is BollaEditUiState.Loading -> CircularProgressIndicator(modifier = Modifier.fillMaxSize())
            is BollaEditUiState.Ready -> BollaEditContent(state, vm, Modifier.padding(padding))
            is BollaEditUiState.Error -> Text(text = state.message, modifier = Modifier.padding(padding))
            is BollaEditUiState.Saved -> {
                // Handled in LaunchedEffect
            }
            is BollaEditUiState.ValidationError -> {
                ValidationErrorContent(errors = state.errors, message = state.message)
            }
        }
    }

    val currentState = uiState
    if (currentState is BollaEditUiState.Saved) {
        LaunchedEffect(currentState) {
            onSaved()
        }
    }
}

@Composable
fun ValidationErrorContent(errors: List<QuickLineError>?, message: String?) {
    Card(modifier = Modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            message?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            errors?.let {
                LazyColumn {
                    items(it) { error ->
                        Text("Riga ${error.line}: ${error.message}")
                    }
                }
            }
        }
    }
}

@Composable
fun BollaEditContent(state: BollaEditUiState.Ready, vm: BollaEditViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        // Header
        Text(text = state.bolla.clienteNome, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Data: ${state.bolla.data}")
        Text(text = "Tipo: ${state.bolla.tipoDocumentoNome}")
        state.bolla.numero?.let { Text(text = "Numero: $it") }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { vm.addRow() }) {
            Text("Aggiungi riga")
        }

        // Table
        LazyColumn {
            itemsIndexed(state.rows) { index, row ->
                EditableRowItem(
                    row = row,
                    onQtyChanged = { newQty -> vm.onQtyChanged(index, newQty) },
                    onRemove = { vm.removeRow(index) },
                    onCodiceChanged = { newCodice -> vm.onCodiceChanged(index, newCodice) }
                )
            }
        }
    }
}

@Composable
fun EditableRowItem(
    row: EditableRow,
    onQtyChanged: (String) -> Unit,
    onRemove: () -> Unit,
    onCodiceChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = row.codice,
                onValueChange = onCodiceChanged,
                label = { Text("Codice") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = row.qtyText,
                onValueChange = onQtyChanged,
                label = { Text("Qtà") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
        Text(
            text = row.descrizione,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
        row.lotto?.let {
            Text(
                text = "Lotto: $it",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}