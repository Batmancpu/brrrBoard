// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import helium314.keyboard.latin.settings.Settings as S

/** UI ownership only: no new stored values, no migration and no engine dependency. */
internal object WorkspaceSettings {
    private val size = setOf(S.PREF_KEYBOARD_CORNER_RADIUS, S.PREF_ENABLE_SPLIT_KEYBOARD,
        S.PREF_SPLIT_SPACER_SCALE_PREFIX, S.PREF_NARROW_KEY_GAPS, S.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX,
        S.PREF_BOTTOM_ROW_SCALE_PREFIX, S.PREF_BOTTOM_PADDING_SCALE_PREFIX, S.PREF_SIDE_PADDING_SCALE_PREFIX)
    private val lettering = setOf(S.PREF_SPACE_BAR_TEXT, SettingsWithoutKey.CUSTOM_FONT, S.PREF_FONT_SCALE)
    private val emoji = setOf(SettingsWithoutKey.CUSTOM_EMOJI_FONT, SettingsWithoutKey.CUSTOM_EMOJI_SEARCH_SHORTCUTS,
        S.PREF_EMOJI_FONT_SCALE, S.PREF_EMOJI_KEY_FIT, S.PREF_EMOJI_SKIN_TONE, S.PREF_PERSISTENT_EMOJI_ROW,
        S.PREF_EMOJI_GLASS_TONE, S.PREF_SHOW_EMOJI_DESCRIPTIONS, S.PREF_EMOJI_MAX_SDK, S.PREF_ABC_AFTER_EMOJI)
    private val gestures = setOf(S.PREF_KEY_LONGPRESS_TIMEOUT, S.PREF_SPACE_HORIZONTAL_SWIPE,
        S.PREF_SPACE_VERTICAL_SWIPE, S.PREF_LANGUAGE_SWIPE_DISTANCE, S.PREF_TOUCHPAD_SENSITIVITY,
        S.PREF_TOUCHPAD_EDGE_SCROLL, S.PREF_DELETE_SWIPE, S.PREF_SPACE_TO_CHANGE_LANG,
        S.PREF_ABC_AFTER_SYMBOL_SPACE, S.PREF_ABC_AFTER_NUMPAD_SPACE)
    private val writing = setOf(S.PREF_AUTO_CAP, S.PREF_KEY_USE_DOUBLE_SPACE_PERIOD,
        S.PREF_AUTOSPACE_AFTER_PUNCTUATION, S.PREF_AUTOSPACE_AFTER_SUGGESTION,
        S.PREF_AUTOSPACE_BEFORE_GESTURE_TYPING, S.PREF_AUTOSPACE_AFTER_GESTURE_TYPING, S.PREF_SHIFT_REMOVES_AUTOSPACE)
    private val correction = setOf(S.PREF_BLOCK_POTENTIALLY_OFFENSIVE, S.PREF_AUTO_CORRECTION,
        S.PREF_MORE_AUTO_CORRECTION, S.PREF_AUTOCORRECT_SHORTCUTS, S.PREF_AUTO_CORRECT_THRESHOLD,
        S.PREF_BACKSPACE_REVERTS_AUTOCORRECT, S.PREF_SPELLCHECK_SUGGEST)
    private val privacy = setOf(S.PREF_ALWAYS_INCOGNITO_MODE, S.PREF_SECURE_APPS, S.PREF_USE_CONTACTS,
        S.PREF_USE_APPS, S.PREF_KEY_USE_PERSONALIZED_DICTS, S.PREF_SAVE_SUBTYPE_PER_APP,
        S.PREF_ADD_TO_PERSONAL_DICTIONARY, SettingsWithoutKey.CLEAR_LEARNED_TYPING_HISTORY)
    val diagnosticKeys = setOf(SettingsWithoutKey.COPY_DIAGNOSTIC_REPORT, SettingsWithoutKey.SAVE_LOG,
        SettingsWithoutKey.REPORT_ISSUE)
    fun appearanceSection(key: String) = when (key) {
        in size -> "size"
        in lettering -> "lettering"
        in emoji -> "emoji"
        S.PREF_MOTION_PROFILE, S.PREF_SUGGESTION_MOTION_STYLE -> "motion"
        else -> "glass"
    }
    fun keysSection(key: String) = when (key) {
        in gestures -> "gestures"
        in privacy, in emoji, S.PREF_ABC_AFTER_CLIP, SettingsWithoutKey.SECONDARY_LAYOUTS_PAGE -> "other"
        else -> "keys"
    }
    fun correctionSection(key: String) = when (key) {
        in correction -> "correction"
        in writing -> "writing"
        in privacy, SettingsWithoutKey.EDIT_PERSONAL_DICTIONARY, S.PREF_SUGGEST_CLIPBOARD_CONTENT -> "other"
        else -> "suggestions"
    }
    fun destination(key: String, registered: String): String = when {
        key == "adjust_frosted_glass_dialog" || key == "keyboard_glass_highlight_dialog" -> SettingsDestination.GlassEditor
        key == SettingsWithoutKey.INLINE_VOICE || key == SettingsWithoutKey.VOICE_SERVICE -> SettingsDestination.Voice
        key in privacy -> SettingsDestination.Privacy
        key in emoji -> SettingsDestination.Emoji
        key in gestures -> SettingsDestination.GestureAssignments
        key in diagnosticKeys -> SettingsDestination.Diagnostics
        key == SettingsWithoutKey.SECONDARY_LAYOUTS_PAGE -> SettingsDestination.Layouts
        key == S.PREF_ABC_AFTER_CLIP -> SettingsDestination.Clipboard
        key == S.PREF_TIMESTAMP_FORMAT || key == S.PREF_URL_DETECTION -> SettingsDestination.TextTools
        key == SettingsWithoutKey.EDIT_PERSONAL_DICTIONARY -> SettingsDestination.PersonalDictionaries
        registered == SettingsDestination.Appearance -> when (appearanceSection(key)) {
            "size" -> SettingsDestination.Size
            "lettering" -> SettingsDestination.Lettering
            "emoji" -> SettingsDestination.Emoji
            "motion" -> SettingsDestination.Motion
            else -> SettingsDestination.Glass
        }
        registered == SettingsDestination.Keys -> SettingsDestination.KeySymbols
        registered == SettingsDestination.TextCorrection -> when (correctionSection(key)) {
            "correction" -> SettingsDestination.Correction
            "writing" -> SettingsDestination.Writing
            else -> SettingsDestination.Suggestions
        }
        registered == SettingsDestination.Advanced -> SettingsDestination.Compatibility
        else -> registered
    }
}
