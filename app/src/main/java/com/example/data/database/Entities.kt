package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jellyfin_servers")
data class JellyfinServer(
    @PrimaryKey val serverUrl: String,
    val username: String,
    val token: String,
    val userId: String,
    val deviceId: String,
    val lastActive: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_songs")
data class CachedSong(
    @PrimaryKey val songId: String,
    val serverUrl: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val filePath: String,
    val cachedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)

@Entity(tableName = "local_favorites")
data class LocalFavorite(
    @PrimaryKey val songId: String,
    val serverUrl: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val addedAt: Long = System.currentTimeMillis()
)
