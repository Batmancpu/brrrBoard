// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.*
import org.junit.Test

class RecoveredTranscriptQueueTest {
    @Test fun newestFirstFiveOnlyAndConsumedEntriesDisappear() {
        val q = RecoveredTranscriptQueue()
        (1..8).forEach { i -> q.add("speech $i", i.toLong()) }
        assertEquals(listOf("speech 8", "speech 7", "speech 6", "speech 5", "speech 4"),
            q.all(9).map { it.text })
        val recovered = q.all(9).first()
        assertEquals(recovered, q.get(recovered.id, 9))
        assertTrue(q.remove(recovered.id))
        assertEquals("speech 7", q.all(9).first().text)
    }

    @Test fun restoreExpiresOldOrFutureRecordsAndRetainsIdOrdering() {
        val now = 2L * 24L * 60L * 60L * 1000L
        val q = RecoveredTranscriptQueue()
        q.restore(listOf(
            RecoveredTranscript(15, now - 1_000, "still valid"),
            RecoveredTranscript(16, 1, "expired"),
            RecoveredTranscript(17, now + 1, "future")), now)
        assertEquals(listOf("still valid"), q.all(now).map { it.text })
        assertEquals(18L, q.add("fresh", now)?.id)
        assertNull(q.add(" ", now))
        assertNull(q.add("x".repeat(RecoveredTranscriptQueue.MAX_TEXT_CHARS + 1), now))
    }
}
