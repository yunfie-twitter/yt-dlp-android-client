package org.yunfie.ytdlpclient.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.yunfie.ytdlpclient.dataStore

class SettingsRepository(private val context: Context) {
    companion object {
        private val KEY_API_URL = stringPreferencesKey("api_url")
        private val KEY_DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
    }

    val apiUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_API_URL]
        }
        
    val downloadLocation: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DOWNLOAD_LOCATION]
        }

    suspend fun saveApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_API_URL] = url
        }
    }
    
    suspend fun saveDownloadLocation(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DOWNLOAD_LOCATION] = uri
        }
    }
}
