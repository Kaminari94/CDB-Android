package com.example.centraledellebolle.ui.bolle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder

@Composable
fun BolleScreen(vm: BolleViewModel) {
    val bolleState by vm.bolleState.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadBolle()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = bolleState) {
            is BolleUiState.Loading -> {
                CircularProgressIndicator()
            }
            is BolleUiState.Success -> {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    item {
                        val prettyJson = GsonBuilder().setPrettyPrinting().create().toJson(state.bolle)
                        Text(text = prettyJson)
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