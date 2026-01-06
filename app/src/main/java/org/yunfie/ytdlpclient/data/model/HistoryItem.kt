package org.yunfie.ytdlpclient.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryItem(
    val url: String,
    val title: String,
    val thumbnail: String?,
    val uploader: String?,
    val timestamp: Long
)
