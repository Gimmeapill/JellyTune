package com.example.data.jellyfin

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class JellyfinClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val itemJsonAdapter = moshi.adapter(JellyfinItemsResponse::class.java)
    private val authResponseAdapter = moshi.adapter(AuthResponse::class.java)
    private val authRequestAdapter = moshi.adapter(AuthRequest::class.java)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Device ID used to identify client session
    var deviceId: String = "jellytune-android-${UUID.randomUUID().toString().take(8)}"

    // Standard MediaBrowser authorization header for Jellyfin & Emby
    fun getAuthHeaderValue(token: String? = null): String {
        return buildString {
            append("MediaBrowser Client=\"JellyTune\", Device=\"Android Mobile\", DeviceId=\"$deviceId\", Version=\"1.0.0\"")
            if (!token.isNullOrBlank()) {
                append(", Token=\"$token\"")
            }
        }
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        val sanitizedUrl = sanitizeUrl(serverUrl)
        val loginUrl = "$sanitizedUrl/Users/AuthenticateByName"

        val jsonBody = authRequestAdapter.toJson(AuthRequest(username = username.trim(), pw = password))
        val authHeader = getAuthHeaderValue()

        val request = Request.Builder()
            .url(loginUrl)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .addHeader("Authorization", authHeader)
            .addHeader("X-Emby-Authorization", authHeader)
            .addHeader("User-Agent", "JellyTune/1.0 (Android)")
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val rawBody = response.body?.string()?.trim()?.take(500) ?: ""
                    val detail = if (rawBody.isNotBlank()) ": $rawBody" else ""
                    return@withContext Result.failure(IOException("Server error ${response.code} (${response.message})$detail"))
                }
                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val authResponse = authResponseAdapter.fromJson(bodyString)
                    ?: return@withContext Result.failure(IOException("Failed to parse auth response"))

                Result.success(authResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchItems(
        serverUrl: String,
        token: String,
        userId: String,
        itemType: String, // "MusicAlbum", "MusicArtist", "Audio"
        parentId: String? = null,
        filters: String? = null,
        minDateLastSaved: String? = null
    ): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        val sanitizedUrl = sanitizeUrl(serverUrl)

        var queryUrl = "$sanitizedUrl/Users/$userId/Items?includeItemTypes=$itemType&recursive=true&fields=PrimaryImageAspectRatio,UserData,ProductionYear,IndexNumber,ParentIndexNumber,ArtistItems,Artists,DateCreated&limit=10000"
        if (parentId != null) {
            queryUrl += "&parentId=$parentId"
        }
        if (filters != null) {
            queryUrl += "&Filters=$filters&filters=$filters"
        }
        if (minDateLastSaved != null) {
            queryUrl += "&minDateLastSaved=$minDateLastSaved"
        }
        
        // Add sorting
        queryUrl += when (itemType) {
            "MusicArtist" -> "&sortBy=SortName&sortOrder=Ascending"
            "MusicAlbum" -> "&sortBy=SortName,ProductionYear&sortOrder=Ascending"
            "Audio" -> {
                if (parentId != null) {
                    "&sortBy=IndexNumber,SortName&sortOrder=Ascending"
                } else {
                    "&sortBy=SortName&sortOrder=Ascending"
                }
            }
            else -> ""
        }

        val authHeader = getAuthHeaderValue(token)
        val request = Request.Builder()
            .url(queryUrl)
            .get()
            .addHeader("Authorization", authHeader)
            .addHeader("X-Emby-Authorization", authHeader)
            .addHeader("X-MediaBrowser-Token", token)
            .addHeader("X-Emby-Token", token)
            .addHeader("User-Agent", "JellyTune/1.0 (Android)")
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code}"))
                }
                val source = response.body?.source()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val responseObj = itemJsonAdapter.fromJson(source)
                    ?: return@withContext Result.failure(IOException("Failed to parse music items"))

                Result.success(responseObj.items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getStreamUrl(serverUrl: String, songId: String, token: String): String {
        val sanitizedUrl = sanitizeUrl(serverUrl)
        return "$sanitizedUrl/Audio/$songId/stream?static=true&api_key=$token"
    }

    fun getArtworkUrl(serverUrl: String, itemId: String, token: String): String {
        val sanitizedUrl = sanitizeUrl(serverUrl)
        return "$sanitizedUrl/Items/$itemId/Images/Primary?api_key=$token"
    }

    suspend fun toggleFavoriteOnServer(
        serverUrl: String,
        token: String,
        userId: String,
        itemId: String,
        isFavorite: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val sanitizedUrl = sanitizeUrl(serverUrl)
        val url = "$sanitizedUrl/Users/$userId/FavoriteItems/$itemId"
        val body = if (isFavorite) "{}".toRequestBody(jsonMediaType) else null
        val authHeader = getAuthHeaderValue(token)
        val request = Request.Builder()
            .url(url)
            .method(if (isFavorite) "POST" else "DELETE", body)
            .addHeader("Authorization", authHeader)
            .addHeader("X-Emby-Authorization", authHeader)
            .addHeader("X-MediaBrowser-Token", token)
            .addHeader("X-Emby-Token", token)
            .addHeader("User-Agent", "JellyTune/1.0 (Android)")
            .addHeader("Accept", "application/json")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("Server error toggleFavoriteOnServer: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserViews(
        serverUrl: String,
        token: String,
        userId: String
    ): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        val sanitizedUrl = sanitizeUrl(serverUrl)
        val queryUrl = "$sanitizedUrl/Users/$userId/Views"

        val authHeader = getAuthHeaderValue(token)
        val request = Request.Builder()
            .url(queryUrl)
            .get()
            .addHeader("Authorization", authHeader)
            .addHeader("X-Emby-Authorization", authHeader)
            .addHeader("X-MediaBrowser-Token", token)
            .addHeader("X-Emby-Token", token)
            .addHeader("User-Agent", "JellyTune/1.0 (Android)")
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code}"))
                }
                val source = response.body?.source()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val responseObj = itemJsonAdapter.fromJson(source)
                    ?: return@withContext Result.failure(IOException("Failed to parse user views"))

                Result.success(responseObj.items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sanitizeUrl(url: String): String {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
            cleanUrl = "http://$cleanUrl"
        }
        while (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1)
        }
        // Remove web client path suffix if user copied URL from browser address bar
        if (cleanUrl.endsWith("/web/index.html", ignoreCase = true)) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - "/web/index.html".length)
        } else if (cleanUrl.endsWith("/web", ignoreCase = true)) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - "/web".length)
        }
        while (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1)
        }
        return cleanUrl
    }
}
