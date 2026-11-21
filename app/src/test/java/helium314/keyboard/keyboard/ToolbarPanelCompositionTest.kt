// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.FrameLayout
import android.widget.ScrollView
import android.view.View
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
    @Test fun toolbarUsesFourColumnVerticalGridBehindProgressiveGlassOverlays() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inflated = LayoutInflater.from(context).inflate(
            R.layout.access_point_menu, null, false
        )
        assertTrue(inflated is ToolbarGlassFrameLayout)
        val panel = inflated as ViewGroup

        val scroll = panel.findViewById<ScrollView>(R.id.access_point_scroll)
        val rail = panel.findViewById<GridLayout>(R.id.access_point_grid)
        val header = panel.findViewById<PanelGlassOverlayView>(R.id.access_point_header_scrim)
        val footer = panel.findViewById<PanelGlassOverlayView>(R.id.access_point_bottom_glass)
        assertNotNull(scroll)
        assertNotNull(rail)
        assertNotNull(header)
        assertNotNull(footer)
        assertEquals(panel, scroll.parent)
        assertEquals(4, rail.columnCount)
        assertEquals(3, rail.rowCount)
        assertEquals(GridLayout.HORIZONTAL, rail.orientation)
        assertEquals(scroll, rail.parent)
        assertTrue(panel.indexOfChild(scroll) < panel.indexOfChild(header))
        assertTrue(panel.indexOfChild(scroll) < panel.indexOfChild(footer))
        // The real ABC/mic footer owns the live lower glass. The scrolling
        // toolbar must not reserve another full-width 56dp dead strip above it.
        assertTrue(scroll.paddingBottom < context.resources.getDimensionPixelSize(
            R.dimen.panel_bottom_glass_height))

        val holder = FrameLayout(context)
        val tile = LayoutInflater.from(context).inflate(R.layout.menu_tile_item, holder, false)
        assertTrue(tile.isClickable)
        assertTrue(tile.isFocusable)
        assertEquals(0, tile.layoutParams.width)
        assertEquals((76 * context.resources.displayMetrics.density).toInt(), tile.layoutParams.height)
        assertEquals(View.VISIBLE, tile.visibility)
    }
}
