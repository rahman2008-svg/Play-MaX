package com.example.data.repository

import android.content.Context
import com.example.data.database.FavoriteEntity
import com.example.data.database.MediaDao
import com.example.data.database.PlaylistEntity
import com.example.data.database.PlaylistItemEntity
import com.example.data.database.RecentEntity
import com.example.data.model.MediaFile
import com.example.data.model.MediaFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val mediaDao: MediaDao) {

    // Database Actions: Favorites
    fun getFavorites(): Flow<List<FavoriteEntity>> = mediaDao.getFavorites()

    suspend fun addFavorite(file: MediaFile) = withContext(Dispatchers.IO) {
        mediaDao.insertFavorite(
            FavoriteEntity(
                mediaPath = file.path,
                title = file.title,
                isVideo = file.isVideo,
                duration = file.duration
            )
        )
    }

    suspend fun removeFavorite(path: String) = withContext(Dispatchers.IO) {
        mediaDao.deleteFavorite(path)
    }

    suspend fun isFavorite(path: String): Boolean = withContext(Dispatchers.IO) {
        mediaDao.isFavorite(path)
    }


    // Database Actions: Recents
    fun getRecents(): Flow<List<RecentEntity>> = mediaDao.getRecents()

    suspend fun addRecent(file: MediaFile, playbackPosition: Long = 0L) = withContext(Dispatchers.IO) {
        mediaDao.insertRecent(
            RecentEntity(
                mediaPath = file.path,
                title = file.title,
                isVideo = file.isVideo,
                duration = file.duration,
                playbackPosition = playbackPosition,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getRecentPosition(path: String): Long = withContext(Dispatchers.IO) {
        mediaDao.getRecentByPath(path)?.playbackPosition ?: 0L
    }

    suspend fun clearRecents() = withContext(Dispatchers.IO) {
        mediaDao.clearRecents()
    }


    // Database Actions: Playlists
    fun getPlaylists(): Flow<List<PlaylistEntity>> = mediaDao.getPlaylists()

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        mediaDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(id: Int) = withContext(Dispatchers.IO) {
        mediaDao.deletePlaylist(id)
    }


    // Database Actions: Playlist Items
    fun getPlaylistItems(playlistId: Int): Flow<List<PlaylistItemEntity>> =
        mediaDao.getPlaylistItems(playlistId)

    suspend fun addPlaylistItem(playlistId: Int, file: MediaFile) = withContext(Dispatchers.IO) {
        mediaDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                mediaPath = file.path,
                title = file.title,
                isVideo = file.isVideo,
                duration = file.duration
            )
        )
    }

    suspend fun removePlaylistItem(playlistId: Int, path: String) = withContext(Dispatchers.IO) {
        mediaDao.deletePlaylistItem(playlistId, path)
    }


    // MediaStore Scan wrapping
    suspend fun scanVideos(context: Context): List<MediaFile> = withContext(Dispatchers.IO) {
        MediaScanner.scanVideos(context)
    }

    suspend fun scanAudios(context: Context): List<MediaFile> = withContext(Dispatchers.IO) {
        MediaScanner.scanAudios(context)
    }

    fun getFolders(files: List<MediaFile>): List<MediaFolder> {
        return MediaScanner.getFolders(files)
    }
}
