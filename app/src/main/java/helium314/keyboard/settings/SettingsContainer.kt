// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.JniUtils
import java.text.Normalizer
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
        SettingsWithoutKey.VERSION_HISTORY -> SettingsDestination.VersionHistory
        SettingsWithoutKey.BACKUP_RESTORE -> SettingsDestination.Backup
        helium314.keyboard.latin.settings.Settings.PREF_ALWAYS_INCOGNITO_MODE,
        helium314.keyboard.latin.settings.Settings.PREF_SECURE_APPS,
        helium314.keyboard.latin.settings.Settings.PREF_USE_CONTACTS,
        helium314.keyboard.latin.settings.Settings.PREF_USE_APPS -> SettingsDestination.Privacy
        helium314.keyboard.latin.settings.Settings.PREF_VIBRATE_ON,
        helium314.keyboard.latin.settings.Settings.PREF_VIBRATION_DURATION_SETTINGS,
        helium314.keyboard.latin.settings.Settings.PREF_IMPORTANT_HAPTICS_IN_DND,
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
        SettingsDestination.Appearance -> "Appearance · Glass & layout"
        SettingsDestination.Keys -> "Typing · Keys & interactions"
        SettingsDestination.Layouts -> "Typing · Languages & layouts"
        SettingsDestination.Toolbar -> "Typing · Toolbar"
        SettingsDestination.TextCorrection -> "Typing · Correction & suggestions"
        SettingsDestination.GestureTyping -> "Typing · Swipe typing"
        SettingsDestination.BrainDashboard -> "BR Brain"
        SettingsDestination.Languages -> "Typing · Languages & layouts"
        SettingsDestination.Voice -> "Voice · On-device dictation"
        SettingsDestination.Sound -> "Sound & haptics"
        SettingsDestination.Clipboard -> "Clipboard · History"
        SettingsDestination.Privacy -> "Privacy & security · Protected apps"
        SettingsDestination.Backup -> "Advanced · Backup & restore"
        SettingsDestination.About -> "More · About brrBoard"
        SettingsDestination.VersionHistory -> "More · About brrBoard · Version history"
        else -> "More · Advanced controls"
    }

    // Offline, deterministic ranking. Matching aliases are scoped to known settings only.
    fun filter(searchTerm: String): List<Setting> {
        val term = normalize(searchTerm)
        if (term.isBlank()) return emptyList()
        val queryTokens = tokens(term).filterNot { it in STOP_WORDS }
        val matchedIntents = CONTEXT_INTENTS.filter { intent ->
            intent.terms.all { wanted -> queryTokens.any { tokenMatches(it, wanted) } }
        }
        return list.mapNotNull { setting ->
            val title = normalize(setting.title)
            val description = normalize(setting.description.orEmpty())
            val category = normalize(breadcrumbFor(setting.key))
            val settingRoute = routeFor(setting.key)
            val contextualMatch = matchedIntents.any { intent ->
                settingRoute == intent.route && (intent.settingKey == null || setting.key == intent.settingKey)
            }
            val aliasMatches = SEARCH_ALIASES.any { (alias, route) ->
                term.contains(alias) && settingRoute == route
            }
            val aliases = SETTING_SYNONYMS[setting.key].orEmpty()
            val searchable = "$title $description $category"
            val candidateTokens = tokens(searchable)
            val matched = queryTokens.count { query ->
                candidateTokens.any { tokenMatches(it, query) } || aliases.any { tokenMatches(it, query) }
            }
            val coverage = if (queryTokens.isEmpty()) 0f else matched.toFloat() / queryTokens.size
            val rank = when {
                title == term -> 0
                title.startsWith(term) -> 1
                title.split(' ', '-', '&').any { it.startsWith(term) } -> 2
                description.contains(term) -> 3
                category.contains(term) -> 4
                aliasMatches || contextualMatch -> 5
                coverage >= .66f -> 6
                coverage >= .5f && queryTokens.size >= 2 -> 7
                else -> return@mapNotNull null
            }
            rank to setting
        }.sortedWith(compareBy<Pair<Int, Setting>> { it.first }.thenBy { it.second.title })
            .take(40).map { it.second }
    }

    private data class ContextIntent(val terms: Set<String>, val route: String, val settingKey: String? = null)

    companion object {
        private val STOP_WORDS = setOf(
            "a", "an", "and", "can", "do", "does", "for", "how", "i", "in", "is", "it",
            "me", "my", "of", "on", "or", "please", "the", "this", "to", "want", "what", "with",
        )
        private val SEARCH_ALIASES = mapOf(
            "glass" to SettingsDestination.Appearance,
            "blur" to SettingsDestination.Appearance,
            "frosted" to SettingsDestination.Appearance,
            "haptic" to SettingsDestination.Sound,
            "vibrate" to SettingsDestination.Sound,
            "dnd" to SettingsDestination.Sound,
            "important haptic" to SettingsDestination.Sound,
            "mic" to SettingsDestination.Voice,
            "dictation" to SettingsDestination.Voice,
            "voice model" to SettingsDestination.Voice,
            "ultra" to SettingsDestination.Voice,
            "brrclean" to SettingsDestination.Voice,
            "private" to SettingsDestination.Privacy,
            "incognito" to SettingsDestination.Privacy,
            "swipe" to SettingsDestination.GestureTyping,
            "height" to SettingsDestination.Appearance,
            "clipboard" to SettingsDestination.Clipboard,
            "reset settings" to SettingsDestination.Backup,
            "backup" to SettingsDestination.Backup,
        )
        private val CONTEXT_INTENTS = listOf(
            ContextIntent(setOf("suggestion", "animation"), SettingsDestination.Appearance,
                helium314.keyboard.latin.settings.Settings.PREF_MOTION_PROFILE),
            ContextIntent(setOf("keyboard", "animation"), SettingsDestination.Appearance,
                helium314.keyboard.latin.settings.Settings.PREF_MOTION_PROFILE),
            ContextIntent(setOf("suggestion", "ios"), SettingsDestination.Appearance,
                helium314.keyboard.latin.settings.Settings.PREF_MOTION_PROFILE),
            ContextIntent(setOf("smart", "emoji"), SettingsDestination.BrainDashboard),
            ContextIntent(setOf("emoji", "suggestion"), SettingsDestination.BrainDashboard),
            ContextIntent(setOf("next", "word"), SettingsDestination.BrainDashboard),
            ContextIntent(setOf("learn", "word"), SettingsDestination.BrainDashboard),
            ContextIntent(setOf("voice", "clean"), SettingsDestination.Voice),
            ContextIntent(setOf("voice", "noise"), SettingsDestination.Voice),
            ContextIntent(setOf("typing", "private"), SettingsDestination.Privacy),
            ContextIntent(setOf("settings", "reset"), SettingsDestination.Backup),
        )
        private val SETTING_SYNONYMS = mapOf(
            helium314.keyboard.latin.settings.Settings.PREF_MOTION_PROFILE to
                setOf("animation", "suggestion", "suggestions", "typing", "transition", "smooth", "ios"),
            SettingsWithoutKey.BRAIN_PAGE to
                setOf("prediction", "predictive", "emoji", "learning", "correction", "suggestion",
                    "suggestions", "nextword", "smart", "contextual"),
            SettingsWithoutKey.VOICE_PAGE to
                setOf("microphone", "speech", "transcription", "noise", "cleaning", "parakeet"),
            SettingsWithoutKey.BACKUP_RESTORE to setOf("reset", "defaults", "restore", "backup"),
        )

        private fun normalize(value: String): String = Normalizer.normalize(
            value.lowercase(), Normalizer.Form.NFD,
        ).replace(Regex("\\p{M}+"), "").trim()

        private fun tokens(value: String): List<String> =
            Regex("[\\p{L}\\p{N}]+").findAll(value).map { it.value }.toList()

        private fun tokenMatches(candidate: String, query: String): Boolean =
            candidate == query || candidate.startsWith(query) || query.startsWith(candidate)
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
    const val VERSION_HISTORY = "version_history_page"
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
    const val COPY_DIAGNOSTIC_REPORT = "copy_diagnostic_report"
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
