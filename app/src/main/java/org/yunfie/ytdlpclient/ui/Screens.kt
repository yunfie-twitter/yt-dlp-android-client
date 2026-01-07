package org.yunfie.ytdlpclient.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.yunfie.ytdlpclient.workers.DownloadWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedApiUrl by viewModel.apiUrl.collectAsState()
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)

    // Initial Setup Dialog
    if (uiState.isSetupRequired) {
        var tempUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { },
            title = { Text("API設定") },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("APIのベースURL (例: http://192.168.1.100:8000)") }
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.saveApiUrl(tempUrl) }) {
                    Text("保存")
                }
            }
        )
    }

    if (uiState.showSettings) {
        var tempUrl by remember { mutableStateOf(savedApiUrl ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.toggleSettings() },
            title = { Text("設定") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("APIのベースURL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "バッテリーの最適化をオフにすることを推奨します。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.saveApiUrl(tempUrl) }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleSettings() }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
             // Search Bar Style Top App Bar
             DockedSearchBar(
                 query = uiState.urlInput,
                 onQueryChange = { viewModel.onUrlChanged(it) },
                 onSearch = { viewModel.fetchInfo() },
                 active = false,
                 onActiveChange = {},
                 placeholder = { Text("YouTubeのURLを入力") },
                 leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                 trailingIcon = {
                     IconButton(onClick = { viewModel.toggleSettings() }) {
                         Icon(Icons.Default.Settings, contentDescription = "設定")
                     }
                 },
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp)
             ) {}
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Loading Indicator
            if (uiState.isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            // Error Card
            uiState.error?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Video Info Card (Main Content)
            uiState.videoInfo?.let { info ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp) // Updated to Material 3 rounded styling
                    ) {
                        Column {
                            // Header with Avatar (Simulated) and Text
                            ListItem(
                                headlineContent = { Text("動画情報", fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(info.uploader ?: "Unknown Uploader") },
                                leadingContent = {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = (info.uploader?.take(1) ?: "U").uppercase(),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = { /* More options */ }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    }
                                }
                            )

                            // Media Area
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(info.thumbnail)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Thumbnail",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )

                            // Title and Description
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = info.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = info.description ?: "説明なし",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                // Action Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                                                .setInputData(workDataOf(
                                                    DownloadWorker.KEY_URL to uiState.urlInput,
                                                    DownloadWorker.KEY_FORMAT to "best",
                                                    DownloadWorker.KEY_AUDIO_ONLY to false,
                                                    DownloadWorker.KEY_TITLE to info.title
                                                ))
                                                .build()
                                            workManager.enqueue(workRequest)
                                        }
                                    ) {
                                        Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("動画")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                                                .setInputData(workDataOf(
                                                    DownloadWorker.KEY_URL to uiState.urlInput,
                                                    DownloadWorker.KEY_FORMAT to "bestaudio",
                                                    DownloadWorker.KEY_AUDIO_ONLY to true,
                                                    DownloadWorker.KEY_TITLE to info.title
                                                ))
                                                .build()
                                            workManager.enqueue(workRequest)
                                        }
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("音楽")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // History Section Header
            item {
                Text(
                    text = "履歴",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Placeholder History Items (To be connected to Room DB later)
            items(3) { index ->
                HistoryItemPlaceholder(index)
            }
        }
    }
}

@Composable
fun HistoryItemPlaceholder(index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        ListItem(
            headlineContent = { Text("履歴アイテム #${index + 1}") },
            supportingContent = { Text("2026/01/07 • 4:30") },
            leadingContent = {
                Icon(Icons.Default.History, contentDescription = null)
            },
            trailingContent = {
                 IconButton(onClick = {}) {
                     Icon(Icons.Default.Download, contentDescription = "再ダウンロード")
                 }
            }
        )
    }
}
