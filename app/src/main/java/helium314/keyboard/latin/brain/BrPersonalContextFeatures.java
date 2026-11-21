// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import androidx.annotation.NonNull;

import helium314.keyboard.latin.NgramContext;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Ephemeral, read-only candidate features for an eventual on-device ContextLM.
 *
 * <p>Crucially, the existing user-history dictionary is consulted by the native suggestion
 * engine first. The learned candidate's source and native score become <em>model inputs</em>,
 * rather than an unrelated post-inference boost. No dictionary bytes are enumerated, copied,
 * changed, logged or persisted by this class.
 *
 * <p>This is an input contract, NOT a trained model or a change to displayed suggestions.
 */
public final class BrPersonalContextFeatures {
    public static final int MAX_CANDIDATES = 12;
    public static final int MAX_CONTEXT_TOKENS = 8;
    public static final int MAX_WORD_LENGTH = 48;

    private BrPersonalContextFeatures() { }

    public enum Source { NATIVE, LEARNED_HISTORY, PERSONAL_DICTIONARY }

    public static final class Candidate {
        @NonNull public final String word;
        @NonNull public final Source source;
        /** Normalized and clipped native probability signal, passed directly to the model. */
        public final float nativeScore;
        /** 1.0 for the first native result; 1/(rank+1) for subsequent results. */
        public final float nativeRank;
        /** Set only for source-specific, existing user-history matches. */
        public final float learnedHistory;
        /** Set only for an explicitly user-controlled personal dictionary. */
        public final float personalWord;

        private Candidate(final String word, final Source source, final int nativeScore,
                          final int rank) {
            this.word = word;
            this.source = source;
            // Native scores have several special sentinel values. Never pass huge or NaN values
            // into a quantized ranker, and do not mistake MAX_SCORE (typed literal) for context.
            this.nativeScore = Math.max(0, Math.min(255, nativeScore)) / 255f;
            this.nativeRank = 1f / (rank + 1);
            this.learnedHistory = source == Source.LEARNED_HISTORY ? 1f : 0f;
            this.personalWord = source == Source.PERSONAL_DICTIONARY ? 1f : 0f;
        }

        /** Stable feature order for the trained candidate-scoring head. */
        @NonNull public float[] numericModelInputs() {
            return new float[] {nativeScore, nativeRank, learnedHistory, personalWord};
        }
    }

    public static final class Snapshot {
        @NonNull public final List<String> previousWords;
        @NonNull public final List<Candidate> candidates;
        public final boolean hasInheritedPersonalSignals;

        private Snapshot(final List<String> previousWords, final List<Candidate> candidates,
                         final boolean hasInheritedPersonalSignals) {
            this.previousWords = Collections.unmodifiableList(new ArrayList<>(previousWords));
            this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
            this.hasInheritedPersonalSignals = hasInheritedPersonalSignals;
        }
    }

    /**
     * Call only for an already eligible normal-prose editor, after native next-word generation.
     * The arguments must be from the same request/editor generation. No host-editor IPC occurs.
     */
    @NonNull public static Snapshot fromNative(@NonNull final NgramContext context,
                                               @NonNull final List<SuggestedWordInfo> nativeWords,
                                               final boolean inferenceAllowed,
                                               final boolean personalizedDictionariesEnabled) {
        if (!inferenceAllowed) {
            return new Snapshot(Collections.emptyList(), Collections.emptyList(), false);
        }
        final String[] contextTerms = context.extractPrevWordsContextArray();
        final List<String> previous = new ArrayList<>(MAX_CONTEXT_TOKENS);
        for (int index = Math.max(0, contextTerms.length - MAX_CONTEXT_TOKENS);
             index < contextTerms.length; index++) {
            if (isSafeRomanWord(contextTerms[index])) previous.add(contextTerms[index]);
        }

        final List<Candidate> scored = new ArrayList<>(Math.min(nativeWords.size(), MAX_CANDIDATES));
        final Set<String> seenWords = new HashSet<>();
        boolean inherited = false;
        for (int rank = 0; rank < nativeWords.size() && scored.size() < MAX_CANDIDATES; rank++) {
            final SuggestedWordInfo suggestion = nativeWords.get(rank);
            if (suggestion == null || !isSafeRomanWord(suggestion.mWord)
                    || (suggestion.mKindAndFlags & 0xFF) == SuggestedWordInfo.KIND_BLACKLIST
                    || suggestion.mSourceDict == null) continue;
            final Source source = classifySource(suggestion.mSourceDict.mDictType,
                    personalizedDictionariesEnabled);
            if (source == null) continue;
            if (!seenWords.add(suggestion.mWord.toLowerCase(Locale.ROOT))) continue;
            scored.add(new Candidate(suggestion.mWord, source, suggestion.mScore, rank));
            if (source != Source.NATIVE) inherited = true;
        }
        return new Snapshot(previous, scored, inherited);
    }

    /** Null means the candidate must stay solely on the original native path. */
    static Source classifySource(final String dictionaryType,
                                 final boolean personalizedDictionariesEnabled) {
        if (dictionaryType == null) return null;
        switch (dictionaryType) {
            case Dictionary.TYPE_USER_HISTORY:
                return personalizedDictionariesEnabled ? Source.LEARNED_HISTORY : null;
            case Dictionary.TYPE_USER:
            case Dictionary.TYPE_USER_SHORTCUT:
                return personalizedDictionariesEnabled ? Source.PERSONAL_DICTIONARY : null;
            case Dictionary.TYPE_MAIN:
            case Dictionary.TYPE_BRR_PERSONAL:
                return Source.NATIVE;
            // Contacts and app names are not learned-word signals. The native engine may still
            // show them under its own policy; do not feed them to a neural model.
            case Dictionary.TYPE_CONTACTS:
            case Dictionary.TYPE_APPS:
            case Dictionary.TYPE_USER_TYPED:
            case Dictionary.TYPE_APPLICATION_DEFINED:
            case Dictionary.TYPE_HARDCODED:
            case Dictionary.TYPE_RESUMED:
                return null;
            default:
                // Fail closed for future dictionary types, including unknown editor/host
                // sources and Emoji: only reviewed types are legal inference inputs.
                return null;
        }
    }

    static boolean isSafeRomanWord(final String word) {
        if (word == null || word.isEmpty() || word.length() > MAX_WORD_LENGTH) return false;
        boolean hasLetter = false;
        for (int index = 0; index < word.length(); index++) {
            final char ch = word.charAt(index);
            if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
                hasLetter = true;
            } else if ((ch != '\'' && ch != '-') || index == 0 || index == word.length() - 1) {
                return false;
            }
        }
        return hasLetter;
    }
}
