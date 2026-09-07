import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

bad = """val currentIsFav = song.userData?.isFavorite == true
            val newIsFav = !currentIsFav"""
good = """val newIsFav = repository.isFavorite(song.id)"""
content = content.replace(bad, good)

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
