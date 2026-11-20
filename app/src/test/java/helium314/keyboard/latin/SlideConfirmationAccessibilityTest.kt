// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SlideConfirmationAccessibilityTest {
    @Test fun bothConfirmAndDismissAreAvailableToAssistiveTechnology() {
        val overlay = SlideConfirmationOverlay(ApplicationProvider.getApplicationContext())
        overlay.present(Rect(10, 10, 400, 90), "Erase private patterns", 0xFFE1AF4D.toInt(),
            onConfirmed = {}, onDismissed = {})
        val info = AccessibilityNodeInfo.obtain()
        overlay.onInitializeAccessibilityNodeInfo(info)
        assertTrue(info.isDismissable)
        assertTrue(info.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD })
        assertTrue(info.actionList.any { it.id == AccessibilityNodeInfo.ACTION_DISMISS })
        info.recycle()
        overlay.closeNow()
    }

    @Test fun dismissActionCannotApproveDestructiveOperation() {
        val overlay = SlideConfirmationOverlay(ApplicationProvider.getApplicationContext())
        var committed = false
        overlay.present(Rect(10, 10, 400, 90), "Erase private patterns", 0xFFE1AF4D.toInt(),
            onConfirmed = { committed = true }, onDismissed = {})
        assertTrue(overlay.performAccessibilityAction(AccessibilityNodeInfo.ACTION_DISMISS, null))
        assertFalse(committed)
        // Repeated accessibility actions during the closing animation are ignored.
        assertFalse(overlay.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        overlay.closeNow()
    }

    @Test fun cancelledConfirmAnimationCannotRunDestructiveCallback() {
        val overlay = SlideConfirmationOverlay(ApplicationProvider.getApplicationContext())
        var committed = false
        overlay.present(Rect(10, 10, 400, 90), "Erase private patterns", 0xFFE1AF4D.toInt(),
            onConfirmed = { committed = true }, onDismissed = {})
        assertTrue(overlay.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        overlay.closeNow()
        assertFalse(committed)
    }
}
