import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

old_toggle = """    fun toggleFavorite(song: JellyfinItem) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }"""
new_toggle = """    fun toggleFavorite(song: JellyfinItem) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
            val currentIsFav = song.userData?.isFavorite == true || localFavorites.value.any { it.songId == song.id }
            val newIsFav = !currentIsFav
            val updateItem = { i: com.example.data.jellyfin.JellyfinItem ->
                if (i.id == song.id) {
                    i.copy(userData = i.userData?.copy(isFavorite = newIsFav) ?: com.example.data.jellyfin.UserData(isFavorite = newIsFav))
                } else i
            }
            _albums.value = _albums.value.map(updateItem)
            _artists.value = _artists.value.map(updateItem)
            _songs.value = _songs.value.map(updateItem)
        }
    }"""
content = content.replace(old_toggle, new_toggle)

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
