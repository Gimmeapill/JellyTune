package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CachedSong
import com.example.data.database.LocalFavorite
import com.example.data.jellyfin.JellyfinItem
import com.example.data.repository.JellyfinRepository
import com.example.playback.PlaybackManager
import com.example.playback.PlaybackState
import com.example.playback.toJellyfinItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JellyTuneViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JellyfinRepository.getInstance(application)
    val playbackManager = PlaybackManager.getInstance(application, repository)

    // Exposing session and db state
    val activeServer = repository.activeServer
    val cachedSongs = repository.cachedSongs
    val localFavorites = repository.localFavorites
    val playbackState: StateFlow<PlaybackState> = playbackManager.state

    // UI state loaders
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private val _artists = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val artists = _artists.asStateFlow()

    private val _albums = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val albums = _albums.asStateFlow()

    private val _songs = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val songs = _songs.asStateFlow()

    // Saved preferences configurations
    private val prefs = application.getSharedPreferences("jellytune_prefs", android.content.Context.MODE_PRIVATE)

    // Sorting states
    private val _artistsSortCriteria = MutableStateFlow(SortCriteria.valueOf(prefs.getString("artists_sort_criteria", SortCriteria.ALPHABETICAL.name) ?: SortCriteria.ALPHABETICAL.name))
    val artistsSortCriteria = _artistsSortCriteria.asStateFlow()

    private val _artistsSortDirection = MutableStateFlow(SortDirection.valueOf(prefs.getString("artists_sort_direction", SortDirection.ASCENDING.name) ?: SortDirection.ASCENDING.name))
    val artistsSortDirection = _artistsSortDirection.asStateFlow()

    private val _albumsSortCriteria = MutableStateFlow(SortCriteria.valueOf(prefs.getString("albums_sort_criteria", SortCriteria.ALPHABETICAL.name) ?: SortCriteria.ALPHABETICAL.name))
    val albumsSortCriteria = _albumsSortCriteria.asStateFlow()

    private val _albumsSortDirection = MutableStateFlow(SortDirection.valueOf(prefs.getString("albums_sort_direction", SortDirection.ASCENDING.name) ?: SortDirection.ASCENDING.name))
    val albumsSortDirection = _albumsSortDirection.asStateFlow()

    private val _songsSortCriteria = MutableStateFlow(SortCriteria.valueOf(prefs.getString("songs_sort_criteria", SortCriteria.ALPHABETICAL.name) ?: SortCriteria.ALPHABETICAL.name))
    val songsSortCriteria = _songsSortCriteria.asStateFlow()

    private val _songsSortDirection = MutableStateFlow(SortDirection.valueOf(prefs.getString("songs_sort_direction", SortDirection.ASCENDING.name) ?: SortDirection.ASCENDING.name))
    val songsSortDirection = _songsSortDirection.asStateFlow()

    fun setArtistsSort(criteria: SortCriteria, direction: SortDirection) {
        _artistsSortCriteria.value = criteria
        _artistsSortDirection.value = direction
        prefs.edit()
            .putString("artists_sort_criteria", criteria.name)
            .putString("artists_sort_direction", direction.name)
            .apply()
    }

    fun setAlbumsSort(criteria: SortCriteria, direction: SortDirection) {
        _albumsSortCriteria.value = criteria
        _albumsSortDirection.value = direction
        prefs.edit()
            .putString("albums_sort_criteria", criteria.name)
            .putString("albums_sort_direction", direction.name)
            .apply()
    }

    fun setSongsSort(criteria: SortCriteria, direction: SortDirection) {
        _songsSortCriteria.value = criteria
        _songsSortDirection.value = direction
        prefs.edit()
            .putString("songs_sort_criteria", criteria.name)
            .putString("songs_sort_direction", direction.name)
            .apply()
    }

    // Exposed Music Libraries
    val discoveredLibraries = repository.discoveredLibraries
    val selectedLibraryIds = repository.selectedLibraryIds

    fun toggleLibrarySelected(libraryId: String) {
        repository.toggleLibrarySelected(libraryId)
        refreshLibrary()
    }

    private val networkMonitor = com.example.util.NetworkMonitor(application)
    
    private val _offlineMode = MutableStateFlow(prefs.getBoolean("offline_mode", false))
    val offlineMode = _offlineMode.asStateFlow()


    private val _filterCached = MutableStateFlow(false)
    val filterCached = _filterCached.asStateFlow()

    private val _filterFavorites = MutableStateFlow(false)
    val filterFavorites = _filterFavorites.asStateFlow()

    fun toggleFilterCached() {
        _filterCached.value = !_filterCached.value
    }

    fun toggleFilterFavorites() {
        _filterFavorites.value = !_filterFavorites.value
    }

    fun setOfflineMode(enabled: Boolean) {
        _offlineMode.value = enabled
        prefs.edit().putBoolean("offline_mode", enabled).apply()
    }


    val effectiveOfflineMode: StateFlow<Boolean> = combine(
        _offlineMode,
        
        networkMonitor.isWifiConnected
    ) { offlineSelected,  isWifi ->
        offlineSelected
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _offlineMode.value)

    private val _maxCacheSizeMb = MutableStateFlow(prefs.getLong("max_cache_size_mb", 1024L))
    val maxCacheSizeMb = _maxCacheSizeMb.asStateFlow()

    fun setMaxCacheSizeMb(limitMb: Long) {
        _maxCacheSizeMb.value = limitMb
        prefs.edit().putLong("max_cache_size_mb", limitMb).apply()
        viewModelScope.launch {
            repository.enforceCacheLimit(limitMb)
        }
    }

    private val _loudnessEnhancerEnabled = MutableStateFlow(prefs.getBoolean("loudness_enhancer_enabled", true))
    val loudnessEnhancerEnabled = _loudnessEnhancerEnabled.asStateFlow()

    private val _loudnessEnhancerGain = MutableStateFlow(prefs.getLong("loudness_enhancer_gain", 300L))
    val loudnessEnhancerGain = _loudnessEnhancerGain.asStateFlow()

    fun setLoudnessEnhancerEnabled(enabled: Boolean) {
        _loudnessEnhancerEnabled.value = enabled
        prefs.edit().putBoolean("loudness_enhancer_enabled", enabled).apply()
    }

    fun setLoudnessEnhancerGain(gainMb: Long) {
        _loudnessEnhancerGain.value = gainMb
        prefs.edit().putLong("loudness_enhancer_gain", gainMb).apply()
    }

    fun openSystemEqualizer(context: android.content.Context) {
        playbackManager.openSystemEqualizer(context)
    }

    private val _filterState = combine(_filterCached, _filterFavorites) { c, f -> LibraryFilterState(c, f) }

    // Expose filtered view based on offline/cached/favorite filter state
    val displaySongs: StateFlow<List<JellyfinItem>> = combine(
        _songs,
        cachedSongs,
        localFavorites,
        effectiveOfflineMode,
        _filterState
    ) { serverSongs, cached, favs, offline, filter ->
        val isCachedActive = offline || filter.cached
        val isFavsActive = filter.favorites

        val baseList = if (isCachedActive) {
            cached.map { it.toJellyfinItem() }
        } else {
            serverSongs
        }

        val list = if (isFavsActive) {
            val favSongIds = favs.map { it.songId }.toSet()
            if (isCachedActive) {
                // Both active: intersect cached and favorites
                baseList.filter { it.id in favSongIds }
            } else {
                // Only favs active
                val serverMatching = baseList.filter { it.id in favSongIds || it.userData?.isFavorite == true }
                val extraLocalFavs = favs.filter { fav -> serverMatching.none { it.id == fav.songId } }.map { it.toJellyfinItem() }
                serverMatching + extraLocalFavs
            }
        } else {
            baseList
        }
        
        list.sortedWith(
            compareBy<JellyfinItem> { it.albumName ?: "" }
                .thenBy { it.parentIndexNumber ?: 1 }
                .thenBy { it.indexNumber ?: 0 }
                .thenBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayAlbums: StateFlow<List<JellyfinItem>> = combine(
        _albums,
        cachedSongs,
        localFavorites,
        effectiveOfflineMode,
        _filterState
    ) { serverAlbums, cached, favs, offline, filter ->
        val serverSongs = _songs.value
        val isCachedActive = offline || filter.cached
        val isFavsActive = filter.favorites

        val baseAlbums = if (isCachedActive) {
            val cachedAlbumNames = cached.map { it.album }.filter { it.isNotBlank() }.distinct()
            val cachedSongIds = cached.map { it.songId }.toSet()
            val matchingServerAlbums = serverAlbums.filter { alb ->
                alb.name in cachedAlbumNames || alb.id in cachedSongIds ||
                serverSongs.any { it.albumId == alb.id && it.id in cachedSongIds }
            }
            val extraAlbums = cachedAlbumNames.filter { albName ->
                matchingServerAlbums.none { it.name.equals(albName, ignoreCase = true) }
            }.map { albName ->
                val repSong = cached.firstOrNull { it.album.equals(albName, ignoreCase = true) }
                JellyfinItem(
                    id = repSong?.songId ?: "cached_alb_$albName",
                    name = albName,
                    type = "MusicAlbum",
                    albumArtist = repSong?.artist ?: "Unknown Artist"
                )
            }
            matchingServerAlbums + extraAlbums
        } else {
            serverAlbums
        }

        if (isFavsActive) {
            val favSongIds = favs.map { it.songId }.toSet()
            val favAlbumNames = favs.map { it.album }.filter { it.isNotBlank() }.distinct()
            
            if (isCachedActive) {
                // Intersect cached albums with favs
                baseAlbums.filter { alb -> 
                    alb.userData?.isFavorite == true || alb.name in favAlbumNames ||
                    serverSongs.any { it.albumId == alb.id && (it.id in favSongIds || it.userData?.isFavorite == true) }
                }
            } else {
                val matchingServerAlbums = baseAlbums.filter { alb ->
                    alb.userData?.isFavorite == true || alb.name in favAlbumNames ||
                    serverSongs.any { it.albumId == alb.id && (it.id in favSongIds || it.userData?.isFavorite == true) }
                }
                val extraAlbums = favAlbumNames.filter { albName ->
                    matchingServerAlbums.none { it.name.equals(albName, ignoreCase = true) }
                }.map { albName ->
                    val repSong = favs.firstOrNull { it.album.equals(albName, ignoreCase = true) }
                    JellyfinItem(
                        id = repSong?.songId ?: "fav_alb_$albName",
                        name = albName,
                        type = "MusicAlbum",
                        albumArtist = repSong?.artist ?: "Unknown Artist"
                    )
                }
                matchingServerAlbums + extraAlbums
            }
        } else {
            baseAlbums
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayArtists: StateFlow<List<JellyfinItem>> = combine(
        _artists,
        cachedSongs,
        localFavorites,
        effectiveOfflineMode,
        _filterState
    ) { serverArtists, cached, favs, offline, filter ->
        val serverSongs = _songs.value
        val isCachedActive = offline || filter.cached
        val isFavsActive = filter.favorites

        val baseArtists = if (isCachedActive) {
            val cachedArtists = cached.map { it.artist }.filter { it.isNotBlank() }.distinct()
            val matchingServerArtists = serverArtists.filter { art ->
                cachedArtists.any { it.equals(art.name, ignoreCase = true) }
            }
            val extraArtists = cachedArtists.filter { artName ->
                matchingServerArtists.none { it.name.equals(artName, ignoreCase = true) }
            }.map { artName ->
                val repSong = cached.firstOrNull { it.artist.equals(artName, ignoreCase = true) }
                JellyfinItem(
                    id = repSong?.songId ?: "cached_art_$artName",
                    name = artName,
                    type = "MusicArtist"
                )
            }
            matchingServerArtists + extraArtists
        } else {
            serverArtists
        }

        if (isFavsActive) {
            val favArtists = favs.map { it.artist }.filter { it.isNotBlank() }.distinct()
            val favSongIds = favs.map { it.songId }.toSet()
            
            if (isCachedActive) {
                baseArtists.filter { art ->
                    art.userData?.isFavorite == true ||
                    favArtists.any { it.equals(art.name, ignoreCase = true) } ||
                    serverSongs.any { s ->
                        (s.id in favSongIds || s.userData?.isFavorite == true) &&
                        (s.albumArtist?.equals(art.name, ignoreCase = true) == true || s.artists?.any { it.equals(art.name, ignoreCase = true) } == true)
                    }
                }
            } else {
                val matchingServerArtists = baseArtists.filter { art ->
                    art.userData?.isFavorite == true ||
                    favArtists.any { it.equals(art.name, ignoreCase = true) } ||
                    serverSongs.any { s ->
                        (s.id in favSongIds || s.userData?.isFavorite == true) &&
                        (s.albumArtist?.equals(art.name, ignoreCase = true) == true || s.artists?.any { it.equals(art.name, ignoreCase = true) } == true)
                    }
                }
                val extraArtists = favArtists.filter { artName ->
                    matchingServerArtists.none { it.name.equals(artName, ignoreCase = true) }
                }.map { artName ->
                    val repSong = favs.firstOrNull { it.artist.equals(artName, ignoreCase = true) }
                    JellyfinItem(
                        id = repSong?.songId ?: "fav_art_$artName",
                        name = artName,
                        type = "MusicArtist"
                    )
                }
                matchingServerArtists + extraArtists
            }
        } else {
            baseArtists
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Screen navigation filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Sub-list selectors (Selected Album or Artist detail list)
    private val _selectedAlbum = MutableStateFlow<JellyfinItem?>(null)
    val selectedAlbum = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<JellyfinItem?>(null)
    val selectedArtist = _selectedArtist.asStateFlow()

    private val _albumSongs = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val albumSongs = _albumSongs.asStateFlow()

    private val _artistSongs = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val artistSongs = _artistSongs.asStateFlow()

    private val _artistAlbums = MutableStateFlow<List<JellyfinItem>>(emptyList())
    val artistAlbums = _artistAlbums.asStateFlow()

    private val _showHeroCard = MutableStateFlow(false)
    val showHeroCard = _showHeroCard.asStateFlow()

    fun setHeroCardVisibility(visible: Boolean) {
        _showHeroCard.value = visible
    }

    // Download state track percentage map
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    // Aggregate cache size used in Megabytes
    val currentCacheSizeMb: StateFlow<Double> = repository.cachedSongs
        .map { songs ->
            songs.sumOf {
                val file = java.io.File(it.filePath)
                if (file.exists()) file.length() else 0L
            }.toDouble() / (1024.0 * 1024.0)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private fun sortJellyfinItems(
        list: List<JellyfinItem>,
        criteria: SortCriteria,
        direction: SortDirection
    ): List<JellyfinItem> {
        val comparator = when (criteria) {
            SortCriteria.ALPHABETICAL -> compareBy<JellyfinItem> { it.name.lowercase() }
            SortCriteria.DATE -> compareBy<JellyfinItem> { it.productionYear ?: 0 }
            SortCriteria.DATE_ADDED -> compareBy<JellyfinItem> { it.dateCreated ?: "" }
        }
        return if (direction == SortDirection.ASCENDING) {
            list.sortedWith(comparator)
        } else {
            list.sortedWith(comparator.reversed())
        }
    }

    // Combines search query and libraries with sorting
    val filteredAlbums: StateFlow<List<JellyfinItem>> = combine(
        displayAlbums,
        _searchQuery,
        _albumsSortCriteria,
        _albumsSortDirection
    ) { list, query, criteria, direction ->
        val filtered = if (query.isBlank()) list else list.filter {
            it.name.contains(query, ignoreCase = true) ||
            (it.albumArtist ?: "").contains(query, ignoreCase = true) ||
            it.artists?.any { artist -> artist.contains(query, ignoreCase = true) } == true
        }
        sortJellyfinItems(filtered, criteria, direction)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArtists: StateFlow<List<JellyfinItem>> = combine(
        displayArtists,
        _searchQuery,
        _artistsSortCriteria,
        _artistsSortDirection
    ) { list, query, criteria, direction ->
        val filtered = if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
        sortJellyfinItems(filtered, criteria, direction)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSongs: StateFlow<List<JellyfinItem>> = combine(
        displaySongs,
        _searchQuery,
        _songsSortCriteria,
        _songsSortDirection
    ) { list, query, criteria, direction ->
        val filtered = if (query.isBlank()) list else list.filter {
            it.name.contains(query, ignoreCase = true) || 
            (it.albumName ?: "").contains(query, ignoreCase = true) ||
            (it.albumArtist ?: "").contains(query, ignoreCase = true)
        }
        sortJellyfinItems(filtered, criteria, direction)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatically load library if already logged in on launch
        viewModelScope.launch {
            activeServer.collect { server ->
                if (server != null) {
                    loadLibrary()
                } else {
                    // Reset libraries
                    _artists.value = emptyList()
                    _albums.value = emptyList()
                    _songs.value = emptyList()
                }
            }
        }

        // React silently to background cache updates
        viewModelScope.launch {
            repository.apiCacheUpdated.collect { cacheKey ->
                if (cacheKey.contains("getArtists")) {
                    _artists.value = repository.getArtists()
                } else if (cacheKey.contains("getAlbums")) {
                    _albums.value = repository.getAlbums()
                    // Update current selected artist albums
                    val selectedArt = _selectedArtist.value
                    if (selectedArt != null && cacheKey.contains(selectedArt.id)) {
                        _artistAlbums.value = repository.getAlbums(selectedArt.id)
                    }
                } else if (cacheKey.contains("getSongs")) {
                    _songs.value = repository.getSongs()
                    
                    // Update current selected album songs
                    val selectedAlb = _selectedAlbum.value
                    if (selectedAlb != null && cacheKey.contains(selectedAlb.id)) {
                        _albumSongs.value = repository.getSongs(selectedAlb.id)
                    }
                    
                    // Update current selected artist songs
                    val selectedArt = _selectedArtist.value
                    if (selectedArt != null && cacheKey.contains(selectedArt.id)) {
                        _artistSongs.value = repository.getSongs(selectedArt.id)
                    }
                }
            }
        }
    }

    // --- CONNECTION CONTROL ---

    fun connectServer(url: String, user: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            val result = repository.authenticate(url, user, pass)
            _isLoading.value = false
            if (result.isFailure) {
                _loginError.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }
        }
    }

    fun connectDemo() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.connectDemo()
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            playbackManager.release()
            repository.logout()
        }
    }

    // --- LIBRARY FETCH ---

    fun loadLibrary(forceFull: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch library items sequentially (artists -> albums -> songs)
                // This gives faster apparent load times for the first tab (artists)
                _artists.value = repository.getArtists(forceFull)
                _albums.value = repository.getAlbums(forceFull = forceFull)
                _songs.value = repository.getSongs(forceFull = forceFull)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }

            // Sync favorites in the background without holding the loading UI
            try {
                repository.syncFavorites()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshLibrary() {
        loadLibrary(forceFull = true)
    }

    fun selectAlbum(album: JellyfinItem?) {
        _selectedAlbum.value = album
        if (album == null) {
            _albumSongs.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            
            val isCachedActive = effectiveOfflineMode.value || _filterCached.value
            val isFavsActive = _filterFavorites.value

            val baseSongs = if (isCachedActive) {
                val cached = repository.getCachedSongsList()
                cached
                    .filter { it.album.equals(album.name, ignoreCase = true) || it.songId == album.id }
                    .map { it.toJellyfinItem() }
            } else {
                val serverSongs = repository.getSongs(album.id)
                if (serverSongs.isNotEmpty()) serverSongs else {
                    _songs.value.filter { it.albumName?.equals(album.name, ignoreCase = true) == true }
                }
            }

            val resultSongs = if (isFavsActive) {
                val favs = repository.getLocalFavoritesList()
                if (isCachedActive) {
                    baseSongs.filter { s -> favs.any { it.songId == s.id } }
                } else {
                    if (baseSongs.isNotEmpty()) {
                        baseSongs.filter { s -> favs.any { it.songId == s.id } || s.userData?.isFavorite == true }
                    } else {
                        favs.filter { it.album.equals(album.name, ignoreCase = true) }.map { it.toJellyfinItem() }
                    }
                }
            } else {
                baseSongs
            }

            _albumSongs.value = resultSongs.sortedWith(
                compareBy<JellyfinItem> { it.parentIndexNumber ?: 1 }
                    .thenBy { it.indexNumber ?: 0 }
                    .thenBy { it.name }
            )
            
            _isLoading.value = false
        }
    }

    fun selectArtist(artist: JellyfinItem?) {
        _selectedArtist.value = artist
        if (artist == null) {
            _artistSongs.value = emptyList()
            _artistAlbums.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            
            val isCachedActive = effectiveOfflineMode.value || _filterCached.value
            val isFavsActive = _filterFavorites.value

            var serverSongs = emptyList<JellyfinItem>()
            var serverAlbums = emptyList<JellyfinItem>()

            if (!isCachedActive) {
                try {
                    serverAlbums = repository.getAlbums(artist.id)
                    serverSongs = repository.getSongs(artist.id)
                    if (serverAlbums.isEmpty() && serverSongs.isEmpty()) {
                        serverSongs = _songs.value.filter { song ->
                            song.albumArtist?.equals(artist.name, ignoreCase = true) == true ||
                            song.artists?.any { it.equals(artist.name, ignoreCase = true) } == true ||
                            song.artistItems?.any { it.name.equals(artist.name, ignoreCase = true) } == true
                        }
                        serverAlbums = _albums.value.filter { album ->
                            album.albumArtist?.equals(artist.name, ignoreCase = true) == true ||
                            album.artists?.any { it.equals(artist.name, ignoreCase = true) } == true ||
                            album.artistItems?.any { it.name.equals(artist.name, ignoreCase = true) } == true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val baseSongs = if (isCachedActive) {
                val cached = repository.getCachedSongsList()
                cached.filter { it.artist.equals(artist.name, ignoreCase = true) }.map { it.toJellyfinItem() }
            } else {
                serverSongs
            }

            val resultSongs = if (isFavsActive) {
                val favs = repository.getLocalFavoritesList()
                if (isCachedActive) {
                    baseSongs.filter { s -> favs.any { it.songId == s.id } }
                } else {
                    if (baseSongs.isNotEmpty()) {
                        baseSongs.filter { s -> favs.any { it.songId == s.id } || s.userData?.isFavorite == true }
                    } else {
                        favs.filter { it.artist.equals(artist.name, ignoreCase = true) }.map { it.toJellyfinItem() }
                    }
                }
            } else {
                baseSongs
            }

            _artistSongs.value = resultSongs.sortedWith(
                compareBy<JellyfinItem> { it.albumName ?: "" }
                    .thenBy { it.parentIndexNumber ?: 1 }
                    .thenBy { it.indexNumber ?: 0 }
                    .thenBy { it.name }
            )

            // Calculate albums from resulting songs or server response
            if (isCachedActive || isFavsActive) {
                val albumNames = resultSongs.mapNotNull { it.albumName }.distinct()
                _artistAlbums.value = albumNames.map { albName ->
                    _albums.value.find { it.name.equals(albName, ignoreCase = true) }
                        ?: JellyfinItem(
                            id = resultSongs.firstOrNull { it.albumName.equals(albName, ignoreCase = true) }?.id ?: "local_alb",
                            name = albName,
                            type = "MusicAlbum",
                            albumArtist = artist.name
                        )
                }
            } else {
                _artistAlbums.value = serverAlbums
            }
            
            _isLoading.value = false
        }
    }

    // --- QUERY ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- PLAY CONTROLS INTERFACES ---

    fun playArtistNow(artist: JellyfinItem) {
        viewModelScope.launch {
            _isLoading.value = true
            var artistSongs = repository.getSongs(artist.id)
            if (artistSongs.isEmpty()) {
                val cached = repository.getCachedSongsList()
                artistSongs = cached.filter { it.artist.equals(artist.name, ignoreCase = true) }.map { it.toJellyfinItem() }
            }
            if (artistSongs.isEmpty()) {
                artistSongs = _songs.value.filter {
                    it.albumArtist?.equals(artist.name, ignoreCase = true) == true ||
                    it.artists?.any { a -> a.equals(artist.name, ignoreCase = true) } == true
                }
            }
            artistSongs = artistSongs.sortedWith(
                compareBy<JellyfinItem> { it.albumName ?: "" }
                    .thenBy { it.parentIndexNumber ?: 1 }
                    .thenBy { it.indexNumber ?: 0 }
                    .thenBy { it.name }
            )
            _isLoading.value = false
            if (artistSongs.isNotEmpty()) {
                playbackManager.playQueue(artistSongs, 0)
            }
        }
    }

    fun playAlbumNow(album: JellyfinItem) {
        viewModelScope.launch {
            _isLoading.value = true
            var albumSongs = repository.getSongs(album.id)
            if (albumSongs.isEmpty()) {
                val cached = repository.getCachedSongsList()
                albumSongs = cached.filter { it.album.equals(album.name, ignoreCase = true) || it.songId == album.id }.map { it.toJellyfinItem() }
            }
            if (albumSongs.isEmpty()) {
                albumSongs = _songs.value.filter { it.albumName?.equals(album.name, ignoreCase = true) == true }
            }
            albumSongs = albumSongs.sortedWith(
                compareBy<JellyfinItem> { it.parentIndexNumber ?: 1 }
                    .thenBy { it.indexNumber ?: 0 }
                    .thenBy { it.name }
            )
            _isLoading.value = false
            if (albumSongs.isNotEmpty()) {
                playbackManager.playQueue(albumSongs, 0)
                // Cache the whole album aggressively under high-priority user request
                for (song in albumSongs) {
                    downloadAndCacheTrack(song)
                }
            }
        }
    }

    fun appendAlbumToQueue(album: JellyfinItem) {
        viewModelScope.launch {
            _isLoading.value = true
            var albumSongs = repository.getSongs(album.id)
            if (albumSongs.isEmpty()) {
                val cached = repository.getCachedSongsList()
                albumSongs = cached.filter { it.album.equals(album.name, ignoreCase = true) || it.songId == album.id }.map { it.toJellyfinItem() }
            }
            if (albumSongs.isEmpty()) {
                albumSongs = _songs.value.filter { it.albumName?.equals(album.name, ignoreCase = true) == true }
            }
            albumSongs = albumSongs.sortedWith(
                compareBy<JellyfinItem> { it.parentIndexNumber ?: 1 }
                    .thenBy { it.indexNumber ?: 0 }
                    .thenBy { it.name }
            )
            _isLoading.value = false
            if (albumSongs.isNotEmpty()) {
                playbackManager.appendSongsToQueue(albumSongs)
                // Appending album to play queue also triggers caching the album's tracks
                for (song in albumSongs) {
                    downloadAndCacheTrack(song)
                }
            }
        }
    }

    fun downloadAlbumOffline(album: JellyfinItem) {
        viewModelScope.launch {
            _isLoading.value = true
            var albumSongs = repository.getSongs(album.id)
            if (albumSongs.isEmpty()) {
                albumSongs = _songs.value.filter { it.albumName?.equals(album.name, ignoreCase = true) == true }
            }
            _isLoading.value = false
            for (song in albumSongs) {
                downloadAndCacheTrack(song)
            }
        }
    }

    fun playTrackInQueue(songsList: List<JellyfinItem>, index: Int) {
        playbackManager.playQueue(songsList, index)
        // Aggressively cache all tracks of the album if playing from an album context
        if (songsList.size > 1) {
            for (song in songsList) {
                downloadAndCacheTrack(song)
            }
        }
    }

    fun playCachedSong(song: CachedSong) {
        // Map cached song back to a standard JellyfinItem
        val jItem = song.toJellyfinItem()
        playbackManager.playQueue(listOf(jItem), 0)
    }

    fun playFavoriteSong(song: LocalFavorite) {
        val jItem = song.toJellyfinItem()
        playbackManager.playQueue(listOf(jItem), 0)
    }

    fun getArtworkUrl(itemId: String): String {
        return repository.getArtworkUrl(itemId)
    }

    // --- FAVORITES & CACHE CONTROLS ---

    fun toggleFavorite(song: JellyfinItem) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }

    fun toggleFavoriteLocal(song: CachedSong) {
        viewModelScope.launch {
            repository.toggleFavoriteLocal(song)
        }
    }

    fun toggleFavoriteFav(song: LocalFavorite) {
        viewModelScope.launch {
            repository.toggleFavorite(song.toJellyfinItem())
        }
    }

    fun downloadAndCacheTrack(song: JellyfinItem) {
        viewModelScope.launch {
            _downloadProgress.value = _downloadProgress.value + (song.id to 0.01f)
            repository.downloadAndCache(song) { progress ->
                _downloadProgress.value = _downloadProgress.value + (song.id to progress)
            }
            // Remove progress indicator key when finished
            _downloadProgress.value = _downloadProgress.value - song.id
        }
    }

    fun deleteSongFromCache(songId: String) {
        viewModelScope.launch {
            repository.deleteFromCache(songId)
        }
    }

    fun clearLocalCache() {
        viewModelScope.launch {
            repository.clearCache()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}

data class LibraryFilterState(
    val cached: Boolean = false,
    val favorites: Boolean = false
)

enum class SortCriteria {
    ALPHABETICAL,
    DATE,
    DATE_ADDED
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}
