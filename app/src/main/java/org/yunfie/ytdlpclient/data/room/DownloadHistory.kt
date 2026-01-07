package org.yunfie.ytdlpclient.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val uploader: String,
    val url: String,
    val thumbnail: String?,
    val timestamp: Long,
    val status: String, // "completed", "failed", "downloading"
    val filePath: String? = null,
    val isAudio: Boolean = false
)
