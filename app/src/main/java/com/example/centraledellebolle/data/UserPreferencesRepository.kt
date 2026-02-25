package com.example.centraledellebolle.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val wifiBaseUrl: Flow<String?> = dataStore.data.map {
        it[WIFI_BASE_URL]
    }

    val fallbackBaseUrl: Flow<String?> = dataStore.data.map {
        it[FALLBACK_BASE_URL]
    }

    val printerMac: Flow<String?> = dataStore.data.map {
        it[PRINTER_MAC]
    }

    val printerName: Flow<String?> = dataStore.data.map {
        it[PRINTER_NAME]
    }

    suspend fun saveBaseUrls(wifiUrl: String, fallbackUrl: String) {
        dataStore.edit {
            it[WIFI_BASE_URL] = wifiUrl
            it[FALLBACK_BASE_URL] = fallbackUrl
        }
    }

    suspend fun savePrinter(name: String, mac: String) {
        dataStore.edit {
            it[PRINTER_NAME] = name
            it[PRINTER_MAC] = mac
        }
    }

    private companion object {
        val WIFI_BASE_URL = stringPreferencesKey("wifi_base_url")
        val FALLBACK_BASE_URL = stringPreferencesKey("fallback_base_url")
        val PRINTER_MAC = stringPreferencesKey("printer_mac")
        val PRINTER_NAME = stringPreferencesKey("printer_name")
    }
}