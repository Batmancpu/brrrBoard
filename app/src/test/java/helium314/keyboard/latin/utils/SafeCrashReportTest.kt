// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeCrashReportTest {
    @Test
    fun reportRecordsStackFramesButNeverExceptionMessage() {
        val privateText = "typed-private-text-should-not-appear"
        val failure = IllegalStateException(privateText).apply {
            stackTrace = arrayOf(StackTraceElement("test.Sample", "openProtected", "Sample.kt", 42))
        }

        val report = SafeCrashReport.format(
            timestampMs = 1L,
            appVersion = "1.0.255",
            api = 35,
            manufacturer = "OPPO",
            model = "CPH2531",
            threadName = "main",
            error = failure,
        )

        assertTrue(report.contains("exception_1=java.lang.IllegalStateException"))
        assertTrue(report.contains("test.Sample.openProtected(Sample.kt:42)"))
        assertFalse(report.contains(privateText))
    }
}
