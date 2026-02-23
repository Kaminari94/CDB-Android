package com.example.centraledellebolle.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.centraledellebolle.data.AuthRepository
import com.example.centraledellebolle.data.TokenHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            val result = authRepository.login(username, password)
            _loginState.value = result.fold(
                onSuccess = { response ->
                    TokenHolder.token = response.access
                    LoginUiState.Success
                },
                onFailure = { LoginUiState.Error(it.message ?: "Errore sconosciuto") }
            )
        }
    }
}