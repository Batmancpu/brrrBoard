// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji

import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Regressions: duplicate external category space and opaque browsing top bar. */
@RunWith(RobolectricTestRunner::class)
class EmojiBrowseHeaderMountTest {
    @Test fun existingCategoriesBecomeTransparentTopOverlayWithoutDuplicateStrip() {
        val context = RuntimeEnvironment.getApplication() as Application
        val external = FrameLayout(context)
        val shell = EmojiGlassFrameLayout(context)
        val categories = LinearLayout(context)
        categories.setBackgroundColor(Color.BLACK)
        external.addView(categories)

        EmojiPalettesView.attachFixedCategoryStrip(shell, categories, 72)

        assertEquals(0, external.childCount)
        assertEquals(1, shell.childCount)
        assertSame(shell, categories.parent)
        assertEquals(View.VISIBLE, categories.visibility)
        assertEquals(Color.TRANSPARENT, (categories.background as ColorDrawable).color)
        val params = categories.layoutParams as FrameLayout.LayoutParams
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, params.width)
        assertEquals(72, params.height)
        assertEquals(Gravity.TOP, params.gravity)

        EmojiPalettesView.attachFixedCategoryStrip(shell, categories, 72)
        assertEquals(1, shell.childCount)
    }

    @Test fun optionalSmartPicksAreBelowCategoriesButNeverReserveOuterPanelHeight() {
        val context = RuntimeEnvironment.getApplication() as Application
        val panel = LinearLayout(context)
        val shell = EmojiGlassFrameLayout(context)
        val smart = LinearLayout(context)
        panel.addView(smart)
        panel.addView(shell)

        EmojiPalettesView.attachSmartRowBelowCategories(shell, smart, 64)

        assertEquals(1, panel.childCount)
        assertSame(shell, smart.parent)
        assertEquals(1, shell.childCount)
        val layout = smart.layoutParams as FrameLayout.LayoutParams
        assertEquals(64, layout.topMargin)
        assertEquals(Gravity.TOP, layout.gravity)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, layout.height)
        EmojiPalettesView.attachSmartRowBelowCategories(shell, smart, 64)
        assertEquals(1, shell.childCount)
        assertTrue(panel.getChildAt(0) === shell)
    }

    @Test fun realSmartPicksMoveOnlyTheRecentsListNotTheCategoryBar() {
        assertEquals(44, EmojiCategoryPagerAdapter.rowTopPadding(44, 0, 70, false))
        assertEquals(114, EmojiCategoryPagerAdapter.rowTopPadding(44, 0, 70, true))
        assertEquals(44, EmojiCategoryPagerAdapter.rowTopPadding(44, 0, 0, true))
    }
}
