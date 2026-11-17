// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import helium314.keyboard.latin.R;

/**
 * Renders the scrollable emoji grid sharply only above the fixed bottom glass.
 *
 * The shared glass overlay records the ViewPager directly into a RenderNode,
 * so this parent-level clip does not affect that live blur sample. Without it,
 * sharp emoji glyphs were still visible beneath the blurred row, producing
 * a double-exposed/weak-glass effect and colored ghosts under the controls.
 *
 * Only the emoji pager is clipped. The keyboard host's wallpaper blur and the
 * separate ABC/mic footer are never drawn or blurred a second time here.
 */
public final class EmojiGlassFrameLayout extends FrameLayout {
    public EmojiGlassFrameLayout(final Context context) {
        super(context);
    }

    public EmojiGlassFrameLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public EmojiGlassFrameLayout(final Context context, final AttributeSet attrs,
                                 final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected boolean drawChild(final Canvas canvas, final View child, final long drawingTime) {
        if (child.getId() != R.id.emoji_pager || Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                !canvas.isHardwareAccelerated()) {
            return super.drawChild(canvas, child, drawingTime);
        }
        final View bottomGlass = findViewById(R.id.emoji_bottom_glass);
        if (bottomGlass == null || bottomGlass.getVisibility() != View.VISIBLE ||
                bottomGlass.getTop() <= 0) {
            return super.drawChild(canvas, child, drawingTime);
        }
        final int saved = canvas.save();
        try {
            // The underlying emoji source remains fully scrollable, including
            // its hidden pixels beneath Search/Space/Delete and ABC/mic.
            canvas.clipRect(0, 0, getWidth(), bottomGlass.getTop());
            return super.drawChild(canvas, child, drawingTime);
        } finally {
            canvas.restoreToCount(saved);
        }
    }
}
