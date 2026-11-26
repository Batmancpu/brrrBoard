// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CorrectionRevertGuardTest {
    @Test public void onlyExactCommittedWordAndSeparatorCanBeReverted() {
        assertTrue(CorrectionRevertGuard.matches("corrected ", "corrected", " "));
        assertFalse(CorrectionRevertGuard.matches("unrelated ", "corrected", " "));
        assertFalse(CorrectionRevertGuard.matches("corrected!", "corrected", " "));
        assertFalse(CorrectionRevertGuard.matches("corrected", "corrected", " "));
        assertFalse(CorrectionRevertGuard.matches(null, "corrected", " "));
    }

    @Test public void compoundUnicodeAndPunctuationAreComparedWithoutSplitting() {
        final String word = "cafe\u0301\uD83D\uDE00";
        assertTrue(CorrectionRevertGuard.matches(word + ", ", word, ", "));
        assertFalse(CorrectionRevertGuard.matches(word + ". ", word, ", "));
    }
    @Test public void hostMustAcceptRemovalBeforeOriginalTextCanBeRestored() {
        assertTrue(CorrectionRevertGuard.matchesPrefixAfterDeletion(
                "prefixcorrected ", "prefix", "corrected", " "));
        assertFalse(CorrectionRevertGuard.matchesPrefixAfterDeletion(
                "prefixcorrected ", "prefixcorrected ", "corrected", " "));
        assertFalse(CorrectionRevertGuard.matchesPrefixAfterDeletion(
                "prefixcorrected ", "changed", "corrected", " "));
        assertTrue(CorrectionRevertGuard.matchesPrefixAfterDeletion(
                "corrected ", "", "corrected", " "));
    }

    @Test public void restoredWordNeedsExactHostTextAndSameEditorConnection() {
        assertTrue(CorrectionRevertGuard.confirmsRestoration(
                "prefixcorrected ", "prefixteh", "corrected", " ", "teh", true, true));
        assertFalse(CorrectionRevertGuard.confirmsRestoration(
                "prefixcorrected ", "prefixcorrected teh", "corrected", " ", "teh", true, true));
        assertFalse(CorrectionRevertGuard.confirmsRestoration(
                "prefixcorrected ", "prefixthe", "corrected", " ", "teh", true, true));
        assertFalse(CorrectionRevertGuard.confirmsRestoration(
                "prefixcorrected ", "prefixteh", "corrected", " ", "teh", false, true));
        assertFalse(CorrectionRevertGuard.confirmsRestoration(
                "prefixcorrected ", "prefixteh", "corrected", " ", "teh", true, false));
        assertFalse(CorrectionRevertGuard.confirmsRestoration(
                "prefixcorrected ", null, "corrected", " ", "teh", true, true));
    }

}
