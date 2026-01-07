package org.yunfie.ytdlpclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.yunfie.ytdlpclient.data.VideoRequest

@Composable
fun AppContent(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isSetupRequired) {
        SetupDialog(onUrlSaved = viewModel::saveApiUrl)
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("yt-dlp Client") },
                    actions = {
                        IconButton(onClick = viewModel::toggleSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (uiState.showSettings) {
                    SetupDialog(
                        initialUrl = viewModel.apiUrl.collectAsState().value ?: "",
                        onUrlSaved = viewModel::saveApiUrl,
                        onDismiss = viewModel::toggleSettings
                    )
                } else {
                    HomeContent(
                        uiState = uiState,
                        onUrlChanged = viewModel::onUrlChanged,
                        onFetchInfo = viewModel::fetchInfo,
                        onStartDownload = viewModel::startDownload,
                        onCancelDownload = viewModel::cancelDownload,
                        onClearTask = viewModel::clearTask,
                        onClearError = viewModel::clearError
                    )
                }
            }
        }
    }
}

@Composable
fun SetupDialog(initialUrl: String = "", onUrlSaved: (String) -> Unit, onDismiss: (() -> Unit)? = null) {
    var url by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text("Server Setup") },
        text = {
            Column {
                Text("Enter API Base URL:")
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = { Text("http://192.168.1.10:8000") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onUrlSaved(url) }) {
                Text("Save")
            }
        },
        dismissButton = if (onDismiss != null) {
            { TextButton(onClick = onDismiss) { Text("Cancel") } }
        } else null
    )
}

@Composable
fun HomeContent(
    uiState: MainUiState,
    onUrlChanged: (String) -> Unit,
    onFetchInfo: () -> Unit,
    onStartDownload: (VideoRequest) -> Unit,
    onCancelDownload: () -> Unit,
    onClearTask: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.urlInput,
            onValueChange = onUrlChanged,
            label = { Text("Video URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = onFetchInfo,
            enabled = !uiState.isLoading,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Get Info")
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        uiState.error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = it, color = MaterialTheme.colorScheme.onErrorContainer)
                    IconButton(onClick = onClearError) {
                        Icon(Icons.Default.Close, "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        uiState.videoInfo?.let { info ->
            VideoInfoCard(info, onStartDownload)
        }

        uiState.taskStatus?.let { status ->
            TaskStatusCard(status, onCancelDownload, onClearTask)
        }
    }
}

@Composable
fun VideoInfoCard(info: org.yunfie.ytdlpclient.data.VideoInfo, onDownload: (VideoRequest) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            info.thumbnail?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 8.dp)
                )
            }
            Text(text = info.title, style = MaterialTheme.typography.headlineSmall)
            Text(text = info.uploader ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onDownload(VideoRequest(url = info.webpageUrl, quality = 1080)) }) {
                    Text("Video (Best)")
                }
                OutlinedButton(onClick = { 
                    onDownload(VideoRequest(url = info.webpageUrl, audioOnly = true, audioFormat = "mp3")) 
                }) {
                    Text("Audio (MP3)")
                }
            }
        }
    }
}

@Composable
fun TaskStatusCard(status: org.yunfie.ytdlpclient.data.TaskStatus, onCancel: () -> Unit, onClear: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Task Status: ${status.status}", style = MaterialTheme.typography.titleMedium)
            
            val progress = status.progress?.toFloat()?.div(100f) ?: 0f
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${status.progress}%")
                Text(status.speed ?: "")
                Text(status.eta ?: "")
            }
            
            Text(status.message ?: "")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (status.status == "processing" || status.status == "downloading" || status.status == "queued") {
                    Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Cancel")
                    }
                } else {
                     Button(onClick = onClear) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
