package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaPath: String,
    val title: String,
    val isVideo: Boolean,
    val duration: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val mediaPath: String,
    val title: String,
    val isVideo: Boolean,
    val duration: Long,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playbackPosition: Long = 0L
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val mediaPath: String,
    val title: String,
    val isVideo: Boolean,
    val duration: Long,
    val addedAt: Long = System.currentTimeMillis()
)
