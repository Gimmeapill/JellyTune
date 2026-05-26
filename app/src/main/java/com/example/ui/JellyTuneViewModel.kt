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

    private val repository = JellyfinRepository(application)
    val playbackManager = PlaybackManager(application, repository)

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

    // Download state track percentage map
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    // Combines search query and libraries
    val filteredAlbums: StateFlow<List<JellyfinItem>> = combine(albums, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArtists: StateFlow<List<JellyfinItem>> = combine(artists, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSongs: StateFlow<List<JellyfinItem>> = combine(songs, _searchQuery) { list, query ->
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
                // Fetch artists, albums, songs in parallel
                val artistsJob = launch { _artists.value = repository.getArtists() }
                val albumsJob = launch { _albums.value = repository.getAlbums() }
                val songsJob = launch { _songs.value = repository.getSongs() }

                artistsJob.join()
                albumsJob.join()
                songsJob.join()
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
            _isLoading.value = true
            _albumSongs.value = repository.getSongs(album.id)
            _isLoading.value = false
        }
    }

    fun selectArtist(artist: JellyfinItem?) {
        _selectedArtist.value = artist
        if (artist == null) {
            _artistSongs.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _artistSongs.value = repository.getSongs(artist.id)
            _isLoading.value = false
        }
    }

    // --- QUERY ---

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- PLAY CONTROLS INTERFACES ---

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
