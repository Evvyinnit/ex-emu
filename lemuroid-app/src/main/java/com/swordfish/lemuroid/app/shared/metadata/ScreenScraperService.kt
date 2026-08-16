package com.swordfish.lemuroid.app.shared.metadata

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class ScraperCredentials(
    val devId: String,
    val devPassword: String,
    val username: String,
    val password: String,
) {
    val isConfigured: Boolean
        get() = devId.isNotBlank() && devPassword.isNotBlank()
}

data class ScrapeResult(
    val title: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val developer: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val coverUrl: String? = null,
    val screenshotUrl: String? = null,
)

class ScreenScraperService(
    private val client: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun scrapeGame(
        context: Context,
        game: Game,
        credentials: ScraperCredentials,
    ): ScrapeResult? =
        withContext(Dispatchers.IO) {
            if (!credentials.isConfigured) {
                return@withContext null
            }
            val systemeId = systemToScreenScraperId(game.systemId) ?: return@withContext null
            val md5 =
                Md5Hasher.md5(context, android.net.Uri.parse(game.fileUri)) ?: return@withContext null

            val urlBuilder =
                "https://api.screenscraper.fr/api2/jeuInfos.php"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("devid", credentials.devId)
                    .addQueryParameter("devpassword", credentials.devPassword)
                    .addQueryParameter("softname", "ExEmu")
                    .addQueryParameter("output", "json")
                    .addQueryParameter("romtype", "rom")
                    .addQueryParameter("systemeid", systemeId.toString())
                    .addQueryParameter("md5", md5)
            if (credentials.username.isNotBlank()) {
                urlBuilder
                    .addQueryParameter("ssid", credentials.username)
                    .addQueryParameter("sspassword", credentials.password)
            }

            val request = Request.Builder().url(urlBuilder.build()).get().build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    val jeu = json.decodeFromString<ScreenScraperResponse>(body).response?.jeu?.firstOrNull()
                        ?: return@withContext null
                    jeu.toScrapeResult()
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun ScreenScraperJeu.toScrapeResult(): ScrapeResult {
        val preferredName = noms?.firstOrNull { it.region == "wor" } ?: noms?.firstOrNull()
        val preferredSynopsis = synopsis?.firstOrNull { it.langue == "en" } ?: synopsis?.firstOrNull()
        val preferredGenre = genres?.firstOrNull()?.noms?.firstOrNull()?.text
        return ScrapeResult(
            title = preferredName?.text?.trim(),
            description = preferredSynopsis?.text?.trim(),
            genre = preferredGenre,
            developer = developpeur?.text?.trim(),
            year = dates?.firstOrNull()?.text?.trim(),
            rating = notes?.firstOrNull { it.type == "note" }?.text?.trim(),
            coverUrl = media?.box2d?.url ?: media?.wheel?.url,
            screenshotUrl = media?.screenshot?.url ?: media?.screenshots?.firstOrNull()?.url,
        )
    }

    companion object {
        fun systemToScreenScraperId(dbname: String): Int? =
            when (dbname) {
                "nes" -> 3
                "snes" -> 4
                "md" -> 1
                "gb" -> 9
                "gbc" -> 10
                "gba" -> 11
                "n64" -> 14
                "sms" -> 2
                "psp" -> 42
                "nds" -> 15
                "gg" -> 6
                "atari2600" -> 16
                "psx" -> 39
                "ps2" -> 40
                "fbneo" -> 75
                "mame2003plus" -> 53
                "pce" -> 23
                "lynx" -> 19
                "atari7800" -> 18
                "scd" -> 5
                "ngp" -> 13
                "ngc" -> 52
                "ws" -> 43
                "wsc" -> 44
                "dos" -> 82
                "3ds" -> 38
                else -> null
            }
    }
}

@Serializable
private data class ScreenScraperResponse(
    val response: ScreenScraperJeuResponse? = null,
)

@Serializable
private data class ScreenScraperJeuResponse(
    val jeu: List<ScreenScraperJeu>? = null,
)

@Serializable
private data class ScreenScraperJeu(
    val noms: List<ScreenScraperName>? = null,
    val synopsis: List<ScreenScraperText>? = null,
    val developpeur: ScreenScraperText? = null,
    val genres: List<ScreenScraperGenre>? = null,
    val dates: List<ScreenScraperText>? = null,
    val notes: List<ScreenScraperNote>? = null,
    val media: ScreenScraperMedia? = null,
)

@Serializable
private data class ScreenScraperName(
    val text: String? = null,
    val region: String? = null,
    val langue: String? = null,
)

@Serializable
private data class ScreenScraperText(
    val text: String? = null,
    val langue: String? = null,
)

@Serializable
private data class ScreenScraperGenre(
    val noms: List<ScreenScraperName>? = null,
)

@Serializable
private data class ScreenScraperNote(
    val text: String? = null,
    val type: String? = null,
)

@Serializable
private data class ScreenScraperMedia(
    val box2d: ScreenScraperUrl? = null,
    val wheel: ScreenScraperUrl? = null,
    val screenshot: ScreenScraperUrl? = null,
    val screenshots: List<ScreenScraperUrl>? = null,
)

@Serializable
private data class ScreenScraperUrl(
    val url: String? = null,
)