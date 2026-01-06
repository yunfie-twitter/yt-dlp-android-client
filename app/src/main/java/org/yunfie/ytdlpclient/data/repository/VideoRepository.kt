package org.yunfie.ytdlpclient.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.yunfie.ytdlpclient.data.model.DownloadRequest
import org.yunfie.ytdlpclient.data.model.DownloadResponse
import org.yunfie.ytdlpclient.data.model.InfoRequest
import org.yunfie.ytdlpclient.data.model.TaskStatus
import org.yunfie.ytdlpclient.data.model.VideoInfo
import org.yunfie.ytdlpclient.data.remote.YtDlpApiService

class VideoRepository(private val apiService: YtDlpApiService) {
    suspend fun getVideoInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            val info = apiService.getVideoInfo(InfoRequest(url))
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startDownload(url: String, quality: Int? = null, audioFormat: String? = null): Result<DownloadResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.startDownload(
                    DownloadRequest(url, quality, audioFormat)
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getTaskStatus(taskId: String): Result<TaskStatus> = withContext(Dispatchers.IO) {
        try {
            val status = apiService.getTaskStatus(taskId)
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelDownload(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.cancelDownload(taskId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
