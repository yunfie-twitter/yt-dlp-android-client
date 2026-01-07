package org.yunfie.ytdlpclient.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
import org.yunfie.ytdlpclient.data.room.DownloadHistory

data class MainUiState(
    val urlInput: String = "",
    val videoInfo: VideoInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTaskId: String? = null,
    val taskStatus: TaskStatus? = null,
    val isSetupRequired: Boolean = false,
    val showSettings: Boolean = false,
    val historySearchQuery: String = "",
    val availableVideoHeights: List<Int> = emptyList(),
    // New
    val isDownloading: Boolean = false, // If ANY download is running
    val activeDownloadsCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = getApplication<YtDlpApplication>().settingsRepository
    private val historyRepository = getApplication<YtDlpApplication>().historyRepository
    private val workManager = WorkManager.getInstance(application)
    private var api: YtDlpApi? = null

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    val apiUrl = settingsRepository.apiUrl.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val downloadLocation = settingsRepository.downloadLocation.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // History Flow
    private val _history = historyRepository.allHistory
    
    // Filtered History Flow
    val filteredHistory = combine(_history, _uiState) { history, state ->
        if (state.historySearchQuery.isBlank()) {
            history
        } else {
            history.filter { 
                it.title.contains(state.historySearchQuery, ignoreCase = true) ||
                it.url.contains(state.historySearchQuery, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Active Works Flow
    // We observe all works with our tag to see if any are running
    // Tag "download_work" needs to be added when enqueuing
    val activeWorks = workManager.getWorkInfosByTagFlow("download_work")
        .map { works -> works.filter { !it.state.isFinished } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        
        viewModelScope.launch {
            activeWorks.collect { works ->
                _uiState.update { 
                    it.copy(
                        isDownloading = works.isNotEmpty(),
                        activeDownloadsCount = works.size
                    )
                }
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(urlInput = newUrl) }
    }
    
    fun onHistorySearch(query: String) {
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun saveApiUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.saveApiUrl(url)
            _uiState.update { it.copy(isSetupRequired = false, showSettings = false) }
        }
    }
    
    fun saveDownloadLocation(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.saveDownloadLocation(uri.toString())
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
                
                val heights = info.formats
                    .filter { it.height != null && it.vcodec != "none" }
                    .mapNotNull { it.height }
                    .distinct()
                    .sortedDescending()
                
                _uiState.update { it.copy(
                    isLoading = false, 
                    videoInfo = info,
                    availableVideoHeights = heights
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to fetch info") }
            }
        }
    }
    
    fun deleteHistory(history: DownloadHistory) {
        viewModelScope.launch {
            historyRepository.delete(history)
        }
    }
    
    // Check if a specific history item is currently downloading
    fun isHistoryItemDownloading(history: DownloadHistory, activeWorks: List<WorkInfo>): Boolean {
        // Since we didn't save workId in DB in previous versions, fallback to matching title/url if workId is null
        // But we just added workId, so use it primarily
        if (history.workId != null) {
            return activeWorks.any { it.id.toString() == history.workId }
        }
        // Fallback (weak matching)
        return history.status == "downloading"
    }
}
