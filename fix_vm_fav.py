import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

old_fav = """    fun toggleFavoriteFav(song: LocalFavorite) {
        viewModelScope.launch {
            repository.toggleFavorite(song.toJellyfinItem())
        }
    }"""
new_fav = """    fun toggleFavoriteFav(song: LocalFavorite) {
        viewModelScope.launch {
            val item = song.toJellyfinItem()
            repository.toggleFavorite(item)
            val newIsFav = repository.isFavorite(item.id)
            val updateItem = { i: com.example.data.jellyfin.JellyfinItem ->
                if (i.id == item.id) {
                    i.copy(userData = i.userData?.copy(isFavorite = newIsFav) ?: com.example.data.jellyfin.UserData(isFavorite = newIsFav))
                } else i
            }
            _albums.value = _albums.value.map(updateItem)
            _artists.value = _artists.value.map(updateItem)
            _songs.value = _songs.value.map(updateItem)
        }
    }"""
content = content.replace(old_fav, new_fav)

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
