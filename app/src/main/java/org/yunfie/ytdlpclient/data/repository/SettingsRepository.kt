package org.yunfie.ytdlpclient.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.yunfie.ytdlpclient.dataStore

class SettingsRepository(private val context: Context) {
    companion object {
        private val API_URL_KEY = stringPreferencesKey("api_url")
    }

    val apiUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[API_URL_KEY]
        }

    suspend fun saveApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[API_URL_KEY] = url
        }
    }
}
