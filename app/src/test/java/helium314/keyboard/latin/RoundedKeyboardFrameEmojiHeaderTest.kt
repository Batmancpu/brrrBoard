// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.app.Application
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The parent must hide native-typing headers BEFORE measuring any exclusive panel. */
@RunWith(RobolectricTestRunner::class)
class RoundedKeyboardFrameEmojiHeaderTest {
    @Test fun compactEmojiDoesNotReserveAnInvisibleOuterStripOrPersistentRow() {
        val setup = createFrame(emojiVisible = true)
        setup.frame.measure(exact(500), atMost(1_000))

        assertEquals(View.GONE, setup.strip.visibility)
        assertEquals(View.GONE, setup.persistent.visibility)
        assertEquals(View.GONE, setup.divider.visibility)
        assertEquals(275, setup.frame.measuredHeight)
    }

    @Test fun alphabetCanStillShowItsOwnSuggestionAndPersistentRows() {
        val setup = createFrame(emojiVisible = false)
        setup.frame.measure(exact(500), atMost(1_000))

        assertEquals(View.VISIBLE, setup.strip.visibility)
        assertEquals(View.VISIBLE, setup.persistent.visibility)
        assertEquals(View.VISIBLE, setup.divider.visibility)
        assertEquals(364, setup.frame.measuredHeight)
    }

    @Test fun clipboardAndToolbarCannotInheritAStaleTypingHeader() {
        for (panelId in listOf(R.id.clipboard_history_view, R.id.access_point_menu_view,
                R.id.klipy_palettes_view, R.id.ai_writing_tools_view)) {
            val setup = createFrame(emojiVisible = false, activePanelId = panelId)
            setup.frame.measure(exact(500), atMost(1_000))

            assertEquals("strip for panel $panelId", View.GONE, setup.strip.visibility)
            assertEquals("persistent row for panel $panelId", View.GONE, setup.persistent.visibility)
            assertEquals("divider for panel $panelId", View.GONE, setup.divider.visibility)
            assertEquals("frame for panel $panelId", 275, setup.frame.measuredHeight)
        }
    }

    private data class Fixture(
        val frame: RoundedKeyboardFrameView,
        val strip: View,
        val persistent: View,
        val divider: View,
    )

    private fun createFrame(emojiVisible: Boolean, activePanelId: Int? = null): Fixture {
        val context = RuntimeEnvironment.getApplication() as Application
        val frame = RoundedKeyboardFrameView(context).apply { orientation = LinearLayout.VERTICAL }
        fun addRow(id: Int, height: Int): View = View(context).also {
            it.id = id
            frame.addView(it, LinearLayout.LayoutParams(500, height))
        }
        val persistent = addRow(R.id.persistent_emoji_row_scroll, 40)
        val divider = addRow(R.id.persistent_emoji_row_divider, 1)
        val strip = addRow(R.id.strip_container, 48)
        val wrapper = FrameLayout(context)
        frame.addView(wrapper, LinearLayout.LayoutParams(500, 220))
        val emoji = View(context).apply {
            id = R.id.emoji_palettes_view
            visibility = if (emojiVisible) View.VISIBLE else View.GONE
        }
        wrapper.addView(emoji, FrameLayout.LayoutParams(500, 220))
        if (activePanelId != null) {
            val panel = View(context).apply { id = activePanelId }
            wrapper.addView(panel, FrameLayout.LayoutParams(500, 220))
        }
        addRow(R.id.board_footer, 55)
        return Fixture(frame, strip, persistent, divider)
    }

    private fun exact(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
    private fun atMost(size: Int) = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.AT_MOST)
}
