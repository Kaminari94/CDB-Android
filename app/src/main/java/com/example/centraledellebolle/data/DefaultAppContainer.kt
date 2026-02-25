package com.example.centraledellebolle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.centraledellebolle.network.ApiService
import com.example.centraledellebolle.network.AuthInterceptor
import com.example.centraledellebolle.network.BaseUrlResolver
import com.example.centraledellebolle.network.NetworkMonitor
import com.example.centraledellebolle.printing.BluetoothPrinterService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val USER_PREFERENCES_NAME = "user_preferences"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val tokenStore: TokenStore by lazy {
        TokenStore(context)
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.dataStore)
    }

    private val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    override val baseUrlResolver: BaseUrlResolver by lazy {
        BaseUrlResolver(userPreferencesRepository, networkMonitor)
    }

    override val bluetoothPrinterService: BluetoothPrinterService by lazy {
        BluetoothPrinterService(context)
    }

    private lateinit var apiService: ApiService

    init {
        val initialBaseUrl = runBlocking {
            baseUrlResolver.resolvedBaseUrl.first()
        }
        recreateApiService(initialBaseUrl)

        baseUrlResolver.resolvedBaseUrl
            .distinctUntilChanged()
            .onEach { baseUrl ->
                recreateApiService(baseUrl)
            }
            .launchIn(GlobalScope)
    }

    fun recreateApiService(baseUrl: String) {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(tokenStore)

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override val authRepository: AuthRepository
        get() = AuthRepository(apiService)

    override val bolleRepository: BolleRepository
        get() = BolleRepository(apiService, bluetoothPrinterService)

    override val quickBollaRepository: QuickBollaRepository
        get() = QuickBollaRepository(apiService)

    override val customersRepository: CustomersRepository
        get() = CustomersRepository(apiService)

    override val healthRepository: HealthRepository
        get() = HealthRepository(apiService)
}
