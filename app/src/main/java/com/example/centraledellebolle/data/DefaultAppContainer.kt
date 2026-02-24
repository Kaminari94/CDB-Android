package com.example.centraledellebolle.data

import android.content.Context
import com.example.centraledellebolle.network.ApiService
import com.example.centraledellebolle.network.RetrofitInstance

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val retrofitInstance: RetrofitInstance by lazy {
        RetrofitInstance(tokenStore)
    }

    private val apiService: ApiService by lazy {
        retrofitInstance.api
    }

    override val tokenStore: TokenStore by lazy {
        TokenStore(context)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(apiService)
    }

    override val bolleRepository: BolleRepository by lazy {
        BolleRepository(apiService)
    }

    override val quickBollaRepository: QuickBollaRepository by lazy {
        QuickBollaRepository(apiService)
    }

    override val customersRepository: CustomersRepository by lazy {
        CustomersRepository(apiService)
    }

    override val healthRepository: HealthRepository by lazy {
        HealthRepository(apiService)
    }
}