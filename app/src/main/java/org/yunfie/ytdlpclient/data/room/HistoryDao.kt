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
    suspend fun insert(history: DownloadHistory): Long // Returns rowId

    @Delete
    suspend fun delete(history: DownloadHistory)
    
    @Query("UPDATE download_history SET status = :status, filePath = :filePath WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, filePath: String?)

    @Query("UPDATE download_history SET status = :status WHERE id = :id")
    suspend fun updateStatusOnly(id: Long, status: String)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
