// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;

import helium314.keyboard.latin.KeyboardMotionSpec;

/**
 * Continuity layer for an Emoji Search row that uses an in-place mutable
 * keyboard rather than independently animated child views.
 *
 * The old viewport is captured before the keyboard changes, then dissolves
 * away while the live result comes into focus. The snapshot is visual only:
 * it never owns a key, an accessibility action, or a touch target. Bitmaps
 * are small, kept only for one short transition, and explicitly recycled.
 */
final class EmojiSearchResultMorph {
    private static final long DURATION_MS = 210L;
    private static final int MAX_SNAPSHOT_PIXELS = 400_000;
    private static final PathInterpolator EASE = new PathInterpolator(.20f, 0f, .20f, 1f);

    private final ViewGroup parent;
    private ImageView departing;
    private Bitmap previousFrame;
    private View arriving;

    EmojiSearchResultMorph(final ViewGroup parent) {
        this.parent = parent;
    }

    /** Capture the actual prior viewport; never call after mutating its keys. */
    void capture(final View source) {
        cancel();
        if (!KeyboardMotionSpec.enabled() || source == null || !source.isShown()
                || source.getParent() != parent || source.getWidth() <= 0
                || source.getHeight() <= 0 || !source.isLaidOut()
                || source.isLayoutRequested() || parent.isLayoutRequested()) return;
        final int width = source.getWidth();
        final int height = source.getHeight();
        if ((long) width * height > MAX_SNAPSHOT_PIXELS) return;

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        source.draw(new Canvas(bitmap));
        final ImageView ghost = new ImageView(parent.getContext());
        ghost.setImageBitmap(bitmap);
        ghost.setClickable(false);
        ghost.setFocusable(false);
        ghost.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ghost.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        ghost.layout(source.getLeft(), source.getTop(),
                source.getLeft() + width, source.getTop() + height);
        parent.getOverlay().add(ghost);
        previousFrame = bitmap;
        departing = ghost;
    }

    /** The real current results remain authoritative and clickable throughout. */
    void reveal(final View target) {
        if (departing == null || target == null || !target.isShown()) {
            cancel();
            return;
        }
        arriving = target;
        final float offset = parent.getResources().getDisplayMetrics().density * 3f
                * (target.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? -1f : 1f);
        target.animate().cancel();
        target.setAlpha(.12f);
        target.setTranslationX(offset);
        final long duration = KeyboardMotionSpec.emojiSearchMillis(parent.getContext());
        target.animate().alpha(1f).translationX(0f)
                .setInterpolator(EASE).setDuration(duration).start();
        final ImageView old = departing;
        old.animate().alpha(0f).translationX(-offset)
                .setInterpolator(EASE).setDuration(duration)
                .withEndAction(() -> {
                    if (departing == old) clearSnapshot();
                }).start();
    }

    void cancel() {
        if (arriving != null) {
            arriving.animate().cancel();
            arriving.setAlpha(1f);
            arriving.setTranslationX(0f);
            arriving = null;
        }
        clearSnapshot();
    }

    private void clearSnapshot() {
        final ImageView old = departing;
        departing = null;
        if (old != null) {
            old.animate().cancel();
            parent.getOverlay().remove(old);
            old.setImageDrawable(null);
        }
        if (previousFrame != null) {
            previousFrame.recycle();
            previousFrame = null;
        }
        arriving = null;
    }
}
