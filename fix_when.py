import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

bad_when_pattern = r'        val sortCriteria: SortCriteria[\s\S]*?SortSelectionBar\('

good_when = """        val sortCriteria: SortCriteria
        val sortDirection: SortDirection
        val onSortChanged: (SortCriteria, SortDirection) -> Unit
        
        val isCachedActive by viewModel.filterCached.collectAsState()
        val isFavoritesActive by viewModel.filterFavorites.collectAsState()

        val aaCriteria = viewModel.albumArtistsSortCriteria.collectAsState().value
        val aaDirection = viewModel.albumArtistsSortDirection.collectAsState().value
        val arCriteria = viewModel.artistsSortCriteria.collectAsState().value
        val arDirection = viewModel.artistsSortDirection.collectAsState().value
        val alCriteria = viewModel.albumsSortCriteria.collectAsState().value
        val alDirection = viewModel.albumsSortDirection.collectAsState().value
        val soCriteria = viewModel.songsSortCriteria.collectAsState().value
        val soDirection = viewModel.songsSortDirection.collectAsState().value

        when (subTab) {
            0 -> {
                sortCriteria = aaCriteria
                sortDirection = aaDirection
                onSortChanged = { criteria, direction -> viewModel.setAlbumArtistsSort(criteria, direction) }
            }
            1 -> {
                sortCriteria = arCriteria
                sortDirection = arDirection
                onSortChanged = { criteria, direction -> viewModel.setArtistsSort(criteria, direction) }
            }
            2 -> {
                sortCriteria = alCriteria
                sortDirection = alDirection
                onSortChanged = { criteria, direction -> viewModel.setAlbumsSort(criteria, direction) }
            }
            else -> {
                sortCriteria = soCriteria
                sortDirection = soDirection
                onSortChanged = { criteria, direction -> viewModel.setSongsSort(criteria, direction) }
            }
        }

        SortSelectionBar("""

content = re.sub(bad_when_pattern, good_when, content)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
