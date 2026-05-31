package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ApiCacheDao {
    @Query("SELECT * FROM api_cache WHERE cacheKey = :key")
    suspend fun getCache(key: String): ApiCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: ApiCache)

    @Query("DELETE FROM api_cache")
    suspend fun clearAll()
}
