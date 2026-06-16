package com.example.data.jellyfin

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequest(
    @Json(name = "Username") val username: String,
    @Json(name = "Pw") val pw: String
)

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "Id") val id: String,
    @Json(name = "Name") val name: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "AccessToken") val accessToken: String,
    @Json(name = "User") val user: User,
    @Json(name = "ServerId") val serverId: String? = null
)

@JsonClass(generateAdapter = true)
data class JellyfinItem(
    @Json(name = "Id") val id: String = "",
    @Json(name = "Name") val name: String = "",
    @Json(name = "Type") val type: String = "", // "MusicAlbum", "MusicArtist", "Audio", "Playlist"
    @Json(name = "CollectionType") val collectionType: String? = null,
    @Json(name = "Album") val albumName: String? = null,
    @Json(name = "AlbumId") val albumId: String? = null,
    @Json(name = "Artists") val artists: List<String>? = null,
    @Json(name = "ArtistItems") val artistItems: List<ArtistItem>? = null,
    @Json(name = "AlbumArtist") val albumArtist: String? = null,
    @Json(name = "RunTimeTicks") val runTimeTicks: Long? = null,
    @Json(name = "ImageTags") val imageTags: Map<String, String>? = null,
    @Json(name = "UserData") val userData: UserData? = null,
    @Json(name = "ProductionYear") val productionYear: Int? = null,
    @Json(name = "IndexNumber") val indexNumber: Int? = null,
    @Json(name = "ParentIndexNumber") val parentIndexNumber: Int? = null
) {
    val durationMs: Long
        get() = (runTimeTicks ?: 0L) / 10000L // 10,000 ticks per millisecond

    val primaryImageUrl: String
        get() = "" // Dynamic calculation on client based on serverUrl
}

@JsonClass(generateAdapter = true)
data class ArtistItem(
    @Json(name = "Name") val name: String = "",
    @Json(name = "Id") val id: String = ""
)

@JsonClass(generateAdapter = true)
data class JellyfinItemsResponse(
    @Json(name = "Items") val items: List<JellyfinItem>
)

@JsonClass(generateAdapter = true)
data class UserData(
    @Json(name = "IsFavorite") val isFavorite: Boolean = false,
    @Json(name = "PlayCount") val playCount: Int = 0,
    @Json(name = "LastPlayedDate") val lastPlayedDate: String? = null
)
