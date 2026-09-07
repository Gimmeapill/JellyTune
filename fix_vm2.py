import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

# Replace the specific lines with compilation error
content = content.replace("val currentIsFav = song.userData?.isFavorite == true || localFavorites.value.any { it.songId == song.id }", "val currentIsFav = song.userData?.isFavorite == true")

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
