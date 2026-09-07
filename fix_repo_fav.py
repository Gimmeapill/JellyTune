import re

with open("/app/applet/app/src/main/java/com/example/data/repository/JellyfinRepository.kt", "r") as f:
    content = f.read()

old_toggle = """    suspend fun toggleFavorite(song: JellyfinItem) = withContext(Dispatchers.IO) {
        val server = _activeServer.value ?: return@withContext
        val isFav = favoriteDao.isFavorite(song.id)
        if (isFav) {
            favoriteDao.deleteFavorite(song.id)
        } else {
            val fav = LocalFavorite(
                songId = song.id,
                serverUrl = server.serverUrl,
                title = song.name,
                artist = song.albumArtist ?: song.artists?.firstOrNull() ?: "Unknown Artist",
                album = song.albumName ?: "Unknown Album",
                durationMs = song.durationMs,
                indexNumber = song.indexNumber,
                parentIndexNumber = song.parentIndexNumber
            )
            favoriteDao.insertFavorite(fav)
        }

        // Sync favorite with server if we're not in demo mode
        if (!isDemo()) {
            client.toggleFavoriteOnServer(server.serverUrl, server.token, server.userId, song.id, !isFav)
        }
    }"""

new_toggle = """    suspend fun toggleFavorite(song: JellyfinItem) = withContext(Dispatchers.IO) {
        val server = _activeServer.value ?: return@withContext
        val isFavLocally = favoriteDao.isFavorite(song.id)
        val currentlyFav = isFavLocally || song.userData?.isFavorite == true
        
        if (currentlyFav) {
            favoriteDao.deleteFavorite(song.id)
        } else {
            val fav = LocalFavorite(
                songId = song.id,
                serverUrl = server.serverUrl,
                title = song.name,
                artist = song.albumArtist ?: song.artists?.firstOrNull() ?: "Unknown Artist",
                album = song.albumName ?: "Unknown Album",
                durationMs = song.durationMs,
                indexNumber = song.indexNumber,
                parentIndexNumber = song.parentIndexNumber
            )
            favoriteDao.insertFavorite(fav)
        }

        // Sync favorite with server if we're not in demo mode
        if (!isDemo()) {
            client.toggleFavoriteOnServer(server.serverUrl, server.token, server.userId, song.id, !currentlyFav)
        }
    }"""

content = content.replace(old_toggle, new_toggle)

with open("/app/applet/app/src/main/java/com/example/data/repository/JellyfinRepository.kt", "w") as f:
    f.write(content)
