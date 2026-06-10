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

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Types

class JellyfinRepository(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: JellyfinRepository? = null

        fun getInstance(context: Context): JellyfinRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JellyfinRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val db = AppDatabase.getDatabase(context)
    private val serverDao = db.serverDao()
    private val cachedSongDao = db.cachedSongDao()
    private val favoriteDao = db.favoriteDao()
    private val apiCacheDao = db.apiCacheDao()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, JellyfinItem::class.java)
    private val listAdapter = moshi.adapter<List<JellyfinItem>>(listType)

    private val client = JellyfinClient()
    private val okHttpClient = OkHttpClient()

    private val _activeServer = MutableStateFlow<JellyfinServer?>(null)
    val activeServer: StateFlow<JellyfinServer?> = _activeServer.asStateFlow()

    // Flow definitions for cached items and favorites
    val cachedSongs: Flow<List<CachedSong>> = cachedSongDao.getAllCachedSongsFlow()
    val localFavorites: Flow<List<LocalFavorite>> = favoriteDao.getAllFavorites()

    val apiCacheUpdated = kotlinx.coroutines.flow.MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    private val repositoryScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

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
    private fun formatIso8601(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date(timestamp))
    }

    suspend fun getArtists(forceFull: Boolean = false): List<JellyfinItem> {
        if (isDemo()) return getDemoArtists()
        val server = _activeServer.value ?: return emptyList()
        return fetchWithCache(server, "getArtists", null, forceFull) { minDate ->
            client.fetchItems(server.serverUrl, server.token, server.userId, "MusicArtist", minDateLastSaved = minDate).getOrDefault(emptyList())
        }
    }

    suspend fun getAlbums(parentId: String? = null, forceFull: Boolean = false): List<JellyfinItem> {
        if (isDemo()) {
            val all = getDemoAlbums()
            if (parentId == null) return all
            val artistMap = mapOf(
                "art_1" to "Chillhop Society",
                "art_2" to "Retro Waves",
                "art_3" to "Nora Light",
                "art_4" to "Sierra Echo"
            )
            val name = artistMap[parentId] ?: return all
            return all.filter { it.albumArtist == name }
        }
        val server = _activeServer.value ?: return emptyList()
        return fetchWithCache(server, "getAlbums", parentId, forceFull) { minDate ->
            client.fetchItems(server.serverUrl, server.token, server.userId, "MusicAlbum", parentId, minDateLastSaved = minDate).getOrDefault(emptyList())
        }
    }

    suspend fun getSongs(parentId: String? = null, forceFull: Boolean = false): List<JellyfinItem> {
        if (isDemo()) return getDemoSongs(parentId)
        val server = _activeServer.value ?: return emptyList()
        val songs = fetchWithCache(server, "getSongs", parentId, forceFull) { minDate ->
            val result = client.fetchItems(server.serverUrl, server.token, server.userId, "Audio", parentId, minDateLastSaved = minDate)
            if (result.isFailure) {
                android.util.Log.e("JellyfinRepository", "getSongs failed: ${result.exceptionOrNull()?.message}")
            }
            result.getOrDefault(emptyList())
        }
        syncSongsFavoriteState(songs)
        return songs
    }

    private suspend fun fetchWithCache(
        server: com.example.data.database.JellyfinServer,
        operation: String,
        parentId: String?,
        forceFull: Boolean = false,
        fetchLogic: suspend (String?) -> List<JellyfinItem>
    ): List<JellyfinItem> {
        val cacheKey = "cached_${operation}_${server.serverUrl}_${server.userId}_${parentId ?: "all"}"
        val sanitizedCacheName = "apicache_${cacheKey.hashCode()}"
        val cacheFile = File(context.cacheDir, "$sanitizedCacheName.json")
        val timestampFile = File(context.cacheDir, "$sanitizedCacheName.time")

        if (forceFull) {
            try {
                val freshList = fetchLogic(null)
                if (freshList.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        cacheFile.writeText(listAdapter.toJson(freshList))
                        timestampFile.writeText(System.currentTimeMillis().toString())
                    }
                    apiCacheUpdated.emit(cacheKey)
                }
                return freshList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val cachedList = if (cacheFile.exists()) {
            withContext(Dispatchers.IO) {
                try {
                    listAdapter.fromJson(cacheFile.readText())
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } else null

        if (cachedList != null && cachedList.isNotEmpty()) {
            val lastSyncTime = if (timestampFile.exists()) {
                timestampFile.readText().toLongOrNull() ?: System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }
            repositoryScope.launch {
                try {
                    val isoString = formatIso8601(lastSyncTime)
                    val deltaList = fetchLogic(isoString)
                    if (deltaList.isNotEmpty()) {
                        val mergedMap = cachedList.associateBy { it.id }.toMutableMap()
                        for (item in deltaList) {
                            mergedMap[item.id] = item
                        }
                        val mergedList = mergedMap.values.toList()
                        withContext(Dispatchers.IO) {
                            cacheFile.writeText(listAdapter.toJson(mergedList))
                            timestampFile.writeText(System.currentTimeMillis().toString())
                        }
                        apiCacheUpdated.emit(cacheKey)
                    } else {
                        withContext(Dispatchers.IO) {
                            timestampFile.writeText(System.currentTimeMillis().toString())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return cachedList
        }

        try {
            val freshList = fetchLogic(null)
            if (freshList.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    cacheFile.writeText(listAdapter.toJson(freshList))
                    timestampFile.writeText(System.currentTimeMillis().toString())
                }
            }
            return freshList
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
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
        val isFav = favoriteDao.isFavorite(song.id)
        if (isFav) {
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

        // Sync favorite with server if we're not in demo mode
        if (!isDemo()) {
            client.toggleFavoriteOnServer(server.serverUrl, server.token, server.userId, song.id, !isFav)
        }
    }

    suspend fun toggleFavoriteLocal(song: CachedSong) = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(song.songId)
        if (isFav) {
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

        // Sync favorite with server if we're not in demo mode
        val server = _activeServer.value
        if (!isDemo() && server != null && server.serverUrl == song.serverUrl) {
            client.toggleFavoriteOnServer(server.serverUrl, server.token, server.userId, song.songId, !isFav)
        }
    }

    suspend fun syncFavorites() = withContext(Dispatchers.IO) {
        if (isDemo()) return@withContext
        val server = _activeServer.value ?: return@withContext
        val result = client.fetchItems(
            serverUrl = server.serverUrl,
            token = server.token,
            userId = server.userId,
            itemType = "Audio",
            parentId = null,
            filters = "IsFavorite"
        )
        if (result.isSuccess) {
            val serverFavs = result.getOrNull() ?: emptyList()
            val serverFavIds = serverFavs.map { it.id }.toSet()
            val allLocal = favoriteDao.getAllFavoritesList()
            val currentLocalIds = allLocal.map { it.songId }.toSet()

            // 1. Sync server-side favorites down to the local database
            for (song in serverFavs) {
                if (song.id !in currentLocalIds) {
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

            // 2. Safe Bi-directional Sync:
            for (localFav in allLocal) {
                if (localFav.serverUrl == server.serverUrl && localFav.songId !in serverFavIds) {
                    val ageMs = System.currentTimeMillis() - localFav.addedAt
                    if (ageMs > 300_000) {
                        // The favorite is older than 5 minutes and is not on the server,
                        // so it was intentionally deleted on another client. Remove it locally.
                        favoriteDao.deleteFavorite(localFav.songId)
                    } else {
                        // The favorite was added recently (possibly offline). Upload it to the server!
                        client.toggleFavoriteOnServer(
                            serverUrl = server.serverUrl,
                            token = server.token,
                            userId = server.userId,
                            itemId = localFav.songId,
                            isFavorite = true
                        )
                    }
                }
            }
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

        // Enforce the cache size limit before adding a new song
        val prefs = context.getSharedPreferences("jellytune_prefs", Context.MODE_PRIVATE)
        val limitMb = prefs.getLong("max_cache_size_mb", 1024L)
        enforceCacheLimit(limitMb)

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

    suspend fun getCachedSongsList(): List<CachedSong> = withContext(Dispatchers.IO) {
        cachedSongDao.getAllCachedSongs()
    }

    suspend fun incrementPlayCount(songId: String) = withContext(Dispatchers.IO) {
        val record = cachedSongDao.getCachedSongById(songId)
        if (record != null) {
            val updated = record.copy(
                playCount = record.playCount + 1,
                lastPlayedAt = System.currentTimeMillis()
            )
            cachedSongDao.insertCachedSong(updated)
        }
    }

    suspend fun enforceCacheLimit(maxSizeMb: Long) = withContext(Dispatchers.IO) {
        if (maxSizeMb == Long.MAX_VALUE || maxSizeMb <= 0) return@withContext

        val maxSizeBytes = maxSizeMb * 1024 * 1024
        val currentCached = cachedSongDao.getAllCachedSongs()
        
        var totalSize = currentCached.sumOf {
            val file = File(it.filePath)
            if (file.exists()) file.length() else 0L
        }

        if (totalSize > maxSizeBytes) {
            // Group other songs by album to evict at the album level.
            // If some items have no album, group them as unique "singles" so they can be evicted individually,
            // rather than evicting all unknowns at once.
            val albumGroups = currentCached.groupBy { song ->
                if (song.album.isBlank() || song.album.equals("Unknown Album", ignoreCase = true)) {
                    "Single_${song.songId}"
                } else {
                    song.album
                }
            }

            class AlbumMetric(
                val albumName: String,
                val songs: List<CachedSong>,
                val totalPlays: Int,
                val maxLastPlayedAt: Long,
                val minCachedAt: Long,
                val totalSize: Long
            )

            val albumsWithMetrics = albumGroups.map { (albumName, songs) ->
                val totalPlays = songs.sumOf { it.playCount }
                val maxLastPlayedAt = songs.maxOfOrNull { it.lastPlayedAt } ?: 0L
                val minCachedAt = songs.minOfOrNull { it.cachedAt } ?: 0L
                val albumSize = songs.sumOf {
                    val file = File(it.filePath)
                    if (file.exists()) file.length() else 0L
                }
                AlbumMetric(
                    albumName = albumName,
                    songs = songs,
                    totalPlays = totalPlays,
                    maxLastPlayedAt = maxLastPlayedAt,
                    minCachedAt = minCachedAt,
                    totalSize = albumSize
                )
            }

            // Evict worst-ranking albums first:
            // 1) total play count ascending (least played first)
            // 2) maxLastPlayedAt ascending (longest ago played first)
            // 3) minCachedAt ascending (longest time in cache first)
            val sortedAlbums = albumsWithMetrics.sortedWith(
                compareBy<AlbumMetric> { it.totalPlays }
                    .thenBy { it.maxLastPlayedAt }
                    .thenBy { it.minCachedAt }
            )

            for (albumToEvict in sortedAlbums) {
                if (totalSize <= maxSizeBytes) break
                
                // Prune the entire album group
                for (songToEvict in albumToEvict.songs) {
                    val file = File(songToEvict.filePath)
                    val fileSize = if (file.exists()) file.length() else 0L
                    if (file.exists()) {
                        file.delete()
                    }
                    cachedSongDao.deleteCachedSong(songToEvict.songId)
                    totalSize -= fileSize
                }
            }
        }
    }

    suspend fun syncSongsFavoriteState(songs: List<JellyfinItem>) = withContext(Dispatchers.IO) {
        if (isDemo()) return@withContext
        val server = _activeServer.value ?: return@withContext
        val currentLocalFavs = favoriteDao.getAllFavoritesList()
        val currentLocalIds = currentLocalFavs.map { it.songId }.toSet()

        for (song in songs) {
            val isServerFav = song.userData?.isFavorite == true
            val isLocalFav = song.id in currentLocalIds

            if (isServerFav && !isLocalFav) {
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
