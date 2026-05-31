package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.MainActivity
import com.example.data.repository.JellyfinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaybackService : Service() {

    private lateinit var mediaSession: MediaSession
    private lateinit var playbackManager: PlaybackManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "jellytune_playback_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_PLAY_PAUSE = "com.example.playback.PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.example.playback.PREVIOUS"
        const val ACTION_NEXT = "com.example.playback.NEXT"
        const val ACTION_STOP = "com.example.playback.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("PlaybackService", "Service onCreate")
        createNotificationChannel()

        val repository = JellyfinRepository.getInstance(applicationContext)
        playbackManager = PlaybackManager.getInstance(applicationContext, repository)

        mediaSession = MediaSession(applicationContext, "JellyTuneMediaSession").apply {
            isActive = true
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    playbackManager.togglePlayPause()
                }

                override fun onPause() {
                    playbackManager.togglePlayPause()
                }

                override fun onSkipToNext() {
                    playbackManager.skipNext()
                }

                override fun onSkipToPrevious() {
                    playbackManager.skipPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    playbackManager.seekTo(pos)
                }
            })
        }

        // 1. Immediately become a foreground service to prevent crashes from startForegroundService
        val placeholder = buildPlaceholderNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, placeholder, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, placeholder)
        }

        // Reactively observe playback changes and update notification in real time
        serviceScope.launch {
            playbackManager.state.collectLatest { state ->
                updateNotificationAndSession(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("PlaybackService", "Service Action: $action")

        val state = playbackManager.state.value
        val song = state.currentSong

        // Robust guard to guarantee startForeground is called immediately
        if (song == null) {
            val placeholder = buildPlaceholderNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, placeholder, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, placeholder)
            }
            if (action == null) {
                @Suppress("DEPRECATION")
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
        } else {
            updateNotificationAndSession(state)
        }

        when (action) {
            ACTION_PLAY_PAUSE -> playbackManager.togglePlayPause()
            ACTION_NEXT -> playbackManager.skipNext()
            ACTION_PREVIOUS -> playbackManager.skipPrevious()
            ACTION_STOP -> {
                @Suppress("DEPRECATION")
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun buildPlaceholderNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flagUpdateCurrent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, flagUpdateCurrent)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder.setContentTitle("JellyTune")
            .setContentText("Preparing audio stream...")
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
            )
            .build()
    }

    private var lastNotifiedSongId: String? = null
    private var lastNotifiedPlayState: Boolean? = null

    private fun updateNotificationAndSession(state: PlaybackState) {
        val song = state.currentSong
        if (song == null) {
            @Suppress("DEPRECATION")
            stopForeground(true)
            stopSelf()
            return
        }

        // 1. Update MediaSession playback state
        val speed = if (state.isPlaying) 1.0f else 0.0f
        val sessionState = android.media.session.PlaybackState.Builder()
            .setActions(
                android.media.session.PlaybackState.ACTION_PLAY or
                        android.media.session.PlaybackState.ACTION_PAUSE or
                        android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT or
                        android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        android.media.session.PlaybackState.ACTION_SEEK_TO
            )
            .setState(
                if (state.isPlaying) android.media.session.PlaybackState.STATE_PLAYING else android.media.session.PlaybackState.STATE_PAUSED,
                state.positionMs,
                speed
            )
            .build()
        mediaSession.setPlaybackState(sessionState)

        val shouldUpdateNotification = lastNotifiedSongId != song.id || lastNotifiedPlayState != state.isPlaying
        if (!shouldUpdateNotification) {
            return
        }

        lastNotifiedSongId = song.id
        lastNotifiedPlayState = state.isPlaying

        // 2. Update MediaSession metadata
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.name)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.albumArtist ?: "Unknown Artist")
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.albumName ?: "Unknown Album")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)
            .build()
        mediaSession.setMetadata(metadata)

        // 3. Prepare Notification custom design & PendingIntents
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flagUpdateCurrent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, flagUpdateCurrent)

        val prevIntent = Intent(this, PlaybackService::class.java).apply { this.action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, flagUpdateCurrent)

        val playPauseIntent = Intent(this, PlaybackService::class.java).apply { this.action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(this, 2, playPauseIntent, flagUpdateCurrent)

        val nextIntent = Intent(this, PlaybackService::class.java).apply { this.action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, flagUpdateCurrent)

        val stopIntent = Intent(this, PlaybackService::class.java).apply { this.action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, flagUpdateCurrent)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val playPauseIcon = if (state.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        builder.setContentTitle(song.name)
            .setContentText(song.albumArtist ?: "Unknown Artist")
            .setSubText(song.albumName ?: "Unknown Album")
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(state.isPlaying)
            .setCategory(Notification.CATEGORY_SERVICE)

        // Add action buttons
        builder.addAction(Notification.Action.Builder(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent).build())
        builder.addAction(Notification.Action.Builder(playPauseIcon, if (state.isPlaying) "Pause" else "Play", playPausePendingIntent).build())
        builder.addAction(Notification.Action.Builder(android.R.drawable.ic_media_next, "Next", nextPendingIntent).build())
        builder.addAction(Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent).build())

        // Set stylish media layout on platforms supporting it
        builder.setStyle(
            Notification.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )

        val notification = builder.build()

        // 4. Update foreground state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JellyTune Media Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground playback controls and dynamic updates"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
