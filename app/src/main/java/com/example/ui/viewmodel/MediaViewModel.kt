package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.FavoriteEntity
import com.example.data.database.MediaDatabase
import com.example.data.database.PlaylistEntity
import com.example.data.database.PlaylistItemEntity
import com.example.data.database.RecentEntity
import com.example.data.model.MediaFile
import com.example.data.model.MediaFolder
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MediaViewModel(
    application: Application,
    private val repository: MediaRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()

    private val _audios = MutableStateFlow<List<MediaFile>>(emptyList())
    val audios: StateFlow<List<MediaFile>> = _audios.asStateFlow()

    private val _folders = MutableStateFlow<List<MediaFolder>>(emptyList())
    val folders: StateFlow<List<MediaFolder>> = _folders.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredVideos: StateFlow<List<MediaFile>> = combine(_videos, _searchQuery) { list, query ->
        if (query.isEmpty()) list else list.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAudios: StateFlow<List<MediaFile>> = combine(_audios, _searchQuery) { list, query ->
        if (query.isEmpty()) list else list.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected state context elements
    var selectedFolder = MutableStateFlow<MediaFolder?>(null)
    var activePlaylistId = MutableStateFlow<Int?>(null)
    var activePlaylistName = MutableStateFlow("")

    val favorites: StateFlow<List<FavoriteEntity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recents: StateFlow<List<RecentEntity>> = repository.getRecents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen navigation
    var currentPlayingFile = MutableStateFlow<MediaFile?>(null)
    var currentPlayingList = MutableStateFlow<List<MediaFile>>(emptyList())

    init {
        refreshMedia()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshMedia() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                // Fetch assets
                val fetchedVideos = repository.scanVideos(context)
                val fetchedAudios = repository.scanAudios(context)

                _videos.value = fetchedVideos
                _audios.value = fetchedAudios

                val combined = fetchedVideos + fetchedAudios
                _folders.value = repository.getFolders(combined)
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Refresh scanning error", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Favorites Interaction
    fun toggleFavorite(file: MediaFile) {
        viewModelScope.launch {
            val isFav = repository.isFavorite(file.path)
            if (isFav) {
                repository.removeFavorite(file.path)
            } else {
                repository.addFavorite(file)
            }
        }
    }

    suspend fun isFavoriteState(path: String): Boolean {
        return repository.isFavorite(path)
    }

    // Recents Interaction
    fun addFileToRecents(file: MediaFile, position: Long = 0L) {
        viewModelScope.launch {
            repository.addRecent(file, position)
        }
    }

    suspend fun getRecentPlaybackPosition(path: String): Long {
        return repository.getRecentPosition(path)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearRecents()
        }
    }

    // Playlist Interaction
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addFileToPlaylist(playlistId: Int, file: MediaFile) {
        viewModelScope.launch {
            repository.addPlaylistItem(playlistId, file)
        }
    }

    fun removeFileFromPlaylist(playlistId: Int, path: String) {
        viewModelScope.launch {
            repository.removePlaylistItem(playlistId, path)
        }
    }

    fun getPlaylistItems(playlistId: Int): Flow<List<PlaylistItemEntity>> {
        return repository.getPlaylistItems(playlistId)
    }
}

class MediaViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            val database = MediaDatabase.getDatabase(context)
            val repository = MediaRepository(database.mediaDao())
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(context.applicationContext as Application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
