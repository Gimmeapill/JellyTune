package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.CachedSong
import com.example.data.database.LocalFavorite
import com.example.data.jellyfin.JellyfinItem
import com.example.playback.PlaybackState
import com.example.playback.toJellyfinItem
import com.example.ui.JellyTuneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLibraryScreen(
    viewModel: JellyTuneViewModel,
    onExpandPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val activeServer by viewModel.activeServer.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()

    // Handle android system back gesture inside sub-screens and secondary tabs to prevent premature app exit
    if (selectedAlbum != null) {
        BackHandler {
            viewModel.selectAlbum(null)
        }
    } else if (selectedArtist != null) {
        BackHandler {
            viewModel.selectArtist(null)
        }
    } else if (activeTab != 0) {
        BackHandler {
            activeTab = 0
        }
    }

    // Safety check: force reload on start
    LaunchedEffect(Unit) {
        viewModel.loadLibrary()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Sticky Mini Player
                if (playbackState.currentSong != null) {
                    MiniPlayer(
                        playbackState = playbackState,
                        artworkUrl = viewModel.getArtworkUrl(playbackState.currentSong?.id ?: ""),
                        onPlayPauseToggle = { viewModel.playbackManager.togglePlayPause() },
                        onSkipNext = { viewModel.playbackManager.skipNext() },
                        onClick = onExpandPlayer
                    )
                }

                // Standard M3 Navigation Bottom Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val tabs = listOf(
                        Triple("Explore", Icons.Default.LibraryMusic, 0),
                        Triple("Favorites", Icons.Default.Favorite, 1),
                        Triple("Cached", Icons.Default.CloudDone, 2),
                        Triple("Settings", Icons.Default.Settings, 3)
                    )
                    tabs.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = activeTab == index,
                            onClick = {
                                activeTab = index
                                // Reset sub-views when backing out or changing layouts
                                viewModel.selectAlbum(null)
                                viewModel.selectArtist(null)
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // JellyTune Custom Header
                HeaderBlock(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onRefresh = { viewModel.refreshLibrary() },
                    serverName = activeServer?.username ?: "Demo"
                )

                // Render respective tab layout containing search queries
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (activeTab) {
                        0 -> ExploreTab(viewModel)
                        1 -> FavoritesTab(viewModel)
                        2 -> CachedTab(viewModel)
                        3 -> SettingsTab(viewModel)
                    }
                }
            }

            // Slide-over Screen for Artist Info Details
            AnimatedVisibility(
                visible = selectedArtist != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                selectedArtist?.let { artist ->
                    ArtistDetailsScreen(
                        artist = artist,
                        viewModel = viewModel,
                        onBack = { viewModel.selectArtist(null) }
                    )
                }
            }

            // Slide-over Screen for Album Info Details
            AnimatedVisibility(
                visible = selectedAlbum != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                selectedAlbum?.let { album ->
                    AlbumDetailsScreen(
                        album = album,
                        viewModel = viewModel,
                        onBack = { viewModel.selectAlbum(null) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreHeroCard(viewModel: JellyTuneViewModel) {
    val songs by viewModel.filteredSongs.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val activeServer by viewModel.activeServer.collectAsState()

    val featureTitle: String
    val featureArtist: String
    val featureAlbum: String
    val featureArtUrl: String?
    val playAction: () -> Unit

    val currentSong = playbackState.currentSong
    if (currentSong != null) {
        featureTitle = currentSong.name
        featureArtist = currentSong.albumArtist ?: "Unknown Artist"
        featureAlbum = currentSong.albumName ?: ""
        featureArtUrl = viewModel.getArtworkUrl(currentSong.id)
        playAction = { viewModel.playbackManager.togglePlayPause() }
    } else if (songs.isNotEmpty()) {
        val firstSong = songs.first()
        featureTitle = firstSong.name
        featureArtist = firstSong.albumArtist ?: "Unknown Artist"
        featureAlbum = firstSong.albumName ?: ""
        featureArtUrl = viewModel.getArtworkUrl(firstSong.id)
        playAction = { viewModel.playTrackInQueue(songs, 0) }
    } else {
        featureTitle = "Midnight City"
        featureArtist = "M83"
        featureAlbum = "Hurry Up, We're Dreaming"
        featureArtUrl = null
        playAction = {}
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (featureArtUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(featureArtUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "STREAMING FROM SERVER: ${activeServer?.username ?: "Demo Mode"}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = featureTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$featureArtist • $featureAlbum",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.secondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.Button(
                    onClick = playAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (currentSong != null && playbackState.isPlaying) "Pause" else "Play Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.playbackManager.toggleShuffle() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Close / Dismiss button aligned to TopEnd of the Box container
        IconButton(
            onClick = { viewModel.setHeroCardVisibility(false) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hide Featured",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// --- EXPLORE VIEW & THE SUB-TABS (ALBUM, ARTIST, TRACKS) ---

@Composable
fun ExploreTab(viewModel: JellyTuneViewModel) {
    var subTab by remember { mutableIntStateOf(0) }
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showHeroCard by viewModel.showHeroCard.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (searchQuery.isEmpty() && showHeroCard) {
            ExploreHeroCard(viewModel = viewModel)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                modifier = Modifier.weight(1f),
                selectedTabIndex = subTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[subTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                val subTabs = listOf("Artists", "Albums", "Tracks")
                subTabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        text = {
                            Text(
                                label,
                                fontWeight = if (subTab == idx) FontWeight.Bold else FontWeight.Medium,
                                color = if (subTab == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            IconButton(
                onClick = { viewModel.setHeroCardVisibility(!showHeroCard) },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (showHeroCard) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showHeroCard) "Hide Hero" else "Show Hero",
                    tint = if (showHeroCard) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (subTab) {
                0 -> ExploreArtistsGrid(viewModel)
                1 -> ExploreAlbumsGrid(viewModel)
                2 -> ExploreSongsList(viewModel)
            }
            if (isLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExploreAlbumsGrid(viewModel: JellyTuneViewModel) {
    val albums by viewModel.filteredAlbums.collectAsState()
    var activeActionAlbum by remember { mutableStateOf<JellyfinItem?>(null) }

    if (albums.isEmpty()) {
        EmptyStateBlock("No albums found. Refresh or adjust search.")
        return
    }

    if (activeActionAlbum != null) {
        AlbumActionDialog(
            album = activeActionAlbum!!,
            onDismiss = { activeActionAlbum = null },
            onPlay = { viewModel.playAlbumNow(activeActionAlbum!!) },
            onAddToQueue = { viewModel.appendAlbumToQueue(activeActionAlbum!!) },
            onDownload = { viewModel.downloadAlbumOffline(activeActionAlbum!!) }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums) { album ->
            AlbumCard(
                album = album,
                artworkUrl = viewModel.getArtworkUrl(album.id),
                onClick = { viewModel.selectAlbum(album) },
                onLongClick = { activeActionAlbum = album }
            )
        }
    }
}

@Composable
fun AlbumActionDialog(
    album: JellyfinItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Text(
                text = album.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPlay()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Now",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Play Now",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddToQueue()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Queue,
                        contentDescription = "Add to Play Queue",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Add to play queue",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDownload()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Add to Local Cache Only",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Add to local cache only",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ExploreArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.filteredArtists.collectAsState()

    if (artists.isEmpty()) {
        EmptyStateBlock("No artists found.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists) { artist ->
            ArtistCard(
                artist = artist,
                artworkUrl = viewModel.getArtworkUrl(artist.id),
                onClick = { viewModel.selectArtist(artist) }
            )
        }
    }
}

@Composable
fun ExploreSongsList(viewModel: JellyTuneViewModel) {
    val songs by viewModel.filteredSongs.collectAsState()
    val currentSong = viewModel.playbackState.collectAsState().value.currentSong
    val cachedSongs by viewModel.cachedSongs.collectAsState(initial = emptyList())
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())
    val dlMap by viewModel.downloadProgress.collectAsState()

    if (songs.isEmpty()) {
        EmptyStateBlock("No songs found.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        itemsIndexed(songs) { idx, song ->
            val isCurrent = song.id == currentSong?.id
            val isCached = cachedSongs.any { it.songId == song.id }
            val isFav = localFavs.any { it.songId == song.id }
            val dlProgress = dlMap[song.id]

            SongItemRow(
                index = idx + 1,
                song = song,
                artworkUrl = viewModel.getArtworkUrl(song.id),
                isCurrent = isCurrent,
                isCached = isCached,
                isFav = isFav,
                dlProgress = dlProgress,
                onPlay = { viewModel.playTrackInQueue(songs, idx) },
                onFavToggle = { viewModel.toggleFavorite(song) },
                onCacheClick = {
                    if (isCached) {
                        viewModel.deleteSongFromCache(song.id)
                    } else {
                        viewModel.downloadAndCacheTrack(song)
                    }
                }
            )
        }
    }
}

// --- SUB TABS COMPOSABLES FOR INDIVIDUAL TILES ---

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AlbumCard(
    album: JellyfinItem,
    artworkUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUrl)
                    .crossfade(true)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .build(),
                contentDescription = "${album.name} album cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = album.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.albumArtist ?: "Various Artists",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ArtistCard(
    artist: JellyfinItem,
    artworkUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = artist.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SongItemRow(
    index: Int,
    song: JellyfinItem,
    artworkUrl: String,
    isCurrent: Boolean,
    isCached: Boolean,
    isFav: Boolean,
    dlProgress: Float?,
    onPlay: () -> Unit,
    onFavToggle: () -> Unit,
    onCacheClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index indicator
        Text(
            text = index.toString(),
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(32.dp)
        )

        // Thumbnail cover art
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.albumArtist ?: song.artists?.firstOrNull() ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action Buttons: Cache status download indicator and local Favorite Bookmark icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Precise Caching Button
            IconButton(onClick = onCacheClick) {
                if (dlProgress != null) {
                    // Downloading in real time
                    CircularProgressIndicator(
                        progress = { dlProgress },
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isCached) Icons.Default.CloudDone else Icons.Default.ArrowDownward,
                        contentDescription = "Cache Song Offline",
                        tint = if (isCached) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Favorite button
            IconButton(onClick = onFavToggle) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Add to Favorites",
                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// --- SEC-TIONAL DETAILS OVERLAYS FOR ARTISTS & ALBUMS ---

@Composable
fun AlbumDetailsScreen(
    album: JellyfinItem,
    viewModel: JellyTuneViewModel,
    onBack: () -> Unit
) {
    val artworkUrl = viewModel.getArtworkUrl(album.id)
    val albumSongs by viewModel.albumSongs.collectAsState()
    val currentSong = viewModel.playbackState.collectAsState().value.currentSong
    val cachedSongs by viewModel.cachedSongs.collectAsState(initial = emptyList())
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Album Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Header album art design
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUrl)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = album.albumArtist ?: "Various Artists",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "${albumSongs.size} Tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Tracks
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                itemsIndexed(albumSongs) { idx, song ->
                    val isCurrent = song.id == currentSong?.id
                    val isCached = cachedSongs.any { it.songId == song.id }
                    val isFav = localFavs.any { it.songId == song.id }

                    SongItemRow(
                        index = idx + 1,
                        song = song,
                        artworkUrl = viewModel.getArtworkUrl(song.id),
                        isCurrent = isCurrent,
                        isCached = isCached,
                        isFav = isFav,
                        dlProgress = null,
                        onPlay = { viewModel.playTrackInQueue(albumSongs, idx) },
                        onFavToggle = { viewModel.toggleFavorite(song) },
                        onCacheClick = {
                            if (isCached) {
                                viewModel.deleteSongFromCache(song.id)
                            } else {
                                viewModel.downloadAndCacheTrack(song)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ArtistDetailsScreen(
    artist: JellyfinItem,
    viewModel: JellyTuneViewModel,
    onBack: () -> Unit
) {
    val artworkUrl = viewModel.getArtworkUrl(artist.id)
    val artistSongs by viewModel.artistSongs.collectAsState()
    val artistAlbums by viewModel.artistAlbums.collectAsState()
    val currentSong = viewModel.playbackState.collectAsState().value.currentSong
    val cachedSongs by viewModel.cachedSongs.collectAsState(initial = emptyList())
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())

    var activeActionAlbum by remember { mutableStateOf<JellyfinItem?>(null) }

    if (activeActionAlbum != null) {
        AlbumActionDialog(
            album = activeActionAlbum!!,
            onDismiss = { activeActionAlbum = null },
            onPlay = { viewModel.playAlbumNow(activeActionAlbum!!) },
            onAddToQueue = { viewModel.appendAlbumToQueue(activeActionAlbum!!) },
            onDownload = { viewModel.downloadAlbumOffline(activeActionAlbum!!) }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Artist Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Artist Banner Cover
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUrl)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = "${artistAlbums.size} Albums • ${artistSongs.size} Songs available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                // Feature 2: Display artist's albums vertically as a proper list
                if (artistAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(artistAlbums) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { viewModel.selectAlbum(album) },
                                    onLongClick = { activeActionAlbum = album }
                                )
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(viewModel.getArtworkUrl(album.id))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = album.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.albumArtist ?: "Various Artists",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Header for songs section
                if (artistSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    itemsIndexed(artistSongs) { idx, song ->
                        val isCurrent = song.id == currentSong?.id
                        val isCached = cachedSongs.any { it.songId == song.id }
                        val isFav = localFavs.any { it.songId == song.id }

                        SongItemRow(
                            index = idx + 1,
                            song = song,
                            artworkUrl = viewModel.getArtworkUrl(song.id),
                            isCurrent = isCurrent,
                            isCached = isCached,
                            isFav = isFav,
                            dlProgress = null,
                            onPlay = { viewModel.playTrackInQueue(artistSongs, idx) },
                            onFavToggle = { viewModel.toggleFavorite(song) },
                            onCacheClick = {
                                if (isCached) {
                                    viewModel.deleteSongFromCache(song.id)
                                } else {
                                    viewModel.downloadAndCacheTrack(song)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- LOCAL PERSISTENT SAVED AUDIO TABS (FAVORITE & OFFLINE CACHES) ---

@Composable
fun FavoritesTab(viewModel: JellyTuneViewModel) {
    val favorites by viewModel.localFavorites.collectAsState(initial = emptyList())

    if (favorites.isEmpty()) {
        EmptyStateBlock("No favorites added yet.\nPress the heart icon beside any track.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(favorites) { idx, favorite ->
            val mappedSongItem = favorite.toJellyfinItem()
            val playbackState = viewModel.playbackState.collectAsState().value
            val isCurrent = mappedSongItem.id == playbackState.currentSong?.id

            Row(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .clickable { viewModel.playFavoriteSong(favorite) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(viewModel.getArtworkUrl(favorite.songId))
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = favorite.title,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = favorite.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { viewModel.toggleFavoriteFav(favorite) }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Remove from Favorites"
                    )
                }
            }
        }
    }
}

@Composable
fun CachedTab(viewModel: JellyTuneViewModel) {
    val cachedList by viewModel.cachedSongs.collectAsState(initial = emptyList())

    if (cachedList.isEmpty()) {
        EmptyStateBlock("No cached offline songs.\nWe will cache tracks as you stream, or you can manually save tracks from the grid.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(cachedList) { song ->
            val mappedSongItem = song.toJellyfinItem()
            val playbackState = viewModel.playbackState.collectAsState().value
            val isCurrent = mappedSongItem.id == playbackState.currentSong?.id

            Row(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .clickable { viewModel.playCachedSong(song) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(viewModel.getArtworkUrl(song.songId))
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { viewModel.deleteSongFromCache(song.songId) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        contentDescription = "Delete Copy"
                    )
                }
            }
        }
    }
}

// --- SETTINGS CONTROL PANEL COMPOSABLE TAB ---

@Composable
fun SettingsTab(viewModel: JellyTuneViewModel) {
    val server by viewModel.activeServer.collectAsState()
    val cachedList by viewModel.cachedSongs.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Active Session",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Logged in as:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = server?.username ?: "Demo User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Server Endpoint:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = server?.serverUrl ?: "http://demo.jellytune.local",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect & Log Out", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Offline Memory / Storage",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Wipe Storage Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cached Tracks:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${cachedList.size} cached items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.clearLocalCache() },
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                    ) {
                        Text("Wipe Storage", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                // Offline Mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Mode Only",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Only display and play tracks stored in your local storage cache.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    val isOffline by viewModel.offlineMode.collectAsState()
                    androidx.compose.material3.Switch(
                        checked = isOffline,
                        onCheckedChange = { viewModel.setOfflineMode(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                val maxLimitMb by viewModel.maxCacheSizeMb.collectAsState()
                val currentSizeMb by viewModel.currentCacheSizeMb.collectAsState()
                val limits = listOf(250L, 500L, 1024L, 2048L, 5120L, Long.MAX_VALUE)

                Text(
                    text = "Max Cache Limit • Used: ${String.format("%.2f", currentSizeMb)} MB",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Less-played songs are automatically pruned once this size limit is reached.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(limits) { limit ->
                        val isSelected = maxLimitMb == limit
                        val label = when (limit) {
                            Long.MAX_VALUE -> "Unlimited"
                            250L, 500L -> "$limit MB"
                            1024L -> "1 GB"
                            2048L -> "2 GB"
                            5120L -> "5 GB"
                            else -> "${limit / 1024} GB"
                        }

                        androidx.compose.material3.FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setMaxCacheSizeMb(limit) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(180.dp))
    }
}

// --- SUB-ELEMENTS: STICKY MINI PLAYER, HEADER & STANDBY UI BLOCKS ---

@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    artworkUrl: String,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit
) {
    val currentSong = playbackState.currentSong ?: return
    val progress = if (playbackState.durationMs > 0) playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat() else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUrl)
                            .build(),
                        contentDescription = "Currently playing artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong.albumArtist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayPauseToggle) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Toggle Player Action",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Accurate visual seek progress at bottom edge of Mini Player
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun HeaderBlock(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    serverName: String
) {
    var searchVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "JellyTune",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Mode: $serverName library",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { searchVisible = !searchVisible }) {
                    Icon(
                        imageVector = if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Filter Library"
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Items")
                }
            }
        }

        AnimatedVisibility(visible = searchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Filter items...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filter_input")
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "") }
            )
        }
    }
}

@Composable
fun EmptyStateBlock(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
