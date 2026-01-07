package org.yunfie.ytdlpclient.data.repository

import kotlinx.coroutines.flow.Flow
import org.yunfie.ytdlpclient.data.room.HistoryDao
import org.yunfie.ytdlpclient.data.room.DownloadHistory

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<DownloadHistory>> = historyDao.getAllHistory()

    suspend fun insert(history: DownloadHistory) {
        historyDao.insert(history)
    }

    suspend fun delete(history: DownloadHistory) {
        historyDao.delete(history)
    }

    suspend fun clearAll() {
        historyDao.clearAll()
    }
}
