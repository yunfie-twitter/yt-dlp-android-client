package org.yunfie.ytdlpclient.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InfoRequest(
    val url: String
)

@Serializable
data class VideoInfo(
    val id: String? = null,
    val title: String,
    val description: String? = null,
    val duration: Long? = null,
    @SerialName("view_count") val viewCount: Long? = null,
    @SerialName("like_count") val likeCount: Long? = null,
    val uploader: String? = null,
    val thumbnail: String? = null,
    @SerialName("webpage_url") val webpageUrl: String,
    val formats: List<VideoFormat> = emptyList()
)

@Serializable
data class VideoFormat(
    val format_id: String? = null,
    val ext: String? = null,
    val resolution: String? = null,
    val height: Int? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val filesize: Long? = null
)

@Serializable
data class VideoRequest(
    val url: String,
    val format: String? = null,
    @SerialName("audio_only") val audioOnly: Boolean? = false,
    @SerialName("audio_format") val audioFormat: String? = null,
    val quality: Int? = null
)

@Serializable
data class TaskStartResponse(
    @SerialName("task_id") val taskId: String
)

@Serializable
data class TaskStatus(
    val id: String? = null,
    val status: String? = null, // queued, processing, downloading, completed, error
    val progress: Double? = 0.0,
    val message: String? = null,
    val filename: String? = null,
    val speed: String? = null,
    val eta: String? = null,
    val total_bytes: Long? = null,
    val downloaded_bytes: Long? = null
)
