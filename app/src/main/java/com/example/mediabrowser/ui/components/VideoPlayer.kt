package com.example.mediabrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Wraps Media3's ExoPlayer + PlayerView with dynamic media item reloading,
 * loading/error states, a tap-to-toggle play/pause overlay, and (when
 * [showControls] is true) a scrubber with elapsed/total time plus a
 * mute/unmute toggle — used by the full-screen single-video modal.
 */
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    startMuted: Boolean = false,
    loop: Boolean = true,
    showControls: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    isFullscreen: Boolean = false
) {
    val context = LocalContext.current

    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isMuted by remember { mutableStateOf(startMuted) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableStateOf(0L) }
    var videoAspectRatio by remember { mutableStateOf(16f / 9f) }

    // FIXED: Build the player instance exactly ONCE per composition lifecycle rather than allocations per URL
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                }

                override fun onPlayerError(error: PlaybackException) {
                    hasError = true
                    isBuffering = false
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }
            })
        }
    }

    // FIXED: React to changing video URLs by mutating properties on the same player instance dynamically.
    // autoPlay is intentionally NOT a key here — pager pages prepare their video while
    // off-screen (paused) and then flip playWhenReady on swipe, so playback starts
    // instantly instead of re-buffering. See the LaunchedEffect below.
    LaunchedEffect(videoUrl, loop, startMuted) {
        hasError = false
        isBuffering = true
        isMuted = startMuted

        exoPlayer.stop()
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
        exoPlayer.volume = if (startMuted) 0f else 1f
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoPlay
    }

    // TikTok-style page changes: toggling autoPlay only flips playWhenReady on the
    // already-prepared player — no stop/prepare cycle, so swiped-to videos start
    // immediately and swiped-away videos pause (and rewind for a clean re-entry).
    LaunchedEffect(autoPlay) {
        exoPlayer.playWhenReady = autoPlay
        if (!autoPlay) exoPlayer.seekTo(0L)
    }

    // Poll playback position/duration while controls are visible, so the
    // scrubber and time labels stay live without needing a Player.Listener
    // for every single position tick.
    LaunchedEffect(showControls) {
        if (showControls) {
            while (true) {
                if (!isScrubbing) {
                    currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                }
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                delay(250)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // Auto-hide the controls a few seconds after they're shown (while playing).
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            ErrorState()
        } else {
            // The video + its controls live in a box sized to the video's aspect
            // ratio and centered on screen. This keeps the controls flush at the
            // bottom of the VIDEO itself instead of stranded at the screen bottom
            // with a black letterbox gap between them.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoAspectRatio.coerceIn(0.4f, 3f)),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        }
                    },
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                    }
                )

            // Tap anywhere toggles the controls' visibility.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { controlsVisible = !controlsVisible },
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(color = Color.White)
                } else if (controlsVisible) {
                    // Center transport row: back 5s, play/pause, forward 5s.
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay5,
                            contentDescription = "Back 5 seconds",
                            tint = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    val target = (exoPlayer.currentPosition - 5000).coerceAtLeast(0L)
                                    exoPlayer.seekTo(target)
                                    controlsVisible = true
                                }
                        )
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .clickable {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    controlsVisible = true
                                }
                        )
                        Icon(
                            imageVector = Icons.Filled.Forward5,
                            contentDescription = "Forward 5 seconds",
                            tint = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                                    val target = (exoPlayer.currentPosition + 5000).coerceAtMost(dur)
                                    exoPlayer.seekTo(target)
                                    controlsVisible = true
                                }
                        )
                    }
                }
            }

            if (showControls && controlsVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                        .padding(start = 16.dp, end = 12.dp, top = 20.dp, bottom = 10.dp)
                ) {
                    // Controls row: elapsed/total on the left, volume + fullscreen on the right.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMillis(if (isScrubbing) scrubPositionMs else currentPositionMs) +
                                " / " + formatMillis(durationMs),
                            color = Color.White,
                            fontSize = 13.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    exoPlayer.volume = if (isMuted) 0f else 1f
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            if (onToggleFullscreen != null) {
                                IconButton(
                                    onClick = { onToggleFullscreen() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFullscreen)
                                            Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Thin scrubber spanning the full width, just below the controls row.
                    Slider(
                        value = (if (isScrubbing) scrubPositionMs else currentPositionMs)
                            .coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
                        onValueChange = {
                            isScrubbing = true
                            scrubPositionMs = it.toLong()
                            controlsVisible = true
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(scrubPositionMs)
                            currentPositionMs = scrubPositionMs
                            isScrubbing = false
                        },
                        valueRange = 0f..(durationMs.coerceAtLeast(1L)).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            } // end aspect-ratio video box
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun ErrorState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp)
        )
    }
}