package com.example.mediabrowser.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.mediabrowser.domain.model.DownloadItem
import com.example.mediabrowser.domain.model.DownloadStatus
import com.example.mediabrowser.ui.components.MasonryGrid
import com.example.mediabrowser.ui.components.PostFeedScreen
import com.example.mediabrowser.ui.components.appBackgroundGradient
import kotlinx.coroutines.delay

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    val completedPosts by viewModel.completedPosts.collectAsState() // FIXED: Read flattened state from ViewModel
    val viewStyle by viewModel.viewStyle.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.appBackgroundGradient(Color(0xFF2DD4BF))
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Text(text = "Downloads", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = {
                            viewModel.setViewStyle(
                                if (viewStyle == DownloadViewStyle.LIST) DownloadViewStyle.GRID else DownloadViewStyle.LIST
                            )
                        }) {
                            Icon(
                                imageVector = if (viewStyle == DownloadViewStyle.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                                contentDescription = "Toggle view",
                                tint = Color.White
                            )
                        }
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(Icons.Filled.Sort, contentDescription = "Sort", tint = Color.White)
                            }
                            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("By date") },
                                    onClick = { viewModel.setSortOrder(DownloadSortOrder.DATE); sortMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("By size") },
                                    onClick = { viewModel.setSortOrder(DownloadSortOrder.SIZE); sortMenuExpanded = false }
                                )
                            }
                        }
                    }
                }

                if (downloads.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No downloads yet.", color = Color(0xFF8A8D91))
                    }
                } else when (viewStyle) {
                    DownloadViewStyle.LIST -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(downloads, key = { it.id }) { item ->
                                DownloadRow(
                                    item = item,
                                    onDelete = { viewModel.deleteDownload(item.id) },
                                    // FIXED: Relies on verified data synchronization logic mapping items natively
                                    onClick = { viewModel.openDownloadPost(item.toPost()) }
                                )
                            }
                        }
                    }
                    DownloadViewStyle.GRID -> {
                        // FIXED: Replaced nested parent layout container with a clean standalone container
                        MasonryGrid(
                            items = downloads,
                            columns = 3,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            aspectRatioOf = { item ->
                                if (item.height > 0) item.width.toFloat() / item.height.toFloat() else 0.75f
                            }
                        ) { item ->
                            DownloadGridCell(
                                item = item,
                                onClick = { viewModel.openDownloadPost(item.toPost()) }
                            )
                        }
                    }
                }
            }
        }

        toastMessage?.let { message ->
            LaunchedEffect(message) {
                delay(2000)
                viewModel.clearToast()
            }
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier.background(Color(0xFF1A1D1F), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(text = message, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }

    openFeedIndex?.let { index ->
        PostFeedScreen(
            startIndex = index,
            fixedItems = completedPosts,
            onDismiss = viewModel::closeFeed,
            onToggleFavorite = { /* disabled for downloaded items */ },
            onDownload = { /* disabled for downloaded items */ },
            getPostDetail = { post -> viewModel.getPostDetail(post) }
        )
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onDelete: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) {
            AsyncImage(
                model = item.localUri ?: item.fileUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(text = item.fileName, color = Color.White, maxLines = 1)

            when (item.status) {
                DownloadStatus.IN_PROGRESS, DownloadStatus.QUEUED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = item.progress / 100f,
                            modifier = Modifier.weight(1f).padding(top = 4.dp, end = 8.dp)
                        )
                        Text(text = "${item.progress}%", color = Color(0xFF8A8D91), fontSize = 12.sp)
                    }
                }
                DownloadStatus.COMPLETED -> {
                    Row {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(text = " Completed", color = Color(0xFF8A8D91), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                DownloadStatus.FAILED -> {
                    Row {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(text = " Failed", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                DownloadStatus.CANCELLED -> {
                    Row {
                        Icon(Icons.Filled.PauseCircle, contentDescription = null, tint = Color(0xFF8A8D91), modifier = Modifier.size(16.dp))
                        Text(text = " Cancelled", color = Color(0xFF8A8D91), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
        }
    }
}

@Composable
private fun DownloadGridCell(item: DownloadItem, onClick: () -> Unit) {
    val aspectRatio = if (item.height > 0) item.width.toFloat() / item.height.toFloat() else 0.75f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.5f, 1.6f))
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = item.localUri ?: item.fileUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (item.status == DownloadStatus.IN_PROGRESS || item.status == DownloadStatus.QUEUED) {
            Box(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                LinearProgressIndicator(progress = item.progress / 100f, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}