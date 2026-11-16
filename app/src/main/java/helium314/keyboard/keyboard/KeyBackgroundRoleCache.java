// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import helium314.keyboard.latin.common.ColorType;

import java.util.EnumMap;
import java.util.IdentityHashMap;

/**
 * Keeps role-specific tinted backgrounds separate from the shared key drawables.
 *
 * <p>A single keyboard layout uses the same resource-backed drawable for many keys.
 * Re-tinting that drawable for an accent/control key can leak the tint to ordinary
 * keys after an editor or layout change. Clone each drawable once per color role
 * and copy the active key state before drawing instead.</p>
 */
final class KeyBackgroundRoleCache {
    interface Colorizer {
        void color(@NonNull Drawable drawable, @NonNull ColorType role);
    }

    private final IdentityHashMap<Drawable, EnumMap<ColorType, Drawable>> mDrawables =
            new IdentityHashMap<>();

    @NonNull
    Drawable get(@NonNull final Drawable source, @NonNull final ColorType role,
            @NonNull final Colorizer colorizer) {
        EnumMap<ColorType, Drawable> byRole = mDrawables.get(source);
        if (byRole == null) {
            byRole = new EnumMap<>(ColorType.class);
            mDrawables.put(source, byRole);
        }
        Drawable roleDrawable = byRole.get(role);
        if (roleDrawable == null) {
            final Drawable.ConstantState constantState = source.getConstantState();
            if (constantState == null) {
                // Unexpected custom drawable: keep the old render path rather
                // than changing its shape or failing to draw a key.
                roleDrawable = source;
            } else {
                roleDrawable = constantState.newDrawable().mutate();
            }
            colorizer.color(roleDrawable, role);
            byRole.put(role, roleDrawable);
        }
        roleDrawable.setState(source.getState());
        return roleDrawable;
    }

    void clear() {
        mDrawables.clear();
    }
}
