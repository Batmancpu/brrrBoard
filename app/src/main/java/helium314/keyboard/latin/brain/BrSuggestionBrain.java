// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

import helium314.keyboard.keyboard.emoji.BrrSmartEmojiEngine;
import helium314.keyboard.keyboard.emoji.EmojiContextEligibility;
import helium314.keyboard.latin.LatinIME;
import helium314.keyboard.latin.NgramContext;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.settings.SettingsValues;
import helium314.keyboard.latin.utils.NoBackupStorageMigration;

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
            @NonNull final List<String> candidates,
            final int maxPicks) {
        if (!isEligible(ime, editor, values) || maxPicks <= 0) return Collections.emptyList();
        return BrrSmartEmojiEngine.picksFromSnapshot(candidates, maxPicks);
    }

    /** Use the current editor receipt and the stricter existing protected-text policy. */
    public static boolean isOfflineTextEligible(@Nullable final LatinIME ime,
            @Nullable final SettingsValues values, final long editorGeneration) {
        return ime != null && values != null && editorGeneration >= 0
                && ime.editorGeneration() == editorGeneration && Settings.getValues() == values
                && values.mInputAttributes.mShouldShowSuggestions
                && !values.isApplicationSpecifiedCompletionsOn()
                && isEligible(ime, ime.getCurrentInputEditorInfo(), values)
                && ime.mayUseTransientCorrectionMemory(values);
    }

    /** The native user-history dictionary already participated before this call. */
    @NonNull public static List<SuggestedWordInfo> rerankManualSuggestions(
            @NonNull final LatinIME ime, @Nullable final EditorInfo editor,
            @Nullable final SettingsValues values, @NonNull final NgramContext context,
            @NonNull final List<SuggestedWordInfo> nativeSuggestions,
            final long editorGeneration) {
        return rerankManualSuggestions(ime, editor, values, context, nativeSuggestions,
                editorGeneration, -1L, null);
    }

    @NonNull public static List<SuggestedWordInfo> rerankManualSuggestions(
            @NonNull final LatinIME ime, @Nullable final EditorInfo editor,
            @Nullable final SettingsValues values, @NonNull final NgramContext context,
            @NonNull final List<SuggestedWordInfo> nativeSuggestions,
            final long editorGeneration, final long requestSerial,
            @Nullable final LongSupplier requestSerialSupplier) {
        // The disabled production path must not acquire new locks or call device services.
        if (!BrContextLmExperiment.isEnabled(ime)) return nativeSuggestions;
        if (!contextLmEditorIsCurrent(ime, editor, values, editorGeneration)) {
            BrContextLmExperiment.clearContextCache();
            return nativeSuggestions;
        }
        if (requestSerialSupplier != null
                && requestSerialSupplier.getAsLong() != requestSerial) return nativeSuggestions;
        final List<SuggestedWordInfo> reranked = BrContextLmExperiment.rerankManualSuggestions(
                ime, context, new ArrayList<>(nativeSuggestions), editorGeneration, true,
                requestSerial, requestSerialSupplier);
        if (!contextLmEditorIsCurrent(ime, editor, values, editorGeneration)) {
            BrContextLmExperiment.clearContextCache();
            return nativeSuggestions;
        }
        if (requestSerialSupplier != null
                && requestSerialSupplier.getAsLong() != requestSerial) return nativeSuggestions;
        return reranked;
    }

    private static boolean contextLmEditorIsCurrent(LatinIME ime, EditorInfo editor,
            SettingsValues values, long generation) {
        return ime.editorGeneration() == generation && ime.getCurrentInputEditorInfo() == editor
                && Settings.getValues() == values && isEligible(ime, editor, values)
                && userUnlocked(ime) && !helium314.keyboard.compat.IsLockedCompatKt.isDeviceLocked(ime);
    }

    /** Returns the existing confirmed-swipe adapter only for an eligible editor. */
    @Nullable public static SwipeCorrectionLearner swipeLearnerIfAllowed(
            @Nullable final LatinIME ime, @Nullable final EditorInfo editor,
            @Nullable final SettingsValues values) {
        if (!isEligible(ime, editor, values) || !userUnlocked(ime)
                || !SwipeCorrectionLearner.isAllowed(ime, editor,
                        values.mIncognitoModeEnabled,
                        values.isSuggestionsEnabledPerUserSettings())
                || !NoBackupStorageMigration.migrateEncryptedRecord(
                        ime, "brr_swipe_corrections_v2.aesgcm")
                || !NoBackupStorageMigration.migrateEncryptedRecord(
                        ime, "brr_swipe_corrections_v1")) {
            return null;
        }
        return SwipeCorrectionLearner.get(ime);
    }

    /** Coordinate touch learning through the same privacy boundary as other Brain adapters. */
    public static boolean adaptiveTouchAllowed(@NonNull LatinIME ime, @NonNull SettingsValues values) {
        return values.mAdaptiveTouchEnabled && values.mUsePersonalizedDicts
                && isEligible(ime,ime.getCurrentInputEditorInfo(),values)
                && ime.mayUseTransientCorrectionMemory(values);
    }

    public static void learnAdaptiveTouch(@NonNull LatinIME ime, @NonNull SettingsValues values,
            long layout, int[] n, double[] x, double[] y, AdaptiveTouchEvidence evidence) {
        if (adaptiveTouchAllowed(ime,values))
            AdaptiveTouchStore.get(ime).learn(layout,n,x,y,evidence);
    }

    private static boolean userUnlocked(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true;
        final UserManager manager = context.getSystemService(UserManager.class);
        return manager != null && manager.isUserUnlocked();
    }
}
