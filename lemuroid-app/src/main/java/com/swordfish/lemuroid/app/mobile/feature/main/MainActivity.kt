package com.swordfish.lemuroid.app.mobile.feature.main

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.favorites.FavoritesScreen
import com.swordfish.lemuroid.app.mobile.feature.favorites.FavoritesViewModel
import com.swordfish.lemuroid.app.mobile.feature.gamedetails.GameDetailsScreen
import com.swordfish.lemuroid.app.mobile.feature.gamedetails.GameDetailsViewModel
import com.swordfish.lemuroid.app.mobile.feature.games.GamesScreen
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewModel
import com.swordfish.lemuroid.app.mobile.feature.home.HomeScreen
import com.swordfish.lemuroid.app.mobile.feature.home.HomeViewModel
import com.swordfish.lemuroid.app.mobile.feature.search.SearchScreen
import com.swordfish.lemuroid.app.mobile.feature.search.SearchViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.advanced.AdvancedSettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.advanced.AdvancedSettingsViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.bios.BiosScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.bios.BiosSettingsViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.coreselection.CoresSelectionScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.coreselection.CoresSelectionViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.general.SettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.general.SettingsViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.hiddengames.HiddenGamesScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.hiddengames.HiddenGamesViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.inputdevices.InputDevicesSettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.inputdevices.InputDevicesSettingsViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.savesync.SaveSyncSettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.savesync.SaveSyncSettingsViewModel
import com.swordfish.lemuroid.app.mobile.feature.setup.SetupScreen
import com.swordfish.lemuroid.app.mobile.feature.shortcuts.ShortcutsGenerator
import com.swordfish.lemuroid.app.mobile.feature.systems.MetaSystemsScreen
import com.swordfish.lemuroid.app.mobile.feature.systems.MetaSystemsViewModel
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppThemePreferences
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.GameLauncher
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.main.BusyActivity
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.app.shared.settings.SettingsInteractor
import com.swordfish.lemuroid.app.shared.settings.StorageFrameworkPickerLauncher
import com.swordfish.lemuroid.common.coroutines.safeLaunch
import com.swordfish.lemuroid.ext.feature.review.ReviewManager
import com.swordfish.lemuroid.lib.android.RetrogradeComponentActivity
import com.swordfish.lemuroid.lib.bios.BiosManager
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.injection.PerActivity
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.savesync.SaveSyncManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import dagger.Provides
import de.charlex.compose.material3.HtmlText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject

@OptIn(DelicateCoroutinesApi::class)
class MainActivity : RetrogradeComponentActivity(), BusyActivity {
    @Inject
    lateinit var gameLaunchTaskHandler: GameLaunchTaskHandler

    @Inject
    lateinit var saveSyncManager: SaveSyncManager

    @Inject
    lateinit var retrogradeDb: RetrogradeDatabase

    @Inject
    lateinit var gameInteractor: GameInteractor

    @Inject
    lateinit var biosManager: BiosManager

    @Inject
    lateinit var coresSelection: CoresSelection

    @Inject
    lateinit var settingsInteractor: SettingsInteractor

    @Inject
    lateinit var inputDeviceManager: InputDeviceManager

    private val reviewManager = ReviewManager()

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(applicationContext, saveSyncManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        GlobalScope.safeLaunch {
            reviewManager.initialize(applicationContext)
        }

        setContent {
            val navController = rememberNavController()
            MainScreen(navController)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen(navController: NavHostController) {
        val context = LocalContext.current
        var darkTheme by remember { mutableStateOf(AppThemePreferences.isDarkTheme(context)) }
        DisposableEffect(context) {
            val prefs = SharedPreferencesHelper.getSharedPreferences(context)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == AppThemePreferences.THEME_MODE_PREF_KEY) {
                    darkTheme = AppThemePreferences.isDarkTheme(context)
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

        AppTheme(darkTheme = darkTheme) {
            val setupPrefs = SharedPreferencesHelper.getSharedPreferences(context)
            var setupComplete by
                remember {
                    mutableStateOf(setupPrefs.getBoolean(context.getString(R.string.pref_key_setup_complete), false))
                }

            if (!setupComplete) {
                SetupScreen(
                    onPickFolder = { StorageFrameworkPickerLauncher.pickFolder(context) },
                    onComplete = {
                        setupPrefs.edit().putBoolean(context.getString(R.string.pref_key_setup_complete), true).apply()
                        setupComplete = true
                    },
                )
                return@AppTheme
            }

            val navBackStackEntry = navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry.value?.destination
            val currentRoute =
                currentDestination?.route
                    ?.let { MainRoute.findByRoute(it) }
                    ?: MainRoute.HOME

            val infoDialogDisplayed =
                remember {
                    mutableStateOf(false)
                }

            LaunchedEffect(currentRoute) {
                mainViewModel.changeRoute(currentRoute)
            }

            val selectedGameState =
                remember {
                    mutableStateOf<Game?>(null)
                }

            val onGameLongClick = { game: Game ->
                selectedGameState.value = game
            }

            val onGameClick = { game: Game ->
                navController.navigate("game/${game.id}")
            }

            val onGameFavoriteToggle = { game: Game, isFavorite: Boolean ->
                gameInteractor.onFavoriteToggle(game, isFavorite)
            }

            val onHelpPressed = {
                infoDialogDisplayed.value = true
            }

            val mainUIState =
                mainViewModel.state
                    .collectAsState(MainViewModel.UiState())
                    .value

            Scaffold(
                topBar = {
                    MainTopBar(
                        currentRoute = currentRoute,
                        navController = navController,
                        onHelpPressed = onHelpPressed,
                        mainUIState = mainUIState,
                        onUpdateQueryString = { mainViewModel.changeQueryString(it) },
                    )
                },
                bottomBar = { MainNavigationBar(currentRoute, navController) },
            ) { padding ->
                NavHost(
                    modifier = Modifier.fillMaxSize(),
                    navController = navController,
                    startDestination = MainRoute.HOME.route,
                ) {
                    composable(MainRoute.HOME) {
                        HomeScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory =
                                        HomeViewModel.Factory(
                                            applicationContext,
                                            retrogradeDb,
                                            coresSelection,
                                        ),
                                ),
                            onGameClick = onGameClick,
                            onGameLongClick = onGameLongClick,
                            onOpenCoreSelection = { navController.navigateToRoute(MainRoute.SETTINGS_CORES_SELECTION) },
                        )
                    }
                    composable(MainRoute.FAVORITES) {
                        FavoritesScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory = FavoritesViewModel.Factory(retrogradeDb),
                                ),
                            onGameClick = onGameClick,
                            onGameLongClick = onGameLongClick,
                        )
                    }
                    composable(MainRoute.SEARCH) {
                        SearchScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory = SearchViewModel.Factory(retrogradeDb),
                                ),
                            searchQuery = mainUIState.searchQuery,
                            onGameClick = onGameClick,
                            onGameLongClick = onGameLongClick,
                            onGameFavoriteToggle = onGameFavoriteToggle,
                            onResetSearchQuery = { mainViewModel.changeQueryString("") },
                        )
                    }
                    composable(MainRoute.SYSTEMS) {
                        MetaSystemsScreen(
                            modifier = Modifier.padding(padding),
                            navController = navController,
                            viewModel =
                                viewModel(
                                    factory =
                                        MetaSystemsViewModel.Factory(
                                            retrogradeDb,
                                            applicationContext,
                                        ),
                                ),
                        )
                    }
                    composable(MainRoute.SYSTEM_GAMES) { entry ->
                        val metaSystemId = entry.arguments?.getString("metaSystemId")
                        GamesScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory =
                                        GamesViewModel.Factory(
                                            retrogradeDb,
                                            applicationContext,
                                            MetaSystemID.valueOf(metaSystemId!!),
                                        ),
                                ),
                            onGameClick = onGameClick,
                            onGameLongClick = onGameLongClick,
                            onGameFavoriteToggle = onGameFavoriteToggle,
                        )
                    }
                    composable(MainRoute.GAME_DETAILS) { entry ->
                        val gameId = entry.arguments?.getInt("gameId")!!
                        GameDetailsScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory =
                                        GameDetailsViewModel.Factory(
                                            retrogradeDb,
                                            applicationContext,
                                            gameId,
                                        ),
                                ),
                            onGamePlay = { gameInteractor.onGamePlay(it) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(MainRoute.SETTINGS) {
                        SettingsScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory =
                                        SettingsViewModel.Factory(
                                            applicationContext,
                                            settingsInteractor,
                                            saveSyncManager,
                                            FlowSharedPreferences(
                                                SharedPreferencesHelper.getLegacySharedPreferences(
                                                    applicationContext,
                                                ),
                                            ),
                                            retrogradeDb,
                                        ),
                                ),
                            navController = navController,
                        )
                    }
                    composable(MainRoute.SETTINGS_ADVANCED) {
                        AdvancedSettingsScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory =
                                        AdvancedSettingsViewModel.Factory(
                                            applicationContext,
                                            settingsInteractor,
                                        ),
                                ),
                            navController = navController,
                        )
                    }
                    composable(MainRoute.SETTINGS_BIOS) {
                        BiosScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory = BiosSettingsViewModel.Factory(biosManager),
                                ),
                        )
                    }
                    composable(MainRoute.SETTINGS_CORES_SELECTION) {
                        CoresSelectionScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory =
                                        CoresSelectionViewModel.Factory(
                                            applicationContext,
                                            coresSelection,
                                        ),
                                ),
                        )
                    }
                    composable(MainRoute.SETTINGS_INPUT_DEVICES) {
                        InputDevicesSettingsScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory =
                                        InputDevicesSettingsViewModel.Factory(
                                            applicationContext,
                                            inputDeviceManager,
                                        ),
                                ),
                        )
                    }
                    composable(MainRoute.SETTINGS_SAVE_SYNC) {
                        SaveSyncSettingsScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory =
                                        SaveSyncSettingsViewModel.Factory(
                                            application,
                                            saveSyncManager,
                                        ),
                                ),
                        )
                    }
                    composable(MainRoute.SETTINGS_HIDDEN_GAMES) {
                        HiddenGamesScreen(
                            modifier = Modifier.padding(padding),
                            viewModel =
                                viewModel(
                                    factory = HiddenGamesViewModel.Factory(retrogradeDb),
                                ),
                        )
                    }
                }
            }

            MainGameContextActions(
                selectedGameState = selectedGameState,
                shortcutSupported = gameInteractor.supportShortcuts(),
                onGamePlay = { gameInteractor.onGamePlay(it) },
                onGameRestart = { gameInteractor.onGameRestart(it) },
                onFavoriteToggle = { game: Game, isFavorite: Boolean ->
                    gameInteractor.onFavoriteToggle(game, isFavorite)
                },
                onCreateShortcut = { gameInteractor.onCreateShortcut(it) },
            )

            if (infoDialogDisplayed.value) {
                val message =
                    remember {
                        val systemFolders =
                            SystemID.values()
                                .joinToString(", ") { "<i>${it.dbname}</i>" }

                        getString(R.string.lemuroid_help_content)
                            .replace("\$SYSTEMS", systemFolders)
                    }

                AlertDialog(
                    text = { HtmlText(text = message) },
                    onDismissRequest = { infoDialogDisplayed.value = false },
                    confirmButton = { },
                )
            }
        }
    }

    override fun activity(): Activity = this

    override fun isBusy(): Boolean = mainViewModel.state.value.operationInProgress ?: false

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            BaseGameActivity.REQUEST_PLAY_GAME -> {
                GlobalScope.safeLaunch {
                    gameLaunchTaskHandler.handleGameFinish(
                        true,
                        this@MainActivity,
                        resultCode,
                        data,
                    )
                }
            }
        }
    }

    @dagger.Module
    abstract class Module {
        @dagger.Module
        companion object {
            @Provides
            @PerActivity
            @JvmStatic
            fun settingsInteractor(
                activity: MainActivity,
                directoriesManager: DirectoriesManager,
            ) = SettingsInteractor(activity, directoriesManager)

            @Provides
            @PerActivity
            @JvmStatic
            fun gameInteractor(
                activity: MainActivity,
                retrogradeDb: RetrogradeDatabase,
                shortcutsGenerator: ShortcutsGenerator,
                gameLauncher: GameLauncher,
            ) = GameInteractor(activity, retrogradeDb, false, shortcutsGenerator, gameLauncher)
        }
    }
}
