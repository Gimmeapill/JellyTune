package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JellyfinServerDao {
    @Query("SELECT * FROM jellyfin_servers ORDER BY lastActive DESC")
    fun getAllServers(): Flow<List<JellyfinServer>>

    @Query("SELECT * FROM jellyfin_servers ORDER BY lastActive DESC LIMIT 1")
    suspend fun getActiveServer(): JellyfinServer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: JellyfinServer)

    @Query("DELETE FROM jellyfin_servers WHERE serverUrl = :serverUrl")
    suspend fun deleteServer(serverUrl: String)

    @Query("DELETE FROM jellyfin_servers")
    suspend fun clearServers()
}

@Dao
interface CachedSongDao {
    @Query("SELECT * FROM cached_songs ORDER BY cachedAt DESC")
    fun getAllCachedSongsFlow(): Flow<List<CachedSong>>

    @Query("SELECT * FROM cached_songs ORDER BY cachedAt DESC")
    suspend fun getAllCachedSongs(): List<CachedSong>

    @Query("SELECT * FROM cached_songs WHERE songId = :songId")
    suspend fun getCachedSongById(songId: String): CachedSong?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSong(song: CachedSong)

    @Query("DELETE FROM cached_songs WHERE songId = :songId")
    suspend fun deleteCachedSong(songId: String)

    @Query("DELETE FROM cached_songs")
    suspend fun clearCache()
}

@Dao
interface LocalFavoriteDao {
    @Query("SELECT * FROM local_favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<LocalFavorite>>

    @Query("SELECT * FROM local_favorites ORDER BY addedAt DESC")
    suspend fun getAllFavoritesList(): List<LocalFavorite>

    @Query("SELECT EXISTS(SELECT 1 FROM local_favorites WHERE songId = :songId)")
    suspend fun isFavorite(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(song: LocalFavorite)

    @Query("DELETE FROM local_favorites WHERE songId = :songId")
    suspend fun deleteFavorite(songId: String)
}
