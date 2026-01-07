package org.yunfie.ytdlpclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.yunfie.ytdlpclient.data.VideoRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedApiUrl by viewModel.apiUrl.collectAsState()
    
    // Initial Setup Dialog
    if (uiState.isSetupRequired) {
        var tempUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { }, // Force setup
            title = { Text("Setup API URL") },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("API Base URL (e.g. http://192.168.1.100:8000)") }
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.saveApiUrl(tempUrl) }) {
                    Text("Save")
                }
            }
        )
    }

    if (uiState.showSettings) {
        var tempUrl by remember { mutableStateOf(savedApiUrl ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.toggleSettings() },
            title = { Text("Settings") },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("API Base URL") }
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.saveApiUrl(tempUrl) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleSettings() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("yt-dlp Client") },
                actions = {
                    IconButton(onClick = { viewModel.toggleSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        HomeScreen(
            modifier = Modifier.padding(innerPadding),
            viewModel = viewModel
        )
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.urlInput,
            onValueChange = { viewModel.onUrlChanged(it) },
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        )

        Button(
            onClick = { viewModel.fetchInfo() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && uiState.urlInput.isNotBlank()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Fetch Info")
            }
        }

        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        uiState.videoInfo?.let { info ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = info.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AsyncImage(
                        model = info.thumbnail,
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                viewModel.startDownload(
                                    VideoRequest(url = uiState.urlInput, format = "best")
                                ) 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Video")
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                viewModel.startDownload(
                                    VideoRequest(url = uiState.urlInput, audioOnly = true)
                                ) 
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Audio")
                        }
                    }
                }
            }
        }
        
        uiState.taskStatus?.let { status ->
             Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Status: ${status.status}", style = MaterialTheme.typography.labelLarge)
                    
                    status.progress?.let { progress ->
                        LinearProgressIndicator(
                            progress = { (progress / 100).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                        Text("${String.format("%.1f", progress)}% - ${status.speed ?: "N/A"}")
                        Text("ETA: ${status.eta ?: "N/A"}")
                    }
                    
                    if (status.status == "downloading" || status.status == "processing") {
                         Button(
                            onClick = { viewModel.cancelDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
