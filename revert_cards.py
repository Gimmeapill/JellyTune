import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# 1. Update AlbumCard signature
old_album_sig = """fun AlbumCard(
    album: com.example.data.jellyfin.JellyfinItem,
    artworkUrl: String,
    isFavorite: Boolean,
    onFavToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
)"""
new_album_sig = """fun AlbumCard(
    album: com.example.data.jellyfin.JellyfinItem,
    artworkUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
)"""
content = content.replace(old_album_sig, new_album_sig)

# Remove IconButton from AlbumCard
album_icon_box = """                IconButton(
                    onClick = onFavToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }"""
content = content.replace(album_icon_box, "")


# 2. Update ExploreAlbumsGrid
old_album_call = """                val isFav = album.userData?.isFavorite == true || localFavs.any { it.songId == album.id }
                AlbumCard(
                    album = album,
                    artworkUrl = viewModel.getArtworkUrl(album.id),
                    isFavorite = isFav,
                    onFavToggle = { viewModel.toggleFavorite(album) },
                    onClick = {"""
new_album_call = """                AlbumCard(
                    album = album,
                    artworkUrl = viewModel.getArtworkUrl(album.id),
                    onClick = {"""
content = content.replace(old_album_call, new_album_call)

# 3. Update ArtistCard signature
old_artist_sig = """fun ArtistCard(
    artist: JellyfinItem,
    artworkUrl: String,
    isFavorite: Boolean,
    onFavToggle: () -> Unit,
    onClick: () -> Unit
)"""
new_artist_sig = """fun ArtistCard(
    artist: JellyfinItem,
    artworkUrl: String,
    onClick: () -> Unit
)"""
content = content.replace(old_artist_sig, new_artist_sig)

# Remove IconButton from ArtistCard
artist_icon_box = """                IconButton(
                    onClick = onFavToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha=0.4f), androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }"""
content = content.replace(artist_icon_box, "")

# 4. Update ExploreArtistsGrid and ExploreAlbumArtistsGrid
old_artist_call = """                val isFav = artist.userData?.isFavorite == true || localFavs.any { it.songId == artist.id }
                ArtistCard(
                    artist = artist,
                    artworkUrl = viewModel.getArtworkUrl(artist.id),
                    isFavorite = isFav,
                    onFavToggle = { viewModel.toggleFavorite(artist) },
                    onClick = {"""
new_artist_call = """                ArtistCard(
                    artist = artist,
                    artworkUrl = viewModel.getArtworkUrl(artist.id),
                    onClick = {"""
content = content.replace(old_artist_call, new_artist_call)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
