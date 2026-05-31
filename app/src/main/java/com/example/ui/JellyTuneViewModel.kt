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

    private val _offlineMode = MutableStateFlow(prefs.getBoolean("offline_mode", false))
    val offlineMode = _offlineMode.asStateFlow()

    private val _maxCacheLimit = MutableStateFlow(prefs.getInt("max_cache_limit", 50))
    val maxCacheLimit = _maxCacheLimit.asStateFlow()

    fun setOfflineMode(enabled: Boolean) {
        _offlineMode.value = enabled
        prefs.edit().putBoolean("offline_mode", enabled).apply()
    }

    fun setMaxCacheLimit(limit: Int) {
        _maxCacheLimit.value = limit
        prefs.edit().putInt("max_cache_limit", limit).apply()
        viewModelScope.launch {
            repository.enforceCacheLimit(limit)
        }
    }

    // Expose filtered view based on offline/cached state
    val displaySongs: StateFlow<List<JellyfinItem>> = combine(_songs, cachedSongs, _offlineMode) { serverSongs, cached, offline ->
        if (offline) {
            cached.map { it.toJellyfinItem() }
        } else {
            serverSongs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayAlbums: StateFlow<List<JellyfinItem>> = combine(_albums, cachedSongs, _offlineMode) { serverAlbums, cached, offline ->
        if (offline) {
            cached.map { it.album }.distinct().map { albumName ->
                serverAlbums.find { it.name.equals(albumName, ignoreCase = true) }
                    ?: JellyfinItem(
                        id = cached.firstOrNull { it.album.equals(albumName, ignoreCase = true) }?.songId ?: "cached_alb",
                        name = albumName,
                        type = "MusicAlbum",
                        albumArtist = cached.firstOrNull { it.album.equals(albumName, ignoreCase = true) }?.artist ?: "Unknown Artist"
                    )
            }
        } else {
            serverAlbums
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayArtists: StateFlow<List<JellyfinItem>> = combine(_artists, cachedSongs, _offlineMode) { serverArtists, cached, offline ->
        if (offline) {
            cached.map { it.artist }.distinct().map { artistName ->
                serverArtists.find { it.name.equals(artistName, ignoreCase = true) }
                    ?: JellyfinItem(
                        id = cached.firstOrNull { it.artist.equals(artistName, ignoreCase = true) }?.songId ?: "cached_art",
                        name = artistName,
                        type = "MusicArtist"
                    )
            }
        } else {
            serverArtists
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

    // Combines search query and libraries
    val filteredAlbums: StateFlow<List<JellyfinItem>> = combine(displayAlbums, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.name.contains(query, ignoreCase = true) ||
            (it.albumArtist ?: "").contains(query, ignoreCase = true) ||
            it.artists?.any { artist -> artist.contains(query, ignoreCase = true) } == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArtists: StateFlow<List<JellyfinItem>> = combine(displayArtists, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSongs: StateFlow<List<JellyfinItem>> = combine(displaySongs, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.name.contains(query, ignoreCase = true) || 
            (it.albumName ?: "").contains(query, ignoreCase = true) ||
            (it.albumArtist ?: "").contains(query, ignoreCase = true)
        }
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

    fun loadLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch artists, albums, songs, and sync favorites in parallel
                val artistsJob = launch { _artists.value = repository.getArtists() }
                val albumsJob = launch { _albums.value = repository.getAlbums() }
                val songsJob = launch { _songs.value = repository.getSongs() }
                val syncFavsJob = launch { repository.syncFavorites() }

                artistsJob.join()
                albumsJob.join()
                songsJob.join()
                syncFavsJob.join()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshLibrary() {
        loadLibrary()
    }

    fun selectAlbum(album: JellyfinItem?) {
        _selectedAlbum.value = album
        if (album == null) {
            _albumSongs.value = emptyList()
            return
        }
        viewModelScope.launch {
            if (_offlineMode.value) {
                val cached = repository.getCachedSongsList()
                _albumSongs.value = cached
                    .filter { it.album.equals(album.name, ignoreCase = true) }
                    .map { it.toJellyfinItem() }
            } else {
                _isLoading.value = true
                _albumSongs.value = repository.getSongs(album.id)
                _isLoading.value = false
            }
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
            if (_offlineMode.value) {
                val cached = repository.getCachedSongsList()
                val artistSongs = cached
                    .filter { it.artist.equals(artist.name, ignoreCase = true) }
                    .map { it.toJellyfinItem() }
                _artistSongs.value = artistSongs

                val artistAlbums = cached
                    .filter { it.artist.equals(artist.name, ignoreCase = true) }
                    .map { it.album }
                    .distinct()
                    .map { albumName ->
                        JellyfinItem(
                            id = cached.firstOrNull { it.album.equals(albumName, ignoreCase = true) }?.songId ?: "cached_alb",
                            name = albumName,
                            type = "MusicAlbum",
                            albumArtist = artist.name
                        )
                    }
                _artistAlbums.value = artistAlbums
            } else {
                _isLoading.value = true
                try {
                    val albumsJob = launch { _artistAlbums.value = repository.getAlbums(artist.id) }
                    val songsJob = launch { _artistSongs.value = repository.getSongs(artist.id) }
                    albumsJob.join()
                    songsJob.join()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    // --- QUERY ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- PLAY CONTROLS INTERFACES ---

    fun playAlbumNow(album: JellyfinItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val albumSongs = repository.getSongs(album.id)
            _isLoading.value = false
            if (albumSongs.isNotEmpty()) {
                playbackManager.playQueue(albumSongs, 0)
            }
        }
    }

    fun appendAlbumToQueue(album: JellyfinItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val albumSongs = repository.getSongs(album.id)
            _isLoading.value = false
            if (albumSongs.isNotEmpty()) {
                playbackManager.appendSongsToQueue(albumSongs)
            }
        }
    }

    fun downloadAlbumOffline(album: JellyfinItem) {
        viewModelScope.launch {
            _isLoading.value = true
            val albumSongs = repository.getSongs(album.id)
            _isLoading.value = false
            for (song in albumSongs) {
                downloadAndCacheTrack(song)
            }
        }
    }

    fun playTrackInQueue(songsList: List<JellyfinItem>, index: Int) {
        playbackManager.playQueue(songsList, index)
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
