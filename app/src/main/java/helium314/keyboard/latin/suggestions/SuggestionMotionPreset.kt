// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.content.Context
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs

/** Independent suggestion-strip style; never changes decoding or global keyboard motion. */
internal enum class SuggestionMotionPreset(
    val id: String, val glyphMs: Long, val replacementMs: Long,
    val entryScale: Float, val compression: Float, val staggerMs: Long,
) {
    CLASSIC("classic", 167L, 133L, 1f, 0f, 0L),
    SCALE("scale", 145L, 130L, .12f, 1f, 0L),
    SPRING("spring", 195L, 175L, .48f, .40f, 0L),
    RIPPLE("ripple", 150L, 135L, .28f, .72f, 6L);

    companion object {
        fun fromId(id: String?): SuggestionMotionPreset =
            values().firstOrNull { it.id == id } ?: SCALE

        fun selected(context: Context): SuggestionMotionPreset = fromId(
            context.prefs().getString(Settings.PREF_SUGGESTION_MOTION_STYLE,
                Defaults.PREF_SUGGESTION_MOTION_STYLE))
    }
}
