package com.example.centraledellebolle.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector

const val LOGIN_ROUTE = "login"
const val MAIN_APP_ROUTE = "main"

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Bolle : Screen("bolle", "Bolle", Icons.Default.List)
    object QuickBolla : Screen("quickbolla", "Quick", Icons.Default.AddCircle)
    object StockMove : Screen("stockmove", "Movimenti", Icons.Default.CheckCircle)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Bolle,
    Screen.QuickBolla,
    Screen.StockMove,
    Screen.Settings
)
