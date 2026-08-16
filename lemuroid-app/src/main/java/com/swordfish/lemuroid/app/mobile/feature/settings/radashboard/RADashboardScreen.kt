package com.swordfish.lemuroid.app.mobile.feature.settings.radashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.shared.metadata.RaRecentlyPlayed
import com.swordfish.lemuroid.app.shared.metadata.RaUserSummary

@Composable
fun RADashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: RADashboardViewModel,
) {
    val state by viewModel.state.collectAsState()

    when (val currentState = state) {
        is RADashboardViewModel.State.Loading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        is RADashboardViewModel.State.Error -> {
            LemuroidEmptyView(
                modifier = modifier,
                text = stringResource(R.string.ra_dashboard_error),
            )
        }
        is RADashboardViewModel.State.Success -> {
            RADashboardContent(modifier = modifier, summary = currentState.summary)
        }
    }
}

@Composable
private fun RADashboardContent(
    modifier: Modifier = Modifier,
    summary: RaUserSummary,
) {
    LemuroidSettingsPage(modifier = modifier) {
        LemuroidCardSettingsGroup(
            title = { Text(text = stringResource(id = R.string.settings_category_achievements)) },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = summary.username ?: "",
                    style = MaterialTheme.typography.titleLarge,
                )
                summary.memberSince?.let {
                    Text(
                        text = stringResource(R.string.ra_member_since, it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.ra_points, summary.totalPoints),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        if (summary.recentlyPlayed.isEmpty()) {
            Text(
                text = stringResource(R.string.game_details_achievements_none),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LemuroidCardSettingsGroup(
                title = { Text(text = stringResource(R.string.ra_recently_played)) },
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(summary.recentlyPlayed, key = { it.title }) { game ->
                        RecentlyPlayedRow(game = game)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedRow(game: RaRecentlyPlayed) {
    val progress =
        if (game.maxPossible > 0) {
            game.numAchieved.toFloat() / game.maxPossible.toFloat()
        } else {
            0f
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Text(
                    text = game.consoleName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${game.numAchieved}/${game.maxPossible}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}