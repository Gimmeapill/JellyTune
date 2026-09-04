import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# Fix play/download
content = content.replace("viewModel.playSongNow(song, songs)", "val index = songs.indexOf(song)\n                        if (index != -1) viewModel.playTrackInQueue(songs, index)")
content = content.replace("viewModel.downloadSongOffline(song)", "viewModel.downloadAndCacheTrack(song)")

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
