package com.example.centraledellebolle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.centraledellebolle.data.AuthRepository
import com.example.centraledellebolle.data.BolleRepository
import com.example.centraledellebolle.network.RetrofitInstance
import com.example.centraledellebolle.ui.bolle.BolleScreen
import com.example.centraledellebolle.ui.bolle.BolleViewModel
import com.example.centraledellebolle.ui.bolle.BolleViewModelFactory
import com.example.centraledellebolle.ui.login.LoginScreen
import com.example.centraledellebolle.ui.login.LoginViewModel
import com.example.centraledellebolle.ui.login.LoginViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val authRepo = AuthRepository(RetrofitInstance.api)
            val factory = LoginViewModelFactory(authRepo)
            val loginVm: LoginViewModel = viewModel(factory = factory)

            LoginScreen(
                vm = loginVm,
                onLoginSuccess = {
                    navController.navigate("bolle") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("bolle") {
            val bolleRepo = BolleRepository(RetrofitInstance.api)
            val factory = BolleViewModelFactory(bolleRepo)
            val bolleVm: BolleViewModel = viewModel(factory = factory)
            BolleScreen(vm = bolleVm)
        }
    }
}