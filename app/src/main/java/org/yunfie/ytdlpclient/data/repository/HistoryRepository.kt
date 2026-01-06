package org.yunfie.ytdlpclient.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.yunfie.ytdlpclient.data.model.HistoryItem
import org.yunfie.ytdlpclient.dataStore

class HistoryRepository(private val context: Context) {
    companion object {
        private val HISTORY_KEY = stringPreferencesKey("download_history")
        private const val MAX_HISTORY = 20
        private val json = Json { ignoreUnknownKeys = true }
    }

    val history: Flow<List<HistoryItem>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[HISTORY_KEY] ?: return@map emptyList()
        try {
            json.decodeFromString<List<HistoryItem>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addHistoryItem(item: HistoryItem) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[HISTORY_KEY]
            val currentList = currentJson?.let {
                try {
                    json.decodeFromString<List<HistoryItem>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            val updatedList = (listOf(item) + currentList.filter { it.url != item.url })
                .take(MAX_HISTORY)

            preferences[HISTORY_KEY] = json.encodeToString(updatedList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it.remove(HISTORY_KEY) }
    }

    suspend fun deleteHistoryItem(url: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[HISTORY_KEY]
            val currentList = currentJson?.let {
                try {
                    json.decodeFromString<List<HistoryItem>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            val updatedList = currentList.filter { it.url != url }
            preferences[HISTORY_KEY] = json.encodeToString(updatedList)
        }
    }
}
