package com.swordfish.lemuroid.app.shared.settings

import android.content.Context
import android.content.SharedPreferences
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Smart performance presets.
 *
 * Walks every system's exposed core options and rewrites the matching per-system
 * SharedPreferences entries (`cv_<system>_<option>`, same storage used by the
 * in-game Core Options menu) so a single tap tunes the whole library.
 *
 * Only options whose allowed values are known are ever touched:
 *  - explicit candidate lists declared in [GameSystem] registry,
 *  - well-known boolean conventions (enabled/disabled),
 *  - numeric frameskips adjusted within a safe range.
 */
object PerformanceProfiles {
    enum class Profile(val id: String) {
        PERFORMANCE("performance"),
        BALANCED("balanced"),
        QUALITY("quality"),
        BATTERY_SAVER("battery_saver"),
        ;

        companion object {
            fun fromId(id: String?): Profile? = entries.firstOrNull { it.id == id }
        }
    }

    const val PREF_KEY_ACTIVE_PROFILE = "performance_profile"

    fun getActiveProfile(prefs: SharedPreferences): Profile? =
        Profile.fromId(prefs.getString(PREF_KEY_ACTIVE_PROFILE, null))

    /** Applies [profile] to every system. Returns how many options were changed. */
    suspend fun applyProfile(
        context: Context,
        profile: Profile,
    ): Int =
        withContext(Dispatchers.IO) {
            val prefs = SharedPreferencesHelper.getSharedPreferences(context)
            val editor = prefs.edit()
            var changed = 0

            GameSystem.all().forEach { system ->
                val systemDbName = system.id.dbname
                system.systemCoreConfigs.forEach { config ->
                    val defaults = config.defaultSettings.associate { it.key to it.value }
                    (config.exposedSettings + config.exposedAdvancedSettings).forEach { exposed ->
                        val prefKey =
                            CoreVariablesManager.computeSharedPreferenceKey(exposed.key, systemDbName)
                        val current = prefs.getString(prefKey, null) ?: defaults[exposed.key]
                        val candidates =
                            if (exposed.values.isEmpty()) null else exposed.values.map { it.key }
                        val target =
                            chooseTarget(
                                profile,
                                exposed.key.lowercase(Locale.US),
                                candidates,
                                current,
                            )
                        if (target != null && target != current) {
                            editor.putString(prefKey, target)
                            changed++
                        }
                    }
                }
            }

            editor.putString(PREF_KEY_ACTIVE_PROFILE, profile.id)
            editor.apply()
            changed
        }

    /**
     * Returns the value [profile] wants for option [key], or null to leave it untouched.
     * [candidates] holds the explicitly declared allowed values, or null when they are
     * only known at core runtime.
     */
    private fun chooseTarget(
        profile: Profile,
        key: String,
        candidates: List<String>?,
        current: String?,
    ): String? {
        if (current == null) return null

        // Frameskip: quality keeps every frame, speed profiles skip more.
        if ("frameskip" in key || "frame_skip" in key) {
            return when {
                candidates != null -> pickFrameskip(profile, candidates)
                profile == Profile.QUALITY -> "0"
                profile == Profile.PERFORMANCE || profile == Profile.BATTERY_SAVER ->
                    bumpNumeric(current, 1, 0, 3)
                else -> null
            }
        }

        // Internal resolution / screen size: smaller renders faster.
        if ("resolution" in key || "screensize" in key || "screen_size" in key) {
            return pickByNumber(profile, candidates)
        }

        // Texture scaling: heaviest visual cost on 3D cores.
        if ("texture_scaling" in key) {
            return pickByNumber(profile, candidates)
        }

        // CPU execution mode: always prefer the fastest available one.
        if ("cpu_core" in key || "cpucore" in key) {
            val pool = candidates ?: return null
            val preferred =
                pool.firstOrNull { it.equals("dynamic_recompiler", true) }
                    ?: pool.firstOrNull { it.equals("JIT", true) }
                    ?: pool.firstOrNull { !it.contains("interpreter", true) }
                    ?: pool.first()
            return preferred.takeIf { it != current }
        }

        // Dynamic recompiler for PSX: big win, keep off only when untouched.
        if (key == "pcsx_rearmed_drc") {
            if (profile == Profile.BATTERY_SAVER) return null
            return explicitTarget("enabled", current, candidates)
        }

        // Sprite limit removal is an accuracy fix: quality enables it, speed disables it.
        if ("sprite_limit" in key || "nospritelimit" in key) {
            return when (profile) {
                Profile.QUALITY -> explicitTarget("enabled", current, candidates)
                Profile.PERFORMANCE, Profile.BATTERY_SAVER -> explicitTarget("disabled", current, candidates)
                else -> null
            }
        }

        // Cosmetic video filters and frame blending cost time with no gameplay value.
        val cosmeticFilter =
            (("filter" in key && "dark_filter" !in key) ||
                "mix_frames" in key ||
                "interframe_blending" in key)
        if (cosmeticFilter && (profile == Profile.PERFORMANCE || profile == Profile.BATTERY_SAVER)) {
            return disableTarget(current, candidates)
        }

        return null
    }

    private fun pickFrameskip(
        profile: Profile,
        candidates: List<String>,
    ): String? =
        when {
            profile == Profile.QUALITY ->
                candidates.firstOrNull { it.equals("disabled", true) || it == "OFF" || it == "0" }
                    ?: lowestNumeric(candidates)
            else ->
                candidates.firstOrNull { it.equals("auto", true) } ?: highestNumeric(candidates)
        }?.takeIf { it.isNotEmpty() }

    private fun pickByNumber(
        profile: Profile,
        candidates: List<String>?,
    ): String? {
        val numbers = candidates?.mapNotNull { it.extractNumber() } ?: return null
        if (numbers.size < 2) return null
        val sorted = candidates!!.sortedBy { it.extractNumber() ?: Int.MAX_VALUE }
        return when (profile) {
            Profile.PERFORMANCE, Profile.BATTERY_SAVER -> sorted.first()
            Profile.BALANCED -> sorted[sorted.size / 2]
            Profile.QUALITY -> sorted.last()
        }
    }

    private fun explicitTarget(
        target: String,
        current: String,
        candidates: List<String>?,
    ): String? {
        if (candidates != null && target !in candidates) return null
        return target.takeIf { it != current }
    }

    private fun disableTarget(
        current: String,
        candidates: List<String>?,
    ): String? {
        val target =
            listOf("disabled", "OFF", "off", "0").firstOrNull { candidate ->
                candidates == null || candidate in candidates
            } ?: "disabled"
        return target.takeIf { it != current }
    }

    private fun lowestNumeric(candidates: List<String>): String? =
        candidates.sortedBy { it.extractNumber() ?: Int.MAX_VALUE }.firstOrNull()

    private fun highestNumeric(candidates: List<String>): String? =
        candidates.sortedBy { it.extractNumber() ?: Int.MIN_VALUE }.lastOrNull()

    private fun bumpNumeric(
        value: String,
        delta: Int,
        min: Int,
        max: Int,
    ): String? =
        value.toIntOrNull()?.let { (it + delta).coerceIn(min, max).toString() }

    private fun String.extractNumber(): Int? = Regex("\\d+").find(this)?.value?.toIntOrNull()
}
