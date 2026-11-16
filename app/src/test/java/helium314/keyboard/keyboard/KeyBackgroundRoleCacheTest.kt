// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import helium314.keyboard.latin.common.ColorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyBackgroundRoleCacheTest {
    @Test
    fun coloredControlDoesNotRetintOrdinaryKeysOrTheirSource() {
        val normalColor = Color.rgb(48, 50, 54)
        val accentColor = Color.rgb(183, 128, 93)
        val source = ColorDrawable(Color.WHITE)
        val cache = KeyBackgroundRoleCache()
        var recolorCount = 0
        val colorize = KeyBackgroundRoleCache.Colorizer { drawable, role ->
            recolorCount++
            (drawable as ColorDrawable).color =
                if (role == ColorType.KEY_BACKGROUND) normalColor else accentColor
        }

        val normal = cache.get(source, ColorType.KEY_BACKGROUND, colorize)
        val accent = cache.get(source, ColorType.OG_FROST_ACCENT_BACKGROUND, colorize)
        val normalAfterSwitch = cache.get(source, ColorType.KEY_BACKGROUND, colorize)

        assertNotSame(source, normal)
        assertNotSame(source, accent)
        assertNotSame(normal, accent)
        assertSame(normal, normalAfterSwitch)
        assertEquals(2, recolorCount)
        // Check the drawable's actual color state: Robolectric's bitmap raster
        // pipeline does not reliably render Drawable.setTint for a pixel probe.
        assertEquals(normalColor, (normalAfterSwitch as ColorDrawable).color)
        assertEquals(accentColor, (accent as ColorDrawable).color)
        assertEquals(Color.WHITE, source.color)
    }

    @Test
    fun themeChangeDiscardsRoleDrawables() {
        val source = ColorDrawable(Color.WHITE)
        val cache = KeyBackgroundRoleCache()
        var activeColor = Color.BLUE
        val colorize = KeyBackgroundRoleCache.Colorizer { drawable, _ ->
            (drawable as ColorDrawable).color = activeColor
        }

        val previous = cache.get(source, ColorType.KEY_BACKGROUND, colorize)
        assertEquals(Color.BLUE, (previous as ColorDrawable).color)
        activeColor = Color.GREEN
        cache.clear()
        val refreshed = cache.get(source, ColorType.KEY_BACKGROUND, colorize)

        assertNotSame(previous, refreshed)
        assertEquals(Color.BLUE, (previous as ColorDrawable).color)
        assertEquals(Color.GREEN, (refreshed as ColorDrawable).color)
    }
}
