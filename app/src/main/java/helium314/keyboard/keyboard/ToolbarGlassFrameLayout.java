// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import helium314.keyboard.latin.R;

/**
 * Only the middle of the Toolbar list is sharp. Its top and bottom continue
 * scrolling beneath fixed controls, but are composited solely by the live
 * PanelGlassOverlayView instances instead of being drawn twice.
 *
 * The overlays record the ScrollView itself, bypassing this parent's clip.
 * This never touches the IME's already-blurred host-app background.
 */
public final class ToolbarGlassFrameLayout extends FrameLayout {
    public ToolbarGlassFrameLayout(final Context context) {
        super(context);
    }

    public ToolbarGlassFrameLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public ToolbarGlassFrameLayout(final Context context, final AttributeSet attrs,
                                   final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean drawChild(final Canvas canvas, final View child, final long drawingTime) {
        if (child.getId() != R.id.access_point_scroll ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                !canvas.isHardwareAccelerated()) {
            return super.drawChild(canvas, child, drawingTime);
        }

        final View topGlass = findViewById(R.id.access_point_header_scrim);
        final View bottomGlass = findViewById(R.id.access_point_bottom_glass);
        if (topGlass == null || bottomGlass == null ||
                topGlass.getVisibility() != View.VISIBLE ||
                bottomGlass.getVisibility() != View.VISIBLE ||
                topGlass.getBottom() <= 0 || bottomGlass.getTop() <= topGlass.getBottom()) {
            return super.drawChild(canvas, child, drawingTime);
        }

        final int saved = canvas.save();
        try {
            // The top title fade and bottom action region receive only the
            // current, separately blurred pixels from this exact ScrollView.
            canvas.clipRect(0, topGlass.getBottom(), getWidth(), bottomGlass.getTop());
            return super.drawChild(canvas, child, drawingTime);
        } finally {
            canvas.restoreToCount(saved);
        }
    }
}
