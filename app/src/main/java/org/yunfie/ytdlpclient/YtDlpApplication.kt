package org.yunfie.ytdlpclient

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.yunfie.ytdlpclient.data.YtDlpApi
import org.yunfie.ytdlpclient.data.repository.HistoryRepository
import org.yunfie.ytdlpclient.data.repository.SettingsRepository
import org.yunfie.ytdlpclient.data.room.AppDatabase
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class YtDlpApplication : Application() {
    
    // Repository instances
    lateinit var settingsRepository: SettingsRepository
    lateinit var historyRepository: HistoryRepository
    
    // Database instance
    lateinit var database: AppDatabase

    // Setup JSON configuration
    private val networkJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ytdlp-database"
        ).build()

        historyRepository = HistoryRepository(database.historyDao())
    }

    fun createApi(baseUrl: String): YtDlpApi {
        // Ensure URL ends with /
        val validUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(validUrl)
            .client(client)
            .addConverterFactory(networkJson.asConverterFactory(contentType))
            .build()
            .create(YtDlpApi::class.java)
    }
}
