// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToolbarPanelCompositionTest {
    @Test fun toolbarHasOneScrollSourceBehindTwoSeparateGlassOverlays() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflated = LayoutInflater.from(context).inflate(
            R.layout.access_point_menu, null, false
        )
        assertTrue(inflated is ToolbarGlassFrameLayout)
        val panel = inflated as ViewGroup

        val scroll = panel.findViewById<ScrollView>(R.id.access_point_scroll)
        val header = panel.findViewById<PanelGlassOverlayView>(R.id.access_point_header_scrim)
        val footer = panel.findViewById<PanelGlassOverlayView>(R.id.access_point_bottom_glass)
        assertNotNull(scroll)
        assertNotNull(header)
        assertNotNull(footer)
        assertEquals(panel, scroll.parent)
        assertTrue(panel.indexOfChild(scroll) < panel.indexOfChild(header))
        assertTrue(panel.indexOfChild(scroll) < panel.indexOfChild(footer))
        // The real ABC/mic footer owns the live lower glass. The scrolling
        // toolbar must not reserve another full-width 56dp dead strip above it.
        assertTrue(scroll.paddingBottom < context.resources.getDimensionPixelSize(
            R.dimen.panel_bottom_glass_height))
    }
}
