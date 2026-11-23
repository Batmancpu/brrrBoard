// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import android.content.Context;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import helium314.keyboard.keyboard.emoji.BrrSmartEmojiEngine;
import helium314.keyboard.keyboard.emoji.EmojiContextEligibility;
import helium314.keyboard.latin.LatinIME;
import helium314.keyboard.latin.NgramContext;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.settings.SettingsValues;

/**
 * The single policy/coordination entry point for on-device suggestion features.
 *
 * <p>The native decoder remains the immediate candidate producer. Emoji and
 * confirmed-swipe components remain small domain adapters, but callers use this
 * coordinator for privacy eligibility, learning, and any optional reranking.
 * The rejected ContextLM candidate stays opt-in and this class never changes
 * autocorrection or blocks the key-input path on model work.
 */
public final class BrSuggestionBrain {
    private BrSuggestionBrain() { }

    /** Warm the reviewed Emoji index outside the per-key ranking path. */
    public static void warmEmojiIndex(@NonNull final Context context) {
        BrrSmartEmojiEngine.warm(context);
    }

    /** Shared fail-closed editor gate for personal Brain features. */
    public static boolean isEligible(@Nullable final LatinIME ime,
            @Nullable final EditorInfo editor, @Nullable final SettingsValues values) {
        if (ime == null || editor == null || values == null
                || values.mInputAttributes == null
                || !isPrivacyEligible(editor, values.mIncognitoModeEnabled,
                        values.mInputAttributes.mNoLearning,
                        values.mInputAttributes.mIsGeneralTextInput)) {
            return false;
        }
        // SettingsValues already incorporates the package-exact secure-app policy
        // and the user's one-editor temporary opt-out. Rechecking the raw package
        // list here would incorrectly block that deliberate override.
        return true;
    }

    /** Pure editor policy, kept testable without constructing the IME service. */
    public static boolean isPrivacyEligible(@Nullable final EditorInfo editor,
            final boolean incognito, final boolean noLearning,
            final boolean generalTextInput) {
        return generalTextInput && !noLearning
                && EmojiContextEligibility.isEligible(editor, incognito);
    }

    /** Existing Smart Emoji scorer, reached through the Brain policy boundary. */
    @NonNull public static List<BrrSmartEmojiEngine.Pick> suggestEmoji(
            @NonNull final Context context, @Nullable final LatinIME ime,
            @Nullable final EditorInfo editor, @Nullable final SettingsValues values,
            @Nullable final String transientContext, final int limit,
            final boolean includeRecents) {
        if (!isEligible(ime, editor, values) || transientContext == null
                || transientContext.trim().isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        return BrrSmartEmojiEngine.suggest(context, editor, transientContext, limit,
                includeRecents);
    }

    /** A committed Emoji choice is the only event forwarded to Emoji learning. */
    public static void learnEmoji(@NonNull final Context context,
            @Nullable final LatinIME ime, @Nullable final EditorInfo editor,
            @Nullable final SettingsValues values, @Nullable final CharSequence precedingText,
            @Nullable final String emoji) {
        if (!isEligible(ime, editor, values) || emoji == null || emoji.isEmpty()) return;
        BrrSmartEmojiEngine.learn(context, editor, precedingText, emoji);
    }

    /** Reuse the exact-editor snapshot produced during typing without editor IPC. */
    @NonNull public static List<BrrSmartEmojiEngine.Pick> picksFromSnapshot(
            @Nullable final LatinIME ime, @Nullable final EditorInfo editor,
            @Nullable final SettingsValues values,
            @NonNull final List<BrrSmartEmojiEngine.Pick> candidates,
            final int maxPicks) {
        if (!isEligible(ime, editor, values) || maxPicks <= 0) return Collections.emptyList();
        return BrrSmartEmojiEngine.picksFromSnapshot(candidates, maxPicks);
    }

    /** The native user-history dictionary already participated before this call. */
    @NonNull public static List<SuggestedWordInfo> rerankManualSuggestions(
            @NonNull final LatinIME ime, @Nullable final EditorInfo editor,
            @Nullable final SettingsValues values, @NonNull final NgramContext context,
            @NonNull final List<SuggestedWordInfo> nativeSuggestions,
            final long editorGeneration) {
        if (!isEligible(ime, editor, values)) return nativeSuggestions;
        return BrContextLmExperiment.rerankManualSuggestions(
                ime, context, nativeSuggestions, editorGeneration, true);
    }

    /** Returns the existing confirmed-swipe adapter only for an eligible editor. */
    @Nullable public static SwipeCorrectionLearner swipeLearnerIfAllowed(
            @Nullable final LatinIME ime, @Nullable final EditorInfo editor,
            @Nullable final SettingsValues values) {
        if (!isEligible(ime, editor, values)
                || !SwipeCorrectionLearner.isAllowed(ime, editor,
                        values.mIncognitoModeEnabled,
                        values.isSuggestionsEnabledPerUserSettings())) {
            return null;
        }
        return SwipeCorrectionLearner.get(ime);
    }
}
