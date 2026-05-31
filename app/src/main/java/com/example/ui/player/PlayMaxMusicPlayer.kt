package com.example.ui.player

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.MediaFile
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayMaxMusicPlayer(
    audioFile: MediaFile,
    playlist: List<MediaFile>,
    onBack: () -> Unit,
    onToggleFavorite: (MediaFile) -> Unit,
    isFavorite: Boolean,
    onSaveRecent: (Long) -> Unit,
    initialPosition: Long = 0L
) {
    val context = LocalContext.current

    // Observe active playback item indices inside the queue
    var currentItemIndex by remember {
        mutableStateOf(playlist.indexOfFirst { it.path == audioFile.path }.coerceAtLeast(0))
    }
    val currentFile = remember(currentItemIndex, playlist) {
        if (playlist.isEmpty()) audioFile else playlist[currentItemIndex]
    }

    // Instantiation and management of our ExoPlayer session instance matching
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    // Load active track source segments
    LaunchedEffect(currentFile) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        val mediaItem = MediaItem.fromUri(currentFile.path)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (initialPosition > 0L) {
            exoPlayer.seekTo(initialPosition)
        }
        exoPlayer.play()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Media states observers
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }

    // Synchronize states periodically with Player session
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration
                } else if (playbackState == Player.STATE_ENDED) {
                    // Trigger playlist step
                    if (currentItemIndex < playlist.size - 1) {
                        currentItemIndex++
                    } else {
                        currentItemIndex = 0
                    }
                }
            }
        })
    }

    // Progress tick coroutines
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(1000)
        }
    }

    BackHandler {
        onSaveRecent(exoPlayer.currentPosition)
        onBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("music_player_screen"),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    onSaveRecent(exoPlayer.currentPosition)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = "PLAYING NOW",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(onClick = { onToggleFavorite(currentFile) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spinning CD Disk Artwork Design Area
            val infiniteTransition = rememberInfiniteTransition(label = "music_rotation")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "disk_rotation"
            )

            val rotationFactor = if (isPlaying) angle else 0f

            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF222222),
                                Color(0xFF111111),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .rotate(rotationFactor),
                contentAlignment = Alignment.Center
            ) {
                // Radial lines representing the grooves of a vinyl CD disc
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Central Album Label Display
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                // Tiny center spindle hole
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }

            Spacer(modifier = Modifier.height(42.dp))

            // Music Description Area
            Text(
                text = currentFile.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = currentFile.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Control sliders
            Slider(
                value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                onValueChange = { percent ->
                    val targetPos = (percent * totalDuration).toLong()
                    exoPlayer.seekTo(targetPos)
                    currentPosition = targetPos
                },
                modifier = Modifier
                    .fillModifier()
                    .testTag("music_scrub_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Duration stats display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatProgress(currentPosition),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatProgress(totalDuration),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main playback action console
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    exoPlayer.repeatMode = when (exoPlayer.repeatMode) {
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_ONE
                    }
                }) {
                    Icon(
                        imageVector = if (exoPlayer.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Loop track mode",
                        tint = if (exoPlayer.repeatMode == Player.REPEAT_MODE_ONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        if (currentItemIndex > 0) {
                            currentItemIndex--
                        } else {
                            currentItemIndex = playlist.size - 1
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Skip Previous", modifier = Modifier.size(32.dp))
                }

                // Play / Pause Circle Action Button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle play/pause",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (currentItemIndex < playlist.size - 1) {
                            currentItemIndex++
                        } else {
                            currentItemIndex = 0
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip Next", modifier = Modifier.size(32.dp))
                }

                IconButton(onClick = {
                    val shuffleState = exoPlayer.shuffleModeEnabled
                    exoPlayer.shuffleModeEnabled = !shuffleState
                }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (exoPlayer.shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Helper formatting method
fun formatProgress(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()
