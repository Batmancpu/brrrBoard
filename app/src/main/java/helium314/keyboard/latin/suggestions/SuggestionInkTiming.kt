// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import helium314.keyboard.latin.KeyboardMotionSpec

/** Presentation-only timing; pending correction acceptance is never delayed. */
internal object SuggestionInkTiming {
    fun reveal(elapsedMs: Float): Float {
        val linear = ((elapsedMs - KeyboardMotionSpec.IOS_SUGGESTION_REVEAL_DELAY_MS) /
            KeyboardMotionSpec.IOS_SUGGESTION_REVEAL_MS).coerceIn(0f, 1f)
        // Quick onset followed by a soft landing, as seen in the 30-fps reference.
        val remaining = 1f - linear
        return 1f - remaining * remaining * remaining
    }
}
