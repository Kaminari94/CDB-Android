package com.example.centraledellebolle.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.AuthRepository
import com.example.centraledellebolle.data.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            val result = authRepository.login(username, password)
            result.fold(
                onSuccess = { response ->
                    // Salva i token nel DataStore
                    tokenStore.saveTokens(response.access, response.refresh)
                    _loginState.value = LoginUiState.Success
                },
                onFailure = {
                    _loginState.value = LoginUiState.Error(it.message ?: "Errore sconosciuto")
                }
            )
        }
    }
}