package com.swordfish.lemuroid.app.shared.metadata

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class RaCredentials(
    val username: String,
    val apiKey: String,
) {
    val isConfigured: Boolean
        get() = username.isNotBlank() && apiKey.isNotBlank()
}

data class RaUserSummary(
    val username: String? = null,
    val memberSince: String? = null,
    val totalPoints: Int = 0,
    val totalTruePoints: Int = 0,
    val recentlyPlayed: List<RaRecentlyPlayed> = emptyList(),
)

data class RaRecentlyPlayed(
    val title: String,
    val consoleName: String,
    val numAchieved: Int,
    val maxPossible: Int,
)

data class RaGameAchievements(
    val gameTitle: String? = null,
    val achievements: List<RaAchievement> = emptyList(),
)

data class RaAchievement(
    val title: String,
    val description: String,
    val points: Int,
    val earned: Boolean,
)

class RetroAchievementsService(
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchUserSummary(credentials: RaCredentials): RaUserSummary? =
        withContext(Dispatchers.IO) {
            if (!credentials.isConfigured) {
                return@withContext null
            }
            val url =
                apiUrl()
                    .addQueryParameter("z", credentials.username)
                    .addQueryParameter("y", credentials.apiKey)
                    .addQueryParameter("r", "user-summary")
                    .build()
            execute(url) { response ->
                val summary = json.decodeFromString<RaUserSummaryResponse>(response)
                RaUserSummary(
                    username = summary.User,
                    memberSince = summary.MemberSince,
                    totalPoints = summary.TotalPoints ?: 0,
                    totalTruePoints = summary.TotalTruePoints ?: 0,
                    recentlyPlayed =
                        summary.RecentlyPlayed
                            ?.map { game ->
                                RaRecentlyPlayed(
                                    title = game.Title ?: "",
                                    consoleName = game.ConsoleName ?: "",
                                    numAchieved = game.NumAchieved ?: 0,
                                    maxPossible = game.MaxPossible ?: 0,
                                )
                            }
                            ?: emptyList(),
                )
            }
        }

    suspend fun fetchGameAchievements(
        credentials: RaCredentials,
        md5: String,
    ): RaGameAchievements? =
        withContext(Dispatchers.IO) {
            if (!credentials.isConfigured) {
                return@withContext null
            }
            val url =
                apiUrl()
                    .addQueryParameter("z", credentials.username)
                    .addQueryParameter("y", credentials.apiKey)
                    .addQueryParameter("r", "game")
                    .addQueryParameter("m", md5)
                    .build()
            execute(url) { response ->
                val game = json.decodeFromString<RaGameResponse>(response)
                RaGameAchievements(
                    gameTitle = game.Game?.Title,
                    achievements =
                        game.Achievements
                            ?.map { achievement ->
                                RaAchievement(
                                    title = achievement.Title ?: "",
                                    description = achievement.Description ?: "",
                                    points = achievement.Points ?: 0,
                                    earned = achievement.Earned ?: false,
                                )
                            }
                            ?: emptyList(),
                )
            }
        }

    private fun apiUrl() =
        "https://retroachievements.org/API/API_JSON.php"
            .toHttpUrl()
            .newBuilder()

    private suspend fun <T> execute(
        url: okhttp3.HttpUrl,
        parse: (String) -> T?,
    ): T? =
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use null
                }
                val body = response.body?.string() ?: return@use null
                parse(body)
            }
        } catch (e: Exception) {
            null
        }
}

@Serializable
private data class RaUserSummaryResponse(
    val User: String? = null,
    val TotalPoints: Int? = null,
    val TotalTruePoints: Int? = null,
    val MemberSince: String? = null,
    val RecentlyPlayed: List<RaRecentlyPlayedResponse>? = null,
)

@Serializable
private data class RaRecentlyPlayedResponse(
    val Title: String? = null,
    val ConsoleName: String? = null,
    val MaxPossible: Int? = null,
    val NumAchieved: Int? = null,
)

@Serializable
private data class RaGameResponse(
    val Game: RaGameInfoResponse? = null,
    val Achievements: List<RaAchievementResponse>? = null,
)

@Serializable
private data class RaGameInfoResponse(
    val Title: String? = null,
)

@Serializable
private data class RaAchievementResponse(
    val Title: String? = null,
    val Description: String? = null,
    val Points: Int? = null,
    val Earned: Boolean? = null,
)