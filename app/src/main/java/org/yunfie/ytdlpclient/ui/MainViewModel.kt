package org.yunfie.ytdlpclient.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.yunfie.ytdlpclient.YtDlpApplication
import org.yunfie.ytdlpclient.data.InfoRequest
import org.yunfie.ytdlpclient.data.TaskStatus
import org.yunfie.ytdlpclient.data.VideoInfo
import org.yunfie.ytdlpclient.data.VideoRequest
import org.yunfie.ytdlpclient.data.YtDlpApi

data class MainUiState(
    val urlInput: String = "",
    val videoInfo: VideoInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTaskId: String? = null,
    val taskStatus: TaskStatus? = null,
    val isSetupRequired: Boolean = false,
    val showSettings: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = getApplication<YtDlpApplication>().settingsRepository
    private var api: YtDlpApi? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    val apiUrl = repository.apiUrl.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            apiUrl.collect { url ->
                if (url.isNullOrBlank()) {
                    _uiState.update { it.copy(isSetupRequired = true) }
                } else {
                    _uiState.update { it.copy(isSetupRequired = false) }
                    try {
                        api = getApplication<YtDlpApplication>().createApi(url)
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = "Invalid API URL format") }
                    }
                }
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl) }
    }

    fun saveApiUrl(url: String) {
        viewModelScope.launch {
            repository.saveApiUrl(url)
            _uiState.update { it.copy(isSetupRequired = false, showSettings = false) }
        }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun fetchInfo() {
        val url = _uiState.value.urlInput
        if (url.isBlank() || api == null) return

        _uiState.update { it.copy(isLoading = true, error = null, videoInfo = null) }

        viewModelScope.launch {
            try {
                val info = api!!.getVideoInfo(InfoRequest(url))
                _uiState.update { it.copy(isLoading = false, videoInfo = info) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to fetch info") }
            }
        }
    }

    fun startDownload(videoRequest: VideoRequest) {
        if (api == null) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val response = api!!.startDownload(videoRequest)
                val taskId = response.taskId
                _uiState.update { it.copy(isLoading = false, currentTaskId = taskId) }
                startPolling(taskId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to start download") }
            }
        }
    }

    private fun startPolling(taskId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val status = api!!.getTaskStatus(taskId)
                    _uiState.update { it.copy(taskStatus = status) }
                    
                    if (status.status == "completed" || status.status == "error") {
                        break
                    }
                } catch (e: Exception) {
                    // Ignore transient errors during polling
                }
                delay(1000)
            }
        }
    }

    fun cancelDownload() {
        val taskId = _uiState.value.currentTaskId ?: return
        if (api == null) return

        viewModelScope.launch {
            try {
                api!!.cancelDownload(taskId)
                pollingJob?.cancel()
                _uiState.update { it.copy(taskStatus = it.taskStatus?.copy(status = "cancelling")) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to cancel: ${e.message}") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearTask() {
        pollingJob?.cancel()
        _uiState.update { it.copy(currentTaskId = null, taskStatus = null) }
    }
}
