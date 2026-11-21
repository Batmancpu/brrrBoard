// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SuggestionDeliveryPolicyTest {
    @Test public void acceptsOnlyCurrentEditorCurrentRequestAndCurrentPrefix() {
        assertTrue(SuggestionDeliveryPolicy.isCurrent(4, 4, 17, 17, "th", "th", 8, 8, 8, 8));
        assertFalse(SuggestionDeliveryPolicy.isCurrent(4, 5, 17, 17, "th", "th", 8, 8, 8, 8));
        assertFalse(SuggestionDeliveryPolicy.isCurrent(4, 4, 17, 18, "th", "th", 8, 8, 8, 8));
        assertFalse(SuggestionDeliveryPolicy.isCurrent(4, 4, 17, 17, "th", "the", 8, 8, 8, 8));
        assertFalse(SuggestionDeliveryPolicy.isCurrent(4, 4, 17, 17, "th", "t", 8, 8, 8, 8));
        assertFalse(SuggestionDeliveryPolicy.isCurrent(4, 4, 17, 17, "th", "th", 8, 9, 8, 9));
        assertFalse(SuggestionDeliveryPolicy.isCurrent(4, 4, 17, 17, "th", "th", 8, 8, 8, 7));
    }

    @Test public void returningToTheSamePrefixDoesNotReviveObsoleteRequest() {
        assertFalse(SuggestionDeliveryPolicy.isCurrent(9, 9, 1, 3, "t", "t", 1, 1, 1, 1));
    }
}
