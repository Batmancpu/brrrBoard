// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.ClipboardManager
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TransitionDiagnosticsTest {
    @Test fun copyProducesOneSensitivePlainTextClipAndDoesNotReadViewText() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val root = android.widget.FrameLayout(ctx)
        val text = android.widget.EditText(ctx).apply {
            id = helium314.keyboard.latin.R.id.emoji_search_bar_container
            setText("DO_NOT_EXPORT_PRIVATE_TEXT")
            contentDescription = "DO_NOT_EXPORT_DESCRIPTION"
        }
        root.addView(text)
        TransitionDiagnostics.begin(TransitionDiagnostics.Event.ENTER_SEARCH)
        TransitionDiagnostics.measure()
        TransitionDiagnostics.frame(root)
        val trace = TransitionDiagnostics.traceText()
        assertTrue(trace.contains("draw=1"))
        assertTrue(trace.contains("measure=1"))
        assertFalse(trace.contains("DO_NOT_EXPORT"))
        assertTrue(TransitionDiagnostics.copyReport(ctx))
        val clip = (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!
        assertEquals(1, clip.itemCount)
        assertTrue(clip.description.hasMimeType("text/plain"))
        assertTrue(clip.description.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
        val report = clip.getItemAt(0).text.toString()
        assertTrue(report.contains("brrBoard diagnostic report v1"))
        assertTrue(report.contains("recent transitions"))
        assertTrue(report.contains("safe crash and configuration"))
        assertFalse(report.contains("DO_NOT_EXPORT"))
    }
    @Test fun traceIsBoundedAndDoesNotRunOnOrdinaryTypingForever() {
        repeat(150) {
            TransitionDiagnostics.begin(TransitionDiagnostics.Event.ENTER_BROWSE)
            TransitionDiagnostics.event(TransitionDiagnostics.Event.PANEL_COMMITTED)
        }
        assertTrue(TransitionDiagnostics.traceText().lines().size <= 96)
    }
}
