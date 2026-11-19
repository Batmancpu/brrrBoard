// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.animation.ValueAnimator
import android.os.Build
import android.view.View
import helium314.keyboard.latin.utils.dpToPx

/** A single intentionally restrained motion vocabulary for keyboard-owned content. */
object KeyboardMotionSpec {
    const val WORD_CROSSFADE_MS = 150L
    const val SEARCH_RESULT_MS = 170L
    const val SEARCH_PENDING_MS = 90L
    const val FOOTER_ACTION_MS = 150L
    const val VOICE_PULSE_CYCLE_MS = 1_600L

    @JvmStatic fun enabled(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()

    /** Do not animate content replacements that would expose a stale panel or old editor. */
    @JvmStatic fun shouldMorphWords(old: String?, next: String?, sameSurface: Boolean,
            motionEnabled: Boolean): Boolean = sameSurface && motionEnabled &&
        !old.isNullOrBlank() && !next.isNullOrBlank() && old != next

    /** Transitions presentation only; content and its real touch targets are current. */
    @JvmStatic fun revealSearchResults(view: View) {
        view.animate().cancel()
        if (!enabled() || !view.isShown) {
            reset(view)
            return
        }
        view.alpha = 0.72f
        view.translationY = 2.dpToPx(view.resources).toFloat()
        view.animate().alpha(1f).translationY(0f)
            .setDuration(SEARCH_RESULT_MS).start()
    }

    @JvmStatic fun pendingSearchResults(view: View) {
        view.animate().cancel()
        if (!enabled()) {
            view.alpha = 0.72f
            return
        }
        view.animate().alpha(0.72f).setDuration(SEARCH_PENDING_MS).start()
    }

    @JvmStatic fun reset(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.translationY = 0f
    }
}
