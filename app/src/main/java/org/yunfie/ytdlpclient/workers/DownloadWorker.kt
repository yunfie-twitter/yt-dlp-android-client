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
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.ResponseBody
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
        
        // Fix: Use correct import for firstOrNull() instead of inline package name
        val baseUrl = app.settingsRepository.apiUrl.firstOrNull() 
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
                
                updateNotification(notificationId, title, "サーバーで処理中... ${String.format("%.1f", serverProgress)}%", displayProgress)

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
            updateNotification(notificationId, title, "端末にダウンロード中...", 50)
            
            val responseBody = api.downloadFile(serverFilename)
            val savedUri = saveFileToMediaStore(responseBody, serverFilename, audioOnly)

            updateNotification(notificationId, title, "ダウンロード完了", 100, false)
            
            Result.success(workDataOf("uri" to savedUri.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification(notificationId, "エラー", e.message ?: "Unknown error", 0, false)
            Result.failure(workDataOf("error" to e.message))
        }
    }

    private suspend fun saveFileToMediaStore(body: ResponseBody, filename: String, isAudio: Boolean): Uri? {
        val resolver = applicationContext.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, if (isAudio) "audio/mp3" else "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES)
            // For Android 10+ (Q), IS_PENDING is used while writing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (isAudio) {
             MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
             MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val uri = resolver.insert(collection, contentValues) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                body.byteStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Finish writing (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
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
            .setContentText("開始中...")
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
