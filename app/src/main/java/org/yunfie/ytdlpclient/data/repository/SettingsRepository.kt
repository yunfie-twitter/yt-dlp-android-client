package org.yunfie.ytdlpclient.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.yunfie.ytdlpclient.dataStore

class SettingsRepository(private val context: Context) {
    companion object {
        private val API_BASE_URL = stringPreferencesKey("api_base_url")
        private val LANGUAGE = stringPreferencesKey("language")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    val apiBaseUrl: Flow<String?> = context.dataStore.data.map { it[API_BASE_URL] }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "ja" }
    val darkMode: Flow<Boolean?> = context.dataStore.data.map { it[DARK_MODE] }
    val setupCompleted: Flow<Boolean> = context.dataStore.data.map { it[SETUP_COMPLETED] ?: false }

    suspend fun setApiBaseUrl(url: String) {
        context.dataStore.edit { it[API_BASE_URL] = url }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { it[SETUP_COMPLETED] = completed }
    }
}
