package org.yunfie.ytdlpclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.yunfie.ytdlpclient.viewmodel.DownloadsViewModel
import org.yunfie.ytdlpclient.viewmodel.HomeViewModel
import org.yunfie.ytdlpclient.viewmodel.SettingsViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    onNavigateToDownloads: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("YouTube URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.fetchVideoInfo(url) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fetch Info")
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
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.downloadVideo(url) }) {
                        Text("Download Video")
                    }
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        uiState.error?.let { error ->
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = viewModel(factory = DownloadsViewModel.Factory)
) {
    // Implementation for Downloads screen
    Text("Downloads Screen")
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    // Implementation for Settings screen
    Text("Settings Screen")
}
