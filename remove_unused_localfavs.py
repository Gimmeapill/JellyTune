import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# For ExploreAlbumArtistsGrid
content = re.sub(
    r"(fun ExploreAlbumArtistsGrid\(viewModel: JellyTuneViewModel\) \{\s*val artists by viewModel\.filteredAlbumArtists\.collectAsState\(\)\s*val sortCriteria by viewModel\.albumArtistsSortCriteria\.collectAsState\(\)\s*val isAlphabetical = sortCriteria == SortCriteria\.ALPHABETICAL)\s*val localFavs by viewModel\.localFavorites\.collectAsState\(initial = emptyList\(\)\)",
    r"\1",
    content
)

# Let's do it simply using replace
content = content.replace(
"""fun ExploreAlbumArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.filteredAlbumArtists.collectAsState()
    val sortCriteria by viewModel.albumArtistsSortCriteria.collectAsState()
    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())""",
"""fun ExploreAlbumArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.filteredAlbumArtists.collectAsState()
    val sortCriteria by viewModel.albumArtistsSortCriteria.collectAsState()
    val isAlphabetical = sortCriteria == SortCriteria.ALPHABETICAL"""
)

content = content.replace(
"""fun ExploreArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.displayArtists.collectAsState()
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())""",
"""fun ExploreArtistsGrid(viewModel: JellyTuneViewModel) {
    val artists by viewModel.displayArtists.collectAsState()"""
)

content = content.replace(
"""fun ExploreAlbumsGrid(viewModel: JellyTuneViewModel) {
    val albums by viewModel.displayAlbums.collectAsState()
    val localFavs by viewModel.localFavorites.collectAsState(initial = emptyList())""",
"""fun ExploreAlbumsGrid(viewModel: JellyTuneViewModel) {
    val albums by viewModel.displayAlbums.collectAsState()"""
)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)

