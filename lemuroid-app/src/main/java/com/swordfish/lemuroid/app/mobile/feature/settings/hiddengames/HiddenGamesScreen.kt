package com.swordfish.lemuroid.app.mobile.feature.settings.hiddengames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun HiddenGamesScreen(
    modifier: Modifier = Modifier,
    viewModel: HiddenGamesViewModel,
) {
    val hiddenGames by viewModel.hiddenGames.collectAsState()

    if (hiddenGames.isEmpty()) {
        LemuroidEmptyView(
            modifier = modifier,
            text = stringResource(R.string.game_details_hidden_empty),
        )
        return
    }

    LemuroidSettingsPage(modifier = modifier) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(hiddenGames, key = { it.id }) { game ->
                HiddenGameRow(
                    game = game,
                    onUnhide = { viewModel.onUnhideGame(game) },
                )
            }
        }
    }
}

@Composable
private fun HiddenGameRow(
    game: Game,
    onUnhide: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = game.fileName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        TextButton(onClick = onUnhide) {
            Text(text = stringResource(R.string.game_details_unhide))
        }
    }
}