import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

giant_block = """@Composable
fun ExploreAlbumArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.filteredAlbumArtists.collectAsState()
    val sortCriteria by viewModel.albumArtistsSortCriteria.collectAsState()
    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    
    if (artists.isEmpty()) {
        EmptyStateBlock("No album artists found. Refresh or adjust search.")
        return
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val scrollbarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .drawGridScrollbar(gridState, scrollbarColor)
        ) {
            items(artists) { artist ->
                ArtistCard(
                    artist = artist,
                    artworkUrl = viewModel.getArtworkUrl(artist.id),
                    onClick = {
                        keyboardController?.hide()
                        viewModel.selectArtist(artist)
                    }
                )
            }
        }
        if (isAlphabetical && gridState.isScrollInProgress) {
            AlphabetOverlay(firstVisibleItemIndex = gridState.firstVisibleItemIndex, items = artists)
        }
    }
}

@Composable
fun ExploreArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.filteredArtists.collectAsState()
    val sortCriteria by viewModel.artistsSortCriteria.collectAsState()
    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    
    if (artists.isEmpty()) {
        EmptyStateBlock("No artists found. Refresh or adjust search.")
        return
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val scrollbarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .drawGridScrollbar(gridState, scrollbarColor)
        ) {
            items(artists) { artist ->
                ArtistCard(
                    artist = artist,
                    artworkUrl = viewModel.getArtworkUrl(artist.id),
                    onClick = {
                        keyboardController?.hide()
                        viewModel.selectArtist(artist)
                    }
                )
            }
        }
        if (isAlphabetical && gridState.isScrollInProgress) {
            AlphabetOverlay(firstVisibleItemIndex = gridState.firstVisibleItemIndex, items = artists)
        }
    }
}

@Composable
fun ExploreAlbumsGrid(viewModel: JellyTuneViewModel) {
    val albums by viewModel.filteredAlbums.collectAsState()
    val sortCriteria by viewModel.albumsSortCriteria.collectAsState()
    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    var activeActionAlbum by remember { mutableStateOf<com.example.data.jellyfin.JellyfinItem?>(null) }
    
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

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val scrollbarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .drawGridScrollbar(gridState, scrollbarColor)
        ) {
            items(albums) { album ->
                AlbumCard(
                    album = album,
                    artworkUrl = viewModel.getArtworkUrl(album.id),
                    onClick = {
                        keyboardController?.hide()
                        viewModel.selectAlbum(album)
                    },
                    onLongClick = { activeActionAlbum = album }
                )
            }
        }
        if (isAlphabetical && gridState.isScrollInProgress) {
            AlphabetOverlay(firstVisibleItemIndex = gridState.firstVisibleItemIndex, items = albums)
        }
    }
}

@Composable
fun AlbumActionDialog(
    album: com.example.data.jellyfin.JellyfinItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = {
            Text("Options for ${album.name}", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.TextButton(onClick = { onPlay(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                        Text("Play Next", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                androidx.compose.material3.TextButton(onClick = { onAddToQueue(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                        Text("Add to Queue", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                androidx.compose.material3.TextButton(onClick = { onDownload(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
                        Text("Download Offline", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    )
}

@Composable
fun ExploreSongsList(viewModel: JellyTuneViewModel) {
    val songs by viewModel.filteredSongs.collectAsState()
    val currentSong = viewModel.playbackState.collectAsState().value.currentSong
    val cachedSongs by viewModel.cachedSongs.collectAsState(initial = emptyList())
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())
    val sortCriteria by viewModel.songsSortCriteria.collectAsState()
    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    
    if (songs.isEmpty()) {
        EmptyStateBlock("No songs found. Refresh or adjust search.")
        return
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scrollbarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            modifier = Modifier
                .fillMaxSize()
                .drawListScrollbar(listState, scrollbarColor)
        ) {
            items(songs) { song ->
                val isPlaying = currentSong?.id == song.id
                val isCached = cachedSongs.any { it.songId == song.id }
                val isFav = localFavs.any { it.songId == song.id }
                TrackListItem(
                    song = song,
                    artworkUrl = viewModel.getArtworkUrl(song.id),
                    isPlaying = isPlaying,
                    isCached = isCached,
                    isFavorite = isFav,
                    onClick = {
                        keyboardController?.hide()
                        viewModel.playSongNow(song, songs)
                    },
                    onDownload = {
                        viewModel.downloadSongOffline(song)
                    }
                )
            }
        }
        if (isAlphabetical && listState.isScrollInProgress) {
            AlphabetOverlay(firstVisibleItemIndex = listState.firstVisibleItemIndex, items = songs)
        }
    }
}

"""

# Replace everything from ExploreAlbumArtistsGrid to just before ArtistCard
content = re.sub(r'@Composable\s*fun ExploreAlbumArtistsGrid[\s\S]*?(?=@Composable\s*fun ArtistCard)', giant_block, content)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)

