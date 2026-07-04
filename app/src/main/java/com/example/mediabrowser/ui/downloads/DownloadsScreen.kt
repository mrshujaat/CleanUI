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
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.PlayCircle
import kotlinx.coroutines.withContext
import coil.request.videoFrameMillis
import com.example.mediabrowser.domain.model.ArtistProfile
import com.example.mediabrowser.domain.model.DownloadItem
import com.example.mediabrowser.domain.model.DownloadStatus
import com.example.mediabrowser.ui.components.MasonryGrid
import com.example.mediabrowser.ui.components.PostFeedScreen
import com.example.mediabrowser.ui.components.appBackgroundGradient
import kotlinx.coroutines.delay

@Composable
fun DownloadsScreen(
    onNavigateToArtist: (ArtistProfile) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.sectionedDownloads.collectAsState()
    val completedPosts by viewModel.sectionedCompletedPosts.collectAsState()
    val section by viewModel.section.collectAsState()
    val viewStyle by viewModel.viewStyle.collectAsState()
    val openFeedIndex by viewModel.openFeedIndex.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.appBackgroundGradient(Color(0xFF2DD4BF), androidx.compose.material3.MaterialTheme.colorScheme.background)
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Downloads",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                    )
                    Row(modifier = Modifier.align(Alignment.CenterEnd)) {
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

                // Images / Videos section toggle.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    SectionToggleButton(
                        label = "Images",
                        selected = section == DownloadSection.IMAGES,
                        onClick = { viewModel.setSection(DownloadSection.IMAGES) }
                    )
                    SectionToggleButton(
                        label = "Videos",
                        selected = section == DownloadSection.VIDEOS,
                        onClick = { viewModel.setSection(DownloadSection.VIDEOS) }
                    )
                }

                if (downloads.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (section == DownloadSection.VIDEOS) "No videos downloaded yet."
                            else "No images downloaded yet.",
                            color = Color(0xFF8A8D91)
                        )
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
                            columns = settings.gridColumns.coerceIn(1, 4),
                            key = { item -> item.id },
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
            getPostDetail = { post -> viewModel.getPostDetail(post) },
            onNavigateToArtist = onNavigateToArtist
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
            MediaThumbnail(item = item, modifier = Modifier.fillMaxSize())
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
        MediaThumbnail(item = item, modifier = Modifier.fillMaxSize())
        if (item.status == DownloadStatus.IN_PROGRESS || item.status == DownloadStatus.QUEUED) {
            Box(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                LinearProgressIndicator(progress = item.progress / 100f, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
/** Pill toggle button for the Images/Videos section switch. */
@Composable
private fun SectionToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Color.White else Color(0xFF1A1D1F),
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF111111) else Color(0xFF8A8D91),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Shows a thumbnail for a downloaded item. For images it loads normally via Coil.
 * For videos it extracts the first frame with MediaMetadataRetriever (no ExoPlayer,
 * no extra dependency) and caches it in state — this is what makes downloaded video
 * thumbnails appear without spinning up a player per cell.
 */
@Composable
private fun MediaThumbnail(item: DownloadItem, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isVideo = item.mediaType == com.example.mediabrowser.domain.model.MediaType.VIDEO
    val source = item.localUri ?: item.fileUrl

    if (!isVideo) {
        AsyncImage(
            model = source,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        return
    }

    // Extract the first video frame once, off the main thread, keyed by the source.
    var frame by remember(source) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(source) {
        frame = withContext(kotlinx.coroutines.Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                if (source.startsWith("file://") || source.startsWith("/")) {
                    retriever.setDataSource(android.net.Uri.parse(source).path)
                } else {
                    retriever.setDataSource(source, HashMap())
                }
                // Frame at ~1s, or the first available frame.
                retriever.getFrameAtTime(1_000_000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            } catch (e: Exception) {
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bmp = frame
        if (bmp != null) {
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Small play badge so videos read as videos.
        Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(24.dp)
        )
    }
}