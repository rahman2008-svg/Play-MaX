package com.example.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AspectRatioMode {
    FIT, FILL, STRETCH, RATIO_16_9, RATIO_4_3
}

class PlayMaxPlayerState {
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var playbackSpeed by mutableStateOf(1.0f)
    var aspectRatioMode by mutableStateOf(AspectRatioMode.FIT)
    var subtitlePath by mutableStateOf<String?>(null)
    var isBackgroundPlaybackEnabled by mutableStateOf(false)

    // Gesture HUD overlay states
    var activeGestureBrightness by mutableStateOf<Float?>(null) // null when not active, 0.0 to 1.0 when active
    var activeGestureVolume by mutableStateOf<Int?>(null) // null when not active, 0 to max_volume when active
    var activeGestureSeekOffset by mutableStateOf<Long?>(null) // null when not active, seek offset in ms when active
}
