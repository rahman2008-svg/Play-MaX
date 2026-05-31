package com.example.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.database.FavoriteEntity
import com.example.data.database.PlaylistEntity
import com.example.data.database.PlaylistItemEntity
import com.example.data.database.RecentEntity
import com.example.data.model.MediaFile
import com.example.data.model.MediaFolder
import com.example.data.model.formatSize
import com.example.ui.viewmodel.MediaViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayMaxMainDashboard(
    viewModel: MediaViewModel,
    onPlayFile: (MediaFile, List<MediaFile>) -> Unit
) {
    val context = LocalContext.current

    // Observe permission states
    var isPermissionGranted by remember {
        mutableStateOf(hasStoragePermission(context))
    }

    // Permission launcher contract
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionGranted = permissions.values.any { it }
        if (isPermissionGranted) {
            viewModel.refreshMedia()
        }
    }

    LaunchedEffect(Unit) {
        if (!isPermissionGranted) {
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.refreshMedia()
        }
    }

    // Tab state indices: 0: Videos, 1: Audios, 2: Folders, 3: Playlists & Recents, 4: Storage File Explorer
    var selectedTab by remember { mutableStateOf(0) }
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Database flow streams observer values
    val isScanning by viewModel.isScanning.collectAsState()
    val videos by viewModel.filteredVideos.collectAsState()
    val audios by viewModel.filteredAudios.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    // File selection modal support
    var pendingAddToPlaylistFile by remember { mutableStateOf<MediaFile?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistNameInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_dashboard_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Brand display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PlayMax",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row {
                        IconButton(onClick = { viewModel.refreshMedia() }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync scanned items", tint = if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Search Field Control
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search title, album or folder files...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple(0, "Videos", Icons.Default.VideoLibrary),
                    Triple(1, "Audios", Icons.Default.Audiotrack),
                    Triple(2, "Folders", Icons.Default.FolderCopy),
                    Triple(3, "My Library", Icons.Default.LibraryMusic),
                    Triple(4, "Storage", Icons.Default.Storage)
                )

                items.forEach { (index, title, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isPermissionGranted) {
                // Permission warning card layout placeholder
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permissions Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allow Storage reading permissions to automatically scan, display and play local video files/audios.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_VIDEO,
                                    Manifest.permission.READ_MEDIA_AUDIO
                                )
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(permissionsToRequest)
                        }
                    ) {
                        Text("Grant Permissions")
                    }
                }
            } else {
                // Main Switch Workspace
                when (selectedTab) {
                    0 -> VideosTabPanel(
                        videos = videos,
                        onPlayFile = onPlayFile,
                        onAddToPlaylist = { pendingAddToPlaylistFile = it }
                    )
                    1 -> AudiosTabPanel(
                        audios = audios,
                        onPlayFile = onPlayFile,
                        onAddToPlaylist = { pendingAddToPlaylistFile = it }
                    )
                    2 -> FoldersTabPanel(
                        folders = folders,
                        videos = videos,
                        audios = audios,
                        onPlayFile = onPlayFile
                    )
                    3 -> MyLibraryTabPanel(
                        viewModel = viewModel,
                        videos = videos,
                        audios = audios,
                        favorites = favorites,
                        recents = recents,
                        playlists = playlists,
                        onPlayFile = onPlayFile
                    )
                    4 -> StorageExplorerPanel(
                        onPlayFile = { path, isVideo ->
                            val fileObj = File(path)
                            if (fileObj.exists()) {
                                onPlayFile(
                                    MediaFile(
                                        path = path,
                                        title = fileObj.name,
                                        duration = 0,
                                        size = fileObj.length(),
                                        isVideo = isVideo,
                                        folderName = fileObj.parentFile?.name ?: "Unknown"
                                    ),
                                    emptyList()
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal dialog trigger to select Playlist to insert media item
    if (pendingAddToPlaylistFile != null) {
        val targetFile = pendingAddToPlaylistFile!!
        AlertDialog(
            onDismissRequest = { pendingAddToPlaylistFile = null },
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Adding: ${targetFile.title}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            showCreatePlaylistDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Playlist")
                    }

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No custom playlists created yet. Create a new one to save files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(playlists) { listObj ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addFileToPlaylist(listObj.id, targetFile)
                                            pendingAddToPlaylistFile = null
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = listObj.name, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingAddToPlaylistFile = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistNameInput,
                    onValueChange = { newPlaylistNameInput = it },
                    placeholder = { Text("E.g. Favorites, Gym, Slow tracks...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistNameInput.trim().isNotEmpty()) {
                            viewModel.createPlaylist(newPlaylistNameInput.trim())
                            newPlaylistNameInput = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Videos Tab Listing Layout
// -------------------------------------------------------------
@Composable
fun VideosTabPanel(
    videos: List<MediaFile>,
    onPlayFile: (MediaFile, List<MediaFile>) -> Unit,
    onAddToPlaylist: (MediaFile) -> Unit
) {
    if (videos.isEmpty()) {
        EmptyPlaceholderScreen(
            icon = Icons.Outlined.VideoLibrary,
            title = "No videos detected",
            description = "Ensure videos are added to your device folders or pull-to-sync files."
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(videos) { video ->
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayFile(video, videos) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .background(Color.Black.copy(alpha = 0.82f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Media visual card backdrop icon representation
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            modifier = Modifier.size(48.dp)
                        )

                        // Duration text badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = video.durationString,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Play Action overlay indicator
                        IconButton(
                            onClick = { onPlayFile(video, videos) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = video.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = video.sizeString,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onAddToPlaylist(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Audios Tab Listing Layout
// -------------------------------------------------------------
@Composable
fun AudiosTabPanel(
    audios: List<MediaFile>,
    onPlayFile: (MediaFile, List<MediaFile>) -> Unit,
    onAddToPlaylist: (MediaFile) -> Unit
) {
    if (audios.isEmpty()) {
        EmptyPlaceholderScreen(
            icon = Icons.Outlined.Audiotrack,
            title = "No audio files detected",
            description = "Ensure music tracks, audiobooks, or system sounds are saved in storage lists."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            items(audios) { audio ->
                ListItem(
                    modifier = Modifier.clickable { onPlayFile(audio, audios) },
                    headlineContent = {
                        Text(
                            text = audio.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = audio.artist ?: "Unknown Artist", maxLines = 1)
                            Text(text = "•")
                            Text(text = audio.sizeString)
                        }
                    },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = audio.durationString,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { onAddToPlaylist(audio) }) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// -------------------------------------------------------------
// Folders Tab Browsing Layout
// -------------------------------------------------------------
@Composable
fun FoldersTabPanel(
    folders: List<MediaFolder>,
    videos: List<MediaFile>,
    audios: List<MediaFile>,
    onPlayFile: (MediaFile, List<MediaFile>) -> Unit
) {
    var activeFolderObj by remember { mutableStateOf<MediaFolder?>(null) }

    if (activeFolderObj != null) {
        val selectedFolder = activeFolderObj!!
        // Filter elements belonging to this folder.
        val folderFiles = remember(selectedFolder, videos, audios) {
            val combined = videos + audios
            combined.filter { File(it.path).parent == selectedFolder.path }
        }

        BackHandler {
            activeFolderObj = null
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeFolderObj = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to Folders")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = selectedFolder.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${folderFiles.size} items in directory path",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(folderFiles) { file ->
                    ListItem(
                        modifier = Modifier.clickable { onPlayFile(file, folderFiles) },
                        headlineContent = { Text(text = file.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(text = "${file.sizeString} • ${file.durationString}") },
                        leadingContent = {
                            Icon(
                                imageVector = if (file.isVideo) Icons.Default.Movie else Icons.Default.AudioFile,
                                contentDescription = if (file.isVideo) "Video" else "Audio",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
    } else {
        if (folders.isEmpty()) {
            EmptyPlaceholderScreen(
                icon = Icons.Outlined.FolderCopy,
                title = "No folders identified",
                description = "We couldn't spot valid folder targets containing video or music stems."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(folders) { folder ->
                    ListItem(
                        modifier = Modifier.clickable { activeFolderObj = folder },
                        headlineContent = {
                            Text(text = folder.name, fontWeight = FontWeight.Bold)
                        },
                        supportingContent = {
                            Text(
                                text = "Path: ${folder.path}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = "Folder",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(42.dp)
                            )
                        },
                        trailingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${folder.videoCount}🎬, ${folder.audioCount}🎵",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Playlists, Favorites and Recents Panel Workspace
// -------------------------------------------------------------
@Composable
fun MyLibraryTabPanel(
    viewModel: MediaViewModel,
    videos: List<MediaFile>,
    audios: List<MediaFile>,
    favorites: List<FavoriteEntity>,
    recents: List<RecentEntity>,
    playlists: List<PlaylistEntity>,
    onPlayFile: (MediaFile, List<MediaFile>) -> Unit
) {
    var selectedPlaylistId by remember { mutableStateOf<Int?>(null) }
    var selectedPlaylistName by remember { mutableStateOf("") }

    if (selectedPlaylistId != null) {
        val activePlaylistIdVal = selectedPlaylistId!!
        val playlistItemsFlow = remember(activePlaylistIdVal) {
            viewModel.getPlaylistItems(activePlaylistIdVal)
        }
        val pItems by playlistItemsFlow.collectAsState(initial = emptyList())

        // Map PlaylistItemEntity elements to active MediaFile
        val playlistMediaFiles = remember(pItems, videos, audios) {
            pItems.mapNotNull { item ->
                (videos + audios).find { it.path == item.mediaPath } ?: MediaFile(
                    path = item.mediaPath,
                    title = item.title,
                    isVideo = item.isVideo,
                    duration = item.duration,
                    size = 0,
                    folderName = "Playlist"
                )
            }
        }

        BackHandler {
            selectedPlaylistId = null
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedPlaylistId = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = selectedPlaylistName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${playlistMediaFiles.size} tracks added",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = {
                    viewModel.deletePlaylist(activePlaylistIdVal)
                    selectedPlaylistId = null
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (playlistMediaFiles.isEmpty()) {
                EmptyPlaceholderScreen(
                    icon = Icons.Outlined.PlaylistPlay,
                    title = "Empty Playlist",
                    description = "No media files added to this playlist yet."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(playlistMediaFiles) { itemFile ->
                        ListItem(
                            modifier = Modifier.clickable { onPlayFile(itemFile, playlistMediaFiles) },
                            headlineContent = { Text(itemFile.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(text = "${itemFile.sizeString} • ${itemFile.durationString}") },
                            leadingContent = {
                                Icon(
                                    imageVector = if (itemFile.isVideo) Icons.Default.Movie else Icons.Default.AudioFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    viewModel.removeFileFromPlaylist(activePlaylistIdVal, itemFile.path)
                                }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    } else {
        // Normal summary lists
        var currentSection by remember { mutableStateOf(0) } // 0: Playlists, 1: Favorites, 2: History

        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = currentSection) {
                Tab(
                    selected = currentSection == 0,
                    onClick = { currentSection = 0 },
                    text = { Text("Playlists") }
                )
                Tab(
                    selected = currentSection == 1,
                    onClick = { currentSection = 1 },
                    text = { Text("Favorites") }
                )
                Tab(
                    selected = currentSection == 2,
                    onClick = { currentSection = 2 },
                    text = { Text("History") }
                )
            }

            when (currentSection) {
                0 -> {
                    // Custom user playlists
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Playlists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.createPlaylist("Playlist " + (playlists.size + 1)) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New")
                        }
                    }

                    if (playlists.isEmpty()) {
                        EmptyPlaceholderScreen(
                            icon = Icons.Outlined.FeaturedPlayList,
                            title = "No Playlists",
                            description = "Organize files by categories."
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(playlists) { listObj ->
                                ListItem(
                                    modifier = Modifier.clickable {
                                        selectedPlaylistId = listObj.id
                                        selectedPlaylistName = listObj.name
                                    },
                                    headlineContent = {
                                        Text(listObj.name, fontWeight = FontWeight.Bold)
                                    },
                                    supportingContent = {
                                        Text("Tap to review contents")
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.FeaturedPlayList,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    },
                                    trailingContent = {
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
                1 -> {
                    // Favorites Elements Lists
                    val favMediaFiles = remember(favorites, videos, audios) {
                        favorites.mapNotNull { fav ->
                            (videos + audios).find { it.path == fav.mediaPath } ?: MediaFile(
                                path = fav.mediaPath,
                                title = fav.title,
                                isVideo = fav.isVideo,
                                duration = fav.duration,
                                size = 0,
                                folderName = "Favorite"
                            )
                        }
                    }

                    if (favMediaFiles.isEmpty()) {
                        EmptyPlaceholderScreen(
                            icon = Icons.Outlined.FavoriteBorder,
                            title = "No Favorites Yet",
                            description = "Tap the heart icon next to any song or video while playing to save here."
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(favMediaFiles) { file ->
                                ListItem(
                                    modifier = Modifier.clickable { onPlayFile(file, favMediaFiles) },
                                    headlineContent = { Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text("${file.sizeString} • ${file.durationString}") },
                                    leadingContent = {
                                        Icon(
                                            imageVector = if (file.isVideo) Icons.Default.Movie else Icons.Default.AudioFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { viewModel.toggleFavorite(file) }) {
                                            Icon(Icons.Default.Favorite, contentDescription = "Remove From Favorites", tint = Color.Red)
                                        }
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
                2 -> {
                    // History files elements
                    val recentMediaFiles = remember(recents, videos, audios) {
                        recents.mapNotNull { recent ->
                            (videos + audios).find { it.path == recent.mediaPath } ?: MediaFile(
                                path = recent.mediaPath,
                                title = recent.title,
                                isVideo = recent.isVideo,
                                duration = recent.duration,
                                size = 0,
                                folderName = "History"
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent history",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (recentMediaFiles.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (recentMediaFiles.isEmpty()) {
                        EmptyPlaceholderScreen(
                            icon = Icons.Outlined.History,
                            title = "History Clear",
                            description = "Play matching audio or video files to review tracks history."
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(recentMediaFiles) { item ->
                                ListItem(
                                    modifier = Modifier.clickable { onPlayFile(item, recentMediaFiles) },
                                    headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text("Played from folder: ${item.folderName}") },
                                    leadingContent = {
                                        Icon(
                                            imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.AudioFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIRECT FILE SYSTEM EXPLORER integration panel
// -------------------------------------------------------------
@Composable
fun StorageExplorerPanel(
    onPlayFile: (String, Boolean) -> Unit
) {
    val rootStoragePath = Environment.getExternalStorageDirectory()
    var currentDirPath by remember { mutableStateOf<File>(rootStoragePath) }

    // List of directory files in current path
    val filesList = remember(currentDirPath) {
        try {
            currentDirPath.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    BackHandler(enabled = currentDirPath != rootStoragePath) {
        currentDirPath = currentDirPath.parentFile ?: rootStoragePath
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentDirPath.absolutePath.replace(rootStoragePath.absolutePath, "Storage"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (currentDirPath != rootStoragePath) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { currentDirPath = currentDirPath.parentFile ?: rootStoragePath }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Back to Parent Folder...", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }

        if (filesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This folder is empty or inaccessible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filesList) { file ->
                    val isVideo = file.name.endsWith(".mp4", ignoreCase = true) || file.name.endsWith(".mkv", ignoreCase = true) || file.name.endsWith(".3gp", ignoreCase = true)
                    val isAudio = file.name.endsWith(".mp3", ignoreCase = true) || file.name.endsWith(".wav", ignoreCase = true) || file.name.endsWith(".m4a", ignoreCase = true) || file.name.endsWith(".aac", ignoreCase = true)
                    val isSubtitle = file.name.endsWith(".srt", ignoreCase = true)

                    val isPlayable = isVideo || isAudio

                    ListItem(
                        modifier = Modifier.clickable {
                            if (file.isDirectory) {
                                currentDirPath = file
                            } else if (isPlayable) {
                                onPlayFile(file.absolutePath, isVideo)
                            }
                        },
                        headlineContent = {
                            Text(
                                text = file.name,
                                fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPlayable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            if (file.isDirectory) {
                                Text("Folder Directory")
                            } else {
                                Text(text = formatSize(file.length()))
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = when {
                                    file.isDirectory -> Icons.Default.Folder
                                    isVideo -> Icons.Default.Movie
                                    isAudio -> Icons.Default.Audiotrack
                                    isSubtitle -> Icons.Default.ClosedCaption
                                    else -> Icons.Default.InsertDriveFile
                                },
                                contentDescription = null,
                                tint = when {
                                    file.isDirectory -> MaterialTheme.colorScheme.secondary
                                    isPlayable -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Helper Display Composables
// -------------------------------------------------------------
@Composable
fun EmptyPlaceholderScreen(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.61f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Permissions verification extension helper
fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
