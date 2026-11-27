// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;
import org.junit.Test;
import static org.junit.Assert.*;
public class SearchResultTouchGateTest {
    @Test public void searchCannotInheritGestureAfterPendingAndReplacement() {
        SearchResultTouchGate gate=new SearchResultTouchGate();
        gate.setPending(false);
        assertTrue(gate.begin());
        assertTrue(gate.continuationAllowed());
        gate.setPending(true);
        assertFalse(gate.continuationAllowed());
        assertFalse(gate.begin());
        gate.setPending(false);
        assertFalse(gate.continuationAllowed());
        assertTrue(gate.begin());
        gate.invalidate();
        assertFalse(gate.continuationAllowed());
    }
    @Test public void browseRemainsUnmanaged() {
        SearchResultTouchGate gate=new SearchResultTouchGate();
        assertFalse(gate.managed());
        assertTrue(gate.begin());
        gate.invalidate();
        assertTrue(gate.continuationAllowed());
    }
}
