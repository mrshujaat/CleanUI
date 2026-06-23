package com.example.mediabrowser.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post

/**
 * Full-screen, continuous vertical scroll feed (manhwa/webtoon-style) used
 * everywhere a post is opened.
 */
@Composable
fun PostFeedScreen(
    startIndex: Int,
    onDismiss: () -> Unit,
    onToggleFavorite: (Post) -> Unit,
    onDownload: (Post) -> Unit,
    getPostDetail: suspend (Post) -> com.example.mediabrowser.domain.model.PostDetail?,
    items: LazyPagingItems<Post>? = null,
    fixedItems: List<Post>? = null
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    var detailPost by remember { mutableStateOf<Post?>(null) }

    val itemCount = items?.itemCount ?: fixedItems?.size ?: 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(count = itemCount) { index ->
                    // FIXED: Read cleanly from itemSnapshotList or the fixed list to dodge evaluation loops
                    val post = items?.itemSnapshotList?.getOrNull(index) ?: fixedItems?.getOrNull(index)
                    
                    if (post != null) {
                        FeedItem(
                            post = post,
                            onToggleFavorite = { onToggleFavorite(post) },
                            onDownload = { onDownload(post) },
                            onShowDetails = { detailPost = post }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            AnimatedVisibility(
                visible = detailPost != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val post = detailPost
                if (post != null) {
                    PostDetailLoader(
                        post = post,
                        getPostDetail = getPostDetail,
                        onDismiss = { detailPost = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedItem(
    post: Post,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onShowDetails: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val aspectRatio = if (post.height > 0) post.width.toFloat() / post.height.toFloat() else 1f

        Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio.coerceIn(0.4f, 2.2f))) {
            when (post.fileType) {
                MediaType.VIDEO -> VideoPlayer(videoUrl = post.fileUrl, autoPlay = true, startMuted = true, loop = true)
                MediaType.IMAGE, MediaType.GIF -> AsyncImage(
                    model = post.fileUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (post.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (post.isFavorite) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
                }
            }
            Text(
                text = "Details",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable(onClick = onShowDetails)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PostDetailLoader(
    post: Post,
    getPostDetail: suspend (Post) -> com.example.mediabrowser.domain.model.PostDetail?,
    onDismiss: () -> Unit
) {
    var detail by remember(post.id) { mutableStateOf<com.example.mediabrowser.domain.model.PostDetail?>(null) }

    LaunchedEffect(post.id) {
        detail = getPostDetail(post)
    }

    detail?.let {
        DetailPageStandalone(detail = it, onDismiss = onDismiss)
    }
}