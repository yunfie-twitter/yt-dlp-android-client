package org.yunfie.ytdlpclient

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.yunfie.ytdlpclient.data.api.YtDlpApiService
import org.yunfie.ytdlpclient.data.repository.HistoryRepository
import org.yunfie.ytdlpclient.data.repository.SettingsRepository
import retrofit2.Retrofit

// Extension property for DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

class YtDlpApplication : Application() {
    // ... existing code ...
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(private val context: Context) {
    private val baseUrl = "http://10.0.2.2:8000" // Emulator localhost

    private val json = Json { ignoreUnknownKeys = true }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: YtDlpApiService by lazy {
        retrofit.create(YtDlpApiService::class.java)
    }

    val settingsRepository by lazy {
        SettingsRepository(context.dataStore)
    }

    val historyRepository by lazy {
        HistoryRepository(context.dataStore)
    }
}
