package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.main.PlayMaxMainDashboard
import com.example.ui.player.PlayMaxMusicPlayer
import com.example.ui.player.PlayMaxVideoPlayer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.MediaViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core business logic model
        val viewModel by viewModels<MediaViewModel> {
            MediaViewModelFactory(applicationContext)
        }

        setContent {
            MyApplicationTheme {
                val currentFile by viewModel.currentPlayingFile.collectAsStateWithLifecycle()
                val currentPlaylist by viewModel.currentPlayingList.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    if (currentFile != null) {
                        val activeFile = currentFile!!
                        
                        // Query last playback checkpoint position
                        var initialPosition by remember(activeFile.path) { mutableStateOf(0L) }
                        LaunchedEffect(activeFile.path) {
                            initialPosition = viewModel.getRecentPlaybackPosition(activeFile.path)
                        }

                        if (activeFile.isVideo) {
                            PlayMaxVideoPlayer(
                                videoFile = activeFile,
                                playlist = if (currentPlaylist.isEmpty()) listOf(activeFile) else currentPlaylist,
                                onBack = { viewModel.currentPlayingFile.value = null },
                                onSaveRecent = { position ->
                                    viewModel.addFileToRecents(activeFile, position)
                                },
                                initialPosition = initialPosition
                            )
                        } else {
                            var isFav by remember(activeFile.path) { mutableStateOf(false) }
                            LaunchedEffect(activeFile.path) {
                                isFav = viewModel.isFavoriteState(activeFile.path)
                            }
                            
                            PlayMaxMusicPlayer(
                                audioFile = activeFile,
                                playlist = if (currentPlaylist.isEmpty()) listOf(activeFile) else currentPlaylist,
                                onBack = { viewModel.currentPlayingFile.value = null },
                                onToggleFavorite = { file ->
                                    viewModel.toggleFavorite(file)
                                    isFav = !isFav
                                },
                                isFavorite = isFav,
                                onSaveRecent = { position ->
                                    viewModel.addFileToRecents(activeFile, position)
                                },
                                initialPosition = initialPosition
                            )
                        }
                    } else {
                        PlayMaxMainDashboard(
                            viewModel = viewModel,
                            onPlayFile = { file, list ->
                                viewModel.currentPlayingFile.value = file
                                viewModel.currentPlayingList.value = list
                            }
                        )
                    }
                }
            }
        }
    }
}
