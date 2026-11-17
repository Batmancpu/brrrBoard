// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji

import android.view.LayoutInflater
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import helium314.keyboard.keyboard.PanelGlassOverlayView
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Source-hierarchy gates; real blur/drag still require GPU/device inspection. */
@RunWith(RobolectricTestRunner::class)
class EmojiPanelCompositionTest {
    @Test fun actionKeysOverlayFullWidthCategoryPagerAndSearchKeepsOneResultRow() {
        val ime = Robolectric.setupService(LatinIME::class.java)
        val panel = LayoutInflater.from(ime).inflate(R.layout.emoji_palettes_view, null)
        val shell = panel.findViewById<View>(R.id.emoji_grid_shell)
        val pager = panel.findViewById<View>(R.id.emoji_pager)
        val keys = panel.findViewById<View>(R.id.bottom_row_keyboard)
        assertTrue(pager is ViewPager2)
        assertEquals(shell, keys.parent)
        assertTrue(panel.findViewById<View>(R.id.emoji_header_scrim) is PanelGlassOverlayView)
        assertTrue(panel.findViewById<View>(R.id.emoji_bottom_glass) is PanelGlassOverlayView)
        assertNotNull(panel.findViewById<View>(R.id.emoji_search_empty))
        val results = panel.findViewById<View>(R.id.emoji_search_results_scroll)
        assertEquals((56 * ime.resources.displayMetrics.density).toInt(), results.layoutParams.height)
    }
}
