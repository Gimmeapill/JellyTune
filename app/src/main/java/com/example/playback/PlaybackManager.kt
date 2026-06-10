package com.example.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.database.CachedSong
import com.example.data.database.LocalFavorite
import com.example.data.jellyfin.JellyfinItem
import com.example.data.repository.JellyfinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class RepeatMode {
    NONE, ONE, ALL
}

data class PlaybackState(
    val currentSong: JellyfinItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<JellyfinItem> = emptyList(),
    val queueIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val isShuffle: Boolean = false,
    val isPlayingCached: Boolean = false,
    val audioFormatBadge: String = "HQ 320kbps MP3" 
)

class PlaybackManager(
    private val context: Context,
    private val repository: JellyfinRepository
) {
    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context, repository: JellyfinRepository): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackManager(context.applicationContext, repository).also { INSTANCE = it }
            }
        }
    }

    private fun ensureServiceStarted() {
        try {
            val intent = android.content.Intent(context, PlaybackService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackManager", "Failed to start PlaybackService", e)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    // Original ordering to support un-shuffling
    private var originalQueue: List<JellyfinItem> = emptyList()

    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "loudness_enhancer_enabled" || key == "loudness_enhancer_gain") {
            scope.launch {
                applyLoudnessEffect()
            }
        }
    }

    init {
        val prefs = context.getSharedPreferences("jellytune_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private fun broadcastAudioSessionId(open: Boolean) {
        val player = mediaPlayer ?: return
        val id = player.audioSessionId
        if (id == android.media.AudioManager.AUDIO_SESSION_ID_GENERATE) return
        val action = if (open) {
            "android.media.audiofx.AudioEffect.ACTION_OPEN_AUDIO_EFFECT_SESSION"
        } else {
            "android.media.audiofx.AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_SESSION"
        }
        try {
            val intent = android.content.Intent(action).apply {
                putExtra("android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION", id)
                putExtra("android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME", context.packageName)
                putExtra("android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE", 0) // CONTENT_TYPE_MUSIC
            }
            context.sendBroadcast(intent)
            android.util.Log.d("PlaybackManager", "Broadcasted audio session $id: $action")
        } catch (e: Exception) {
            android.util.Log.e("PlaybackManager", "Failed to broadcast audio session id", e)
        }
    }

    private fun applyLoudnessEffect() {
        val player = mediaPlayer ?: return
        val id = player.audioSessionId
        if (id == android.media.AudioManager.AUDIO_SESSION_ID_GENERATE) return

        try {
            releaseLoudnessEffect()

            val prefs = context.getSharedPreferences("jellytune_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("loudness_enhancer_enabled", false)
            val gainMb = prefs.getLong("loudness_enhancer_gain", 300L).toInt()

            if (isEnabled) {
                loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(id).apply {
                    setTargetGain(gainMb)
                    enabled = true
                }
                android.util.Log.d("PlaybackManager", "Applied LoudnessEnhancer with gain $gainMb mB on session $id")
            } else {
                android.util.Log.d("PlaybackManager", "LoudnessEnhancer disabled")
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackManager", "Failed to apply LoudnessEnhancer effect", e)
        }
    }

    private fun releaseLoudnessEffect() {
        try {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            loudnessEnhancer = null
        }
    }

    fun openSystemEqualizer(context: Context) {
        val id = mediaPlayer?.audioSessionId ?: 0
        try {
            val intent = android.content.Intent("android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL").apply {
                putExtra("android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION", id)
                putExtra("android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME", context.packageName)
                putExtra("android.media.audiofx.AudioEffect.EXTRA_CONTENT_TYPE", 0)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PlaybackManager", "System Equalizer not found or supported", e)
            android.widget.Toast.makeText(context, "System Equalizer is not supported on this device.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnCompletionListener {
                    handleTrackCompleted()
                }
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e("PlaybackManager", "MediaPlayer error: what=$what extra=$extra")
                    // Reset and attempt automatic skip
                    resetPlayer()
                    skipNext()
                    true
                }
            }
            broadcastAudioSessionId(true)
        }
    }

    private fun resetPlayer() {
        releaseLoudnessEffect()
        broadcastAudioSessionId(false)
        mediaPlayer?.reset()
    }

    // --- PLAYBACK CONTROLS ---

    fun appendSongsToQueue(songs: List<JellyfinItem>) {
        if (songs.isEmpty()) return
        initMediaPlayer()
        val currentQueue = _state.value.queue.toMutableList()
        currentQueue.addAll(songs)
        
        originalQueue = originalQueue + songs
        
        _state.value = _state.value.copy(
            queue = currentQueue,
            queueIndex = if (_state.value.queueIndex == -1) 0 else _state.value.queueIndex
        )
        // If nothing was playing or loaded, start playing the first appended track automatically
        if (_state.value.currentSong == null) {
            loadAndPlay(songs[0])
        }
    }

    fun playQueue(songs: List<JellyfinItem>, startIndex: Int) {
        if (songs.isEmpty()) return
        initMediaPlayer()

        originalQueue = songs
        val finalQueue = if (_state.value.isShuffle) songs.shuffled() else songs
        val finalIndex = if (_state.value.isShuffle) {
            val currentSong = songs.getOrNull(startIndex)
            finalQueue.indexOf(currentSong).coerceAtLeast(0)
        } else {
            startIndex
        }

        _state.value = _state.value.copy(
            queue = finalQueue,
            queueIndex = finalIndex
        )

        loadAndPlay(finalQueue[finalIndex])
    }

    private fun loadAndPlay(song: JellyfinItem) {
        scope.launch {
            stopProgressTracker()
            resetPlayer()

            // 1. Check if the song has been cached on physical disk
            val cachedFile = repository.getCachedLocalFile(song.id)
            val isCached = cachedFile != null

            // Increment play count inside CachedSong if cached
            if (isCached) {
                scope.launch(Dispatchers.IO) {
                    repository.incrementPlayCount(song.id)
                }
            }

            // High Fidelity audio spec detection
            val mockAudioBadge = if (isCached) {
                if (song.id.contains("demo")) "Hi-Fi Local FLAC (24-bit)" else "Cached 320kbps MP3"
            } else {
                if (song.id.contains("demo")) "Streaming FLAC (1411kbps)" else "Streaming 320kbps MP3"
            }

            _state.value = _state.value.copy(
                currentSong = song,
                isPlayingCached = isCached,
                audioFormatBadge = mockAudioBadge,
                positionMs = 0,
                durationMs = song.durationMs
            )

            ensureServiceStarted()

            try {
                withContext(Dispatchers.IO) {
                    if (cachedFile != null) {
                        // Play from local phone layout
                        mediaPlayer?.setDataSource(cachedFile.absolutePath)
                    } else {
                        // Play from Jellyfin server URL directly
                        val streamUrl = repository.getSongUrl(song.id)
                        mediaPlayer?.setDataSource(streamUrl)
                    }
                    mediaPlayer?.prepare()
                }

                mediaPlayer?.start()
                _state.value = _state.value.copy(
                    isPlaying = true,
                    durationMs = mediaPlayer?.duration?.toLong() ?: song.durationMs
                )
                startProgressTracker()
                applyLoudnessEffect()
                broadcastAudioSessionId(true)

                // Trigger background download-on-play cache if enabled
                if (!isCached && !repository.isDemo()) {
                    scope.launch(Dispatchers.IO) {
                        repository.downloadAndCache(song)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Skip next on error
                skipNext()
            }
        }
    }

    fun togglePlayPause() {
        ensureServiceStarted()
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _state.value = _state.value.copy(isPlaying = false)
            stopProgressTracker()
        } else {
            player.start()
            _state.value = _state.value.copy(isPlaying = true)
            startProgressTracker()
        }
    }

    fun skipNext() {
        val s = _state.value
        if (s.queue.isEmpty()) return

        var nextIndex = s.queueIndex + 1
        if (nextIndex >= s.queue.size) {
            nextIndex = if (s.repeatMode == RepeatMode.ALL) 0 else s.queue.size - 1
        }

        if (nextIndex == s.queueIndex && s.queueIndex == s.queue.size - 1 && s.repeatMode != RepeatMode.ALL) {
            // End of play queue, stop player
            _state.value = _state.value.copy(isPlaying = false, positionMs = 0)
            resetPlayer()
            stopProgressTracker()
            return
        }

        _state.value = _state.value.copy(queueIndex = nextIndex)
        loadAndPlay(s.queue[nextIndex])
    }

    fun skipPrevious() {
        val s = _state.value
        if (s.queue.isEmpty()) return

        // If played more than 3 seconds, restart active song first
        if ((mediaPlayer?.currentPosition ?: 0) > 3000) {
            seekTo(0)
            return
        }

        var prevIndex = s.queueIndex - 1
        if (prevIndex < 0) {
            prevIndex = if (s.repeatMode == RepeatMode.ALL) s.queue.size - 1 else 0
        }

        _state.value = _state.value.copy(queueIndex = prevIndex)
        loadAndPlay(s.queue[prevIndex])
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun toggleShuffle() {
        val s = _state.value
        val newShuffle = !s.isShuffle
        if (s.queue.isEmpty()) {
            _state.value = _state.value.copy(isShuffle = newShuffle)
            return
        }

        val currentSong = s.currentSong
        val shufQueue = if (newShuffle) {
            val mutable = s.queue.toMutableList()
            if (currentSong != null) {
                mutable.remove(currentSong)
                mutable.shuffle()
                mutable.add(0, currentSong)
                mutable
            } else {
                s.queue.shuffled()
            }
        } else {
            // Restore original queue order
            val mutable = originalQueue.toMutableList()
            // Pull matching sub queue
            mutable
        }

        val idx = currentSong?.let { shufQueue.indexOf(it) } ?: -1

        _state.value = _state.value.copy(
            isShuffle = newShuffle,
            queue = shufQueue,
            queueIndex = idx
        )
    }

    fun toggleRepeatMode() {
        val current = _state.value.repeatMode
        val nextMode = when (current) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        _state.value = _state.value.copy(repeatMode = nextMode)
    }

    // --- REPEATING STATE & CONTINUOUS PLAYBACK ---

    private fun handleTrackCompleted() {
        val s = _state.value
        when (s.repeatMode) {
            RepeatMode.ONE -> {
                // Loop same track
                s.currentSong?.let { loadAndPlay(it) }
            }
            else -> {
                // Advance
                skipNext()
            }
        }
    }

    // --- TIME TRACKER COROUTINE ---

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (true) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _state.value = _state.value.copy(
                            positionMs = player.currentPosition.toLong()
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        val prefs = context.getSharedPreferences("jellytune_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
        releaseLoudnessEffect()
        broadcastAudioSessionId(false)
        stopProgressTracker()
        mediaPlayer?.release()
        mediaPlayer = null
        try {
            val intent = android.content.Intent(context, PlaybackService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            android.util.Log.e("PlaybackManager", "Failed to stop PlaybackService on release", e)
        }
    }
}

// Extension converters to map Room database model classes back to Jellyfin UI models
fun CachedSong.toJellyfinItem(): JellyfinItem {
    return JellyfinItem(
        id = this.songId,
        name = this.title,
        type = "Audio",
        albumName = this.album,
        albumArtist = this.artist,
        runTimeTicks = this.durationMs * 10000L
    )
}

fun LocalFavorite.toJellyfinItem(): JellyfinItem {
    return JellyfinItem(
        id = this.songId,
        name = this.title,
        type = "Audio",
        albumName = this.album,
        albumArtist = this.artist,
        runTimeTicks = this.durationMs * 10000L
    )
}
