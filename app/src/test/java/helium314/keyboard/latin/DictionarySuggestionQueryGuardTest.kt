// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DictionarySuggestionQueryGuardTest {
    @Test fun successReturnsCandidatesAndReleasesWaiter() {
        val completed = CountDownLatch(1)
        val result = runDictionarySuggestionQuery(
            query = { listOf("native") }, onFailure = { throw AssertionError(it) },
            onComplete = { completed.countDown() }
        )
        assertEquals(listOf("native"), result)
        assertTrue(completed.await(1, TimeUnit.SECONDS))
    }

    @Test fun failedDictionaryDegradesToNoCandidatesAndReleasesWaiter() {
        val completed = CountDownLatch(1)
        val failure = IllegalStateException("fixture")
        var observed: Exception? = null
        val result = runDictionarySuggestionQuery(
            query = { throw failure }, onFailure = { observed = it },
            onComplete = { completed.countDown() }
        )
        assertNull(result)
        assertSame(failure, observed)
        assertTrue(completed.await(1, TimeUnit.SECONDS))
    }

    @Test fun cancellationPropagatesAfterReleasingWaiter() {
        val completed = CountDownLatch(1)
        try {
            runDictionarySuggestionQuery(
                query = { throw CancellationException("fixture") }, onFailure = { throw AssertionError(it) },
                onComplete = { completed.countDown() }
            )
            throw AssertionError("Cancellation must propagate")
        } catch (expected: CancellationException) {
            assertEquals("fixture", expected.message)
        }
        assertTrue(completed.await(1, TimeUnit.SECONDS))
    }

    @Test fun concurrentLocaleFailureCannotLeaveTheMergeWaiting() {
        val executor = Executors.newFixedThreadPool(2)
        val completed = CountDownLatch(2)
        try {
            executor.execute {
                runDictionarySuggestionQuery(
                    query = { throw IllegalStateException("one dictionary unavailable") },
                    onFailure = {}, onComplete = { completed.countDown() }
                )
            }
            executor.execute {
                runDictionarySuggestionQuery(
                    query = { listOf("native") }, onFailure = { throw AssertionError(it) },
                    onComplete = { completed.countDown() }
                )
            }
            assertTrue("both locale tasks release their completion slot",
                completed.await(5, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }
    }
}
