// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Reproduces B14V's synchronous setAdapter -> empty-state callback crash. */
@RunWith(RobolectricTestRunner::class)
class ClipboardPanelLifecycleTest {
    @Test fun headerBindingSurvivesEmptyItemsAndRepeatedPanelReattachment() {
        val ime = Robolectric.setupService(LatinIME::class.java)
        val panel = LayoutInflater.from(ime).inflate(R.layout.clipboard_history_view, null)
                as ClipboardHistoryView
        val header = panel.findViewById<View>(R.id.clipboard_header_scrim)
        val list = panel.findViewById<ClipboardHistoryRecyclerView>(R.id.clipboard_list)
        assertNotNull(header)
        // The B14V lookup mistakenly resolved the header relative to *list*.
        assertEquals(null, list.findViewById<View>(R.id.clipboard_header_scrim))

        val initialize = ClipboardHistoryView::class.java.getDeclaredMethod("initialize")
        initialize.isAccessible = true
        initialize.invoke(panel)

        val items = MutableRows()
        repeat(12) {
            // Closing/reopening (including IME hide/show) detaches and reattaches
            // the adapter and executes the callback synchronously each time.
            list.adapter = null
            list.adapter = items
            assertEquals(View.INVISIBLE, header.visibility)
            items.rows = 2
            items.notifyDataSetChanged()
            assertEquals(View.VISIBLE, header.visibility)
            items.rows = 0
            items.notifyDataSetChanged()
            assertEquals(View.INVISIBLE, header.visibility)
        }
    }

    private class MutableRows : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var rows = 0
        override fun getItemCount() = rows
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(FrameLayout(parent.context)) {}
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
    }
}
