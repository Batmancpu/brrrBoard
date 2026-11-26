/*
 * Copyright (C) 2026
 * SPDX-License-Identifier: GPL-3.0-only
 */

package helium314.keyboard.latin.inputlogic;

import helium314.keyboard.latin.LastComposedWord;

/** Decides whether a committed composition is confirmed language-preference evidence. */
final class LanguageConfidenceEvidencePolicy {
    private LanguageConfidenceEvidencePolicy() {}

    static boolean shouldUse(final int commitType, final String chosenWord,
            final String typedWord) {
        // Manual choices and literal commits are direct user intent. A decided word only
        // provides evidence when it preserved the exact typed form.
        return commitType != LastComposedWord.COMMIT_TYPE_DECIDED_WORD
                || (chosenWord == null ? typedWord == null : chosenWord.equals(typedWord));
    }
}
