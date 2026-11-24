// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.foundation.layout.Column
import dev.chrisbanes.haze.haze
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.FrostAccentRoles
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.preferences.ListPreference
import helium314.keyboard.settings.SettingsWithoutKey
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.preferences.SliderPreference
import helium314.keyboard.settings.preferences.SwitchPreference
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.BackgroundImagePref
import helium314.keyboard.settings.preferences.CustomFontPreference
import helium314.keyboard.settings.preferences.CustomEmojiSearchAliasesPreference
import helium314.keyboard.settings.preferences.KeyboardScalePreference
import helium314.keyboard.settings.preferences.TextInputPreference
import helium314.keyboard.latin.utils.previewDark
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.FoldableUtils
import helium314.keyboard.settings.dialogs.ThreeButtonAlertDialog

@Composable
fun AppearanceScreen(
    onClickBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    val b = (LocalContext.current.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")
    val isFrostedActive = helium314.keyboard.latin.FrostedGlassHelper.isFrostedTheme(ctx)
    val items = listOf(
        R.string.brr_category_visual_material,
        Settings.PREF_MOTION_PROFILE,
        Settings.PREF_THEME_ACTION_TINT,
        FrostAccentRoles.PREF_APPLE_PALETTE,
        FrostAccentRoles.PREF_ROUNDED_KEYCAPS,
        Settings.PREF_THEME_KEY_BORDERS,
        Settings.PREF_NAVBAR_COLOR,
        if (isFrostedActive) "adjust_frosted_glass_dialog" else null,
        // Reflection styles now live inside Glass Studio, not in a duplicate dialog row.
        SettingsWithoutKey.BACKGROUND_IMAGE,
        SettingsWithoutKey.BACKGROUND_IMAGE_LANDSCAPE,
        R.string.brr_category_keyboard_geometry,
        Settings.PREF_KEYBOARD_CORNER_RADIUS,
        Settings.PREF_ENABLE_SPLIT_KEYBOARD,
        if (prefs.getBoolean(Settings.PREF_ENABLE_SPLIT_KEYBOARD_LANDSCAPE, Defaults.PREF_ENABLE_SPLIT_KEYBOARD)
            || prefs.getBoolean(Settings.PREF_ENABLE_SPLIT_KEYBOARD, Defaults.PREF_ENABLE_SPLIT_KEYBOARD)
            || prefs.getBoolean(Settings.PREF_ENABLE_SPLIT_KEYBOARD_FOLDED, Defaults.PREF_ENABLE_SPLIT_KEYBOARD)
            || prefs.getBoolean(Settings.PREF_ENABLE_SPLIT_KEYBOARD_FOLDED_LANDSCAPE, Defaults.PREF_ENABLE_SPLIT_KEYBOARD)
            )
            Settings.PREF_SPLIT_SPACER_SCALE_PREFIX else null,
        if (prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS))
            Settings.PREF_NARROW_KEY_GAPS else null,
        Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX,
        Settings.PREF_BOTTOM_ROW_SCALE_PREFIX,
        Settings.PREF_BOTTOM_PADDING_SCALE_PREFIX,
        Settings.PREF_SIDE_PADDING_SCALE_PREFIX,
        Settings.PREF_SPACE_BAR_TEXT,
        R.string.brr_category_lettering_emoji,
        SettingsWithoutKey.CUSTOM_FONT,
        Settings.PREF_FONT_SCALE,
        SettingsWithoutKey.CUSTOM_EMOJI_FONT,
        SettingsWithoutKey.CUSTOM_EMOJI_SEARCH_SHORTCUTS,
        Settings.PREF_EMOJI_FONT_SCALE,
        if (prefs.getFloat(Settings.PREF_EMOJI_FONT_SCALE, Defaults.PREF_EMOJI_FONT_SCALE) != 1f)
            Settings.PREF_EMOJI_KEY_FIT else null,
        if (prefs.getInt(Settings.PREF_EMOJI_MAX_SDK, 0) >= 24)
            Settings.PREF_EMOJI_SKIN_TONE else null,
        Settings.PREF_PERSISTENT_EMOJI_ROW,
        Settings.PREF_EMOJI_GLASS_TONE,
    )
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            helium314.keyboard.settings.SettingsActivity.activeOverlay = null
        }
    }

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_appearance),
        settings = items
    )
}

fun createAppearanceSettings(context: Context) = listOf(
    Setting(context, Settings.PREF_MOTION_PROFILE, R.string.motion_profile_title,
        R.string.motion_profile_summary) { setting ->
        ListPreference(
            setting,
            listOf(
                stringResource(R.string.motion_profile_ios27) to "ios27",
                stringResource(R.string.motion_profile_previous) to "previous_default"
            ),
            Defaults.PREF_MOTION_PROFILE
        ) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
    Setting(context, Settings.PREF_EMOJI_GLASS_TONE,
        R.string.emoji_glass_tone_title, R.string.emoji_glass_tone_summary) {
        SwitchPreference(it, Defaults.PREF_EMOJI_GLASS_TONE) {
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }
    },
    Setting(context, Settings.PREF_THEME_ACTION_TINT, R.string.theme_action_tint, R.string.theme_action_tint_summary) {
        SwitchPreference(it, Defaults.PREF_THEME_ACTION_TINT) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, FrostAccentRoles.PREF_APPLE_PALETTE, R.string.theme_apple_palette, R.string.theme_apple_palette_summary) { setting ->
        val names = listOf("Blue", "Red", "Orange", "Yellow", "Green", "Mint", "Teal", "Cyan", "Indigo", "Purple", "Pink", "Brown")
        ListPreference(setting, listOf("Wallpaper · automatic" to FrostAccentRoles.DEFAULT_PALETTE) +
            names.zip(FrostAccentRoles.applePaletteIds), FrostAccentRoles.DEFAULT_PALETTE) {
            context.prefs().edit().putBoolean(Settings.PREF_THEME_ACTION_TINT, true).apply()
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }
    },
    Setting(context, FrostAccentRoles.PREF_ROUNDED_KEYCAPS, R.string.theme_rounded_board_keycaps, R.string.theme_rounded_board_keycaps_summary) {
        SwitchPreference(it, FrostAccentRoles.DEFAULT_ROUNDED_KEYCAPS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_THEME_KEY_BORDERS, R.string.key_borders) {
        SwitchPreference(it, Defaults.PREF_THEME_KEY_BORDERS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_NAVBAR_COLOR, R.string.theme_navbar, R.string.day_night_mode_summary) {
        SwitchPreference(it, Defaults.PREF_NAVBAR_COLOR)
    },
    Setting(context, SettingsWithoutKey.BACKGROUND_IMAGE, R.string.customize_background_image) {
        BackgroundImagePref(it, false)
    },
    Setting(context, SettingsWithoutKey.BACKGROUND_IMAGE_LANDSCAPE,
        R.string.customize_background_image_landscape, R.string.summary_customize_background_image_landscape)
    {
        BackgroundImagePref(it, true)
    },
    Setting(context, Settings.PREF_KEYBOARD_CORNER_RADIUS, R.string.prefs_keyboard_corner_radius) { setting ->
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = Defaults.PREF_KEYBOARD_CORNER_RADIUS,
            range = Settings.KEYBOARD_CORNER_RADIUS_MIN_DP.toFloat()..Settings.KEYBOARD_CORNER_RADIUS_MAX_DP.toFloat(),
            stepSize = 1,
            description = { "$it dp" }
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_ENABLE_SPLIT_KEYBOARD, R.string.enable_split_keyboard) {
        var show by remember { mutableStateOf(false) }
        val prefAndName = listOfNotNull(
            Settings.PREF_ENABLE_SPLIT_KEYBOARD to stringResource(R.string.button_default),
            Settings.PREF_ENABLE_SPLIT_KEYBOARD_LANDSCAPE to stringResource(R.string.landscape),
            if (!FoldableUtils.isFoldable) null else
                Settings.PREF_ENABLE_SPLIT_KEYBOARD_FOLDED to stringResource(R.string.folded),
            if (!FoldableUtils.isFoldable) null else
                Settings.PREF_ENABLE_SPLIT_KEYBOARD_FOLDED_LANDSCAPE to stringResource(R.string.folded) + " / " + stringResource(R.string.landscape)
        )
        Preference(
            name = stringResource(R.string.enable_split_keyboard),
            onClick = { show = true },
            description = prefAndName.filter { LocalContext.current.prefs().getBoolean(it.first, Defaults.PREF_ENABLE_SPLIT_KEYBOARD) }
                .joinToString(", ") { it.second }.takeIf { it.isNotEmpty() }
        )
        if (show) {
            ThreeButtonAlertDialog(
                onDismissRequest = { show = false },
                onConfirmed = {},
                confirmButtonText = null,
                cancelButtonText = stringResource(R.string.dialog_close),
                content = {
                    Column {
                        prefAndName.forEach {
                            SwitchPreference(name = it.second, key = it.first, default = Defaults.PREF_ENABLE_SPLIT_KEYBOARD)
                        }
                    }
                }
            )
        }
    },
    Setting(context, Settings.PREF_SPLIT_SPACER_SCALE_PREFIX, R.string.split_spacer_scale) { setting ->
        KeyboardScalePreference(
            name = setting.title,
            baseKey = setting.key,
            dimensions = listOf(stringResource(R.string.landscape), stringResource(R.string.folded)),
            defaults = Defaults.PREF_SPLIT_SPACER_SCALE,
            range = 0.5f..2f,
            description = { "${(100 * it).toInt()}%" }
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    // todo: also for landscape + folded, but maybe consider this variable gap setting first so it could be a scale setting (was in some PR)
    Setting(context, Settings.PREF_NARROW_KEY_GAPS, R.string.prefs_narrow_key_gaps) {
        SwitchPreference(it, Defaults.PREF_NARROW_KEY_GAPS) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX, R.string.prefs_keyboard_height_scale) { setting ->
        KeyboardScalePreference(
            name = setting.title,
            baseKey = setting.key,
            dimensions = listOf(stringResource(R.string.landscape), stringResource(R.string.folded)),
            defaults = Defaults.PREF_KEYBOARD_HEIGHT_SCALE,
            range = Settings.KEYBOARD_HEIGHT_SCALE_MIN..Settings.KEYBOARD_HEIGHT_SCALE_MAX,
            description = { "${(100 * it).toInt()}%" }
        ) { KeyboardSwitcher.getInstance().onKeyboardHeightScaleChanged() }
    },
    Setting(context, Settings.PREF_BOTTOM_ROW_SCALE_PREFIX, R.string.prefs_bottom_row_scale) { setting ->
        KeyboardScalePreference(
            name = setting.title,
            baseKey = setting.key,
            dimensions = listOf(stringResource(R.string.landscape), stringResource(R.string.folded)),
            defaults = Defaults.PREF_BOTTOM_ROW_SCALE,
            range = 0.5f..2f,
            description = { "${(100 * it).toInt()}%" }
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_BOTTOM_PADDING_SCALE_PREFIX, R.string.prefs_bottom_padding_scale) { setting ->
        KeyboardScalePreference(
            name = setting.title,
            baseKey = setting.key,
            dimensions = listOf(stringResource(R.string.landscape), stringResource(R.string.folded)),
            defaults = Defaults.PREF_BOTTOM_PADDING_SCALE,
            range = 0f..5f,
            description = { "${(100 * it).toInt()}%" }
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SIDE_PADDING_SCALE_PREFIX, R.string.prefs_side_padding_scale) { setting ->
        KeyboardScalePreference(
            name = setting.title,
            baseKey = setting.key,
            dimensions = listOf(stringResource(R.string.landscape), stringResource(R.string.split), stringResource(R.string.folded)),
            defaults = Defaults.PREF_SIDE_PADDING_SCALE,
            range = 0f..3f,
            description = { "${(100 * it).toInt()}%" }
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_SPACE_BAR_TEXT, R.string.prefs_space_bar_text) {
        TextInputPreference(it, Defaults.PREF_SPACE_BAR_TEXT)
    },
    Setting(context, SettingsWithoutKey.CUSTOM_FONT, R.string.custom_font) {
        CustomFontPreference(it, Settings.getCustomFontFile(LocalContext.current), R.string.custom_font)
    },
    Setting(context, Settings.PREF_FONT_SCALE, R.string.prefs_font_scale) { def ->
        SliderPreference(
            name = def.title,
            key = def.key,
            default = Defaults.PREF_FONT_SCALE,
            range = 0.5f..1.5f,
            description = { "${(100 * it).toInt()}%" }
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, SettingsWithoutKey.CUSTOM_EMOJI_FONT, R.string.custom_emoji_font) {
        CustomFontPreference(it, Settings.getCustomEmojiFontFile(LocalContext.current), R.string.custom_emoji_font)
    },
    Setting(context, SettingsWithoutKey.CUSTOM_EMOJI_SEARCH_SHORTCUTS,
        R.string.emoji_describe_shortcuts_title, R.string.emoji_describe_shortcuts_summary) {
        CustomEmojiSearchAliasesPreference(it)
    },
    Setting(context, Settings.PREF_EMOJI_FONT_SCALE, R.string.prefs_emoji_font_scale) { setting ->
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = Defaults.PREF_EMOJI_FONT_SCALE,
            range = 0.5f..1.5f,
            description = { "${(100 * it).toInt()}%" }
        ) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_EMOJI_KEY_FIT, R.string.prefs_emoji_key_fit) {
        SwitchPreference(it, Defaults.PREF_EMOJI_KEY_FIT) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_EMOJI_SKIN_TONE, R.string.prefs_emoji_skin_tone) { setting ->
        val items = listOf(
            stringResource(R.string.prefs_emoji_skin_tone_neutral) to "",
            "\uD83C\uDFFB" to "\uD83C\uDFFB",
            "\uD83C\uDFFC" to "\uD83C\uDFFC",
            "\uD83C\uDFFD" to "\uD83C\uDFFD",
            "\uD83C\uDFFE" to "\uD83C\uDFFE",
            "\uD83C\uDFFF" to "\uD83C\uDFFF"
        )
        ListPreference(setting, items, Defaults.PREF_EMOJI_SKIN_TONE) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, Settings.PREF_PERSISTENT_EMOJI_ROW, R.string.prefs_persistent_emoji_row) {
        SwitchPreference(it, Defaults.PREF_PERSISTENT_EMOJI_ROW) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
    Setting(context, "adjust_frosted_glass_dialog", R.string.button_adjust_frosted_glass) { setting ->
        Preference(
            name = setting.title,
            onClick = {
                helium314.keyboard.settings.SettingsActivity.activeOverlay = { hazeState ->
                    helium314.keyboard.settings.dialogs.FrostedGlassAdjustDialog(
                        hazeState = hazeState,
                        onDismissRequest = { helium314.keyboard.settings.SettingsActivity.activeOverlay = null }
                    )
                }
            }
        )
    },
    Setting(context, "keyboard_glass_highlight_dialog",
        R.string.keyboard_highlight_settings_entry,
        R.string.keyboard_highlight_settings_description
    ) { setting ->
        Preference(
            name = setting.title,
            description = setting.description,
            onClick = {
                helium314.keyboard.settings.SettingsActivity.activeOverlay = { hazeState ->
                    helium314.keyboard.settings.dialogs.FrostedGlassAdjustDialog(
                        hazeState = hazeState,
                        onDismissRequest = { helium314.keyboard.settings.SettingsActivity.activeOverlay = null },
                        startAtHighlight = true
                    )
                }
            }
        )
    },
)

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            AppearanceScreen { }
        }
    }
}
