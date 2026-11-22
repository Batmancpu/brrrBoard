// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.animation.ValueAnimator
import android.animation.TimeInterpolator
import android.content.Context
import android.os.Build
import android.view.View
import android.view.animation.PathInterpolator
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.dpToPx
import helium314.keyboard.latin.utils.prefs

/** A single intentionally restrained motion vocabulary for keyboard-owned content. */
object KeyboardMotionSpec {
    const val PROFILE_IOS27 = "ios27"
    const val PROFILE_PREVIOUS_DEFAULT = "previous_default"
    // Suggestion text and tap targets update synchronously. This short visual
    // settle is presentation-only and never holds decoder results or typing.
    const val WORD_CROSSFADE_MS = 126L
    const val SUGGESTION_SIDE_STAGGER_MS = 8L
    const val SUGGESTION_START_ALPHA = .28f
    const val SUGGESTION_START_OFFSET_DP = 2.5f
    const val SUGGESTION_START_SCALE = .985f
    const val SEARCH_RESULT_MS = 170L
    const val SEARCH_PENDING_MS = 90L
    const val FOOTER_ACTION_MS = 150L
    const val VOICE_PULSE_CYCLE_MS = 1_600L
    const val TRACKPAD_FOOTER_EXIT_MS = 220L
    const val TRACKPAD_FOOTER_RETURN_MS = 220L

    // The legacy preset is deliberately immutable: selecting Previous Default must
    // reproduce the prior choreography, not merely recolor the current renderer.
    private val iosEase by lazy { PathInterpolator(.20f, .0f, .20f, 1f) }

    @JvmStatic fun isIos27Profile(profile: String?): Boolean = profile != PROFILE_PREVIOUS_DEFAULT

    @JvmStatic fun isIos27(context: Context): Boolean = isIos27Profile(
        context.prefs().getString(Settings.PREF_MOTION_PROFILE, Defaults.PREF_MOTION_PROFILE))

    @JvmStatic fun easing(context: Context): TimeInterpolator? =
        if (isIos27(context)) iosEase else null

    @JvmStatic fun keyPreviewEnterMillis(context: Context): Long = if (isIos27(context)) 85L else 0L
    @JvmStatic fun keyPreviewExitMillis(context: Context): Long = if (isIos27(context)) 90L else 0L
    @JvmStatic fun keyDownMillis(context: Context): Long = if (isIos27(context)) 50L else 0L
    @JvmStatic fun keyUpMillis(context: Context): Long = if (isIos27(context)) 110L else 0L
    @JvmStatic fun panelTransitionMillis(context: Context): Long = if (isIos27(context)) 180L else 160L
    @JvmStatic fun emojiSearchMillis(context: Context): Long = if (isIos27(context)) 190L else 210L
    @JvmStatic fun emojiHeightMillis(context: Context): Long = if (isIos27(context)) 225L else 260L
    @JvmStatic fun searchResultMillis(context: Context): Long = if (isIos27(context)) 185L else SEARCH_RESULT_MS
    @JvmStatic fun searchPendingMillis(context: Context): Long = if (isIos27(context)) 95L else SEARCH_PENDING_MS

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
        val ios = isIos27(view.context)
        view.alpha = if (ios) 0.86f else 0.72f
        view.translationY = (if (ios) 1 else 2).dpToPx(view.resources).toFloat()
        val animation = view.animate().alpha(1f).translationY(0f)
            .setDuration(searchResultMillis(view.context))
        if (ios) animation.setInterpolator(iosEase)
        animation.start()
    }

    @JvmStatic fun pendingSearchResults(view: View) {
        view.animate().cancel()
        val pendingAlpha = if (isIos27(view.context)) 0.86f else 0.72f
        if (!enabled()) {
            view.alpha = pendingAlpha
            return
        }
        val animation = view.animate().alpha(pendingAlpha).setDuration(searchPendingMillis(view.context))
        easing(view.context)?.let { animation.setInterpolator(it) }
        animation.start()
    }

    @JvmStatic fun reset(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.translationY = 0f
    }
}
