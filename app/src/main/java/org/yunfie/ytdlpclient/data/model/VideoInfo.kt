package org.yunfie.ytdlpclient.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    val id: String? = null,
    val title: String,
    val description: String? = null,
    val duration: Int? = null,
    @SerialName("view_count") val viewCount: Long? = null,
    @SerialName("like_count") val likeCount: Long? = null,
    val uploader: String? = null,
    val channel: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    val thumbnail: String? = null,
    @SerialName("webpage_url") val webpageUrl: String,
    @SerialName("is_live") val isLive: Boolean = false,
    val formats: List<Format> = emptyList()
)

@Serializable
data class Format(
    @SerialName("format_id") val formatId: String? = null,
    val ext: String? = null,
    val height: Int? = null,
    val width: Int? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val fps: Int? = null,
    val filesize: Long? = null
)
