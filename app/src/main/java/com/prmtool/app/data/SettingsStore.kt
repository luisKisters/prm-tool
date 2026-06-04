package com.prmtool.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val apiBaseUrlKey = stringPreferencesKey("api_base_url")
    private val apiTokenKey = stringPreferencesKey("api_token")

    /** Base URL of the Vercel backend, e.g. https://prm.example.com (no trailing /api). */
    val apiBaseUrl: Flow<String> =
        context.dataStore.data.map { it[apiBaseUrlKey].orEmpty() }

    /** Shared secret sent as `Authorization: Bearer <token>` (APP_API_SECRET). */
    val apiToken: Flow<String> =
        context.dataStore.data.map { it[apiTokenKey].orEmpty() }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[apiBaseUrlKey] = url.trim() }
    }

    suspend fun setApiToken(token: String) {
        context.dataStore.edit { it[apiTokenKey] = token.trim() }
    }
}
