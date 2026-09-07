import re

with open("/app/applet/app/src/main/java/com/example/data/repository/JellyfinRepository.kt", "r") as f:
    content = f.read()

bad_toggle = """        // Update local memory models so the UI updates if userData was originally true
        val updateItem = { item: JellyfinItem ->
            if (item.id == song.id) {
                item.copy(userData = item.userData?.copy(isFavorite = !isFav) ?: com.example.data.jellyfin.UserData(isFavorite = !isFav))
            } else item
        }
        _albums.value = _albums.value.map(updateItem)
        _artists.value = _artists.value.map(updateItem)
        _songs.value = _songs.value.map(updateItem)"""

content = content.replace(bad_toggle, "")

with open("/app/applet/app/src/main/java/com/example/data/repository/JellyfinRepository.kt", "w") as f:
    f.write(content)
