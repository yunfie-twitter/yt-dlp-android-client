package org.yunfie.ytdlpclient.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM download_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<DownloadHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: DownloadHistory)

    @Delete
    suspend fun delete(history: DownloadHistory)
    
    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
