import re

with open("/app/applet/app/src/main/java/com/example/data/repository/JellyfinRepository.kt", "r") as f:
    content = f.read()

toggle_fav_function = """    suspend fun toggleFavorite(song: JellyfinItem) = withContext(Dispatchers.IO) {
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

new_toggle_fav = """    suspend fun toggleFavorite(song: JellyfinItem) = withContext(Dispatchers.IO) {
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
        
        // Update local memory models so the UI updates if userData was originally true
        val updateItem = { item: JellyfinItem ->
            if (item.id == song.id) {
                item.copy(userData = item.userData?.copy(isFavorite = !isFav) ?: com.example.data.jellyfin.UserData(isFavorite = !isFav))
            } else item
        }
        _albums.value = _albums.value.map(updateItem)
        _artists.value = _artists.value.map(updateItem)
        _songs.value = _songs.value.map(updateItem)

        // Sync favorite with server if we're not in demo mode
        if (!isDemo()) {
            client.toggleFavoriteOnServer(server.serverUrl, server.token, server.userId, song.id, !isFav)
        }
    }"""

content = content.replace(toggle_fav_function, new_toggle_fav)

with open("/app/applet/app/src/main/java/com/example/data/repository/JellyfinRepository.kt", "w") as f:
    f.write(content)
print("Updated JellyfinRepository")
