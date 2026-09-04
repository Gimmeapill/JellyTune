import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# 1. Update AlbumCard signature and box
content = content.replace(
"""fun AlbumCard(
    album: com.example.data.jellyfin.JellyfinItem,
    artworkUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {""",
"""fun AlbumCard(
    album: com.example.data.jellyfin.JellyfinItem,
    artworkUrl: String,
    isFavorite: Boolean,
    onFavToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {"""
)

content = content.replace(
"""            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {""",
"""            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
"""
)
# We will just inject the icon at the end of the Box.
box_end = """                    Icon(
                        Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))"""
new_box_end = """                    Icon(
                        Icons.Default.Album,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                IconButton(
                    onClick = onFavToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))"""
content = content.replace(box_end, new_box_end)


# 2. Update ExploreAlbumsGrid
old_albums = """fun ExploreAlbumsGrid(viewModel: JellyTuneViewModel) {
    val albums by viewModel.displayAlbums.collectAsState()"""
new_albums = """fun ExploreAlbumsGrid(viewModel: JellyTuneViewModel) {
    val albums by viewModel.displayAlbums.collectAsState()
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())"""
content = content.replace(old_albums, new_albums)

old_album_call = """                AlbumCard(
                    album = album,
                    artworkUrl = viewModel.getArtworkUrl(album.id),
                    onClick = {"""
new_album_call = """                val isFav = album.userData?.isFavorite == true || localFavs.any { it.songId == album.id }
                AlbumCard(
                    album = album,
                    artworkUrl = viewModel.getArtworkUrl(album.id),
                    isFavorite = isFav,
                    onFavToggle = { viewModel.toggleFavorite(album) },
                    onClick = {"""
content = content.replace(old_album_call, new_album_call)


# 3. Update ArtistCard signature and box
content = content.replace(
"""fun ArtistCard(
    artist: JellyfinItem,
    artworkUrl: String,
    onClick: () -> Unit
) {""",
"""fun ArtistCard(
    artist: JellyfinItem,
    artworkUrl: String,
    isFavorite: Boolean,
    onFavToggle: () -> Unit,
    onClick: () -> Unit
) {"""
)

artist_box_end = """                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))"""
new_artist_box_end = """                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onFavToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))"""
content = content.replace(artist_box_end, new_artist_box_end)

# 4. Update ExploreArtistsGrid and ExploreAlbumArtistsGrid
old_artists1 = """fun ExploreArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.displayArtists.collectAsState()"""
new_artists1 = """fun ExploreArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.displayArtists.collectAsState()
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())"""
content = content.replace(old_artists1, new_artists1)

old_artists2 = """fun ExploreAlbumArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.displayAlbumArtists.collectAsState()"""
new_artists2 = """fun ExploreAlbumArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.displayAlbumArtists.collectAsState()
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())"""
content = content.replace(old_artists2, new_artists2)

old_artist_call = """                ArtistCard(
                    artist = artist,
                    artworkUrl = viewModel.getArtworkUrl(artist.id),
                    onClick = {"""
new_artist_call = """                val isFav = artist.userData?.isFavorite == true || localFavs.any { it.songId == artist.id }
                ArtistCard(
                    artist = artist,
                    artworkUrl = viewModel.getArtworkUrl(artist.id),
                    isFavorite = isFav,
                    onFavToggle = { viewModel.toggleFavorite(artist) },
                    onClick = {"""
content = content.replace(old_artist_call, new_artist_call)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
