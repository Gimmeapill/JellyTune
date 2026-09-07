import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

# Helper block for selection update
selection_update = """            _albums.value = _albums.value.map(updateItem)
            _artists.value = _artists.value.map(updateItem)
            _songs.value = _songs.value.map(updateItem)
            
            _selectedArtist.value?.let {
                if (it.id == song.id) _selectedArtist.value = updateItem(it)
            }
            _selectedAlbum.value?.let {
                if (it.id == song.id) _selectedAlbum.value = updateItem(it)
            }"""

selection_update_item = """            _albums.value = _albums.value.map(updateItem)
            _artists.value = _artists.value.map(updateItem)
            _songs.value = _songs.value.map(updateItem)
            
            _selectedArtist.value?.let {
                if (it.id == item.id) _selectedArtist.value = updateItem(it)
            }
            _selectedAlbum.value?.let {
                if (it.id == item.id) _selectedAlbum.value = updateItem(it)
            }"""

content = content.replace(
    """            _albums.value = _albums.value.map(updateItem)
            _artists.value = _artists.value.map(updateItem)
            _songs.value = _songs.value.map(updateItem)""",
    selection_update,
    1
)

content = content.replace(
    """            _albums.value = _albums.value.map(updateItem)
            _artists.value = _artists.value.map(updateItem)
            _songs.value = _songs.value.map(updateItem)""",
    selection_update_item
)

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
