import re

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "r") as f:
    content = f.read()

# 1. Update subTabs
content = content.replace('val subTabs = listOf("Artists", "Albums", "Tracks")', 'val subTabs = listOf("Album Artists", "Artists", "Albums", "Tracks")')

# 2. Update when(subTab) inside Box
when_subtab = """            when (subTab) {
                0 -> ExploreAlbumArtistsGrid(viewModel)
                1 -> ExploreArtistsGrid(viewModel)
                2 -> ExploreAlbumsGrid(viewModel)
                3 -> ExploreSongsList(viewModel)
            }"""
content = re.sub(r'            when \(subTab\) \{\s*0 -> ExploreArtistsGrid\(viewModel\)\s*1 -> ExploreAlbumsGrid\(viewModel\)\s*2 -> ExploreSongsList\(viewModel\)\s*\}', when_subtab, content)

# 3. Update sort criteria when(subTab)
sort_when = """        when (subTab) {
            0 -> {
                sortCriteria = viewModel.albumArtistsSortCriteria.collectAsState().value
                sortDirection = viewModel.albumArtistsSortDirection.collectAsState().value
                onSortChanged = { criteria, direction -> viewModel.setAlbumArtistsSort(criteria, direction) }
            }
            1 -> {
                sortCriteria = viewModel.artistsSortCriteria.collectAsState().value
                sortDirection = viewModel.artistsSortDirection.collectAsState().value
                onSortChanged = { criteria, direction -> viewModel.setArtistsSort(criteria, direction) }
            }
            2 -> {
                sortCriteria = viewModel.albumsSortCriteria.collectAsState().value
                sortDirection = viewModel.albumsSortDirection.collectAsState().value
                onSortChanged = { criteria, direction -> viewModel.setAlbumsSort(criteria, direction) }
            }
            else -> {
                sortCriteria = viewModel.songsSortCriteria.collectAsState().value
                sortDirection = viewModel.songsSortDirection.collectAsState().value
                onSortChanged = { criteria, direction -> viewModel.setSongsSort(criteria, direction) }
            }
        }"""
content = re.sub(r'        when \(subTab\) \{\s*0 -> \{\s*sortCriteria = viewModel\.artistsSortCriteria\.collectAsState\(\)\.value.*?\}\s*\}', sort_when, content, flags=re.DOTALL)

with open("/app/applet/app/src/main/java/com/example/ui/screens/MainLibraryScreen.kt", "w") as f:
    f.write(content)
