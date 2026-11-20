// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import helium314.keyboard.latin.utils.JniUtils
import helium314.keyboard.settings.screens.createAboutSettings
import helium314.keyboard.settings.screens.createAdvancedSettings
import helium314.keyboard.settings.screens.createAppearanceSettings
import helium314.keyboard.settings.screens.createCloudSettings
import helium314.keyboard.settings.screens.createCorrectionSettings
import helium314.keyboard.settings.screens.createGestureTypingSettings
import helium314.keyboard.settings.screens.createLayoutSettings
import helium314.keyboard.settings.screens.createPreferencesSettings
import helium314.keyboard.settings.screens.createToolbarSettings
import helium314.keyboard.settings.preferences.Preference

class SettingsContainer(context: Context) {
    private val sections = listOf(
        SettingsDestination.About to createAboutSettings(context),
        SettingsDestination.Appearance to createAppearanceSettings(context),
        SettingsDestination.TextCorrection to createCorrectionSettings(context),
        SettingsDestination.Keys to createPreferencesSettings(context),
        SettingsDestination.Toolbar to createToolbarSettings(context),
        SettingsDestination.Layouts to createLayoutSettings(context),
        SettingsDestination.Advanced to createAdvancedSettings(context),
        SettingsDestination.Advanced to createCloudSettings(context),
        SettingsDestination.GestureTyping to if (JniUtils.sHaveGestureLib) createGestureTypingSettings(context) else emptyList(),
        SettingsDestination.Voice to listOf(
            Setting(context, SettingsWithoutKey.VOICE_PAGE, R.string.brr_voice_page_title, R.string.brr_voice_page_summary) {
                Preference(it.title, onClick = { SettingsDestination.navigateTo(SettingsDestination.Voice) }, description = it.description)
            }
        ),
        SettingsDestination.BrainDashboard to listOf(
            Setting(context, SettingsWithoutKey.BRAIN_PAGE, R.string.brr_brain_page_title, R.string.brr_brain_page_summary) {
                Preference(it.title, onClick = { SettingsDestination.navigateTo(SettingsDestination.BrainDashboard) }, description = it.description)
            }
        ),
        SettingsDestination.Languages to listOf(
            Setting(context, SettingsWithoutKey.LANGUAGE_PAGE, R.string.language_and_layouts_title) {
                Preference(it.title, onClick = { SettingsDestination.navigateTo(SettingsDestination.Languages) })
            }
        ),
        SettingsDestination.Layouts to listOf(
            Setting(context, SettingsWithoutKey.SECONDARY_LAYOUTS_PAGE, R.string.settings_screen_secondary_layouts) {
                Preference(it.title, onClick = { SettingsDestination.navigateTo(SettingsDestination.Layouts) })
            }
        ),
    )
    private val list = sections.flatMap { it.second }
    private val routes = sections.flatMap { (route, settings) -> settings.map { it.key to route } }.toMap()
    private val map: Map<String, Setting> = HashMap<String, Setting>(list.size).apply {
        list.forEach {
            if (put(it.key, it) != null)
                throw IllegalArgumentException("key $it added twice")
        }
    }

    operator fun get(key: Any): Setting? = map[key]

    /** Context-aware destinations reuse the original preference objects without duplicating them. */
    fun routeFor(key: String): String = when (key) {
        SettingsWithoutKey.BACKUP_RESTORE -> SettingsDestination.Backup
        helium314.keyboard.latin.settings.Settings.PREF_ALWAYS_INCOGNITO_MODE,
        helium314.keyboard.latin.settings.Settings.PREF_SECURE_APPS,
        helium314.keyboard.latin.settings.Settings.PREF_USE_CONTACTS,
        helium314.keyboard.latin.settings.Settings.PREF_USE_APPS -> SettingsDestination.Privacy
        helium314.keyboard.latin.settings.Settings.PREF_VIBRATE_ON,
        helium314.keyboard.latin.settings.Settings.PREF_VIBRATION_DURATION_SETTINGS,
        helium314.keyboard.latin.settings.Settings.PREF_VIBRATE_IN_DND_MODE,
        helium314.keyboard.latin.settings.Settings.PREF_SOUND_ON,
        helium314.keyboard.latin.settings.Settings.PREF_KEYPRESS_SOUND_VOLUME -> SettingsDestination.Sound
        helium314.keyboard.latin.settings.Settings.PREF_ENABLE_CLIPBOARD_HISTORY,
        helium314.keyboard.latin.settings.Settings.PREF_SHOW_SCREENSHOTS_IN_CLIPBOARD,
        helium314.keyboard.latin.settings.Settings.PREF_CHANGE_SCREENSHOTS_FOLDER,
        helium314.keyboard.latin.settings.Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME,
        helium314.keyboard.latin.settings.Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST,
        helium314.keyboard.latin.settings.Settings.PREF_SUGGEST_CLIPBOARD_CONTENT -> SettingsDestination.Clipboard
        helium314.keyboard.latin.settings.Settings.PREF_KEY_LONGPRESS_TIMEOUT,
        helium314.keyboard.latin.settings.Settings.PREF_SPACE_HORIZONTAL_SWIPE,
        helium314.keyboard.latin.settings.Settings.PREF_SPACE_VERTICAL_SWIPE,
        helium314.keyboard.latin.settings.Settings.PREF_LANGUAGE_SWIPE_DISTANCE,
        helium314.keyboard.latin.settings.Settings.PREF_TOUCHPAD_SENSITIVITY,
        helium314.keyboard.latin.settings.Settings.PREF_TOUCHPAD_EDGE_SCROLL,
        helium314.keyboard.latin.settings.Settings.PREF_DELETE_SWIPE,
        helium314.keyboard.latin.settings.Settings.PREF_SPACE_TO_CHANGE_LANG -> SettingsDestination.Keys
        else -> routes[key] ?: SettingsDestination.Settings
    }

    fun breadcrumbFor(key: String): String = when (routeFor(key)) {
        SettingsDestination.Appearance -> "Make it yours · Appearance & glass"
        SettingsDestination.Keys -> "Make it yours · Keys & layout"
        SettingsDestination.Layouts -> "Make it yours · Languages & layouts"
        SettingsDestination.Toolbar -> "Make it yours · Toolbar"
        SettingsDestination.TextCorrection -> "Typing · Correction & suggestions"
        SettingsDestination.GestureTyping -> "Typing · Swipe typing"
        SettingsDestination.BrainDashboard -> "Typing · Dictionary & learning"
        SettingsDestination.Languages -> "Make it yours · Languages & layouts"
        SettingsDestination.Voice -> "Everyday tools · Offline voice"
        SettingsDestination.Sound -> "Everyday tools · Sound & haptics"
        SettingsDestination.Clipboard -> "Everyday tools · Clipboard"
        SettingsDestination.Privacy -> "Privacy & data · Protected apps"
        SettingsDestination.Backup -> "Privacy & data · Backup & restore"
        SettingsDestination.About -> "More · About brrBoard"
        else -> "More · Advanced controls"
    }

    // Offline, deterministic ranking. Matching aliases are scoped to known settings only.
    fun filter(searchTerm: String): List<Setting> {
        val term = searchTerm.trim().lowercase()
        if (term.isBlank()) return emptyList()
        val aliasRoutes = mapOf(
            "glass" to SettingsDestination.Appearance,
            "blur" to SettingsDestination.Appearance,
            "frosted" to SettingsDestination.Appearance,
            "haptic" to SettingsDestination.Sound,
            "vibrate" to SettingsDestination.Sound,
            "mic" to SettingsDestination.Voice,
            "dictation" to SettingsDestination.Voice,
            "private" to SettingsDestination.Privacy,
            "incognito" to SettingsDestination.Privacy,
            "swipe" to SettingsDestination.GestureTyping,
            "height" to SettingsDestination.Appearance,
            "clipboard" to SettingsDestination.Clipboard,
        )
        return list.mapNotNull { setting ->
            val title = setting.title.lowercase()
            val description = setting.description?.lowercase().orEmpty()
            val category = breadcrumbFor(setting.key).lowercase()
            val aliasMatches = aliasRoutes.any { (alias, route) ->
                alias.contains(term) && routeFor(setting.key) == route
            }
            val rank = when {
                title == term -> 0
                title.startsWith(term) -> 1
                title.split(' ', '-', '&').any { it.startsWith(term) } -> 2
                description.contains(term) -> 3
                category.contains(term) -> 4
                aliasMatches -> 5
                else -> return@mapNotNull null
            }
            rank to setting
        }.sortedWith(compareBy<Pair<Int, Setting>> { it.first }.thenBy { it.second.title })
            .take(40).map { it.second }
    }
}

@Immutable
class Setting(
    context: Context,
    val key: String,
    @StringRes titleId: Int,
    @StringRes descriptionId: Int? = null,
    private val content: @Composable (Setting) -> Unit
) {
    val title = context.getString(titleId)
    val description = descriptionId?.let { context.getString(it) }

    @Composable
    fun Preference() {
        content(this)
    }
}

object SettingsWithoutKey {
    const val EDIT_PERSONAL_DICTIONARY = "edit_personal_dictionary"
    const val CLEAR_LEARNED_TYPING_HISTORY = "clear_learned_typing_history"
    const val APP = "app"
    const val SETUP_WIZARD = "setup_wizard_help"
    const val VOICE_PAGE = "offline_voice_settings_page"
    const val BRAIN_PAGE = "brain_dashboard_settings_page"
    const val LANGUAGE_PAGE = "language_settings_page"
    const val SECONDARY_LAYOUTS_PAGE = "secondary_layouts_settings_page"
    const val VERSION = "version"
    const val PRIVACY_POLICY = "privacy_policy"
    const val LICENSE = "license"
    const val HIDDEN_FEATURES = "hidden_features"
    const val GITHUB = "github"
    const val GITHUB_WIKI = "github_wiki"
    const val MANGOLOADS = "mangoloads"
    const val STAR_REPOSITORY = "star_repository"
    const val REPORT_ISSUE = "report_issue"
    const val INLINE_VOICE = "inline_voice"
    const val VOICE_SERVICE = "voice_service"
    const val SAVE_LOG = "save_log"
    const val BACKUP_RESTORE = "backup_restore"
    const val DEBUG_SETTINGS = "screen_debug"
    const val LOAD_GESTURE_LIB = "load_gesture_library"
    const val BACKGROUND_IMAGE = "background_image"
    const val BACKGROUND_IMAGE_LANDSCAPE = "background_image_landscape"
    const val CUSTOM_FONT = "custom_font"
    const val CUSTOM_EMOJI_FONT = "custom_emoji_font"
    const val CUSTOM_EMOJI_SEARCH_SHORTCUTS = "custom_emoji_search_shortcuts"
}
