// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;

import helium314.keyboard.latin.SuggestedWords;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

/**
 * A no-lookup, zero-network bridge between the key event and the native decoder.
 * Only candidates already provided by this editor's dictionary are carried forward.
 * In particular this must never invent an auto-correction or reuse a candidate
 * that does not start with the user's CURRENT typed prefix.
 */
public final class SuggestionPrefixPreview {
    private SuggestionPrefixPreview() { }

    public static SuggestedWords forTypedPrefix(final SuggestedWords previous,
            final String prefix, final boolean forbidTextCompletion) {
        final SuggestedWordInfo typed = new SuggestedWordInfo(prefix, "",
                SuggestedWordInfo.MAX_SCORE, SuggestedWordInfo.KIND_TYPED,
                Dictionary.DICTIONARY_USER_TYPED, SuggestedWordInfo.NOT_AN_INDEX,
                SuggestedWordInfo.NOT_A_CONFIDENCE);
        final ArrayList<SuggestedWordInfo> infos = new ArrayList<>(4);
        infos.add(typed);
        if (!forbidTextCompletion && previous != null && !previous.isPunctuationSuggestions()
                && previous.mInputStyle != SuggestedWords.INPUT_STYLE_APPLICATION_SPECIFIED) {
            final HashSet<String> seen = new HashSet<>();
            seen.add(prefix.toLowerCase(Locale.ROOT));
            for (int i = 0; i < previous.size() && infos.size() < 4; i++) {
                final SuggestedWordInfo candidate = previous.getInfo(i);
                final String word = candidate.mWord;
                if (candidate.isEmoji() || candidate.isKindOf(SuggestedWordInfo.KIND_TYPED)
                        || candidate.isKindOf(SuggestedWordInfo.KIND_SESSION_RECALL)
                        || candidate.isKindOf(SuggestedWordInfo.KIND_APP_DEFINED)
                        || word.length() <= prefix.length()
                        || !word.regionMatches(true, 0, prefix, 0, prefix.length())
                        || !seen.add(word.toLowerCase(Locale.ROOT))) continue;
                infos.add(candidate);
            }
        }
        // These are suggestions, not a decoder's new confidence decision.
        return new SuggestedWords(infos, null, typed, false, false, false,
                SuggestedWords.INPUT_STYLE_TYPING, SuggestedWords.NOT_A_SEQUENCE_NUMBER);
    }
}
