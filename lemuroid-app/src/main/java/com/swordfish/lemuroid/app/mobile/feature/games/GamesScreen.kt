package com.swordfish.lemuroid.app.mobile.feature.games

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidErrorView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameImage
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameListRow
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.MaxContentWidth
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    modifier: Modifier = Modifier,
    viewModel: GamesViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
) {
    val games = viewModel.games.collectAsLazyPagingItems()
    val refreshState = games.loadState.refresh
    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    var letterFilter by remember { mutableStateOf<String?>(null) }
    var showRandomDialog by remember { mutableStateOf(false) }

    MaxContentWidth(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
        GamesHeaderRow(
            sortMode = sortMode,
            viewMode = viewMode,
            onSortSelected = viewModel::setSortMode,
            onViewModeSelected = viewModel::setViewMode,
            onRandomClicked = { showRandomDialog = true },
        )
        LetterFilterRow(
            selected = letterFilter,
            onLetterSelected = {
                letterFilter = it
                viewModel.setLetterFilter(it)
            },
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                refreshState is LoadState.Error && games.itemCount == 0 -> {
                    LemuroidErrorView(
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = { games.retry() },
                    )
                }
                refreshState is LoadState.Loading && games.itemCount == 0 -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                games.itemCount == 0 -> {
                    LemuroidEmptyView()
                }
                else -> {
                    when (viewMode) {
                        GamesViewMode.LIST -> GamesListView(games, onGameClick, onGameLongClick, onGameFavoriteToggle)
                        GamesViewMode.GRID -> GamesGridView(games, onGameClick, onGameLongClick)
                        GamesViewMode.CAROUSEL -> GamesCarouselView(games, onGameClick, onGameLongClick)
                    }
                }
            }
        }
        }
    }

    if (showRandomDialog) {
        RandomGameDialog(
            viewModel = viewModel,
            onPlay = {
                showRandomDialog = false
                onGameClick(it)
            },
            onDismiss = { showRandomDialog = false },
        )
    }
}

@Composable
private fun GamesHeaderRow(
    sortMode: GamesSortMode,
    viewMode: GamesViewMode,
    onSortSelected: (GamesSortMode) -> Unit,
    onViewModeSelected: (GamesViewMode) -> Unit,
    onRandomClicked: () -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            IconButton(onClick = { sortMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = stringResource(R.string.title_sort),
                )
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.games_sort_title)) },
                    onClick = {
                        sortMenuExpanded = false
                        onSortSelected(GamesSortMode.TITLE)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.games_sort_recent)) },
                    onClick = {
                        sortMenuExpanded = false
                        onSortSelected(GamesSortMode.RECENT)
                    },
                )
            }
        }
        Text(
            text =
                stringResource(
                    if (sortMode == GamesSortMode.RECENT) {
                        R.string.games_sort_recent
                    } else {
                        R.string.games_sort_title
                    },
                ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.weight(1f))
        IconButton(onClick = { onViewModeSelected(GamesViewMode.LIST) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = stringResource(R.string.games_view_list),
                tint =
                    if (viewMode == GamesViewMode.LIST) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        IconButton(onClick = { onViewModeSelected(GamesViewMode.GRID) }) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = stringResource(R.string.games_view_grid),
                tint =
                    if (viewMode == GamesViewMode.GRID) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        IconButton(onClick = { onViewModeSelected(GamesViewMode.CAROUSEL) }) {
            Icon(
                imageVector = Icons.Default.ViewCarousel,
                contentDescription = stringResource(R.string.games_view_carousel),
                tint =
                    if (viewMode == GamesViewMode.CAROUSEL) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        IconButton(onClick = onRandomClicked) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(R.string.game_details_random),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LetterFilterRow(
    selected: String?,
    onLetterSelected: (String?) -> Unit,
) {
    val letters = remember { ('A'..'Z').map { it.toString() } }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onLetterSelected(null) },
                label = { Text(stringResource(R.string.games_letter_all)) },
            )
        }
        items(letters, key = { it }) { letter ->
            FilterChip(
                selected = selected == letter,
                onClick = { onLetterSelected(if (selected == letter) null else letter) },
                label = { Text(letter) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GamesListView(
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(games.itemCount, key = { games[it]?.id ?: it }) { index ->
            val game = games[index] ?: return@items

            LemuroidGameListRow(
                modifier = Modifier.animateItem(),
                game = game,
                onClick = { onGameClick(game) },
                onLongClick = { onGameLongClick(game) },
                onFavoriteToggle = { isFavorite -> onGameFavoriteToggle(game, isFavorite) },
            )
        }
        gamesAppendState(games)
    }
}

@Composable
private fun GamesGridView(
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(games.itemCount, key = { games[it]?.id ?: it }) { index ->
            val game = games[index] ?: return@items

            LemuroidGameCard(
                game = game,
                onClick = { onGameClick(game) },
                onLongClick = { onGameLongClick(game) },
            )
        }
        gamesAppendState(games)
    }
}

@Composable
private fun GamesCarouselView(
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(games.itemCount, key = { games[it]?.id ?: it }) { index ->
            val game = games[index] ?: return@items

            LemuroidGameCard(
                modifier = Modifier.width(180.dp),
                game = game,
                onClick = { onGameClick(game) },
                onLongClick = { onGameLongClick(game) },
            )
        }
        gamesAppendState(games)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.gamesAppendState(games: LazyPagingItems<Game>) {
    when (val appendState = games.loadState.append) {
        is LoadState.Error -> {
            item(key = "append-error") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { games.retry() }) {
                        Text(stringResource(id = R.string.retry))
                    }
                }
            }
        }
        is LoadState.Loading -> {
            item(key = "append-loading") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        else -> Unit
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.gamesAppendState(games: LazyPagingItems<Game>) {
    when (val appendState = games.loadState.append) {
        is LoadState.Error -> {
            item(key = "append-error") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { games.retry() }) {
                        Text(stringResource(id = R.string.retry))
                    }
                }
            }
        }
        is LoadState.Loading -> {
            item(key = "append-loading") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun RandomGameDialog(
    viewModel: GamesViewModel,
    onPlay: (Game) -> Unit,
    onDismiss: () -> Unit,
) {
    var games by remember { mutableStateOf<List<Game>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        games = viewModel.randomGames(30)
    }

    LaunchedEffect(games) {
        if (games.isEmpty()) {
            return@LaunchedEffect
        }
        while (true) {
            delay(250)
            selectedIndex = (selectedIndex + 1) % games.size
        }
    }

    val selected = games.getOrNull(selectedIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.game_details_random)) },
        text = {
            if (selected == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    LemuroidGameImage(game = selected)
                    Text(
                        text = selected.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let(onPlay) },
            ) {
                Text(text = stringResource(R.string.game_details_play))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}