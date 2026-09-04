import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# For ExploreAlbumArtistsGrid
content = re.sub(
    r"(fun ExploreAlbumArtistsGrid\(viewModel: JellyTuneViewModel\) \{)",
    r"\1\n    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())",
    content
)

# For ExploreArtistsGrid
content = re.sub(
    r"(fun ExploreArtistsGrid\(viewModel: JellyTuneViewModel\) \{)",
    r"\1\n    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())",
    content
)

# For ExploreAlbumsGrid
content = re.sub(
    r"(fun ExploreAlbumsGrid\(viewModel: JellyTuneViewModel\) \{)",
    r"\1\n    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())",
    content
)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)

