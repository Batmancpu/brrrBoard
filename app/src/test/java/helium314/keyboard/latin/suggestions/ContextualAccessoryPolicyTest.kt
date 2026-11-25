// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions
import helium314.keyboard.latin.brain.SmartPastePolicy.Destination
import org.junit.Assert.*
import org.junit.Test
class ContextualAccessoryPolicyTest {
    @Test fun onlyWholeUnambiguousCopies() {
        assertEquals(ContextualAccessoryPolicy.Kind.MAIL, ContextualAccessoryPolicy.choose("hello@example.com", Destination.GENERAL)?.kind)
        assertEquals(ContextualAccessoryPolicy.Kind.DIAL, ContextualAccessoryPolicy.choose("+91 98765 43210", Destination.GENERAL)?.kind)
        assertEquals(ContextualAccessoryPolicy.Kind.MAP, ContextualAccessoryPolicy.choose("12 Main Road", Destination.GENERAL)?.kind)
        assertEquals("hello world", ContextualAccessoryPolicy.choose("hello   world", Destination.GENERAL)?.value)
        for (text in listOf("call me at +91 98765 43210", "123456", "https://example.com/", "password: abc123", "hello@example.com and another@example.com", "a =  b", "9876543210"))
            assertNull(text, ContextualAccessoryPolicy.choose(text, Destination.GENERAL))
    }
    @Test fun structuredFieldsNeverGetExternalActions() {
        for (destination in Destination.values().filter { it != Destination.GENERAL && it != Destination.SEARCH })
            assertNull(ContextualAccessoryPolicy.choose("hello@example.com", destination))
    }
}
