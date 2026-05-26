package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.CachedSong
import com.example.data.database.JellyfinServer
import com.example.data.database.LocalFavorite
import com.example.data.jellyfin.JellyfinClient
import com.example.data.jellyfin.JellyfinItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class JellyfinRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val serverDao = db.serverDao()
    private val cachedSongDao = db.cachedSongDao()
    private val favoriteDao = db.favoriteDao()

    private val client = JellyfinClient()
    private val okHttpClient = OkHttpClient()

    private val _activeServer = MutableStateFlow<JellyfinServer?>(null)
    val activeServer: StateFlow<JellyfinServer?> = _activeServer.asStateFlow()

    // Flow definitions for cached items and favorites
    val cachedSongs: Flow<List<CachedSong>> = cachedSongDao.getAllCachedSongsFlow()
    val localFavorites: Flow<List<LocalFavorite>> = favoriteDao.getAllFavorites()

    init {
        // Run auto-login from the active server in Room
        kotlinx.coroutines.MainScope().launch {
            try {
                val active = withContext(Dispatchers.IO) {
                    serverDao.getActiveServer()
                }
                _activeServer.value = active
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Result<JellyfinServer> {
        val authResult = client.authenticate(serverUrl, username, password)
        if (authResult.isSuccess) {
            val auth = authResult.getOrThrow()
            val server = JellyfinServer(
                serverUrl = serverUrl,
                username = username,
                token = auth.accessToken,
                userId = auth.user.id,
                deviceId = client.deviceId
            )
            withContext(Dispatchers.IO) {
                serverDao.insertServer(server)
            }
            _activeServer.value = server
            return Result.success(server)
        }
        return Result.failure(authResult.exceptionOrNull() ?: java.io.IOException("Authentication failed"))
    }

    suspend fun logout() {
        val current = _activeServer.value
        if (current != null) {
            withContext(Dispatchers.IO) {
                serverDao.deleteServer(current.serverUrl)
            }
            _activeServer.value = null
        }
    }

    // Connect Demo server instantly
    fun connectDemo() {
        _activeServer.value = JellyfinServer(
            serverUrl = "http://demo.jellytune.local",
            username = "DemoUser",
            token = "demo_token",
            userId = "demo_user",
            deviceId = "demo_device_id"
        )
    }

    // Check if current server is Demo
    fun isDemo(): Boolean {
        return _activeServer.value?.serverUrl == "http://demo.jellytune.local"
    }

    // --- FETCH METHODS ---
    suspend fun getArtists(): List<JellyfinItem> {
        if (isDemo()) return getDemoArtists()
        val server = _activeServer.value ?: return emptyList()
        val result = client.fetchItems(server.serverUrl, server.token, server.userId, "MusicArtist")
        return result.getOrDefault(emptyList())
    }

    suspend fun getAlbums(parentId: String? = null): List<JellyfinItem> {
        if (isDemo()) return getDemoAlbums()
        val server = _activeServer.value ?: return emptyList()
        val result = client.fetchItems(server.serverUrl, server.token, server.userId, "MusicAlbum", parentId)
        return result.getOrDefault(emptyList())
    }

    suspend fun getSongs(parentId: String? = null): List<JellyfinItem> {
        if (isDemo()) return getDemoSongs(parentId)
        val server = _activeServer.value ?: return emptyList()
        val result = client.fetchItems(server.serverUrl, server.token, server.userId, "Audio", parentId)
        return result.getOrDefault(emptyList())
    }

    // --- ARTWORK AND SOURCE ENDPOINTS ---
    fun getArtworkUrl(itemId: String): String {
        if (isDemo()) {
            return getDemoArtworkUrl(itemId)
        }
        val server = _activeServer.value ?: return ""
        return client.getArtworkUrl(server.serverUrl, itemId, server.token)
    }

    fun getSongUrl(songId: String): String {
        if (isDemo()) {
            return getDemoSongUrl(songId)
        }
        val server = _activeServer.value ?: return ""
        return client.getStreamUrl(server.serverUrl, songId, server.token)
    }

    // --- FAVORITES ---
    suspend fun isFavorite(songId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(songId)
    }

    suspend fun toggleFavorite(song: JellyfinItem) = withContext(Dispatchers.IO) {
        val server = _activeServer.value ?: return@withContext
        if (favoriteDao.isFavorite(song.id)) {
            favoriteDao.deleteFavorite(song.id)
        } else {
            val fav = LocalFavorite(
                songId = song.id,
                serverUrl = server.serverUrl,
                title = song.name,
                artist = song.albumArtist ?: song.artists?.firstOrNull() ?: "Unknown Artist",
                album = song.albumName ?: "Unknown Album",
                durationMs = song.durationMs
            )
            favoriteDao.insertFavorite(fav)
        }
    }

    suspend fun toggleFavoriteLocal(song: CachedSong) = withContext(Dispatchers.IO) {
        if (favoriteDao.isFavorite(song.songId)) {
            favoriteDao.deleteFavorite(song.songId)
        } else {
            val fav = LocalFavorite(
                songId = song.songId,
                serverUrl = song.serverUrl,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs
            )
            favoriteDao.insertFavorite(fav)
        }
    }

    // --- CACHING ENGINE (FILESYSTEM DOWNLOAD) ---
    suspend fun isCached(songId: String): Boolean = withContext(Dispatchers.IO) {
        cachedSongDao.getCachedSongById(songId) != null
    }

    suspend fun getCachedLocalFile(songId: String): File? = withContext(Dispatchers.IO) {
        val record = cachedSongDao.getCachedSongById(songId) ?: return@withContext null
        val file = File(record.filePath)
        if (file.exists()) file else null
    }

    suspend fun downloadAndCache(song: JellyfinItem, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val server = _activeServer.value ?: return@withContext false
        if (isCached(song.id)) return@withContext true

        val playUrl = getSongUrl(song.id)
        val cacheDir = File(context.cacheDir, "audio_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        // Create cached filename
        val targetFile = File(cacheDir, "song_${song.id}.mp3")

        val request = Request.Builder().url(playUrl).build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                val totalBytes = body.contentLength()

                FileOutputStream(targetFile).use { fos ->
                    val inputStream = body.byteStream()
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = totalRead.toFloat() / totalBytes.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    fos.flush()
                }

                // Insert into Room
                val cached = CachedSong(
                    songId = song.id,
                    serverUrl = server.serverUrl,
                    title = song.name,
                    artist = song.albumArtist ?: song.artists?.firstOrNull() ?: "Unknown Artist",
                    album = song.albumName ?: "Unknown Album",
                    durationMs = song.durationMs,
                    filePath = targetFile.absolutePath
                )
                cachedSongDao.insertCachedSong(cached)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (targetFile.exists()) targetFile.delete()
            return@withContext false
        }
    }

    suspend fun deleteFromCache(songId: String) = withContext(Dispatchers.IO) {
        val record = cachedSongDao.getCachedSongById(songId)
        if (record != null) {
            val file = File(record.filePath)
            if (file.exists()) file.delete()
            cachedSongDao.deleteCachedSong(songId)
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        val songs = cachedSongDao.getAllCachedSongs()
        for (song in songs) {
            val file = File(song.filePath)
            if (file.exists()) file.delete()
        }
        cachedSongDao.clearCache()
    }

    // --- DEMO LIBRARIES AND HELPER IMPLEMENTATIONS ---
    private fun getDemoArtists(): List<JellyfinItem> {
        return listOf(
            JellyfinItem("art_1", "Chillhop Society", "MusicArtist"),
            JellyfinItem("art_2", "Retro Waves", "MusicArtist"),
            JellyfinItem("art_3", "Nora Light", "MusicArtist"),
            JellyfinItem("art_4", "Sierra Echo", "MusicArtist")
        )
    }

    private fun getDemoAlbums(): List<JellyfinItem> {
        return listOf(
            JellyfinItem("alb_1", "Zen Garden", "MusicAlbum", albumArtist = "Chillhop Society"),
            JellyfinItem("alb_2", "Neon Dreams", "MusicAlbum", albumArtist = "Retro Waves"),
            JellyfinItem("alb_3", "Therapy Cafe", "MusicAlbum", albumArtist = "Nora Light"),
            JellyfinItem("alb_4", "Wooden Timber", "MusicAlbum", albumArtist = "Sierra Echo")
        )
    }

    private fun getDemoSongs(parentId: String?): List<JellyfinItem> {
        val allTracks = listOf(
            JellyfinItem("demo_1", "Lofi Sunset", "Audio", "Zen Garden", "alb_1", albumArtist = "Chillhop Society", runTimeTicks = 312 * 10000000L),
            JellyfinItem("demo_2", "Midnight Synthwave", "Audio", "Neon Dreams", "alb_2", albumArtist = "Retro Waves", runTimeTicks = 422 * 10000000L),
            JellyfinItem("demo_3", "Deep Ambient Ocean", "Audio", "Therapy Cafe", "alb_3", albumArtist = "Nora Light", runTimeTicks = 502 * 10000000L),
            JellyfinItem("demo_4", "Acoustic Horizon", "Audio", "Wooden Timber", "alb_4", albumArtist = "Sierra Echo", runTimeTicks = 301 * 10000000L)
        )
        if (parentId == null) return allTracks

        return when {
            parentId.startsWith("art_") -> {
                val artistMap = mapOf(
                    "art_1" to "Chillhop Society",
                    "art_2" to "Retro Waves",
                    "art_3" to "Nora Light",
                    "art_4" to "Sierra Echo"
                )
                val artistName = artistMap[parentId] ?: ""
                allTracks.filter { it.albumArtist == artistName }
            }
            parentId.startsWith("alb_") -> {
                allTracks.filter { it.albumId == parentId }
            }
            else -> allTracks
        }
    }

    private fun getDemoSongUrl(songId: String): String {
        return when (songId) {
            "demo_1" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            "demo_2" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            "demo_3" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
            "demo_4" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"
            else -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        }
    }

    private fun getDemoArtworkUrl(itemId: String): String {
        // High fidelity royalty-free landscape / tech concept artwork URLs as high-contrast album placeholders
        return when (itemId) {
            "alb_1", "demo_1", "art_1" -> "https://picsum.photos/id/111/500/500" // Zen forest
            "alb_2", "demo_2", "art_2" -> "https://picsum.photos/id/104/500/500" // Tech retro neon
            "alb_3", "demo_3", "art_3" -> "https://picsum.photos/id/352/500/500" // Gentle light cafe
            "alb_4", "demo_4", "art_4" -> "https://picsum.photos/id/29/500/500"  // Log cabin/timber wood
            else -> "https://picsum.photos/id/1025/500/500" // Standard dog pup or high contrast art
        }
    }
}
