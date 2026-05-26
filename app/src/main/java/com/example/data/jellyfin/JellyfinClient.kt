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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val itemJsonAdapter = moshi.adapter(JellyfinItemsResponse::class.java)
    private val authResponseAdapter = moshi.adapter(AuthResponse::class.java)

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Create a device ID
    val deviceId = "jellytune-android-${UUID.randomUUID().toString().take(8)}"

    // X-Emby-Authorization header used for authenticating with Jellyfin
    private fun getAuthHeaderValue(username: String): String {
        return "MediaBrowser Client=\"JellyTune\", Device=\"Android Mobile\", DeviceId=\"$deviceId\", Version=\"1.0.0\", User=\"$username\""
    }

    suspend fun authenticate(
        serverUrl: String,
        username: String,
        password: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        val sanitizedUrl = sanitizeUrl(serverUrl)
        val loginUrl = "$sanitizedUrl/Users/AuthenticateByName"

        val jsonBody = """
            {
                "Username": "$username",
                "Pw": "$password"
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(loginUrl)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .addHeader("X-Emby-Authorization", getAuthHeaderValue(username))
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code} ${response.message}"))
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
        parentId: String? = null
    ): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        val sanitizedUrl = sanitizeUrl(serverUrl)

        var queryUrl = "$sanitizedUrl/Users/$userId/Items?IncludeItemTypes=$itemType&Recursive=true&fields=PrimaryImageAspectRatio"
        if (parentId != null) {
            queryUrl += "&ParentId=$parentId"
        }
        
        // Add sorting
        queryUrl += when (itemType) {
            "MusicArtist" -> "&SortBy=SortName&SortOrder=Ascending"
            "MusicAlbum" -> "&SortBy=SortName,ProductionYear&SortOrder=Ascending"
            "Audio" -> "&SortBy=IndexNumber,SortName&SortOrder=Ascending"
            else -> ""
        }

        val request = Request.Builder()
            .url(queryUrl)
            .get()
            .addHeader("X-MediaBrowser-Token", token)
            .addHeader("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code}"))
                }
                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val responseObj = itemJsonAdapter.fromJson(bodyString)
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

    private fun sanitizeUrl(url: String): String {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "http://$cleanUrl"
        }
        while (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length - 1)
        }
        return cleanUrl
    }
}
