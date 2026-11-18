// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Resource smoke gate: every original rounded glyph used by active keyboard/Settings paths parses. */
@RunWith(RobolectricTestRunner::class)
class RoundedIconSurfaceTest {
    @Test fun allStageTwoRoundedIconsInflateAtTheirIntendedIntrinsicSize() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ids = intArrayOf(
            R.drawable.ic_arrow_horizontal_rounded,
            R.drawable.ic_arrow_left_rounded,
            R.drawable.ic_autocorrect_rounded,
            R.drawable.ic_close_rounded,
            R.drawable.ic_dpad_left_rounded,
            R.drawable.ic_redo_rounded,
            R.drawable.ic_select_all_rounded,
            R.drawable.ic_select_rounded,
            R.drawable.ic_to_end_rounded,
            R.drawable.ic_undo_rounded,
            R.drawable.ic_word_left_rounded,
            R.drawable.sym_keyboard_copy_rounded,
            R.drawable.sym_keyboard_cut_rounded,
            R.drawable.sym_keyboard_paste_rounded,
            R.drawable.sym_keyboard_start_onehanded_rounded,
            R.drawable.sym_keyboard_stop_onehanded_rounded,
            R.drawable.ic_arrow_back_rounded,
            R.drawable.ic_edit_rounded,
            R.drawable.ic_plus_rounded,
            R.drawable.ic_chevron_down_rounded,
            R.drawable.ic_drag_indicator_rounded,
            R.drawable.ic_link_rounded,
            R.drawable.ic_share_rounded,
        )
        ids.forEach { id ->
            val drawable = ContextCompat.getDrawable(context, id)
            assertNotNull("Rounded resource $id failed to inflate", drawable)
            assertTrue("Missing intrinsic width for $id", drawable!!.intrinsicWidth > 0)
            assertTrue("Missing intrinsic height for $id", drawable.intrinsicHeight > 0)
        }
    }
}
