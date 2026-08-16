package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.app.mobile.feature.main.navigateToRoute
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSlider
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.intPreferenceState
import com.swordfish.lemuroid.app.utils.android.stringListResource
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    navController: NavController,
) {
    val state =
        viewModel.uiState
            .collectAsState(SettingsViewModel.State())
            .value

    val scanInProgress =
        viewModel.directoryScanInProgress
            .collectAsState(false)
            .value

    val indexingInProgress =
        viewModel.indexingInProgress
            .collectAsState(false)
            .value

    LemuroidSettingsPage(modifier = modifier) {
        RomsSettings(
            state = state,
            onChangeFolder = { viewModel.changeLocalStorageFolder() },
            indexingInProgress = indexingInProgress,
            scanInProgress = scanInProgress,
        )
        GeneralSettings()
        AppearanceSettings()
        MetadataSettings(viewModel = viewModel)
        InputSettings(navController = navController)
        MiscSettings(
            indexingInProgress = indexingInProgress,
            isSaveSyncSupported = state.isSaveSyncSupported,
            navController = navController,
        )
    }
}

@Composable
private fun MiscSettings(
    indexingInProgress: Boolean,
    isSaveSyncSupported: Boolean,
    navController: NavController,
) {
    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_misc)) },
    ) {
        if (isSaveSyncSupported) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.settings_title_save_sync)) },
                subtitle = {
                    Text(text = stringResource(id = R.string.settings_description_save_sync))
                },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_SAVE_SYNC) },
            )
        }
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_open_cores_selection)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_open_cores_selection))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_CORES_SELECTION) },
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_hidden_games)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_hidden_games))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_HIDDEN_GAMES) },
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_display_bios_info)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_display_bios_info))
            },
            enabled = !indexingInProgress,
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_BIOS) },
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_advanced_settings)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_advanced_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_ADVANCED) },
        )
    }
}

@Composable
private fun MetadataSettings(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val prefs = remember {
        SharedPreferencesHelper.getSharedPreferences(context)
    }
    val devIdKey = stringResource(R.string.pref_key_scraper_devid)
    val devPasswordKey = stringResource(R.string.pref_key_scraper_devpassword)
    val usernameKey = stringResource(R.string.pref_key_scraper_username)
    val passwordKey = stringResource(R.string.pref_key_scraper_password)

    var devId by remember { mutableStateOf(prefs.getString(devIdKey, "") ?: "") }
    var devPassword by remember { mutableStateOf(prefs.getString(devPasswordKey, "") ?: "") }
    var username by remember { mutableStateOf(prefs.getString(usernameKey, "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString(passwordKey, "") ?: "") }

    val scrapeState by viewModel.scrapeState.collectAsState()

    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_metadata)) },
    ) {
        MetadataTextField(
            label = stringResource(R.string.settings_title_scraper_devid),
            value = devId,
            onValueChange = {
                devId = it
                prefs.edit().putString(devIdKey, it).apply()
            },
        )
        MetadataTextField(
            label = stringResource(R.string.settings_title_scraper_devpassword),
            value = devPassword,
            isPassword = true,
            onValueChange = {
                devPassword = it
                prefs.edit().putString(devPasswordKey, it).apply()
            },
        )
        MetadataTextField(
            label = stringResource(R.string.settings_title_scraper_username),
            value = username,
            onValueChange = {
                username = it
                prefs.edit().putString(usernameKey, it).apply()
            },
        )
        MetadataTextField(
            label = stringResource(R.string.settings_title_scraper_password),
            value = password,
            isPassword = true,
            onValueChange = {
                password = it
                prefs.edit().putString(passwordKey, it).apply()
            },
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_scrape_all)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_scrape_all))
            },
            enabled = scrapeState !is SettingsViewModel.ScrapeState.Running,
            onClick = { viewModel.onScrapeAll() },
        )
        when (val scrapeStateValue = scrapeState) {
            is SettingsViewModel.ScrapeState.Running -> {
                Text(
                    text =
                        stringResource(
                            R.string.settings_scrape_progress,
                            scrapeStateValue.scraped,
                            scrapeStateValue.total,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is SettingsViewModel.ScrapeState.Done -> {
                Text(
                    text = stringResource(R.string.settings_scrape_done, scrapeStateValue.scraped),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            SettingsViewModel.ScrapeState.Error -> {
                Text(
                    text = stringResource(R.string.settings_scrape_error),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            SettingsViewModel.ScrapeState.Idle -> Unit
        }
    }
}

@Composable
private fun MetadataTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        visualTransformation =
            if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun InputSettings(navController: NavController) {
    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_input)) },
    ) {
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    R.string.pref_key_haptic_feedback_mode,
                    "press",
                    stringListResource(R.array.pref_key_haptic_feedback_mode_values),
                ),
            title = {
                Text(text = stringResource(id = R.string.settings_title_enable_touch_feedback))
            },
            items = stringListResource(R.array.pref_key_haptic_feedback_mode_display_names),
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_gamepad_settings)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_gamepad_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_INPUT_DEVICES) },
        )
    }
}

@Composable
private fun AppearanceSettings() {
    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_appearance)) },
    ) {
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    R.string.pref_key_theme_mode,
                    "system",
                    stringListResource(R.array.pref_key_theme_mode_values).toList(),
                ),
            title = { Text(text = stringResource(id = R.string.settings_title_theme_mode)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_theme_mode)) },
            items = stringListResource(R.array.pref_key_theme_mode_display_names),
        )
    }
}

@Composable
private fun GeneralSettings() {
    val hdMode = booleanPreferenceState(R.string.pref_key_hd_mode, false)
    val immersiveMode = booleanPreferenceState(R.string.pref_key_enable_immersive_mode, false)

    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_general)) },
    ) {
        LemuroidSettingsSwitch(
            state = booleanPreferenceState(R.string.pref_key_autosave, true),
            title = { Text(text = stringResource(id = R.string.settings_title_enable_autosave)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_enable_autosave)) },
        )
        LemuroidSettingsSwitch(
            state = immersiveMode,
            title = { Text(text = stringResource(id = R.string.settings_title_immersive_mode)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_immersive_mode)) },
        )
        LemuroidSettingsSwitch(
            state = hdMode,
            title = { Text(text = stringResource(id = R.string.settings_title_hd_mode)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_hd_mode)) },
        )
        LemuroidSettingsSlider(
            enabled = hdMode.value,
            state =
                intPreferenceState(
                    key = stringResource(id = R.string.pref_key_hd_mode_quality),
                    default = 2,
                ),
            steps = 1,
            valueRange = 0f..2f,
            title = { Text(text = stringResource(R.string.settings_title_hd_quality)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_hd_quality)) },
        )
        LemuroidSettingsList(
            enabled = !hdMode.value,
            state =
                indexPreferenceState(
                    R.string.pref_key_shader_filter,
                    "auto",
                    stringListResource(R.array.pref_key_shader_filter_values).toList(),
                ),
            title = { Text(text = stringResource(id = R.string.display_filter)) },
            items = stringListResource(R.array.pref_key_shader_filter_display_names),
        )
    }
}

@Composable
private fun RomsSettings(
    state: SettingsViewModel.State,
    onChangeFolder: () -> Unit,
    indexingInProgress: Boolean,
    scanInProgress: Boolean,
) {
    val context = LocalContext.current

    val currentDirectory = state.currentDirectory
    val emptyDirectory = stringResource(R.string.none)

    val currentDirectoryName =
        remember(state.currentDirectory) {
            runCatching {
                DocumentFile.fromTreeUri(context, Uri.parse(currentDirectory))?.name
            }.getOrNull() ?: emptyDirectory
        }

    LemuroidCardSettingsGroup(title = { Text(text = stringResource(id = R.string.roms)) }) {
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.directory)) },
            subtitle = { Text(text = currentDirectoryName) },
            onClick = { onChangeFolder() },
            enabled = !indexingInProgress,
        )
        if (scanInProgress) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.stop)) },
                onClick = { LibraryIndexScheduler.cancelLibrarySync(context) },
            )
        } else {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.rescan)) },
                onClick = { LibraryIndexScheduler.scheduleLibrarySync(context) },
                enabled = !indexingInProgress,
            )
        }
    }
}
