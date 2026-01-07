package org.yunfie.ytdlpclient.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface YtDlpApi {
    @POST("info")
    suspend fun getVideoInfo(@Body request: InfoRequest): VideoInfo

    @POST("download/start")
    suspend fun startDownload(@Body request: VideoRequest): TaskStartResponse

    @GET("task/{taskId}")
    suspend fun getTaskStatus(@Path("taskId") taskId: String): TaskStatus

    @POST("download/cancel/{taskId}")
    suspend fun cancelDownload(@Path("taskId") taskId: String)
}
