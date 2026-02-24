package com.example.centraledellebolle.data

interface AppContainer {
    val authRepository: AuthRepository
    val bolleRepository: BolleRepository
    val quickBollaRepository: QuickBollaRepository
    val customersRepository: CustomersRepository
    val healthRepository: HealthRepository
    val tokenStore: TokenStore
}
