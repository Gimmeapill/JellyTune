import re
with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

bad_block = """            _selectedArtist.value?.let {
                if (it.id == item.id) _selectedArtist.value = updateItem(it)
            }
            _selectedAlbum.value?.let {
                if (it.id == item.id) _selectedAlbum.value = updateItem(it)
            }"""

good_block = """            _selectedArtist.value?.let {
                if (it.id == song.id) _selectedArtist.value = updateItem(it)
            }
            _selectedAlbum.value?.let {
                if (it.id == song.id) _selectedAlbum.value = updateItem(it)
            }"""

# Since toggleFavorite uses song, and toggleFavoriteFav uses item...
# Actually both replaced with the same string in my script, wait.
# Oh, I replaced BOTH with `selection_update_item`.
# Let's just fix toggleFavorite to use song, and toggleFavoriteFav is fine using item.

# Find the first one inside toggleFavorite
def replacer(match):
    return match.group(0).replace("item.id", "song.id")

content = re.sub(r'fun toggleFavorite\(song: JellyfinItem\).*?_selectedAlbum\.value \= updateItem\(it\)\n\s*\}', replacer, content, flags=re.DOTALL)

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
