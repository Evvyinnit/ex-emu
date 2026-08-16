package com.swordfish.lemuroid.app.mobile.feature.gamedetails

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.metadata.ScraperCredentials
import com.swordfish.lemuroid.app.shared.metadata.ScreenScraperService
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameDetailsViewModel(
    private val retrogradeDb: RetrogradeDatabase,
    private val applicationContext: Context,
    gameId: Int,
) : ViewModel() {
    class Factory(
        private val retrogradeDb: RetrogradeDatabase,
        private val applicationContext: Context,
        private val gameId: Int,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameDetailsViewModel(retrogradeDb, applicationContext, gameId) as T
        }
    }

    val game: StateFlow<Game?> =
        retrogradeDb
            .gameDao()
            .selectGame(gameId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val scraperService = ScreenScraperService()

    val isScraping = MutableStateFlow(false)

    val lastScrapeFailed = MutableStateFlow(false)

    fun onScrapeMetadata(game: Game) {
        if (isScraping.value) {
            return
        }
        viewModelScope.launch {
            isScraping.value = true
            lastScrapeFailed.value = false
            val credentials = scraperCredentials()
            val result = scraperService.scrapeGame(applicationContext, game, credentials)
            if (result != null) {
                retrogradeDb.gameDao().update(game.copyWithScrapeResult(result))
            } else {
                lastScrapeFailed.value = true
            }
            isScraping.value = false
        }
    }

    private fun scraperCredentials(): ScraperCredentials {
        val prefs = SharedPreferencesHelper.getSharedPreferences(applicationContext)
        return ScraperCredentials(
            devId = prefs.getString(applicationContext.getString(R.string.pref_key_scraper_devid), "") ?: "",
            devPassword = prefs.getString(applicationContext.getString(R.string.pref_key_scraper_devpassword), "") ?: "",
            username = prefs.getString(applicationContext.getString(R.string.pref_key_scraper_username), "") ?: "",
            password = prefs.getString(applicationContext.getString(R.string.pref_key_scraper_password), "") ?: "",
        )
    }

    fun coreConfigs(systemId: String) = GameSystem.findById(systemId).systemCoreConfigs

    fun systemName(systemId: String) = GameSystem.findById(systemId).titleResId

    fun onToggleFavorite(
        game: Game,
        isFavorite: Boolean,
    ) {
        viewModelScope.launch {
            retrogradeDb.gameDao().update(game.copy(isFavorite = isFavorite))
        }
    }

    fun onSetCoreOverride(
        game: Game,
        coreName: String?,
    ) {
        viewModelScope.launch {
            retrogradeDb.gameDao().update(game.copy(coreOverride = coreName))
        }
    }

    fun onHideGame(game: Game) {
        viewModelScope.launch {
            retrogradeDb.gameDao().update(game.copy(isHidden = true))
        }
    }

    fun onUnhideGame(game: Game) {
        viewModelScope.launch {
            retrogradeDb.gameDao().update(game.copy(isHidden = false))
        }
    }

    fun onDeleteGame(
        game: Game,
        onDeleted: () -> Unit,
    ) {
        viewModelScope.launch {
            if (deleteGameFile(applicationContext, game)) {
                retrogradeDb.gameDao().delete(listOf(game))
                onDeleted()
            }
        }
    }

    private fun deleteGameFile(
        context: Context,
        game: Game,
    ): Boolean {
        val uri = Uri.parse(game.fileUri)
        val deletedSingle =
            try {
                if (uri.scheme == "content") {
                    DocumentFile.fromSingleUri(context, uri)?.delete() ?: false
                } else if (uri.scheme == "file") {
                    File(uri.path ?: return false).delete()
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        if (deletedSingle) {
            return true
        }
        return try {
            DocumentFile.fromTreeUri(context, uri)?.delete() ?: false
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun formatPlaytime(
            context: Context,
            millis: Long,
        ): String {
            val totalMinutes = millis / 60_000
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 && minutes > 0 ->
                    context.getString(R.string.game_details_played_hours_minutes, hours, minutes)
                hours > 0 -> context.getString(R.string.game_details_played_hours, hours)
                else -> context.getString(R.string.game_details_played_minutes, minutes)
            }
        }
    }
}