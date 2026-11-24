// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import helium314.keyboard.latin.common.decapitalize
import helium314.keyboard.latin.common.trimTrailingCodePoints
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class UpstreamTypingPolicyTest {
    @Test fun manualShiftSurvivesGlideAndMatchingAutoCapsCanUpdate() {
        assertTrue(UpstreamTypingPolicy.preserveSwipeShift(WordComposer.CAPS_MODE_MANUAL_SHIFTED, 0))
        assertTrue(UpstreamTypingPolicy.preserveSwipeShift(WordComposer.CAPS_MODE_MANUAL_SHIFT_LOCKED, 0x4000))
        assertTrue(UpstreamTypingPolicy.preserveSwipeShift(WordComposer.CAPS_MODE_OFF, 0x4000))
        assertFalse(UpstreamTypingPolicy.preserveSwipeShift(WordComposer.CAPS_MODE_AUTO_SHIFTED, 0x4000))
        assertFalse(UpstreamTypingPolicy.preserveSwipeShift(WordComposer.CAPS_MODE_OFF, 0))
    }
    @Test fun forcedWordCapsDoesNotBecomeHistory() {
        assertFalse(UpstreamTypingPolicy.mayLearnCapitalization(true, false))
        assertTrue(UpstreamTypingPolicy.mayLearnCapitalization(true, true))
        assertTrue(UpstreamTypingPolicy.mayLearnCapitalization(false, false))
    }
    @Test fun capitalizationIsSafeForEmptyAndSupplementaryText() {
        assertEquals("", "".decapitalize(Locale.ROOT))
        assertEquals("hello", "Hello".decapitalize(Locale.ROOT))
        assertEquals("ı", "I".decapitalize(Locale("tr")))
        assertEquals(String(Character.toChars(0x10428)) + "bc",
            (String(Character.toChars(0x10400)) + "bc").decapitalize(Locale.ROOT))
    }
    @Test fun trimmingNeverSplitsSupplementaryCodePointsOrInternalConnectors() {
        val tail = String(Character.toChars(0x1f600))
        val trim = { cp: Int -> cp == 0x1f600 || cp == 39 || cp == 44 }
        assertEquals("don't", trimTrailingCodePoints("don't'," + tail, trim))
        assertEquals("", trimTrailingCodePoints(tail + ",", trim))
        assertEquals("", trimTrailingCodePoints("", trim))
    }
    @Test fun lockedActionsAreNarrowlyScoped() {
        assertTrue(LockedKeyboardActionPolicy.isRestricted(KeyCode.CLIPBOARD))
        assertTrue(LockedKeyboardActionPolicy.isRestricted(KeyCode.CLIPBOARD_PASTE))
        assertTrue(LockedKeyboardActionPolicy.isRestricted(KeyCode.ACCESS_POINT))
        assertTrue(LockedKeyboardActionPolicy.isRestricted(KeyCode.SETTINGS))
        assertFalse(LockedKeyboardActionPolicy.isRestricted(KeyCode.DELETE))
        assertFalse(LockedKeyboardActionPolicy.isRestricted('a'.code))
    }
}
