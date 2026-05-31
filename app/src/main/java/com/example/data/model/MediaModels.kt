package com.example.data.model

data class MediaFile(
    val id: Long = 0,
    val path: String,
    val title: String,
    val duration: Long, // in millis
    val size: Long, // in bytes
    val album: String? = null,
    val artist: String? = null,
    val isVideo: Boolean = true,
    val folderName: String = "",
    val resolution: String? = null,
    val dateModified: Long = 0
) {
    val durationString: String
        get() = formatDuration(duration)

    val sizeString: String
        get() = formatSize(size)
}

data class MediaFolder(
    val name: String,
    val path: String,
    val isVideo: Boolean,
    val videoCount: Int = 0,
    val audioCount: Int = 0
)

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "00:00"
    val totalSeconds = durationMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
