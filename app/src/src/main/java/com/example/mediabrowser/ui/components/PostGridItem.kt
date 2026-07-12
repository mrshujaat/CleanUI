package com.example.mediabrowser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mediabrowser.domain.model.MediaType
import com.example.mediabrowser.domain.model.Post
import com.example.mediabrowser.domain.model.TagCategory

/**
 * Masonry/grid cell: rounded card showing the media at its real aspect
 * ratio, with score/favorite overlays. Tapping an image post opens the
 * full feed/detail modal; tapping a video post opens VideoModal directly.
 * There is no inline tag expansion in the grid — tags are only shown and
 * interactive inside the full detail modal.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PostGridItem(
    post: Post,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit = {},
    onTagClick: (String, TagCategory) -> Unit = { _, _ -> },
    cornerRadiusDp: Int = 16,
    columns: Int = 2,
    imageQuality: com.example.mediabrowser.domain.model.ImageQuality =
        com.example.mediabrowser.domain.model.ImageQuality.MEDIUM,
    modifier: Modifier = Modifier
) {
    var showVideoModal by remember { mutableStateOf(false) }
    val aspectRatio = if (post.height > 0) post.width.toFloat() / post.height.toFloat() else 0.75f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // FIXED: Removed .animateContentSize() to prevent layout measurement overhead during grid scrolling
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio.coerceIn(0.5f, 1.6f))
        ) {
            // r34.app loads fast and looks good because it shows the medium
            // pre-resampled image (sample_url), not the multi-MB original. We do the
            // same here: sampleUrl in the grid for every column count. It's sharp
            // enough and a fraction of the size, so it loads quickly. Videos show
            // their preview thumbnail (the file_url is the actual video).
            val imageUrl = when {
                post.fileType != MediaType.IMAGE -> post.thumbnailUrl
                else -> when (imageQuality) {
                    com.example.mediabrowser.domain.model.ImageQuality.LOW -> post.previewUrl
                    com.example.mediabrowser.domain.model.ImageQuality.MEDIUM -> post.sampleUrl
                    com.example.mediabrowser.domain.model.ImageQuality.HIGH -> post.fileUrl
                }
            }
            // Progressive loading: the tiny preview loads near-instantly and fills
            // the cell immediately (so nothing is blank while scrolling fast). The
            // higher-quality image then loads on top and crossfades in when ready.
            val lowResUrl = post.previewUrl
            if (post.fileType == MediaType.IMAGE && lowResUrl.isNotBlank() && lowResUrl != imageUrl) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(lowResUrl)
                        .memoryCacheKey(lowResUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(imageUrl)
                    .memoryCacheKey(imageUrl)
                    .placeholderMemoryCacheKey(lowResUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {
                            if (post.fileType == MediaType.VIDEO) {
                                showVideoModal = true
                            } else {
                                onClick()
                            }
                        },
                        onLongClick = onToggleFavorite
                    )
            )

            if (post.fileType != MediaType.IMAGE) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            }

            // Score badge moved to top-left.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 2.dp).size(12.dp)
                )
                Text(
                    text = post.score.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Transparent quick-favorite heart at bottom-left. Red when favorited.
            Icon(
                imageVector = if (post.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (post.isFavorite) Color(0xFFE53935) else Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .clickable { onToggleFavorite() }
                    .padding(6.dp)
                    .size(20.dp)
            )

            // Transparent quick-download at bottom-right.
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Download",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .clickable { onDownload() }
                    .padding(6.dp)
                    .size(20.dp)
            )
        }
    }

    if (showVideoModal) {
        VideoModal(
            videoUrl = post.fileUrl,
            onDismiss = { showVideoModal = false },
            isFavorite = post.isFavorite,
            onToggleFavorite = onToggleFavorite
        )
    }
}