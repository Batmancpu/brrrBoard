// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;
import org.junit.Test;
import static org.junit.Assert.*;
public class SuggestionStripRestorePolicyTest {
    @Test public void restoresHiddenStripOnNormalAlphabetFastSwitch() {
        assertTrue(SuggestionStripRestorePolicy.shouldRestore(true, false, false, false));
        assertTrue(SuggestionStripRestorePolicy.shouldRestore(true, true, false, false));
    }
    @Test public void respectsSearchAndUserHiddenToolbar() {
        assertFalse(SuggestionStripRestorePolicy.shouldRestore(true, false, false, true));
        assertFalse(SuggestionStripRestorePolicy.shouldRestore(false, false, false, false));
        assertFalse(SuggestionStripRestorePolicy.shouldRestore(true, true, true, false));
    }
}
