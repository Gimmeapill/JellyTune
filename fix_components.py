import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

components = {
    "ExploreAlbumArtistsGrid": """@Composable
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
""",
    "ExploreArtistsGrid": """@Composable
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
""",
    "ExploreAlbumsGrid": """@Composable
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
""",
    "ExploreSongsList": """@Composable
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
}

# Use regex to find and replace the functions from `@Composable\nfun Name` until the next `@Composable\nfun ` or end of appropriate block
# Since we know the order: ExploreAlbumArtistsGrid, ExploreArtistsGrid, ExploreAlbumsGrid, AlbumActionDialog (or similar), ExploreSongsList, ArtistCard
content = re.sub(r'@Composable\s*fun ExploreAlbumArtistsGrid\([\s\S]*?(?=@Composable\s*fun ExploreArtistsGrid)', components["ExploreAlbumArtistsGrid"] + "\n", content)
content = re.sub(r'@Composable\s*fun ExploreArtistsGrid\([\s\S]*?(?=@Composable\s*fun ExploreAlbumsGrid)', components["ExploreArtistsGrid"] + "\n", content)
content = re.sub(r'@Composable\s*fun ExploreAlbumsGrid\([\s\S]*?(?=@Composable\s*fun AlbumActionDialog)', components["ExploreAlbumsGrid"] + "\n", content)
content = re.sub(r'@Composable\s*fun ExploreSongsList\([\s\S]*?(?=@Composable\s*fun ArtistCard)', components["ExploreSongsList"] + "\n", content)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
