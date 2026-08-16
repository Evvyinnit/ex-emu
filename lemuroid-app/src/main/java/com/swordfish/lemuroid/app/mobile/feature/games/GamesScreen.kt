package com.swordfish.lemuroid.app.mobile.feature.games

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidErrorView
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameListRow
import com.swordfish.lemuroid.lib.library.db.entity.Game

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

    Box(modifier = modifier.fillMaxSize()) {
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
            }
        }
    }
}
