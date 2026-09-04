import re

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "r") as f:
    content = f.read()

# 1. Add albumArtists sorting variables
sorting_vars = """    private val _artistsSortDirection = MutableStateFlow(SortDirection.valueOf(prefs.getString("artists_sort_direction", SortDirection.ASCENDING.name) ?: SortDirection.ASCENDING.name))
    val artistsSortDirection = _artistsSortDirection.asStateFlow()

    private val _albumArtistsSortCriteria = MutableStateFlow(SortCriteria.valueOf(prefs.getString("album_artists_sort_criteria", SortCriteria.ALPHABETICAL.name) ?: SortCriteria.ALPHABETICAL.name))
    val albumArtistsSortCriteria = _albumArtistsSortCriteria.asStateFlow()

    private val _albumArtistsSortDirection = MutableStateFlow(SortDirection.valueOf(prefs.getString("album_artists_sort_direction", SortDirection.ASCENDING.name) ?: SortDirection.ASCENDING.name))
    val albumArtistsSortDirection = _albumArtistsSortDirection.asStateFlow()"""
content = re.sub(r'    private val _artistsSortDirection = MutableStateFlow.*?val artistsSortDirection = _artistsSortDirection.asStateFlow\(\)', sorting_vars, content, flags=re.DOTALL)

# 2. Add setAlbumArtistsSort
sorting_funcs = """    fun setArtistsSort(criteria: SortCriteria, direction: SortDirection) {
        _artistsSortCriteria.value = criteria
        _artistsSortDirection.value = direction
        prefs.edit()
            .putString("artists_sort_criteria", criteria.name)
            .putString("artists_sort_direction", direction.name)
            .apply()
    }

    fun setAlbumArtistsSort(criteria: SortCriteria, direction: SortDirection) {
        _albumArtistsSortCriteria.value = criteria
        _albumArtistsSortDirection.value = direction
        prefs.edit()
            .putString("album_artists_sort_criteria", criteria.name)
            .putString("album_artists_sort_direction", direction.name)
            .apply()
    }"""
content = re.sub(r'    fun setArtistsSort\(criteria: SortCriteria, direction: SortDirection\) \{.*?\.apply\(\)\n    \}', sorting_funcs, content, flags=re.DOTALL)

# 3. Add displayAlbumArtists and filteredAlbumArtists
display_lists = """    val displayAlbumArtists: StateFlow<List<JellyfinItem>> = combine(
        displayArtists,
        _albums
    ) { artists, albums ->
        val albumArtistNames = albums.mapNotNull { it.albumArtist }.distinct()
        artists.filter { artist -> 
            albumArtistNames.any { it.equals(artist.name, ignoreCase = true) } 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAlbumArtists: StateFlow<List<JellyfinItem>> = combine(
        displayAlbumArtists,
        _searchQuery,
        _albumArtistsSortCriteria,
        _albumArtistsSortDirection
    ) { artists, query, criteria, direction ->
        val filtered = if (query.isBlank()) {
            artists
        } else {
            artists.filter { it.name.contains(query, ignoreCase = true) }
        }
        when (criteria) {
            SortCriteria.ALPHABETICAL -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
            SortCriteria.DATE_ADDED -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.dateCreated } else filtered.sortedByDescending { it.dateCreated }
            SortCriteria.RELEASE_YEAR -> if (direction == SortDirection.ASCENDING) filtered.sortedBy { it.productionYear } else filtered.sortedByDescending { it.productionYear }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArtists: StateFlow<List<JellyfinItem>> = combine("""
content = content.replace("    val filteredArtists: StateFlow<List<JellyfinItem>> = combine(", display_lists)

with open("/app/applet/app/src/main/java/com/example/ui/JellyTuneViewModel.kt", "w") as f:
    f.write(content)
