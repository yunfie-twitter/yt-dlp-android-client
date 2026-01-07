package org.yunfie.ytdlpclient.data

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface YtDlpApi {
    @POST("info")
    suspend fun getVideoInfo(@Body request: InfoRequest): VideoInfo

    @POST("download/start")
    suspend fun startDownload(@Body request: VideoRequest): TaskStartResponse

    @GET("task/{taskId}")
    suspend fun getTaskStatus(@Path("taskId") taskId: String): TaskStatus

    @POST("download/cancel/{taskId}")
    suspend fun cancelDownload(@Path("taskId") taskId: String)

    // Add endpoint to download the file from server
    // Assumes server exposes files at /files/{filename}
    // We use @Streaming to handle large files without loading into memory
    @Streaming
    @GET("files/{filename}")
    suspend fun downloadFile(@Path("filename") filename: String): ResponseBody
}
