package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidCardShape
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameImage
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidListShape
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidSystemImage
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.MaxContentWidth
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onSystemClick: (MetaSystemInfo) -> Unit,
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.updatePermissions(applicationContext)
            }
            else -> { }
        }
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                context.displayDetailsSettingsScreen()
            }
        }

    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState())
    val metaSystems = viewModel.metaSystems.collectAsState(emptyList())

    HomeScreen(
        modifier,
        state.value,
        metaSystems.value,
        onGameClick,
        onGameLongClick,
        onOpenCoreSelection,
        onSystemClick,
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@HomeScreen
            }

            permissionsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        { permissionsLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        { viewModel.changeLocalStorageFolder(context) },
        { context.startActivity(Intent(context, ru.playsoftware.j2meloader.MainActivity::class.java)) },
    )
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeViewModel.UIState,
    metaSystems: List<MetaSystemInfo>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onSystemClick: (MetaSystemInfo) -> Unit,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
    onOpenJavaGamesClicked: () -> Unit,
) {
    MaxContentWidth(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(id = R.string.lemuroid_name),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            AnimatedVisibility(state.showNoNotificationPermissionCard) {
                HomeNotification(
                    titleId = R.string.home_notification_title,
                    messageId = R.string.home_notification_message,
                    actionId = R.string.home_notification_action,
                    onAction = onEnableNotificationsClicked,
                )
            }
            AnimatedVisibility(state.showNoGamesCard) {
                HomeNotification(
                    titleId = R.string.home_empty_title,
                    messageId = R.string.home_empty_message,
                    actionId = R.string.home_empty_action,
                    onAction = onSetDirectoryClicked,
                    enabled = !state.indexInProgress,
                )
            }
            AnimatedVisibility(state.showNoMicrophonePermissionCard) {
                HomeNotification(
                    titleId = R.string.home_microphone_title,
                    messageId = R.string.home_microphone_message,
                    actionId = R.string.home_microphone_action,
                    onAction = onEnableMicrophoneClicked,
                )
            }
            AnimatedVisibility(state.showDesmumeDeprecatedCard) {
                HomeNotification(
                    titleId = R.string.home_notification_desmume_deprecated_title,
                    messageId = R.string.home_notification_desmume_deprecated_message,
                    actionId = R.string.home_notification_desmume_deprecated_action,
                    onAction = onOpenCoreSelection,
                )
            }
            if (state.recentGames.isNotEmpty()) {
                ContinuePlayingSection(
                    games = state.recentGames,
                    onGameClicked = onGameClicked,
                    onGameLongClick = onGameLongClick,
                )
            }
            if (metaSystems.isNotEmpty()) {
                HomeSystemsRow(metaSystems = metaSystems, onSystemClick = onSystemClick)
            }
            HomeRow(
                title = stringResource(id = R.string.favorites),
                games = state.favoritesGames,
                onGameClicked = onGameClicked,
                onGameLongClick = onGameLongClick,
            )
            HomeRow(
                title = stringResource(id = R.string.home_recently_added),
                games = state.discoveryGames,
                onGameClicked = onGameClicked,
                onGameLongClick = onGameLongClick,
            )
            HomeJavaGamesCard(onOpenJavaGamesClicked)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinuePlayingSection(
    games: List<Game>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.home_continue_playing).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        val hero = games.first()
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(2.dp, LemuroidCardShape),
            onClick = { onGameClicked(hero) },
            shape = LemuroidCardShape,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(112.dp)
                            .clip(LemuroidListShape),
                ) {
                    LemuroidGameImage(game = hero)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = hero.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val systemName =
                        remember(hero.systemId) {
                            runCatching { context.getString(GameSystem.findById(hero.systemId).titleResId) }
                                .getOrNull()
                                ?: hero.developer?.takeIf { it.isNotBlank() }
                                ?: ""
                        }
                    if (systemName.isNotEmpty()) {
                        Text(
                            text = systemName.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = heroMetaLine(context, hero),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { onGameClicked(hero) },
                        shape = RoundedCornerShape(10.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2ECC71),
                                contentColor = Color(0xFF06240F),
                            ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.game_details_play).uppercase(),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (games.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(games.size - 1, key = { games[it + 1].id }) { index ->
                    val game = games[index + 1]
                    LemuroidGameCard(
                        modifier =
                            Modifier
                                .widthIn(0.dp, 144.dp)
                                .animateItem(),
                        game = game,
                        onClick = { onGameClicked(game) },
                        onLongClick = { onGameLongClick(game) },
                    )
                }
            }
        }
    }
}

private fun heroMetaLine(
    context: android.content.Context,
    game: Game,
): String {
    val parts = mutableListOf<String>()

    game.lastPlayedAt?.let {
        parts.add(
            context.getString(R.string.home_last_played) + " " +
                java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault())
                    .format(Date(it)),
        )
    }

    if (game.timePlayed > 0) {
        val minutes = game.timePlayed / 60
        val hours = minutes / 60
        parts.add(
            if (hours > 0) {
                "${hours}h ${minutes % 60}m"
            } else {
                "${minutes}m"
            },
        )
    }

    return parts.joinToString("  •  ")
}

@Composable
private fun HomeSystemsRow(
    metaSystems: List<MetaSystemInfo>,
    onSystemClick: (MetaSystemInfo) -> Unit,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.title_systems).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(metaSystems.size, key = { metaSystems[it].metaSystem }) { index ->
                val system = metaSystems[index]
                Card(
                    modifier =
                        Modifier
                            .width(120.dp)
                            .shadow(2.dp, LemuroidCardShape),
                    onClick = { onSystemClick(system) },
                    shape = LemuroidCardShape,
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LemuroidSystemImage(system)
                        Text(
                            text = system.getName(context),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp),
                        )
                        Text(
                            text = stringResource(R.string.system_grid_details, system.count).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRow(
    title: String,
    games: List<Game>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    if (games.isEmpty()) {
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        LazyRow(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(games.size, key = { games[it].id }) { index ->
                val game = games[index]
                LemuroidGameCard(
                    modifier =
                        Modifier
                            .widthIn(0.dp, 144.dp)
                            .animateItem(),
                    game = game,
                    onClick = { onGameClicked(game) },
                    onLongClick = { onGameLongClick(game) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun HomeNotification(
    titleId: Int,
    messageId: Int,
    actionId: Int,
    enabled: Boolean = true,
    onAction: () -> Unit = { },
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        shape = LemuroidCardShape,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(titleId),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(messageId),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onAction,
                enabled = enabled,
            ) {
                Text(stringResource(id = actionId))
            }
        }
    }
}

@Composable
private fun HomeJavaGamesCard(
    onOpenJavaGamesClicked: () -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        shape = LemuroidCardShape,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.java_games_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.java_games_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onOpenJavaGamesClicked,
            ) {
                Text(stringResource(id = R.string.java_games_open_action))
            }
        }
    }
}
