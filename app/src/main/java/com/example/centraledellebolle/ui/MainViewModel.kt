package com.example.centraledellebolle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.TokenStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val tokenStore: TokenStore) : ViewModel() {

    /**
     * Espone un Flow che emette `true` se l'utente è loggato (token presente), `false` altrimenti.
     * `stateIn` converte il Flow in uno StateFlow, che mantiene l'ultimo valore.
     */
    val isLoggedIn: StateFlow<Boolean> = tokenStore.accessToken
        .map { !it.isNullOrBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Valore iniziale, assunto non loggato
        )

    /**
     * Esegue il logout cancellando i token dal DataStore.
     */
    fun logout() {
        viewModelScope.launch {
            tokenStore.clear()
        }
    }
}