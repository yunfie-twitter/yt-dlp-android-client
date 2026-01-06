package org.yunfie.ytdlpclient.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InfoRequest(
    val url: String
)

@Serializable
data class DownloadRequest(
    val url: String,
    val quality: Int? = null,
    @SerialName("audio_format") val audioFormat: String? = null
)

@Serializable
data class DownloadResponse(
    @SerialName("task_id") val taskId: String
)

@Serializable
data class TaskStatus(
    val status: String,
    val progress: Float = 0f,
    val message: String? = null,
    val filename: String? = null,
    val speed: String? = null,
    val eta: String? = null
)
