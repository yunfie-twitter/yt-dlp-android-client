package org.yunfie.ytdlpclient.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DownloadHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
