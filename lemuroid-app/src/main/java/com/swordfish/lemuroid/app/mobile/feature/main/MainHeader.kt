package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidCardShape
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidListShape
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork
import java.util.Date
import kotlinx.coroutines.delay

/**
 * NeoStation-style top chrome: a centered translucent tab pill with a sliding primary-color
 * indicator, plus a glassy status pill showing the current time and battery level.
 */
private val TAB_DESTINATIONS = listOf(
    MainNavigationRoutes.HOME,
    MainNavigationRoutes.SYSTEMS,
    MainNavigationRoutes.FAVORITES,
    MainNavigationRoutes.SEARCH,
    MainNavigationRoutes.SETTINGS,
)

private val NeoTabSlotWidth = 42.dp
private val NeoTabSlotHeight = 34.dp

/** NeoStation battery/status palette. */
private fun neoBatteryColor(state: NeoBatteryState): Color =
    when {
        state.charging -> Color(0xFF00BAFE)
        state.percent >= 55 -> Color(0xFF00D390)
        state.percent >= 25 -> Color(0xFFFCB700)
        else -> Color(0xFFFF627D)
    }

private data class NeoBatteryState(val percent: Int = 100, val charging: Boolean = false)

@Composable
private fun rememberNeoClockState(): Pair<String, NeoBatteryState> {
    val context = LocalContext.current.applicationContext
    var timeText by remember { mutableStateOf("--:--") }
    var battery by remember { mutableStateOf(NeoBatteryState()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = DateFormat.getTimeFormat(context).format(Date())

            val intent =
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (level >= 0 && scale > 0) {
                    battery =
                        NeoBatteryState(
                            percent = level * 100 / scale,
                            charging =
                                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                    status == BatteryManager.BATTERY_STATUS_FULL,
                        )
                }
            }
            delay(30_000L)
        }
    }

    return timeText to battery
}

@Composable
fun MainHeader(
    currentRoute: MainRoute,
    navController: NavHostController,
    mainUIState: MainViewModel.UiState,
    onHelpPressed: () -> Unit,
    onUpdateQueryString: (String) -> Unit,
) {
    Column {
        AnimatedVisibility(visible = mainUIState.operationInProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val context = LocalContext.current
            val isSubRoute = currentRoute.parent != null || currentRoute == MainRoute.GAME_DETAILS

            AnimatedVisibility(visible = isSubRoute, enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(id = R.string.back),
                    )
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                when {
                    isSubRoute -> {
                        Text(
                            text = stringResource(currentRoute.titleId),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    currentRoute == MainRoute.SEARCH -> {
                        NeoSearchField(
                            mainUIState = mainUIState,
                            onUpdateQueryString = onUpdateQueryString,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        NeoTabPill(
                            destinations = TAB_DESTINATIONS,
                            selectedRoute = currentRoute.root,
                            onSelectDestination = { destination ->
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !currentRoute.showInTabBar, enter = fadeIn(), exit = fadeOut()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onHelpPressed) {
                        Icon(
                            Icons.Outlined.Info,
                            stringResource(R.string.mobile_settings_help),
                        )
                    }
                    if (mainUIState.saveSyncEnabled) {
                        IconButton(
                            onClick = {
                                SaveSyncWork.enqueueManualWork(context.applicationContext)
                            },
                            enabled = !mainUIState.operationInProgress,
                        ) {
                            Icon(
                                Icons.Outlined.CloudSync,
                                stringResource(R.string.save_sync),
                            )
                        }
                    }
                }
            }

            NeoStatusPill()
        }
    }
}

/** Translucent pill container used across the NeoStation chrome. */
@Composable
private fun NeoChromeSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LemuroidCardShape,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        content = content,
    )
}

@Composable
private fun NeoStatusPill() {
    val (timeText, battery) = rememberNeoClockState()

    NeoChromeSurface {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "${battery.percent}%",
                style = MaterialTheme.typography.labelMedium,
                color = neoBatteryColor(battery),
            )
        }
    }
}

@Composable
private fun NeoTabPill(
    destinations: List<MainNavigationRoutes>,
    selectedRoute: MainRoute?,
    onSelectDestination: (MainRoute) -> Unit,
) {
    NeoChromeSurface {
        val selectedIndex =
            destinations
                .indexOfFirst { it.route == selectedRoute }
                .coerceAtLeast(0)

        val indicatorOffset by animateDpAsState(
            targetValue = NeoTabSlotWidth * selectedIndex,
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            label = "neoTabIndicator",
        )

        Box(modifier = Modifier.padding(3.dp)) {
            Box(
                modifier =
                    Modifier
                        .offset(x = indicatorOffset)
                        .size(NeoTabSlotWidth, NeoTabSlotHeight)
                        .clip(LemuroidListShape)
                        .background(MaterialTheme.colorScheme.primary),
            )
            Row {
                destinations.forEach { destination ->
                    val isSelected = destination.route == selectedRoute
                    Box(
                        modifier =
                            Modifier
                                .size(NeoTabSlotWidth, NeoTabSlotHeight)
                                .clip(LemuroidListShape)
                                .clickable { onSelectDestination(destination.route) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector =
                                if (isSelected) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                            contentDescription = stringResource(destination.titleId),
                            tint =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeoSearchField(
    mainUIState: MainViewModel.UiState,
    onUpdateQueryString: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = modifier.height(48.dp)) {
        NeoChromeSurface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(100),
        ) {}

        TextField(
            value = mainUIState.searchQuery,
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text(stringResource(R.string.title_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            onValueChange = { onUpdateQueryString(it) },
            singleLine = true,
            keyboardActions =
                KeyboardActions(
                    onDone = { focusManager.clearFocus(true) },
                ),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )
    }
}
