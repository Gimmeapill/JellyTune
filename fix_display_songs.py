import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

# Fix displaySongs to not add extraLocalFavs
content = re.sub(
    r"val serverMatching = baseList\.filter \{ it\.id in favSongIds \|\| it\.userData\?\.isFavorite == true \}\n\s*val extraLocalFavs = favs\.filter \{ fav -> serverMatching\.none \{ it\.id == fav\.songId \} \}\.map \{ it\.toJellyfinItem\(\) \}\n\s*serverMatching \+ extraLocalFavs",
    r"baseList.filter { it.id in favSongIds || it.userData?.isFavorite == true }",
    content
)

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)

