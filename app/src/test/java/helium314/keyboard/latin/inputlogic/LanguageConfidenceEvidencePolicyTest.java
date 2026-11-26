/*
 * Copyright (C) 2026
 * SPDX-License-Identifier: GPL-3.0-only
 */

package helium314.keyboard.latin.inputlogic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import helium314.keyboard.latin.LastComposedWord;

public class LanguageConfidenceEvidencePolicyTest {
    @Test
    public void changedAutomaticCorrectionIsNotLanguageEvidence() {
        assertFalse(LanguageConfidenceEvidencePolicy.shouldUse(
                LastComposedWord.COMMIT_TYPE_DECIDED_WORD, "the", "teh"));
    }

    @Test
    public void unchangedAutomaticCommitStillProvidesEvidence() {
        assertTrue(LanguageConfidenceEvidencePolicy.shouldUse(
                LastComposedWord.COMMIT_TYPE_DECIDED_WORD, "their", "their"));
    }

    @Test
    public void manualChoiceIsExplicitEvidence() {
        assertTrue(LanguageConfidenceEvidencePolicy.shouldUse(
                LastComposedWord.COMMIT_TYPE_MANUAL_PICK, "colour", "color"));
    }

    @Test
    public void explicitlyTypedWordRemainsEvidence() {
        assertTrue(LanguageConfidenceEvidencePolicy.shouldUse(
                LastComposedWord.COMMIT_TYPE_USER_TYPED_WORD, "teh", "teh"));
    }
}
