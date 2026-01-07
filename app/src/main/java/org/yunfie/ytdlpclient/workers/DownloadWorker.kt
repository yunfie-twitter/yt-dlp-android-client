package org.yunfie.ytdlpclient.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import okhttp3.ResponseBody
import org.yunfie.ytdlpclient.R
import org.yunfie.ytdlpclient.YtDlpApplication
import org.yunfie.ytdlpclient.data.VideoRequest
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val format = inputData.getString(KEY_FORMAT)
        val audioOnly = inputData.getBoolean(KEY_AUDIO_ONLY, false)
        val title = inputData.getString(KEY_TITLE) ?: "Downloading..."

        val notificationId = title.hashCode()
        setForeground(createForegroundInfo(notificationId, title, 0))

        val app = applicationContext as YtDlpApplication
        // We need a way to get the API. Ideally via Hilt/Koin, but here accessing manually for now.
        // Assuming base URL is saved and we can recreate or access the global one.
        // For simplicity, we assume the app has an initialized repository or we grab the saved URL.
        val baseUrl = app.settingsRepository.apiUrl.kotlinx.coroutines.flow.firstOrNull() 
            ?: return Result.failure(workDataOf("error" to "API URL not set"))
        
        val api = app.createApi(baseUrl)

        return try {
            // 1. Start Server Task
            val request = VideoRequest(url = url, format = format, audioOnly = audioOnly)
            val startResponse = api.startDownload(request)
            val taskId = startResponse.taskId

            // 2. Poll Server Status
            var serverFilename: String? = null
            var isServerDone = false
            
            while (!isServerDone) {
                val status = api.getTaskStatus(taskId)
                
                // Update notification (0-50% for server processing)
                // We map server progress 0-100 to local 0-50
                val serverProgress = status.progress ?: 0.0
                val displayProgress = (serverProgress / 2).toInt()
                
                updateNotification(notificationId, title, "Processing on server... ${String.format("%.1f", serverProgress)}%", displayProgress)

                if (status.status == "completed") {
                    serverFilename = status.filename
                    isServerDone = true
                } else if (status.status == "error") {
                    throw Exception(status.message ?: "Server error")
                }

                if (!isServerDone) delay(1000)
            }

            if (serverFilename == null) throw Exception("No filename returned")

            // 3. Download File from Server to Device
            updateNotification(notificationId, title, "Downloading to device...", 50)
            
            // Assuming the server exposes files at /files/{filename} or similar.
            // We need to extend the API for this, or assume a convention.
            // Let's assume a 'download_file' endpoint or static path relative to base.
            // Since we can't change the API interface easily here without verifying the server,
            // we'll try to download using OkHttp directly or add a method if possible.
            // For now, let's use the `api.downloadFile` which we will add.
            
            val responseBody = api.downloadFile(serverFilename)
            val savedUri = saveFileToMediaStore(responseBody, serverFilename, audioOnly)

            updateNotification(notificationId, title, "Download complete", 100)
            
            Result.success(workDataOf("uri" to savedUri.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification(notificationId, "Error", e.message ?: "Unknown error", 0, false)
            Result.failure(workDataOf("error" to e.message))
        }
    }

    private suspend fun saveFileToMediaStore(body: ResponseBody, filename: String, isAudio: Boolean): Uri? {
        val resolver = applicationContext.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, if (isAudio) "audio/mp3" else "video/mp4") // Simplified
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES)
        }

        val collection = if (isAudio) {
             MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
             MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val uri = resolver.insert(collection, contentValues) ?: return null

        resolver.openOutputStream(uri)?.use { outputStream ->
            body.byteStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return uri
    }

    private fun createForegroundInfo(id: Int, title: String, progress: Int): ForegroundInfo {
        val channelId = "download_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText("Starting...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(id, notification)
    }

    private fun updateNotification(id: Int, title: String, content: String, progress: Int, ongoing: Boolean = true) {
        val notification = NotificationCompat.Builder(applicationContext, "download_channel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(ongoing)
            .build()
        notificationManager.notify(id, notification)
    }

    companion object {
        const val KEY_URL = "key_url"
        const val KEY_FORMAT = "key_format"
        const val KEY_AUDIO_ONLY = "key_audio_only"
        const val KEY_TITLE = "key_title"
    }
}
