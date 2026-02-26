package com.example.centraledellebolle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.centraledellebolle.data.AppContainer
import com.example.centraledellebolle.ui.MainViewModel
import com.example.centraledellebolle.ui.MainViewModelFactory
import com.example.centraledellebolle.ui.bolle.BollaDetailScreen
import com.example.centraledellebolle.ui.bolle.BollaDetailViewModel
import com.example.centraledellebolle.ui.bolle.BollaDetailViewModelFactory
import com.example.centraledellebolle.ui.bolle.BolleScreen
import com.example.centraledellebolle.ui.bolle.BolleViewModel
import com.example.centraledellebolle.ui.bolle.BolleViewModelFactory
import com.example.centraledellebolle.ui.login.LoginScreen
import com.example.centraledellebolle.ui.login.LoginViewModel
import com.example.centraledellebolle.ui.login.LoginViewModelFactory
import com.example.centraledellebolle.ui.navigation.Screen
import com.example.centraledellebolle.ui.navigation.bottomNavItems
import com.example.centraledellebolle.ui.quickbolla.QuickBollaScreen
import com.example.centraledellebolle.ui.quickbolla.QuickBollaViewModel
import com.example.centraledellebolle.ui.quickbolla.QuickBollaViewModelFactory
import com.example.centraledellebolle.ui.settings.SettingsScreen
import com.example.centraledellebolle.ui.settings.SettingsViewModel
import com.example.centraledellebolle.ui.settings.SettingsViewModelFactory
import com.example.centraledellebolle.ui.stockmove.StockMoveScreen
import com.example.centraledellebolle.ui.stockmove.StockMoveViewModel
import com.example.centraledellebolle.ui.stockmove.StockMoveViewModelFactory

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
            val vm: BolleViewModel = viewModel(factory = BolleViewModelFactory(appContainer.bolleRepository, appContainer.userPreferencesRepository))
            BolleScreen(vm = vm, onNavigateToDetail = { bollaId ->
                navController.navigate("bolla_detail/$bollaId")
            })
        }
        composable(
            route = "bolla_detail/{bollaId}",
            arguments = listOf(navArgument("bollaId") { type = NavType.IntType })
        ) {
            val bollaId = it.arguments?.getInt("bollaId") ?: 0
            val vm: BollaDetailViewModel = viewModel(
                factory = BollaDetailViewModelFactory(appContainer.bolleRepository, appContainer.userPreferencesRepository, bollaId)
            )
            BollaDetailScreen(vm = vm, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.QuickBolla.route) {
            val vm: QuickBollaViewModel = viewModel(
                factory = QuickBollaViewModelFactory(appContainer.quickBollaRepository, appContainer.customersRepository)
            )
            QuickBollaScreen(vm = vm, onBollaCreated = { navController.navigate(Screen.Bolle.route) })
        }
        composable(Screen.StockMove.route) {
            val vm: StockMoveViewModel = viewModel(factory = StockMoveViewModelFactory(appContainer.stockMoveRepository))
            StockMoveScreen(vm = vm)
        }
        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(
                    appContainer.userPreferencesRepository,
                    appContainer.healthRepository,
                    appContainer.baseUrlResolver,
                    appContainer.bluetoothPrinterService
                )
            )
            SettingsScreen(viewModel = vm, onLogout = onLogout)
        }
    }
}
