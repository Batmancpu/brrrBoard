// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessoryPriorityPolicyTest {
    @Test fun activeProviderOwnsStripAgainstDictionaryAndClipboardRefresh() {
        assertFalse(AccessoryPriorityPolicy.canReplaceVisible(true, AccessoryPriorityPolicy.Incoming.WORDS))
        assertFalse(AccessoryPriorityPolicy.canReplaceVisible(true, AccessoryPriorityPolicy.Incoming.CLIPBOARD))
        assertTrue(AccessoryPriorityPolicy.canReplaceVisible(true, AccessoryPriorityPolicy.Incoming.INLINE_AUTOFILL))
    }

    @Test fun compactSlotHasExactlyOneOwnerAndRespectsExternalContent() {
        val policy = AccessoryPriorityPolicy
        // Exhaust every visibility/width/provider/clipboard/editor combination.
        for (bits in 0..31) {
            val show = bits and 1 != 0
            val fits = bits and 2 != 0
            val external = bits and 4 != 0
            val clip = bits and 8 != 0
            val edit = bits and 16 != 0
            val actual = policy.compactOwner(show, fits, external, clip, edit)
            val expected = when {
                !show || !fits || external -> AccessoryPriorityPolicy.CompactOwner.NONE
                clip -> AccessoryPriorityPolicy.CompactOwner.CLIPBOARD
                edit -> AccessoryPriorityPolicy.CompactOwner.EDITOR
                else -> AccessoryPriorityPolicy.CompactOwner.NONE
            }
            org.junit.Assert.assertEquals(expected, actual)
        }
    }

    @Test fun exactExpiryAndInvalidClockAreRejected() {
        assertTrue(AccessoryPriorityPolicy.actionIsFresh(9_999, 10_000))
        assertFalse(AccessoryPriorityPolicy.actionIsFresh(10_000, 10_000))
        assertFalse(AccessoryPriorityPolicy.actionIsFresh(10_001, 10_000))
        assertFalse(AccessoryPriorityPolicy.actionIsFresh(0, 0))
        assertFalse(AccessoryPriorityPolicy.actionIsFresh(-1, 10_000))
    }

    @Test fun ordinaryCandidatesResumeWhenProviderIsClosed() {
        for (incoming in AccessoryPriorityPolicy.Incoming.entries) {
            assertTrue(AccessoryPriorityPolicy.canReplaceVisible(false, incoming))
        }
    }

    @Test fun lateResponseAfterEditorChangeOrHideCannotOwnStrip() {
        assertTrue(AccessoryPriorityPolicy.responseBelongsToVisibleEditor(9, 9, true))
        assertFalse(AccessoryPriorityPolicy.responseBelongsToVisibleEditor(9, 10, true))
        assertFalse(AccessoryPriorityPolicy.responseBelongsToVisibleEditor(9, 9, false))
        assertFalse(AccessoryPriorityPolicy.responseBelongsToVisibleEditor(-1, 0, true))
    }
}
