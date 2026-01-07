package org.yunfie.ytdlpclient.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import org.yunfie.ytdlpclient.data.room.DownloadHistory
import org.yunfie.ytdlpclient.workers.DownloadWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.filteredHistory.collectAsState()
    val savedApiUrl by viewModel.apiUrl.collectAsState()
    
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)

    // Permission Launcher for Android 9 and below
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "保存のために権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    // Sheet State
    var showDownloadSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
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
    var selectedScreen by remember { mutableIntStateOf(0) } // 0: Home, 1: History

    Scaffold(
        modifier = modifier,
        topBar = {
             val placeholderText = if (selectedScreen == 0) "YouTubeのURLを入力" else "履歴を検索"
             val query = if (selectedScreen == 0) uiState.urlInput else uiState.historySearchQuery
             
             DockedSearchBar(
                 query = query,
                 onQueryChange = { 
                     if (selectedScreen == 0) viewModel.onUrlChanged(it)
                     else viewModel.onHistorySearch(it)
                 },
                 onSearch = { 
                     if (selectedScreen == 0) viewModel.fetchInfo() 
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
                // Home Screen
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
                                        
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = { showDownloadSheet = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("ダウンロード")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // History Screen
                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("履歴はありません", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history, key = { it.id }) { item ->
                            HistoryItem(item, onDelete = { viewModel.deleteHistory(item) })
                        }
                    }
                }
            }
        }
        
        if (showDownloadSheet && uiState.videoInfo != null) {
            ModalBottomSheet(
                onDismissRequest = { showDownloadSheet = false },
                sheetState = sheetState
            ) {
                DownloadOptionsSheet(
                    videoInfo = uiState.videoInfo!!,
                    availableHeights = uiState.availableVideoHeights,
                    onDownload = { isAudio, quality ->
                        // Check permission for Android 9-
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                             // Simple check, production app might need more logic
                        }
                        
                        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                            .setInputData(workDataOf(
                                DownloadWorker.KEY_URL to uiState.urlInput,
                                DownloadWorker.KEY_AUDIO_ONLY to isAudio,
                                DownloadWorker.KEY_TITLE to uiState.videoInfo!!.title,
                                DownloadWorker.KEY_UPLOADER to uiState.videoInfo!!.uploader,
                                DownloadWorker.KEY_THUMBNAIL to uiState.videoInfo!!.thumbnail,
                                DownloadWorker.KEY_QUALITY to quality // Pass int quality
                            ))
                            .build()
                        workManager.enqueue(workRequest)
                        
                        showDownloadSheet = false
                        Toast.makeText(context, "ダウンロードを開始しました", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun DownloadOptionsSheet(
    videoInfo: org.yunfie.ytdlpclient.data.VideoInfo,
    availableHeights: List<Int>,
    onDownload: (isAudio: Boolean, quality: Int?) -> Unit
) {
    var isAudioMode by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf<Int?>(availableHeights.firstOrNull()) }
    
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text("ダウンロードオプション", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mode Selection
        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = !isAudioMode,
                onClick = { isAudioMode = false },
                label = { Text("動画") },
                leadingIcon = { if (!isAudioMode) Icon(Icons.Default.Download, null) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = isAudioMode,
                onClick = { isAudioMode = true },
                label = { Text("音声のみ") },
                leadingIcon = { if (isAudioMode) Icon(Icons.Default.Download, null) }, // Use Download icon for both for now
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!isAudioMode) {
            Text("画質を選択", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(availableHeights) { height ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedQuality == height),
                                onClick = { selectedQuality = height }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedQuality == height),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${height}p", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
             Text("最高音質でダウンロードされます", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onDownload(isAudioMode, if (isAudioMode) null else selectedQuality) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ダウンロード開始")
        }
    }
}

@Composable
fun HistoryItem(history: DownloadHistory, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            ListItem(
                headlineContent = { Text(history.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { 
                    val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(history.timestamp))
                    Text("$date • ${if (history.isAudio) "音声" else "動画"}") 
                },
                leadingContent = {
                    AsyncImage(
                         model = ImageRequest.Builder(LocalContext.current)
                             .data(history.thumbnail)
                             .crossfade(true)
                             .build(),
                         contentDescription = null,
                         modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                         contentScale = ContentScale.Crop
                    )
                },
                trailingContent = {
                     IconButton(onClick = onDelete) {
                         Icon(Icons.Default.Delete, contentDescription = "削除")
                     }
                }
            )
        }
    }
}
