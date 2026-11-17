// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppleAccentSelectionTest {
    @Test fun grayMaterialYouPaletteRemainsNeutral() {
        for (input in intArrayOf(0xff777777.toInt(), 0xff99958e.toInt(), 0xff8b9295.toInt())) {
            val output = KeyboardTheme.nearestAppleAccent(input)
            val channels = intArrayOf(Color.red(output), Color.green(output), Color.blue(output))
            assertTrue("neutral palette must not turn peach: " + output, channels.max() - channels.min() <= 2)
        }
    }

    @Test fun chromaticMaterialYouPaletteStillMapsToAppleLikeSwatch() {
        val appleRed = 0xffff383c.toInt()
        assertEquals(appleRed, KeyboardTheme.nearestAppleAccent(appleRed))
    }

    @Test fun preservesGrayLightness() {
        val initial = 0xff91918d.toInt()
        val mapped = KeyboardTheme.nearestAppleAccent(initial)
        val before = DoubleArray(3)
        val after = DoubleArray(3)
        ColorUtils.colorToLAB(initial, before)
        ColorUtils.colorToLAB(mapped, after)
        assertTrue(kotlin.math.abs(before[0] - after[0]) < 1.0)
    }
}
