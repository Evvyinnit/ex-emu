package com.swordfish.lemuroid.app.mobile.feature.settings.radashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.metadata.RaCredentials
import com.swordfish.lemuroid.app.shared.metadata.RaUserSummary
import com.swordfish.lemuroid.app.shared.metadata.RetroAchievementsService
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RADashboardViewModel(
    private val context: Context,
) : ViewModel() {
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RADashboardViewModel(context) as T
        }
    }

    sealed class State {
        data object Loading : State()

        data class Success(val summary: RaUserSummary) : State()

        data object Error : State()
    }

    private val service = RetroAchievementsService()

    val state: StateFlow<State> = MutableStateFlow(State.Loading)

    init {
        viewModelScope.launch {
            val credentials = raCredentials()
            val summary = service.fetchUserSummary(credentials)
            state.value =
                if (summary != null) {
                    State.Success(summary)
                } else {
                    State.Error
                }
        }
    }

    private fun raCredentials(): RaCredentials {
        val prefs = SharedPreferencesHelper.getSharedPreferences(context)
        return RaCredentials(
            username = prefs.getString(context.getString(R.string.pref_key_ra_username), "") ?: "",
            apiKey = prefs.getString(context.getString(R.string.pref_key_ra_api_key), "") ?: "",
        )
    }
}