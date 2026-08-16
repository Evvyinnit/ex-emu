package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import com.swordfish.lemuroid.app.shared.metadata.ScreenScraperService
import com.swordfish.lemuroid.app.shared.metadata.ScraperCredentials
import com.swordfish.lemuroid.app.shared.metadata.copyWithScrapeResult
import com.swordfish.lemuroid.app.shared.settings.SettingsInteractor
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.savesync.SaveSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val settingsInteractor: SettingsInteractor,
    saveSyncManager: SaveSyncManager,
    sharedPreferences: FlowSharedPreferences,
    private val retrogradeDb: RetrogradeDatabase,
) : ViewModel() {
    class Factory(
        private val context: Context,
        private val settingsInteractor: SettingsInteractor,
        private val saveSyncManager: SaveSyncManager,
        private val sharedPreferences: FlowSharedPreferences,
        private val retrogradeDb: RetrogradeDatabase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                context,
                settingsInteractor,
                saveSyncManager,
                sharedPreferences,
                retrogradeDb,
            ) as T
        }
    }

    sealed class ScrapeState {
        data object Idle : ScrapeState()

        data class Running(val scraped: Int, val total: Int) : ScrapeState()

        data class Done(val scraped: Int) : ScrapeState()

        data object Error : ScrapeState()
    }

    private val scraperService = ScreenScraperService()

    val scrapeState = MutableStateFlow<ScrapeState>(ScrapeState.Idle)

    fun onScrapeAll() {
        if (scrapeState.value is ScrapeState.Running) {
            return
        }
        viewModelScope.launch {
            val credentials = scraperCredentials()
            if (!credentials.isConfigured) {
                scrapeState.value = ScrapeState.Error
                return@launch
            }
            val dao = retrogradeDb.gameDao()
            val missing = dao.selectGamesMissingMetadata()
            if (missing.isEmpty()) {
                scrapeState.value = ScrapeState.Done(0)
                return@launch
            }
            scrapeState.value = ScrapeState.Running(0, missing.size)
            var scraped = 0
            missing.forEach { game ->
                val result = scraperService.scrapeGame(context, game, credentials)
                if (result != null) {
                    dao.update(game.copyWithScrapeResult(result))
                    scraped++
                }
                scrapeState.value = ScrapeState.Running(scraped, missing.size)
                delay(300)
            }
            scrapeState.value = ScrapeState.Done(scraped)
        }
    }

    private fun scraperCredentials(): ScraperCredentials {
        val prefs = SharedPreferencesHelper.getSharedPreferences(context)
        return ScraperCredentials(
            devId = prefs.getString(context.getString(R.string.pref_key_scraper_devid), "") ?: "",
            devPassword = prefs.getString(context.getString(R.string.pref_key_scraper_devpassword), "") ?: "",
            username = prefs.getString(context.getString(R.string.pref_key_scraper_username), "") ?: "",
            password = prefs.getString(context.getString(R.string.pref_key_scraper_password), "") ?: "",
        )
    }

    data class State(
        val currentDirectory: String = "",
        val isSaveSyncSupported: Boolean = false,
    )

    val indexingInProgress = PendingOperationsMonitor(context).anyLibraryOperationInProgress()

    val directoryScanInProgress = PendingOperationsMonitor(context).isDirectoryScanInProgress()

    val uiState =
        sharedPreferences.getString(context.getString(com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder))
            .asFlow()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Lazily, "")
            .map { State(it, saveSyncManager.isSupported()) }

    fun changeLocalStorageFolder() {
        settingsInteractor.changeLocalStorageFolder()
    }
}
