package com.swordfish.lemuroid.app.mobile.feature.gamedetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidCardShape
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameImage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun GameDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: GameDetailsViewModel,
    onGamePlay: (Game) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val game by viewModel.game.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentGame = game
    if (currentGame == null) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val systemName = stringResource(viewModel.systemName(currentGame.systemId))
    val coreConfigs = viewModel.coreConfigs(currentGame.systemId)
    val coreIndex =
        remember(currentGame.id, currentGame.coreOverride) {
            val overrideIndex =
                currentGame.coreOverride
                    ?.let { override ->
                        coreConfigs.indexOfFirst { it.coreID.coreName == override }
                    }
                    ?: -1
            if (overrideIndex >= 0) overrideIndex + 1 else 0
        }
    val coreState = remember(currentGame.id, coreIndex) { rememberMemoryIntSettingState(coreIndex) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        LemuroidGameImage(game = currentGame)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = currentGame.title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = systemName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val metadataParts =
                listOf(
                    currentGame.developer,
                    currentGame.year,
                    currentGame.genre,
                    currentGame.rating?.let { "$it/5" },
                ).filterNotNull()
            if (metadataParts.isNotEmpty()) {
                Text(
                    text = metadataParts.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (currentGame.timePlayed > 0) {
                Text(
                    text = GameDetailsViewModel.formatPlaytime(context, currentGame.timePlayed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(48.dp),
                    onClick = { onGamePlay(currentGame) },
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.game_details_play))
                }
                IconButton(
                    onClick = {
                        viewModel.onToggleFavorite(currentGame, !currentGame.isFavorite)
                    },
                ) {
                    Icon(
                        imageVector =
                            if (currentGame.isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                        contentDescription = stringResource(R.string.favorites),
                        tint =
                            if (currentGame.isFavorite) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
            if (!currentGame.description.isNullOrBlank()) {
                Text(
                    text = currentGame.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (!currentGame.screenshotUrl.isNullOrBlank()) {
                AsyncImage(
                    model = currentGame.screenshotUrl,
                    contentDescription = currentGame.title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(LemuroidCardShape),
                    contentScale = ContentScale.Crop,
                )
            }
            LemuroidCardSettingsGroup(
                title = {
                    Text(text = stringResource(R.string.game_details_core_override))
                },
            ) {
                LemuroidSettingsList(
                    state = coreState,
                    title = { Text(text = stringResource(R.string.game_details_core_override)) },
                    subtitle = {
                        Text(text = stringResource(R.string.game_details_core_override_subtitle))
                    },
                    items =
                        listOf(stringResource(R.string.game_details_core_system_default)) +
                            coreConfigs.map { it.coreID.coreDisplayName },
                    onItemSelected = { index, _ ->
                        if (index == 0) {
                            viewModel.onSetCoreOverride(currentGame, null)
                        } else {
                            viewModel.onSetCoreOverride(currentGame, coreConfigs[index - 1].coreID.coreName)
                        }
                    },
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.onHideGame(currentGame)
                    onBack()
                },
            ) {
                Text(text = stringResource(R.string.game_details_hide))
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDeleteConfirm = true },
            ) {
                Text(text = stringResource(R.string.game_details_delete))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = stringResource(R.string.game_details_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.game_details_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.onDeleteGame(currentGame, onBack)
                    },
                ) {
                    Text(text = stringResource(R.string.game_details_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}