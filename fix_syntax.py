import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# Fix SortSelectionBar extra braces
# Remove one brace before EmptyStateBlock
content = content.replace("        }\n        }\n    }\n}\n\n@Composable\nfun EmptyStateBlock", "        }\n    }\n}\n\n@Composable\nfun EmptyStateBlock")

# Fix ExploreSongsList unresolved references
# ExploreSongsList had `isAlphabetical` in Albums/Artists but not Songs. Songs uses `songsSortCriteria`.
# Let's add it to ExploreSongsList
songs_list_setup = """fun ExploreSongsList(viewModel: JellyTuneViewModel) {
    val songs by viewModel.filteredSongs.collectAsState()
    val currentSong = viewModel.playbackState.collectAsState().value.currentSong
    val cachedSongs by viewModel.cachedSongs.collectAsState(initial = emptyList())
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())
    
    val sortCriteria by viewModel.songsSortCriteria.collectAsState()
    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL"""
content = re.sub(r'fun ExploreSongsList\(viewModel: JellyTuneViewModel\) \{[\s\S]*?val localFavs.*?collectAsState\(initial = emptyList\(\)\)', songs_list_setup, content)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
