package org.yunfie.ytdlpclient.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val API_URL = stringPreferencesKey("api_url")
        val LANGUAGE = stringPreferencesKey("language") // ja, en
    }

    val apiUrl: Flow<String?> = context.dataStore.data.map { it[API_URL] }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "ja" }

    suspend fun saveApiUrl(url: String) {
        context.dataStore.edit { it[API_URL] = url }
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }
}
