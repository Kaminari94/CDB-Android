package com.example.centraledellebolle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.centraledellebolle.data.AppContainer
import com.example.centraledellebolle.ui.MainViewModel
import com.example.centraledellebolle.ui.MainViewModelFactory
import com.example.centraledellebolle.ui.bolle.BolleScreen
import com.example.centraledellebolle.ui.bolle.BolleViewModel
import com.example.centraledellebolle.ui.bolle.BolleViewModelFactory
import com.example.centraledellebolle.ui.health.HealthScreen
import com.example.centraledellebolle.ui.health.HealthViewModel
import com.example.centraledellebolle.ui.health.HealthViewModelFactory
import com.example.centraledellebolle.ui.login.LoginScreen
import com.example.centraledellebolle.ui.login.LoginViewModel
import com.example.centraledellebolle.ui.login.LoginViewModelFactory
import com.example.centraledellebolle.ui.navigation.Screen
import com.example.centraledellebolle.ui.navigation.bottomNavItems
import com.example.centraledellebolle.ui.quickbolla.QuickBollaScreen
import com.example.centraledellebolle.ui.quickbolla.QuickBollaViewModel
import com.example.centraledellebolle.ui.quickbolla.QuickBollaViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appContainer = (application as BolleApplication).container
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(appContainer.tokenStore))
            AppNavigation(mainViewModel = mainViewModel)
        }
    }
}

@Composable
fun AppNavigation(mainViewModel: MainViewModel) {
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = if (isLoggedIn) "main" else "login") {
        composable("login") {
            val appContainer = (LocalContext.current.applicationContext as BolleApplication).container
            val loginVm: LoginViewModel = viewModel(factory = LoginViewModelFactory(appContainer.authRepository, appContainer.tokenStore))
            LoginScreen(vm = loginVm) {
                navController.navigate("main") { popUpTo("login") { inclusive = true } }
            }
        }
        composable("main") {
            val onLogout = {
                mainViewModel.logout()
                navController.navigate("login") { popUpTo("main") { inclusive = true } }
            }
            MainScreen(onLogout = onLogout)
        }
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) {
        AppNavHost(navController = navController, onLogout = onLogout, modifier = Modifier.padding(it))
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    val appContainer = (LocalContext.current.applicationContext as BolleApplication).container

    NavHost(navController = navController, startDestination = Screen.Bolle.route, modifier = modifier) {
        composable(Screen.Bolle.route) {
            val vm: BolleViewModel = viewModel(factory = BolleViewModelFactory(appContainer.bolleRepository))
            BolleScreen(vm = vm)
        }
        composable(Screen.QuickBolla.route) {
            val vm: QuickBollaViewModel = viewModel(
                factory = QuickBollaViewModelFactory(appContainer.quickBollaRepository, appContainer.customersRepository)
            )
            QuickBollaScreen(vm = vm, onBollaCreated = { navController.navigate(Screen.Bolle.route) })
        }
        composable(Screen.Health.route) {
            val vm: HealthViewModel = viewModel(factory = HealthViewModelFactory(appContainer.healthRepository))
            HealthScreen(vm = vm)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onLogout = onLogout)
        }
    }
}

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onLogout) {
            Text("Logout")
        }
    }
}
