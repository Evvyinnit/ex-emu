package com.swordfish.lemuroid.app.mobile.feature.settings.hiddengames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HiddenGamesViewModel(
    private val retrogradeDb: RetrogradeDatabase,
) : ViewModel() {
    class Factory(private val retrogradeDb: RetrogradeDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HiddenGamesViewModel(retrogradeDb) as T
        }
    }

    val hiddenGames: StateFlow<List<Game>> =
        retrogradeDb
            .gameDao()
            .selectHidden()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onUnhideGame(game: Game) {
        viewModelScope.launch {
            retrogradeDb.gameDao().update(game.copy(isHidden = false))
        }
    }

    fun onUnhideAll() {
        viewModelScope.launch {
            val current = hiddenGames.value
            if (current.isNotEmpty()) {
                retrogradeDb.gameDao().update(current.map { it.copy(isHidden = false) })
            }
        }
    }
}