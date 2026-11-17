// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import helium314.keyboard.latin.R;

/**
 * Render Clipboard rows sharply only above its fixed action-row underlay.
 * The existing PanelGlassOverlayView independently records the LIVE list and
 * feeds the blurred action row and the common ABC/microphone footer.
 *
 * This prevents sharp-thumbnail ghosting beneath those two pieces of glass,
 * without changing their blur radius, appearance, positions or capture source.
 */
public final class ClipboardGlassFrameLayout extends FrameLayout {
    public ClipboardGlassFrameLayout(Context context) { super(context); }
    public ClipboardGlassFrameLayout(Context context, AttributeSet attrs) { super(context, attrs); }
    public ClipboardGlassFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    static int sharpViewportBottom(int overlayTop, int hostHeight) {
        return overlayTop > 0 ? Math.min(hostHeight, overlayTop) : hostHeight;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child.getId() != R.id.clipboard_list || Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                !canvas.isHardwareAccelerated()) {
            return super.drawChild(canvas, child, drawingTime);
        }
        final View glass = findViewById(R.id.clipboard_bottom_glass);
        if (glass == null || glass.getVisibility() != VISIBLE) {
            return super.drawChild(canvas, child, drawingTime);
        }
        final int cropBottom = sharpViewportBottom(glass.getTop(), getHeight());
        if (cropBottom >= getHeight()) return super.drawChild(canvas, child, drawingTime);
        final int saved = canvas.save();
        try {
            canvas.clipRect(0, 0, getWidth(), cropBottom);
            return super.drawChild(canvas, child, drawingTime);
        } finally {
            canvas.restoreToCount(saved);
        }
    }
}
