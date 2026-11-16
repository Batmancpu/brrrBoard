// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import helium314.keyboard.latin.common.ColorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyBackgroundRoleCacheTest {
    private fun pixel(drawable: Drawable): Int {
        val image = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 2, 2)
        drawable.draw(Canvas(image))
        return image.getPixel(0, 0)
    }

    @Test
    fun coloredControlDoesNotRetintOrdinaryKeysOrTheirSource() {
        val normalColor = Color.rgb(48, 50, 54)
        val accentColor = Color.rgb(183, 128, 93)
        val source = ColorDrawable(Color.WHITE)
        val cache = KeyBackgroundRoleCache()
        var recolorCount = 0
        val colorize = KeyBackgroundRoleCache.Colorizer { drawable, role ->
            recolorCount++
            drawable.setTint(if (role == ColorType.KEY_BACKGROUND) normalColor else accentColor)
        }

        val normal = cache.get(source, ColorType.KEY_BACKGROUND, colorize)
        val accent = cache.get(source, ColorType.OG_FROST_ACCENT_BACKGROUND, colorize)
        val normalAfterSwitch = cache.get(source, ColorType.KEY_BACKGROUND, colorize)

        assertNotSame(source, normal)
        assertNotSame(source, accent)
        assertNotSame(normal, accent)
        assertSame(normal, normalAfterSwitch)
        assertEquals(2, recolorCount)
        assertEquals(normalColor, pixel(normalAfterSwitch))
        assertEquals(accentColor, pixel(accent))
        assertEquals(Color.WHITE, pixel(source))
    }

    @Test
    fun themeChangeDiscardsRoleDrawables() {
        val source = ColorDrawable(Color.WHITE)
        val cache = KeyBackgroundRoleCache()
        var activeColor = Color.BLUE
        val colorize = KeyBackgroundRoleCache.Colorizer { drawable, _ -> drawable.setTint(activeColor) }

        val previous = cache.get(source, ColorType.KEY_BACKGROUND, colorize)
        assertEquals(Color.BLUE, pixel(previous))
        activeColor = Color.GREEN
        cache.clear()
        val refreshed = cache.get(source, ColorType.KEY_BACKGROUND, colorize)

        assertNotSame(previous, refreshed)
        assertEquals(Color.BLUE, pixel(previous))
        assertEquals(Color.GREEN, pixel(refreshed))
    }
}
