package com.example.mediabrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Full-screen modal showing exactly one video, with real playback controls
 * (scrubber, time, mute toggle) and a close button. Opened directly from a
 * grid tap on a video post — bypasses the swipeable feed entirely since
 * there's only one item to show.
 */
@Composable
fun VideoModal(
    videoUrl: String,
    onDismiss: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isFullscreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Set orientation from the fullscreen flag without tearing down on every toggle.
    androidx.compose.runtime.LaunchedEffect(isFullscreen) {
        val activity = context as? android.app.Activity
        activity?.requestedOrientation = if (isFullscreen) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    // Restore orientation only when the modal actually closes.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            val activity = context as? android.app.Activity
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            VideoPlayer(
                videoUrl = videoUrl,
                autoPlay = true,
                startMuted = true,
                loop = true,
                showControls = true,
                isFullscreen = isFullscreen,
                onToggleFullscreen = { isFullscreen = !isFullscreen },
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                // Favorite toggle (left). Heart turns red when the video is favorited.
                if (onToggleFavorite != null) {
                    IconButton(onClick = { onToggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite)
                                Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFE53935) else Color.White
                        )
                    }
                } else {
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(1.dp))
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}