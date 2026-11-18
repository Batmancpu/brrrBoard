// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCorrectionSafetyTest {
    @Test fun ordinaryWordsAndSentenceCaseKeepTheDecoderDecision() {
        for (typed in listOf("teh", "ill", "hello", "Hello", "I'll", "mañana", "kal", "yaar", "bhai")) {
            assertTrue(typed, SmartCorrectionSafety.permitsAutomaticReplacement(typed))
        }
    }

    @Test fun identifiersAndDeliberateCapitalizationCannotBeSilentlyReplaced() {
        for (typed in listOf("a@b.test", "example.com", "foo_bar", "my/dir",
                "C:\\temp", "#topic", "x=y", "foo:bar", "iPhone", "JavaClass", "myHandle")) {
            assertFalse(typed, SmartCorrectionSafety.permitsAutomaticReplacement(typed))
        }
    }
}
