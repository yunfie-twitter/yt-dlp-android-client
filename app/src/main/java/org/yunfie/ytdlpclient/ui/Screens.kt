package org.yunfie.ytdlpclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Setup Dialog
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

    // Navigation State
    var selectedScreen by remember { mutableIntStateOf(0) } // 0: Home/Download, 1: History

    Scaffold(
        modifier = modifier,
        topBar = {
             // Dynamic Search Bar
             val placeholderText = if (selectedScreen == 0) "YouTubeのURLを入力" else "履歴を検索"
             DockedSearchBar(
                 query = uiState.urlInput,
                 onQueryChange = { viewModel.onUrlChanged(it) },
                 onSearch = { 
                     if (selectedScreen == 0) viewModel.fetchInfo() 
                     // TODO: Implement history search
                 },
                 active = false,
                 onActiveChange = {},
                 placeholder = { Text(placeholderText) },
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
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "ホーム") },
                    label = { Text("ホーム") },
                    selected = selectedScreen == 0,
                    onClick = { selectedScreen = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "履歴") },
                    label = { Text("履歴") },
                    selected = selectedScreen == 1,
                    onClick = { selectedScreen = 1 }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedScreen == 0) {
                // Home / Download Screen
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.isLoading) {
                        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                    }

                    uiState.error?.let { error ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }

                    uiState.videoInfo?.let { info ->
                        item {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
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
                                                    Text(text = (info.uploader?.take(1) ?: "U").uppercase())
                                                }
                                            }
                                        },
                                        trailingContent = {
                                            IconButton(onClick = { /* More options */ }) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                                            }
                                        }
                                    )

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

                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(text = info.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = info.description ?: "説明なし", style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        
                                        Spacer(modifier = Modifier.height(16.dp))

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
                                                    // TODO: Add to local DB history
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
                                                    // TODO: Add to local DB history
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
                }
            } else {
                // History Screen
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(5) { index -> // Placeholder count
                         HistoryItem(index)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        ListItem(
            headlineContent = { Text("履歴動画タイトル #$index", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text("2026/01/07 • 動画 • 完了") },
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
