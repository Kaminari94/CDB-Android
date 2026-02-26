package com.example.centraledellebolle.data

import com.example.centraledellebolle.network.BaseUrlResolver
import com.example.centraledellebolle.printing.BluetoothPrinterService

interface AppContainer {
    val authRepository: AuthRepository
    val bolleRepository: BolleRepository
    val quickBollaRepository: QuickBollaRepository
    val customersRepository: CustomersRepository
    val healthRepository: HealthRepository
    val stockMoveRepository: StockMoveRepository
    val tokenStore: TokenStore
    val userPreferencesRepository: UserPreferencesRepository
    val baseUrlResolver: BaseUrlResolver
    val bluetoothPrinterService: BluetoothPrinterService
}
