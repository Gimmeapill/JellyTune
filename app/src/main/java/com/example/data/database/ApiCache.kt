package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_cache")
data class ApiCache(
    @PrimaryKey val cacheKey: String,
    val timestamp: Long,
    val dataJson: String
)
