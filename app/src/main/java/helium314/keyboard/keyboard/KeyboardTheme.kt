/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */
package helium314.keyboard.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.AllColors
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.common.DefaultColors
import helium314.keyboard.latin.common.DynamicColors
import helium314.keyboard.latin.common.OgFrostAccentColors
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ResourceUtils
import helium314.keyboard.latin.utils.brightenOrDarken
import helium314.keyboard.latin.utils.isBrightColor
import helium314.keyboard.latin.utils.isGoodContrast
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SettingsActivity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.EnumMap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.ColorUtils

@Serializable
data class FrostedLiveValues(
    val blurRadius: Int,
    val keyTransparency: Int,
    val bgTransparency: Int,
    val colorBlend: Int,
    val saturation: Int,
    val specialVibrancy: Int,
    val alphabetVibrancy: Int,
    val dustEnabled: Boolean,
    val dustAlpha: Float
)

@Serializable
class KeyboardTheme // Note: The themeId should be aligned with "themeId" attribute of Keyboard style in values/themes-<style>.xml.
private constructor(val themeId: Int, @JvmField val mStyleId: Int) {

    override fun equals(other: Any?) = if (other === this) true
        else (other as? KeyboardTheme)?.themeId == themeId

    override fun hashCode(): Int {
        return themeId
    }

    companion object {
        @Volatile
        @JvmStatic
        var themeOverride: String? = null
        
        @JvmStatic
        var livePreviewValues: FrostedLiveValues? = null

        @JvmStatic
        fun isDarkThemeActive(context: Context): Boolean {
            if (themeOverride == "dark") return true
            if (themeOverride == "light") return false
            return ResourceUtils.isNight(context.resources)
        }

        // old themes, now called styles
        const val STYLE_MATERIAL = "Material"
        const val STYLE_HOLO = "Holo"
        const val STYLE_ROUNDED = "Rounded"
        const val STYLE_CIRCLE = "Circle"
        const val STYLE_DEFAULT = "Default"

        // new themes that are just colors
        const val THEME_LIGHT = "light"
        const val THEME_HOLO_WHITE = "holo_white"
        const val THEME_DARK = "dark"
        const val THEME_DARKER = "darker"
        const val THEME_BLACK = "black"
        const val THEME_DYNAMIC = "dynamic"
        const val THEME_BLUE_GRAY = "blue_gray"
        const val THEME_BROWN = "brown"
        const val THEME_CHOCOLATE = "chocolate"
        const val THEME_CLOUDY = "cloudy"
        const val THEME_FOREST = "forest"
        const val THEME_INDIGO = "indigo"
        const val THEME_OCEAN = "ocean"
        const val THEME_PINK = "pink"
        const val THEME_SAND = "sand"
        const val THEME_VIOLETTE = "violette"
        const val THEME_FROSTED_GLASS = "frosted_glass"
        const val THEME_OG_FROST_TINTED = "og_frost_tinted"
        const val THEME_IOS_CLEAR = "frosted_ios_clear"
        const val THEME_IOS_TINTED = "frosted_ios_tinted"

        fun isIosGlassTheme(themeName: String?) =
            themeName == THEME_IOS_CLEAR || themeName == THEME_IOS_TINTED

        private const val PREF_FROSTED_NEUTRAL_DEFAULTS_MIGRATED = "frosted_neutral_defaults_migrated_v1"

        private val boardThemes = listOf(THEME_FROSTED_GLASS)
        private val legacyTintedBoardThemes = setOf(THEME_OG_FROST_TINTED, THEME_IOS_TINTED)

        fun getAvailableDefaultColors() = boardThemes

        fun supportedTheme(themeName: String?): String =
            if (themeName != null && themeName in boardThemes) themeName else THEME_FROSTED_GLASS

        private fun migrateBoardThemePreferences(prefs: SharedPreferences) {
            val migrateLegacyTheme = !prefs.contains(Settings.PREF_THEME_ACTION_TINT)
            val migrateNeutralDefaults = !prefs.getBoolean(PREF_FROSTED_NEUTRAL_DEFAULTS_MIGRATED, false)
            if (!migrateLegacyTheme && !migrateNeutralDefaults) return

            val lightTheme = prefs.getString(Settings.PREF_THEME_COLORS, null)
            val darkTheme = prefs.getString(Settings.PREF_THEME_COLORS_NIGHT, null)
            val legacyColorDefaults = listOf(
                Triple(Settings.PREF_FROSTED_SATURATION, 250, Defaults.PREF_FROSTED_SATURATION),
                Triple(Settings.PREF_FROSTED_SPECIAL_VIBRANCY, 398, Defaults.PREF_FROSTED_SPECIAL_VIBRANCY),
                Triple(Settings.PREF_FROSTED_ALPHABET_VIBRANCY, 500, Defaults.PREF_FROSTED_ALPHABET_VIBRANCY),
                Triple(Settings.PREF_FROSTED_SATURATION_NIGHT, 250, Defaults.PREF_FROSTED_SATURATION_NIGHT),
                Triple(Settings.PREF_FROSTED_SPECIAL_VIBRANCY_NIGHT, 0, Defaults.PREF_FROSTED_SPECIAL_VIBRANCY_NIGHT),
                Triple(Settings.PREF_FROSTED_ALPHABET_VIBRANCY_NIGHT, 72, Defaults.PREF_FROSTED_ALPHABET_VIBRANCY_NIGHT),
            )
            prefs.edit {
                if (migrateLegacyTheme) {
                    putBoolean(
                        Settings.PREF_THEME_ACTION_TINT,
                        lightTheme in legacyTintedBoardThemes || darkTheme in legacyTintedBoardThemes
                    )
                }
                if (migrateNeutralDefaults) {
                    // Replace only values that still equal the previous shipped defaults.
                    // Preserve any frosted-glass values the owner actually customized.
                    legacyColorDefaults.forEach { (key, previousDefault, neutralDefault) ->
                        if (prefs.contains(key) && prefs.getInt(key, previousDefault) == previousDefault) {
                            putInt(key, neutralDefault)
                        }
                    }
                    putBoolean(PREF_FROSTED_NEUTRAL_DEFAULTS_MIGRATED, true)
                }
            }
        }

        val STYLES = arrayOf(STYLE_DEFAULT, STYLE_MATERIAL, STYLE_HOLO, STYLE_ROUNDED, STYLE_CIRCLE)

        // These should be aligned with Keyboard.themeId and Keyboard.Case.keyboardTheme
        // attributes' values in attrs.xml.
        private const val THEME_ID_HOLO_BASE = 0
        private const val THEME_ID_LXX_BASE = 1
        private const val THEME_ID_LXX_BASE_BORDER = 2
        private const val THEME_ID_ROUNDED_BASE = 3
        private const val THEME_ID_ROUNDED_BASE_BORDER = 4
        private const val THEME_ID_DEFAULT_BASE = 5
        private const val THEME_ID_DEFAULT_BASE_BORDER = 6
        private const val DEFAULT_THEME_ID = THEME_ID_LXX_BASE

        private val KEYBOARD_THEMES = arrayOf(
            KeyboardTheme(THEME_ID_HOLO_BASE, R.style.KeyboardTheme_HoloBase),
            KeyboardTheme(THEME_ID_LXX_BASE, R.style.KeyboardTheme_LXX_Base),
            KeyboardTheme(THEME_ID_LXX_BASE_BORDER, R.style.KeyboardTheme_LXX_Base_Border),
            KeyboardTheme(THEME_ID_ROUNDED_BASE, R.style.KeyboardTheme_Rounded_Base),
            KeyboardTheme(THEME_ID_ROUNDED_BASE_BORDER, R.style.KeyboardTheme_Rounded_Base_Border),
            KeyboardTheme(THEME_ID_DEFAULT_BASE, R.style.KeyboardTheme_Default_Base),
            KeyboardTheme(THEME_ID_DEFAULT_BASE_BORDER, R.style.KeyboardTheme_Default_Base_Border)
        )

        // named colors, with names from old settings
        const val COLOR_ACCENT = "accent"
        const val COLOR_GESTURE = "gesture"
        const val COLOR_SUGGESTION_TEXT = "suggestion_text"
        const val COLOR_TEXT = "text"
        const val COLOR_HINT_TEXT = "hint_text"
        const val COLOR_KEYS = "keys"
        const val COLOR_FUNCTIONAL_KEYS = "functional_keys"
        const val COLOR_SPACEBAR = "spacebar"
        const val COLOR_SPACEBAR_TEXT = "spacebar_text"
        const val COLOR_BACKGROUND = "background"

        @JvmStatic
        fun getKeyboardTheme(context: Context): KeyboardTheme {
            val prefs = context.prefs()
            // The board has one fixed iOS-like key and glyph style. Retired style
            // preferences from older installs must not silently alter its shape.
            val style = Defaults.PREF_THEME_STYLE
            val borders = prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
            val matchingId = when (style) {
                STYLE_HOLO -> THEME_ID_HOLO_BASE
                STYLE_ROUNDED, STYLE_CIRCLE -> if (borders) THEME_ID_ROUNDED_BASE_BORDER else THEME_ID_ROUNDED_BASE
                STYLE_DEFAULT -> if (borders) THEME_ID_DEFAULT_BASE_BORDER else THEME_ID_DEFAULT_BASE
                else -> if (borders) THEME_ID_LXX_BASE_BORDER else THEME_ID_LXX_BASE
            }
            return KEYBOARD_THEMES.firstOrNull { it.themeId == matchingId } ?: KEYBOARD_THEMES[DEFAULT_THEME_ID]
        }

        fun getThemeActionAndEmojiKeyLabelFlags(themeId: Int): Int {
            return if (themeId == THEME_ID_LXX_BASE || themeId == THEME_ID_ROUNDED_BASE || themeId == THEME_ID_DEFAULT_BASE) Key.LABEL_FLAGS_KEEP_BACKGROUND_ASPECT_RATIO else 0
        }

        @JvmStatic
        fun getColorsForCurrentTheme(context: Context): Colors {
            val prefs = context.prefs()
            migrateBoardThemePreferences(prefs)
            val isNight = SettingsActivity.forceNight ?: ResourceUtils.isNight(context.resources)

            val themeName = SettingsActivity.forceTheme ?: if (isNight)
                prefs.getString(Settings.PREF_THEME_COLORS_NIGHT, Defaults.PREF_THEME_COLORS_NIGHT)
            else
                prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)
            val themeStyle = Defaults.PREF_THEME_STYLE
            val hasBorders = prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
            helium314.keyboard.latin.utils.Log.i("KeyboardTheme", "Active theme: colors=$themeName, style=$themeStyle, hasBorders=$hasBorders, isNight=$isNight")

            return getThemeColors(supportedTheme(themeName), themeStyle!!, context, prefs, isNight)
        }

        private const val IOS_BLUE = -16742145

        // Default light-appearance system colors from Apple's Human Interface Guidelines.
        private val iosAccentPalette = intArrayOf(
            IOS_BLUE, 0xffff383c.toInt(), 0xffff8d28.toInt(), 0xffffcc00.toInt(),
            0xff34c759.toInt(), 0xff00c8b3.toInt(), 0xff00c3d0.toInt(),
            0xff00c0e8.toInt(), 0xff6155f5.toInt(), 0xffcb30e0.toInt(),
            0xffff2d55.toInt(), 0xffac7f5e.toInt(),
        )
        private val iosAccentLab = iosAccentPalette.map { color ->
            DoubleArray(3).also { ColorUtils.colorToLAB(color, it) }
        }

        @JvmStatic
        fun nearestAppleAccent(androidAccent: Int): Int {
            val lab = DoubleArray(3)
            ColorUtils.colorToLAB(androidAccent, lab)
            val index = iosAccentPalette.indices.minBy { ColorUtils.distanceEuclidean(lab, iosAccentLab[it]) }
            return iosAccentPalette[index]
        }

        private fun iosGlassColors(context: Context, hasBorders: Boolean, themeName: String): Colors {
            val tinted = themeName == THEME_IOS_TINTED
            val hasNativeBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !helium314.keyboard.latin.FrostedGlassHelper.isKnownFrostedGlassBlurUnsupportedDevice()
            val systemAccent = if (tinted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ContextCompat.getColor(context, android.R.color.system_accent1_500) else null
            val searchAccent = systemAccent?.let(::nearestAppleAccent) ?: IOS_BLUE
            // Keep the renderer's base tint white and the keycaps neutral in both presets.
            val whiteGlass = ColorUtils.setAlphaComponent(Color.WHITE, iosGlassAlpha(themeName))
            val background = if (hasNativeBlur) whiteGlass else 0xffeeeeef.toInt()
            return DefaultColors(
                STYLE_MATERIAL, hasBorders, 0xff77777b.toInt(), background,
                Color.WHITE, 0xffdddddf.toInt(), Color.WHITE, 0xff202124.toInt(), 0xff55555a.toInt(),
                isFrosted = true,
                specialKeyBackground = 0xffdddddf.toInt(),
                enterKeyBackground = 0xffdddddf.toInt(),
                iosSearchKeyAccent = searchAccent,
                iosSystemAccent = systemAccent,
            )
        }

        fun iosGlassAlpha(themeName: String): Int = if (themeName == THEME_IOS_TINTED) 205 else 150

        private fun getThemeColors(themeName: String, themeStyle: String, context: Context, prefs: SharedPreferences, isNightParam: Boolean): Colors {
            val isNight = isNightParam
            val hasBorders = prefs.getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
            val backgroundImage = Settings.readUserBackgroundImage(context, isNight)
            return when (themeName) {
                THEME_DYNAMIC -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) DynamicColors(context, themeStyle, hasBorders, backgroundImage)
                    else getThemeColors(THEME_LIGHT, themeStyle, context, prefs, isNight)
                }
                THEME_IOS_CLEAR, THEME_IOS_TINTED -> iosGlassColors(context, hasBorders, themeName)
                THEME_FROSTED_GLASS, THEME_OG_FROST_TINTED -> {
                    val ogTinted = prefs.getBoolean(Settings.PREF_THEME_ACTION_TINT, Defaults.PREF_THEME_ACTION_TINT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val keyTransparency = livePreviewValues?.keyTransparency
                            ?: (if (isNight) prefs.getInt(Settings.PREF_FROSTED_KEY_TRANSPARENCY_NIGHT, Defaults.PREF_FROSTED_KEY_TRANSPARENCY_NIGHT)
                                else prefs.getInt(Settings.PREF_FROSTED_KEY_TRANSPARENCY, Defaults.PREF_FROSTED_KEY_TRANSPARENCY))
                        val colorBlendVal = (livePreviewValues?.colorBlend
                            ?: (if (isNight) prefs.getInt(Settings.PREF_FROSTED_COLOR_BLEND_NIGHT, Defaults.PREF_FROSTED_COLOR_BLEND_NIGHT)
                                else prefs.getInt(Settings.PREF_FROSTED_COLOR_BLEND, Defaults.PREF_FROSTED_COLOR_BLEND))) / 100f
                        val saturationMult = (livePreviewValues?.saturation
                            ?: (if (isNight) prefs.getInt(Settings.PREF_FROSTED_SATURATION_NIGHT, Defaults.PREF_FROSTED_SATURATION_NIGHT)
                                else prefs.getInt(Settings.PREF_FROSTED_SATURATION, Defaults.PREF_FROSTED_SATURATION))) / 100f
                        val bgTransparency = livePreviewValues?.bgTransparency
                            ?: (if (isNight) prefs.getInt(Settings.PREF_FROSTED_BG_TRANSPARENCY_NIGHT, Defaults.PREF_FROSTED_BG_TRANSPARENCY_NIGHT)
                                else prefs.getInt(Settings.PREF_FROSTED_BG_TRANSPARENCY, Defaults.PREF_FROSTED_BG_TRANSPARENCY))
                        val specialVibrancyPref = livePreviewValues?.specialVibrancy
                            ?: (if (isNight) prefs.getInt(Settings.PREF_FROSTED_SPECIAL_VIBRANCY_NIGHT, Defaults.PREF_FROSTED_SPECIAL_VIBRANCY_NIGHT)
                                else prefs.getInt(Settings.PREF_FROSTED_SPECIAL_VIBRANCY, Defaults.PREF_FROSTED_SPECIAL_VIBRANCY))
                        val specialVibrancyVal = specialVibrancyPref / 100f
                        val alphabetVibrancyVal = (livePreviewValues?.alphabetVibrancy
                            ?: (if (isNight) prefs.getInt(Settings.PREF_FROSTED_ALPHABET_VIBRANCY_NIGHT, Defaults.PREF_FROSTED_ALPHABET_VIBRANCY_NIGHT)
                                else prefs.getInt(Settings.PREF_FROSTED_ALPHABET_VIBRANCY, Defaults.PREF_FROSTED_ALPHABET_VIBRANCY))) / 100f

                        val boostSaturation = { color: Int ->
                            val alpha = android.graphics.Color.alpha(color)
                            val hsl = FloatArray(3)
                            ColorUtils.colorToHSL(color, hsl)
                            hsl[1] = (hsl[1] * saturationMult).coerceIn(0f, 1f)
                            val saturatedColor = ColorUtils.HSLToColor(hsl)
                            ColorUtils.setAlphaComponent(saturatedColor, alpha)
                        }
                        val boostSpecialSaturation = { color: Int ->
                            val alpha = android.graphics.Color.alpha(color)
                            val hsl = FloatArray(3)
                            ColorUtils.colorToHSL(color, hsl)
                            hsl[1] = (hsl[1] * specialVibrancyVal).coerceIn(0f, 1f)
                            val saturatedColor = ColorUtils.HSLToColor(hsl)
                            ColorUtils.setAlphaComponent(saturatedColor, alpha)
                        }
                        val boostAlphabetSaturation = { color: Int ->
                            val alpha = android.graphics.Color.alpha(color)
                            val hsl = FloatArray(3)
                            ColorUtils.colorToHSL(color, hsl)
                            hsl[1] = (hsl[1] * alphabetVibrancyVal).coerceIn(0f, 1f)
                            val saturatedColor = ColorUtils.HSLToColor(hsl)
                            ColorUtils.setAlphaComponent(saturatedColor, alpha)
                        }
                        val actionAlpha = (15 + (keyTransparency / 255f * 240)).toInt().coerceIn(0, 255)
                        // Keep automatic OG surfaces neutral; wallpaper color is reserved for keys that act.
                        val systemAccentColor = if (isNight) ContextCompat.getColor(context, android.R.color.system_accent1_100)
                            else ContextCompat.getColor(context, android.R.color.system_accent1_200)
                        val systemNeutralColor = if (isNight) ContextCompat.getColor(context, android.R.color.system_neutral1_900)
                            else ContextCompat.getColor(context, android.R.color.system_neutral1_50)
                        val wallpaperAccent = if (ogTinted)
                            ContextCompat.getColor(context, android.R.color.system_accent1_500) else null
                        val appleActionAccent = wallpaperAccent?.let(::nearestAppleAccent)
                            ?: if (ogTinted) IOS_BLUE else 0
                        val neutralActionColor = if (isNight)
                            ContextCompat.getColor(context, android.R.color.system_neutral1_700)
                        else ContextCompat.getColor(context, android.R.color.system_neutral1_100)
                        val actionColor = if (ogTinted) appleActionAccent else neutralActionColor
                        val accent = ColorUtils.setAlphaComponent(actionColor, actionAlpha)
                        val bgBlendRatio = if (isNight) (colorBlendVal - 0.1f).coerceIn(0f, 1f)
                            else (colorBlendVal + 0.1f).coerceIn(0f, 1f)
                        val baseBg = if (isNight) {
                            val neutralTarget = ContextCompat.getColor(context, android.R.color.system_neutral1_800)
                            ColorUtils.blendARGB(systemNeutralColor, neutralTarget, bgBlendRatio)
                        } else {
                            val neutralTarget = ContextCompat.getColor(context, android.R.color.system_neutral1_100)
                            ColorUtils.blendARGB(systemNeutralColor, neutralTarget, bgBlendRatio)
                        }
                        val bgAlpha = if (isNight) bgTransparency else (bgTransparency * 0.8f).toInt().coerceAtMost(255)
                        val background = boostSaturation(ColorUtils.setAlphaComponent(baseBg, bgAlpha))
                        val keyBgBase = if (isNight) {
                            val baseColor = ContextCompat.getColor(context, android.R.color.system_neutral1_800)
                            val neutralColor = ContextCompat.getColor(context, android.R.color.system_neutral1_700)
                            val finalBlendRatio = (colorBlendVal * 0.7f * alphabetVibrancyVal).coerceIn(0f, 1f)
                            ColorUtils.blendARGB(baseColor, neutralColor, finalBlendRatio)
                        } else {
                            val baseColor = ContextCompat.getColor(context, android.R.color.system_neutral1_0)
                            val neutralColor = ContextCompat.getColor(context, android.R.color.system_neutral1_100)
                            val finalBlendRatio = (colorBlendVal * 0.6f * alphabetVibrancyVal).coerceIn(0f, 1f)
                            ColorUtils.blendARGB(baseColor, neutralColor, finalBlendRatio)
                        }

                        val keyAlpha = (10 + (keyTransparency / 255f * 245)).toInt().coerceIn(0, 255)
                        val keyBackground = boostAlphabetSaturation(ColorUtils.setAlphaComponent(keyBgBase, keyAlpha))
                        val functionalBase = if (ogTinted) appleActionAccent else if (isNight)
                            ContextCompat.getColor(context, android.R.color.system_neutral1_700)
                        else ContextCompat.getColor(context, android.R.color.system_neutral1_100)
                        val functionalKey = ColorUtils.setAlphaComponent(functionalBase, actionAlpha)

                        val neutralSpecialBase = if (isNight) {
                            val baseColor = ContextCompat.getColor(context, android.R.color.system_neutral1_800)
                            val neutralColor = ContextCompat.getColor(context, android.R.color.system_neutral1_700)
                            val finalBlendRatio = (colorBlendVal * 0.7f * specialVibrancyVal).coerceIn(0f, 1f)
                            ColorUtils.blendARGB(baseColor, neutralColor, finalBlendRatio)
                        } else {
                            val baseColor = ContextCompat.getColor(context, android.R.color.system_neutral1_0)
                            val neutralColor = ContextCompat.getColor(context, android.R.color.system_neutral1_100)
                            val finalBlendRatio = (colorBlendVal * 0.6f * specialVibrancyVal).coerceIn(0f, 1f)
                            ColorUtils.blendARGB(baseColor, neutralColor, finalBlendRatio)
                        }
                        val specialBgBase = if (ogTinted) appleActionAccent else neutralSpecialBase
                        val specialKeyBackground = boostSpecialSaturation(ColorUtils.setAlphaComponent(specialBgBase, keyAlpha))
                        val enterKeyBackground = specialKeyBackground
                        val ogFrostKeyAccent = if (ogTinted) appleActionAccent else 0

                        val keyText = if (isNight) ContextCompat.getColor(context, android.R.color.system_neutral1_50)
                            else ContextCompat.getColor(context, android.R.color.system_accent3_900)
                        val keyHintText = if (isNight) keyText
                            else ContextCompat.getColor(context, android.R.color.system_accent3_700)
                        
                        DefaultColors(
                            themeStyle,
                            hasBorders,
                            accent,
                            background,
                            keyBackground,
                            functionalKey,
                            keyBackground,
                            keyText,
                            keyHintText,
                            keyboardBackground = backgroundImage,
                            isFrosted = true,
                            keyBorderColor = null,
                            specialKeyBackground = specialKeyBackground,
                            enterKeyBackground = enterKeyBackground,
                            systemAccent = systemAccentColor,
                            systemNeutral = systemNeutralColor,
                            ogFrostKeyAccent = ogFrostKeyAccent,
                            ogFrostSystemAccent = wallpaperAccent
                        )
                    } else {
                        val fallback = getThemeColors(
                            if (isNight) THEME_DARK else THEME_LIGHT, themeStyle, context, prefs, isNight
                        )
                        if (ogTinted) OgFrostAccentColors(fallback, IOS_BLUE) else fallback
                    }
                }
                THEME_LIGHT -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    ContextCompat.getColor(context, R.color.gesture_trail_color_lxx_light),
                    ContextCompat.getColor(context, R.color.keyboard_background_lxx_light_border),
                    ContextCompat.getColor(context, R.color.key_background_normal_lxx_light_border),
                    ContextCompat.getColor(context, R.color.key_background_functional_lxx_light_border),
                    ContextCompat.getColor(context, R.color.key_background_normal_lxx_light_border),
                    ContextCompat.getColor(context, R.color.key_text_color_lxx_light),
                    ContextCompat.getColor(context, R.color.key_hint_letter_color_lxx_light),
                    keyboardBackground = backgroundImage
                )
                THEME_DARK -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    ContextCompat.getColor(context, R.color.gesture_trail_color_lxx_dark),
                    "#263238".toColorInt(),
                    "#364248".toColorInt(),
                    "#2d393f".toColorInt(),
                    "#364248".toColorInt(),
                    ContextCompat.getColor(context, R.color.key_text_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.key_hint_letter_color_lxx_dark),
                    keyboardBackground = backgroundImage
                )
                THEME_HOLO_WHITE -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.WHITE,
                    "#282828".toColorInt(),
                    Color.WHITE, // drawable is transparent
                    "#444444".toColorInt(), // should be 222222, but the key drawable is already grey
                    Color.WHITE,
                    Color.WHITE,
                    "#282828".toColorInt(),
                    Color.WHITE,
                    "#80FFFFFF".toColorInt(),
                    keyboardBackground = backgroundImage
                )
                THEME_DARKER -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    ContextCompat.getColor(context, R.color.gesture_trail_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.keyboard_background_lxx_dark_border),
                    ContextCompat.getColor(context, R.color.key_background_normal_lxx_dark_border),
                    ContextCompat.getColor(context, R.color.key_background_functional_lxx_dark_border),
                    ContextCompat.getColor(context, R.color.key_background_normal_lxx_dark_border),
                    ContextCompat.getColor(context, R.color.key_text_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.key_hint_letter_color_lxx_dark),
                    keyboardBackground = backgroundImage
                )
                THEME_BLACK -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    ContextCompat.getColor(context, R.color.gesture_trail_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.background_amoled_black),
                    ContextCompat.getColor(context, R.color.background_amoled_dark),
                    ContextCompat.getColor(context, R.color.background_amoled_dark),
                    ContextCompat.getColor(context, R.color.background_amoled_dark),
                    ContextCompat.getColor(context, R.color.key_text_color_lxx_dark),
                    ContextCompat.getColor(context, R.color.key_hint_letter_color_lxx_dark),
                    keyboardBackground = backgroundImage
                )
                THEME_BLUE_GRAY -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(120, 144, 156),
                    Color.rgb(236, 239, 241),
                    Color.WHITE,
                    Color.rgb(207, 216, 220),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_BROWN -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(141, 110, 99),
                    Color.rgb(239, 235, 233),
                    Color.WHITE,
                    Color.rgb(215, 204, 200),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_CHOCOLATE -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(80, 128, 255),
                    Color.rgb(140, 112, 94),
                    Color.rgb(193, 163, 146),
                    Color.rgb(168, 127, 103),
                    Color.rgb(193, 163, 146),
                    Color.WHITE,
                    Color.WHITE,
                    keyboardBackground = backgroundImage
                )
                THEME_CLOUDY -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(255, 113, 129),
                    Color.rgb(81, 97, 113),
                    Color.rgb(117, 128, 142),
                    Color.rgb(99, 109, 121),
                    Color.rgb(117, 128, 142),
                    Color.WHITE,
                    Color.WHITE,
                    keyboardBackground = backgroundImage
                )
                THEME_FOREST -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(75, 110, 75),
                    Color.rgb(181, 125, 88),
                    Color.rgb(228, 212, 191),
                    Color.rgb(212, 186, 153),
                    Color.rgb(228, 212, 191),
                    Color.rgb(0, 50, 0),
                    Color.rgb(0, 50, 0),
                    Color.rgb(0, 50, 0),
                    Color.rgb(0, 80, 0),
                    keyboardBackground = backgroundImage
                )
                THEME_INDIGO -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(92, 107, 192),
                    Color.rgb(232, 234, 246),
                    Color.WHITE,
                    Color.rgb(197, 202, 233),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_OCEAN -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(255, 124, 0),
                    Color.rgb(89, 109, 155),
                    Color.rgb(132, 157, 212),
                    Color.rgb(81, 116, 194),
                    Color.rgb(132, 157, 212),
                    Color.WHITE,
                    Color.WHITE,
                    keyboardBackground = backgroundImage
                )
                THEME_PINK -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(236, 64, 122),
                    Color.rgb(252, 228, 236),
                    Color.WHITE,
                    Color.rgb(248, 187, 208),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_SAND -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(110, 155, 255),
                    Color.rgb(242, 232, 218),
                    Color.WHITE,
                    Color.rgb(234, 211, 185),
                    Color.WHITE,
                    Color.BLACK,
                    Color.BLACK,
                    keyboardBackground = backgroundImage
                )
                THEME_VIOLETTE -> DefaultColors(
                    themeStyle,
                    hasBorders,
                    Color.rgb(255, 96, 255),
                    Color.rgb(112, 112, 174),
                    Color.rgb(150, 150, 216),
                    Color.rgb(123, 123, 206),
                    Color.rgb(150, 150, 216),
                    Color.WHITE,
                    Color.WHITE,
                    keyboardBackground = backgroundImage
                )
                else -> { // user-defined theme
                    val colorSettings = readUserColors(prefs, themeName)
                    val colors = readUserColorTheme(themeStyle, hasBorders, colorSettings, context, isNight, backgroundImage)
                    if (readUserMoreColors(prefs, themeName) == 2)
                        AllColors(readUserAllColors(prefs, themeName, colors), themeStyle, hasBorders, backgroundImage)
                    else {
                        colors
                    }
                }
            }
        }

        fun readUserColorTheme(themeStyle: String, hasBorders: Boolean, colorSettings: List<ColorSetting>, context: Context, isNight: Boolean, backgroundImage: Drawable?): Colors {
            return DefaultColors(
                themeStyle,
                hasBorders,
                determineUserColor(colorSettings, context, COLOR_ACCENT, isNight),
                determineUserColor(colorSettings, context, COLOR_BACKGROUND, isNight),
                determineUserColor(colorSettings, context, COLOR_KEYS, isNight),
                determineUserColor(colorSettings, context, COLOR_FUNCTIONAL_KEYS, isNight),
                determineUserColor(colorSettings, context, COLOR_SPACEBAR, isNight),
                determineUserColor(colorSettings, context, COLOR_TEXT, isNight),
                determineUserColor(colorSettings, context, COLOR_HINT_TEXT, isNight),
                determineUserColor(colorSettings, context, COLOR_SUGGESTION_TEXT, isNight),
                determineUserColor(colorSettings, context, COLOR_SPACEBAR_TEXT, isNight),
                determineUserColor(colorSettings, context, COLOR_GESTURE, isNight),
                backgroundImage,
            )
        }

        fun writeUserColors(prefs: SharedPreferences, themeName: String, colors: List<ColorSetting>) {
            val key = Settings.PREF_USER_COLORS_PREFIX + themeName
            val value = Json.encodeToString(colors.filter { it.color != null || it.auto == false })
            prefs.edit { putString(key, value) }
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }

        fun readUserColors(prefs: SharedPreferences, themeName: String): List<ColorSetting> {
            val key = Settings.PREF_USER_COLORS_PREFIX + themeName
            return Json.decodeFromString(prefs.getString(key, Defaults.PREF_USER_COLORS)!!)
        }

        fun writeUserMoreColors(prefs: SharedPreferences, themeName: String, value: Int) {
            val key = Settings.PREF_USER_MORE_COLORS_PREFIX + themeName
            prefs.edit { putInt(key, value) }
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }

        fun readUserMoreColors(prefs: SharedPreferences, themeName: String): Int {
            val key = Settings.PREF_USER_MORE_COLORS_PREFIX + themeName
            return prefs.getInt(key, Defaults.PREF_USER_MORE_COLORS)
        }

        fun writeUserAllColors(prefs: SharedPreferences, themeName: String, colorMap: EnumMap<ColorType, Int>) {
            val key = Settings.PREF_USER_ALL_COLORS_PREFIX + themeName
            prefs.edit { putString(key, colorMap.map { "${it.key},${it.value}" }.joinToString(";")) }
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }

        fun readUserAllColors(prefs: SharedPreferences, themeName: String, fallback: Colors?): EnumMap<ColorType, Int> {
            val key = Settings.PREF_USER_ALL_COLORS_PREFIX + themeName
            val colorsString = prefs.getString(key, Defaults.PREF_USER_ALL_COLORS)!!
            val colorMap = EnumMap<ColorType, Int>(ColorType::class.java)
            colorsString.split(";").forEach {
                val ct = try {
                    ColorType.valueOf(it.substringBefore(",").uppercase())
                } catch (_: IllegalArgumentException) {
                    return@forEach
                }
                val i = it.substringAfter(",").toIntOrNull() ?: return@forEach
                colorMap[ct] = i
            }
            if (fallback != null && colorMap.size < ColorType.entries.size) {
                ColorType.entries.forEach {
                    if (it in colorMap) return@forEach
                    colorMap[it] = fallback.get(it)
                }
            }
            return colorMap
        }

        fun getUnusedThemeName(initialName: String, prefs: SharedPreferences): String {
            val existingNames = getExistingThemeNames(prefs)
            if (initialName !in existingNames) return initialName
            var i = 1
            while ("$initialName$i" in existingNames)
                i++
            return "$initialName$i"
        }

        private fun getExistingThemeNames(prefs: SharedPreferences) =
            prefs.all.keys.mapNotNull {
                when {
                    it.startsWith(Settings.PREF_USER_COLORS_PREFIX) -> it.substringAfter(Settings.PREF_USER_COLORS_PREFIX)
                    it.startsWith(Settings.PREF_USER_ALL_COLORS_PREFIX) -> it.substringAfter(Settings.PREF_USER_ALL_COLORS_PREFIX)
                    it.startsWith(Settings.PREF_USER_MORE_COLORS_PREFIX) -> it.substringAfter(Settings.PREF_USER_MORE_COLORS_PREFIX)
                    else -> null
                }
            }.toSortedSet()

        // returns false if not renamed due to invalid name or collision
        fun renameUserColors(from: String, to: String, prefs: SharedPreferences): Boolean {
            if (to.isBlank()) return false // don't want that
            if (to == from) return true // nothing to do
            val existingNames = getExistingThemeNames(prefs)
            if (to in existingNames) return false
            // all good, now rename
            prefs.edit {
                if (prefs.contains(Settings.PREF_USER_COLORS_PREFIX + from)) {
                    putString(Settings.PREF_USER_COLORS_PREFIX + to, prefs.getString(Settings.PREF_USER_COLORS_PREFIX + from, ""))
                    remove(Settings.PREF_USER_COLORS_PREFIX + from)
                }
                if (prefs.contains(Settings.PREF_USER_ALL_COLORS_PREFIX + from)) {
                    putString(Settings.PREF_USER_ALL_COLORS_PREFIX + to, prefs.getString(Settings.PREF_USER_ALL_COLORS_PREFIX + from, ""))
                    remove(Settings.PREF_USER_ALL_COLORS_PREFIX + from)
                }
                if (prefs.contains(Settings.PREF_USER_MORE_COLORS_PREFIX + from)) {
                    putInt(Settings.PREF_USER_MORE_COLORS_PREFIX + to, prefs.getInt(Settings.PREF_USER_MORE_COLORS_PREFIX + from, 0))
                    remove(Settings.PREF_USER_MORE_COLORS_PREFIX + from)
                }
                if (prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS) == from)
                    putString(Settings.PREF_THEME_COLORS, to)
                if (prefs.getString(Settings.PREF_THEME_COLORS_NIGHT, Defaults.PREF_THEME_COLORS_NIGHT) == from)
                    putString(Settings.PREF_THEME_COLORS_NIGHT, to)
            }
            return true
        }

        fun determineUserColor(colors: List<ColorSetting>, context: Context, colorName: String, isNight: Boolean): Int {
            val c = colors.firstOrNull { it.name == colorName }
            val color = c?.color
            val auto = c?.auto ?: true
            return if (auto || color == null)
                determineAutoColor(colors, colorName, isNight, context)
            else color
        }

        private fun determineAutoColor(colors: List<ColorSetting>, colorName: String, isNight: Boolean, context: Context): Int {
            when (colorName) {
                COLOR_ACCENT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        // try determining accent color on Android 10 & 11, accent is not available in resources
                        val wrapper: Context = ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault)
                        val value = TypedValue()
                        if (wrapper.theme.resolveAttribute(android.R.attr.colorAccent, value, true)) return value.data
                    }
                    return ContextCompat.getColor(Settings.getDayNightContext(context, isNight), R.color.accent)
                }
                COLOR_GESTURE -> return determineUserColor(colors, context, COLOR_ACCENT, isNight)
                COLOR_SUGGESTION_TEXT ->
                    return determineUserColor(colors, context, COLOR_TEXT, isNight)
                COLOR_TEXT -> {
                    // base it on background color, and not key, because it's also used for suggestions
                    val background = determineUserColor(colors, context, COLOR_BACKGROUND, isNight)
                    return if (isBrightColor(background)) {
                        // but if key borders are enabled, we still want reasonable contrast
                        if (!context.prefs().getBoolean(Settings.PREF_THEME_KEY_BORDERS, Defaults.PREF_THEME_KEY_BORDERS)
                            || isGoodContrast(Color.BLACK, determineUserColor(colors, context, COLOR_KEYS, isNight))
                        ) Color.BLACK
                        else Color.GRAY
                    } else Color.WHITE
                }
                COLOR_HINT_TEXT -> {
                    return if (isBrightColor(determineUserColor(colors, context, COLOR_KEYS, isNight))) Color.DKGRAY
                    else determineUserColor(colors, context, COLOR_TEXT, isNight)
                }
                COLOR_KEYS ->
                    return brightenOrDarken(determineUserColor(colors, context, COLOR_BACKGROUND, isNight), isNight)
                COLOR_FUNCTIONAL_KEYS ->
                    return brightenOrDarken(determineUserColor(colors, context, COLOR_KEYS, isNight), true)
                COLOR_SPACEBAR -> return determineUserColor(colors, context, COLOR_KEYS, isNight)
                COLOR_SPACEBAR_TEXT -> {
                    val spacebar = determineUserColor(colors, context, COLOR_SPACEBAR, isNight)
                    val hintText = determineUserColor(colors, context, COLOR_HINT_TEXT, isNight)
                    if (isGoodContrast(hintText, spacebar)) return hintText and -0x7f000001 // add some transparency
                    val text = determineUserColor(colors, context, COLOR_TEXT, isNight)
                    if (isGoodContrast(text, spacebar)) return text and -0x7f000001
                    return if (isBrightColor(spacebar)) Color.BLACK and -0x7f000001
                    else Color.WHITE and -0x7f000001
                }
                COLOR_BACKGROUND -> return ContextCompat.getColor(
                    Settings.getDayNightContext(context, isNight),
                    R.color.keyboard_background
                )
                else -> return ContextCompat.getColor(Settings.getDayNightContext(context, isNight), R.color.keyboard_background)
            }
        }
    }
}

@Serializable
data class ColorSetting(val name: String, val auto: Boolean?, val color: Int?) {
    var displayName = name
}
