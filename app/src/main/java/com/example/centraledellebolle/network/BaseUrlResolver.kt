package com.example.centraledellebolle.network

import com.example.centraledellebolle.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BaseUrlResolver(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor
) {
    val resolvedBaseUrl: Flow<String> = combine(
        userPreferencesRepository.wifiBaseUrl,
        userPreferencesRepository.fallbackBaseUrl,
        networkMonitor.isWifi
    ) { wifiUrl, fallbackUrl, isWifi ->
        if (isWifi) {
            if (wifiUrl.isNullOrBlank()) "https://192.168.1.176/" else wifiUrl
        } else {
            if (fallbackUrl.isNullOrBlank()) "https://centralebolle.duckdns.org/" else fallbackUrl
        }
    }
}
