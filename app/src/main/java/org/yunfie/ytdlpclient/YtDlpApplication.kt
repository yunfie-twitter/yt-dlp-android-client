package org.yunfie.ytdlpclient

import android.app.Application
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.yunfie.ytdlpclient.data.SettingsRepository
import org.yunfie.ytdlpclient.data.YtDlpApi
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class YtDlpApplication : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }

    // Factory for creating API client with dynamic base URL
    fun createApi(baseUrl: String): YtDlpApi {
        // Ensure trailing slash
        val validUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val json = Json { ignoreUnknownKeys = true }
        
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(validUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YtDlpApi::class.java)
    }
}
