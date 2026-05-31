package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaPath = :mediaPath")
    suspend fun deleteFavorite(mediaPath: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaPath = :mediaPath)")
    suspend fun isFavorite(mediaPath: String): Boolean


    // Recents
    @Query("SELECT * FROM recents ORDER BY lastPlayedAt DESC")
    fun getRecents(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentEntity)

    @Query("DELETE FROM recents WHERE mediaPath = :mediaPath")
    suspend fun deleteRecent(mediaPath: String)

    @Query("SELECT * FROM recents WHERE mediaPath = :mediaPath LIMIT 1")
    suspend fun getRecentByPath(mediaPath: String): RecentEntity?

    @Query("DELETE FROM recents")
    suspend fun clearRecents()


    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Int)


    // Playlist Items
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getPlaylistItems(playlistId: Int): Flow<List<PlaylistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND mediaPath = :mediaPath")
    suspend fun deletePlaylistItem(playlistId: Int, mediaPath: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Int)
}
