// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class CalendarAccessoryPolicyTest {
    private val day = LocalDate.of(2026, 11, 25)
    private val zone = ZoneId.of("Asia/Kolkata")
    @Test fun explicitAndRelativeDates() {
        val all = CalendarAccessoryPolicy.choose("30 November 2026", day, zone)!!
        assertTrue(all.allDay)
        assertEquals(Instant.parse("2026-11-30T00:00:00Z").toEpochMilli(), all.beginMillis)
        assertEquals(86_400_000L, all.endMillis - all.beginMillis)
        val timed = CalendarAccessoryPolicy.choose("Meeting on tomorrow at 10:30 am to 11:30 am", day, zone)!!
        assertEquals("Meeting", timed.title)
        assertFalse(timed.allDay)
        assertEquals(Instant.parse("2026-11-26T05:00:00Z").toEpochMilli(), timed.beginMillis)
        assertEquals(3_600_000L, timed.endMillis - timed.beginMillis)
        assertEquals(3_600_000L, CalendarAccessoryPolicy.choose("2026-11-30 at 14:00", day, zone)!!.let { it.endMillis-it.beginMillis })
        assertNotNull(CalendarAccessoryPolicy.choose("Appointment on Nov 30, 2026", day, zone))
    }
    @Test fun rejectAmbiguityAndInvalidDates() {
        for (text in listOf("11/12/26", "2026-02-30", "2026-11-30 at 25:00", "2026-11-30 at 12:60 pm",
            "maybe tomorrow at 10 am", "Meeting cancelled tomorrow at 10 am", "tomorrow at 10 am PST",
            "tomorrow at 10 pm to 9 pm", "2026-11-30 at 10", "tomorrow or today at 10 am"))
            assertNull(text, CalendarAccessoryPolicy.choose(text, day, zone))
        val ny = ZoneId.of("America/New_York")
        assertNull(CalendarAccessoryPolicy.choose("2026-03-08 at 02:30", day, ny))
        assertNull(CalendarAccessoryPolicy.choose("2026-11-01 at 01:30", day, ny))
    }
    @Test fun extractionFiltersAndRanksWithoutKeepingContents() {
        val text = "Order number: ABC123, email hello@example.com"
        val candidates = ContextualAccessoryPolicy.extractCandidates(text)
        assertEquals(helium314.keyboard.latin.brain.SmartPastePolicy.Kind.EMAIL, candidates.first().kind)
        assertTrue(ContextualAccessoryPolicy.extractCandidates("Order 9876543210").isEmpty())
        assertTrue(ContextualAccessoryPolicy.extractCandidates("OTP 123456").isEmpty())
    }
}
