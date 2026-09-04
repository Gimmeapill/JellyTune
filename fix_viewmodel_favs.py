import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

# Fix displayAlbums
content = content.replace("alb.userData?.isFavorite == true || alb.name in favAlbumNames ||", "alb.userData?.isFavorite == true || alb.name in favAlbumNames || alb.id in favSongIds ||")
# Fix displayArtists
content = content.replace("art.userData?.isFavorite == true ||", "art.userData?.isFavorite == true || art.id in favSongIds ||")

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
