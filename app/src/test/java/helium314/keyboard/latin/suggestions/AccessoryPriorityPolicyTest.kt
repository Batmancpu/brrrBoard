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
