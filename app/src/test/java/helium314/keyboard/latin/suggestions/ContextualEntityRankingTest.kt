// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import helium314.keyboard.latin.brain.SmartPastePolicy
import helium314.keyboard.latin.brain.SmartPastePolicy.Kind
import org.junit.Assert.*
import org.junit.Test

class ContextualEntityRankingTest {
    private val email = SmartPastePolicy.Candidate(Kind.EMAIL, 0, 15, true)
    private val link = SmartPastePolicy.Candidate(Kind.LINK, 17, 35, true)
    private val phone = SmartPastePolicy.Candidate(Kind.PHONE, 40, 55, true)
    private val order = SmartPastePolicy.Candidate(Kind.IDENTIFIER, 60, 66, true)
    private val options = listOf(email, link, phone, order)

    @Test fun neutralAndBoundedPreference() {
        assertEquals(options, ContextualAccessoryPolicy.rankEntities(options) { 0 })
        val ranked = ContextualAccessoryPolicy.rankEntities(options) {
            when (it) { Kind.PHONE -> 100; Kind.EMAIL, Kind.LINK -> -100; else -> 0 }
        }
        assertEquals(phone, ranked.first())
        assertEquals(options.toSet(), ranked.toSet())
        assertEquals(options.size, ranked.size)
        assertEquals(email, ContextualAccessoryPolicy.rankEntities(options) {
            if (it == Kind.IDENTIFIER) Int.MAX_VALUE else 0
        }.first())
    }

    @Test fun onlyActualEntityUseIsLearnable() {
        assertNull(ContextualAccessoryPolicy.learningKind(ContextualAccessoryPolicy.Kind.EXTRACT))
        assertEquals(Kind.TRACKING, ContextualAccessoryPolicy.learningKind(ContextualAccessoryPolicy.Kind.TRACK))
        assertEquals(Kind.DATE, ContextualAccessoryPolicy.learningKind(ContextualAccessoryPolicy.Kind.CALENDAR))
        assertTrue(ContextualAccessoryPolicy.extractCandidates("verification code 123456").isEmpty())
    }
}
