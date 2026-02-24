package com.example.centraledellebolle.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TokenStore(private val context: Context) {

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    val accessToken: Flow<String?> = context.dataStore.data.map {
        it[accessTokenKey]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map {
        it[refreshTokenKey]
    }

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit {
            it[accessTokenKey] = access
            it[refreshTokenKey] = refresh
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.clear()
        }
    }
}