package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.MediaFile
import com.example.data.model.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayMaxVideoPlayer(
    videoFile: MediaFile,
    playlist: List<MediaFile>,
    onBack: () -> Unit,
    onSaveRecent: (Long) -> Unit,
    initialPosition: Long = 0L
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Screen brightness & volume parameters management handles
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var currentItemIndex by remember {
        mutableStateOf(playlist.indexOfFirst { it.path == videoFile.path }.coerceAtLeast(0))
    }
    val currentVideo = remember(currentItemIndex, playlist) {
        if (playlist.isEmpty()) videoFile else playlist[currentItemIndex]
    }

    // Advanced player states matching requirements
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var activeAspectRatio by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Gesture status values overlays
    var overlayVolumeText by remember { mutableStateOf("") }
    var overlayBrightnessText by remember { mutableStateOf("") }
    var overlaySeekText by remember { mutableStateOf("") }

    // Dialog state controllers
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    // Track active subtitles files inside same directory
    var activeSubtitleUri by remember { mutableStateOf<Uri?>(null) }
    val eligibleSubtitles = remember(currentVideo) {
        val list = mutableListOf<File>()
        try {
            val videoFileObj = File(currentVideo.path)
            val parentDir = videoFileObj.parentFile
            if (parentDir != null && parentDir.exists()) {
                parentDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".srt", ignoreCase = true)
                }?.forEach { list.add(it) }
            }
        } catch (e: Exception) {
            // Ignore directory scanning issues
        }
        list
    }

    // Instantiation and configuration of our Media3 ExoPlayer Engine
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    // Load media on selection change
    LaunchedEffect(currentVideo, activeSubtitleUri) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        val mediaSourceBuilder = MediaItem.Builder()
            .setUri(Uri.parse(currentVideo.path))

        if (activeSubtitleUri != null) {
            val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(activeSubtitleUri!!)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaSourceBuilder.setSubtitleConfigurations(listOf(subtitleConfiguration))
        }

        exoPlayer.setMediaItem(mediaSourceBuilder.build())
        exoPlayer.prepare()
        if (initialPosition > 0L) {
            exoPlayer.seekTo(initialPosition)
        }
        exoPlayer.setPlaybackSpeed(playbackSpeed)
        exoPlayer.play()
    }

    DisposableEffect(Unit) {
        // Force full screen flags
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Progress updates interval loops
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(500)
        }
    }

    // Synchronize play listener callbacks
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration
                }
            }
        })
    }

    BackHandler {
        onSaveRecent(exoPlayer.currentPosition)
        onBack()
    }

    // Keep Controls Overlay open briefly on tap
    var isControlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(5000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
            // Tap gestures to toggle toolbar visibility overlay HUD
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { /* Reset gesture trackers */ },
                    onDragEnd = {
                        overlayVolumeText = ""
                        overlayBrightnessText = ""
                        overlaySeekText = ""
                    },
                    onDragCancel = {
                        overlayVolumeText = ""
                        overlayBrightnessText = ""
                        overlaySeekText = ""
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val screenWidth = size.width
                        val screenHeight = size.height

                        // Split screen drag actions handles: Right-side represents volume, Left-side represents brightness
                        if (change.position.x > screenWidth / 2) {
                            // Volume control gestures
                            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val volumeDelta = -(dragAmount.y / screenHeight * maxVolume * 2).toInt()
                            val targetVolume = (currentVolume + volumeDelta).coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                            overlayVolumeText = "Volume: ${(targetVolume.toFloat() / maxVolume * 100).toInt()}%"
                        } else {
                            // Brightness control gestures
                            val attributes = activity?.window?.attributes
                            val currentBrightness = if (attributes?.screenBrightness ?: -1f < 0) 0.5f else attributes?.screenBrightness ?: 0.5f
                            val brightnessDelta = -dragAmount.y / screenHeight * 1.5f
                            val targetBrightness = (currentBrightness + brightnessDelta).coerceIn(0.01f, 1.0f)
                            if (attributes != null) {
                                attributes.screenBrightness = targetBrightness
                                activity.window.attributes = attributes
                            }
                            overlayBrightnessText = "Brightness: ${(targetBrightness * 100).toInt()}%"
                        }
                    }
                )
            }
    ) {
        // Android Surface View containing the Media3 Video player frame container
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = activeAspectRatio
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = activeAspectRatio
            },
            modifier = Modifier.fillMaxSize()
        )

        // Render transient overlay HUD feedback for Gestures controls on screen center
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (overlayVolumeText.isNotEmpty()) {
                GestureFeedbackBadge(icon = Icons.Default.VolumeUp, text = overlayVolumeText)
            }
            if (overlayBrightnessText.isNotEmpty()) {
                GestureFeedbackBadge(icon = Icons.Default.BrightnessMedium, text = overlayBrightnessText)
            }
        }

        // Active Player Controls Overlays HUD
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(350))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Main Header Toolbar Deck controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = {
                            onSaveRecent(exoPlayer.currentPosition)
                            onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentVideo.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = currentVideo.folderName,
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(Icons.Default.ClosedCaption, contentDescription = "Subtitles", tint = if (activeSubtitleUri != null) MaterialTheme.colorScheme.primary else Color.White)
                        }

                        IconButton(onClick = {
                            // Cycle Aspect Ratio layout options representation
                            activeAspectRatio = when (activeAspectRatio) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect ratio settings", tint = Color.White)
                        }

                        IconButton(onClick = { showSpeedDialog = true }) {
                            Icon(Icons.Default.Speed, contentDescription = "Playback speed control", tint = Color.White)
                        }
                    }
                }

                // Medium Playback console buttons center screen
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentItemIndex > 0) {
                                currentItemIndex--
                            } else {
                                currentItemIndex = playlist.size - 1
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = {
                            val newPosition = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPosition)
                            currentPosition = newPosition
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle play pause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newPosition = (exoPlayer.currentPosition + 10000L).coerceAtMost(totalDuration)
                            exoPlayer.seekTo(newPosition)
                            currentPosition = newPosition
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = {
                            if (currentItemIndex < playlist.size - 1) {
                                currentItemIndex++
                            } else {
                                currentItemIndex = 0
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next Track", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Playback progress controls bars on screen bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Progress seeking slider
                        Slider(
                            value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                            onValueChange = { percent ->
                                val targetPos = (percent * totalDuration).toLong()
                                exoPlayer.seekTo(targetPos)
                                currentPosition = targetPos
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                                .testTag("video_scrub_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.DarkGray
                            )
                        )

                        Text(
                            text = formatDuration(totalDuration),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Playback Speed Selector Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed settings") },
            text = {
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    exoPlayer.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${speed}x", fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal)
                            if (playbackSpeed == speed) {
                                Icon(Icons.Default.Check, contentDescription = "Active speed setting", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Subtitle selection dialog
    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Subtitle track selections (.srt)") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeSubtitleUri = null
                                showSubtitleDialog = false
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "No Subtitles track", fontWeight = if (activeSubtitleUri == null) FontWeight.Bold else FontWeight.Normal)
                        if (activeSubtitleUri == null) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (eligibleSubtitles.isEmpty()) {
                        Text(
                            text = "No compatible subtitle file (.srt) located in directory path.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        eligibleSubtitles.forEach { srtFile ->
                            val srtUri = Uri.fromFile(srtFile)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeSubtitleUri = srtUri
                                        showSubtitleDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = srtFile.name,
                                    fontWeight = if (activeSubtitleUri == srtUri) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (activeSubtitleUri == srtUri) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun GestureFeedbackBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

// Extends context class to look up active display Activity context references
fun Context.findActivity(): Activity? {
    var ctxValue = this
    while (ctxValue is ContextWrapper) {
        if (ctxValue is Activity) {
            return ctxValue
        }
        ctxValue = ctxValue.baseContext
    }
    return null
}
