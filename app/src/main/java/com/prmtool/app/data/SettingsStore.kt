package com.prmtool.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val webhookKey = stringPreferencesKey("webhook_url")

    val webhookUrl: Flow<String> =
        context.dataStore.data.map { it[webhookKey].orEmpty() }

    suspend fun setWebhookUrl(url: String) {
        context.dataStore.edit { it[webhookKey] = url.trim() }
    }
}
