package org.yunfie.ytdlpclient.data.remote

import org.yunfie.ytdlpclient.data.model.DownloadRequest
import org.yunfie.ytdlpclient.data.model.DownloadResponse
import org.yunfie.ytdlpclient.data.model.InfoRequest
import org.yunfie.ytdlpclient.data.model.TaskStatus
import org.yunfie.ytdlpclient.data.model.VideoInfo
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface YtDlpApiService {
    @POST("info")
    suspend fun getVideoInfo(@Body request: InfoRequest): VideoInfo

    @POST("download/start")
    suspend fun startDownload(@Body request: DownloadRequest): DownloadResponse

    @GET("task/{task_id}")
    suspend fun getTaskStatus(@Path("task_id") taskId: String): TaskStatus

    @POST("download/cancel/{task_id}")
    suspend fun cancelDownload(@Path("task_id") taskId: String): Map<String, String>
}
