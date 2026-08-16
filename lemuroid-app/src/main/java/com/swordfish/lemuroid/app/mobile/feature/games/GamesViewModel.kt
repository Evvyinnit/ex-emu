package com.swordfish.lemuroid.app.mobile.feature.games

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.swordfish.lemuroid.common.paging.buildFlowPaging
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

enum class GamesSortMode { TITLE, RECENT }

enum class GamesViewMode { LIST, GRID, CAROUSEL }

class GamesViewModel(
    private val retrogradeDb: RetrogradeDatabase,
    private val applicationContext: Context,
    initialMetaSystem: MetaSystemID,
) : ViewModel() {
    class Factory(
        private val retrogradeDb: RetrogradeDatabase,
        private val applicationContext: Context,
        private val initialMetaSystem: MetaSystemID,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GamesViewModel(retrogradeDb, applicationContext, initialMetaSystem) as T
        }
    }

    private val metaSystemId = MutableStateFlow(initialMetaSystem)

    val sortMode = MutableStateFlow(initialSortMode(initialMetaSystem))

    private val letterFilter = MutableStateFlow<String?>(null)

    val viewMode = MutableStateFlow(initialViewMode(initialMetaSystem))

    private val preferences
        get() = SharedPreferencesHelper.getSharedPreferences(applicationContext)

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: Flow<PagingData<Game>> =
        combine(metaSystemId, sortMode, letterFilter) { metaSystem, sort, letter ->
            Triple(metaSystem, sort, letter)
        }.flatMapLatest { (metaSystem, sort, letter) ->
            val systemIds = metaSystem.systemIDs.map { it.dbname }
            when {
                systemIds.isEmpty() -> emptyFlow()
                letter != null ->
                    buildFlowPaging(20, viewModelScope) {
                        retrogradeDb.gameDao().selectBySystemPrefix(systemIds.first(), "$letter%")
                    }
                sort == GamesSortMode.RECENT && systemIds.size == 1 ->
                    buildFlowPaging(20, viewModelScope) {
                        retrogradeDb.gameDao().selectBySystemRecent(systemIds.first())
                    }
                systemIds.size == 1 ->
                    buildFlowPaging(20, viewModelScope) {
                        retrogradeDb.gameDao().selectBySystem(systemIds.first())
                    }
                else ->
                    buildFlowPaging(20, viewModelScope) {
                        retrogradeDb.gameDao().selectBySystems(systemIds)
                    }
            }
        }

    fun setSortMode(mode: GamesSortMode) {
        sortMode.value = mode
        preferences.edit().putInt(sortPrefKey(metaSystemId.value), mode.ordinal).apply()
    }

    fun setLetterFilter(letter: String?) {
        letterFilter.value = letter
    }

    fun setViewMode(mode: GamesViewMode) {
        viewMode.value = mode
        preferences.edit().putInt(viewPrefKey(metaSystemId.value), mode.ordinal).apply()
    }

    suspend fun randomGames(limit: Int): List<Game> {
        val systemIds = metaSystemId.value.systemIDs.map { it.dbname }
        return if (systemIds.size == 1) {
            retrogradeDb.gameDao().selectRandomBySystem(systemIds.first(), limit)
        } else {
            retrogradeDb.gameDao().selectRandomBySystems(systemIds, limit)
        }
    }

    private fun initialSortMode(metaSystem: MetaSystemID): GamesSortMode {
        return GamesSortMode.entries
            .getOrElse(
                preferences.getInt(sortPrefKey(metaSystem), GamesSortMode.TITLE.ordinal),
            ) { GamesSortMode.TITLE }
    }

    private fun initialViewMode(metaSystem: MetaSystemID): GamesViewMode {
        return GamesViewMode.entries
            .getOrElse(
                preferences.getInt(viewPrefKey(metaSystem), GamesViewMode.LIST.ordinal),
            ) { GamesViewMode.LIST }
    }

    private fun sortPrefKey(metaSystem: MetaSystemID) = "games_sort_${metaSystem.name}"

    private fun viewPrefKey(metaSystem: MetaSystemID) = "games_view_${metaSystem.name}"
}