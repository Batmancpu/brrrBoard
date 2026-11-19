/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.keyboard.internal;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import helium314.keyboard.keyboard.Key;
import helium314.keyboard.keyboard.KeyboardTheme;
import helium314.keyboard.latin.KeyboardMotionSpec;
import helium314.keyboard.latin.R;
import helium314.keyboard.latin.common.ColorType;
import helium314.keyboard.latin.common.Colors;
import helium314.keyboard.latin.common.CoordinateUtils;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.utils.ViewLayoutUtils;

import java.util.ArrayDeque;
import java.util.HashMap;

/**
 * This class controls pop up key previews. This class decides:
 * - what kind of key previews should be shown.
 * - where key previews should be placed.
 * - how key previews should be shown and dismissed.
 */
public final class KeyPreviewChoreographer {
    private static final float CIRCULAR_PREVIEW_BACKGROUND_LIGHT_BLEND = 0.86f;
    private static final float CIRCULAR_PREVIEW_BACKGROUND_DARK_BLEND = 0.18f;

    // Free {@link KeyPreviewView} pool that can be used for key preview.
    private final ArrayDeque<KeyPreviewView> mFreeKeyPreviewViews = new ArrayDeque<>();
    // A fading preview cannot be pooled until its exit animation has ended.
    private final ArrayDeque<KeyPreviewView> mFadingKeyPreviewViews = new ArrayDeque<>();
    // Map from {@link Key} to {@link KeyPreviewView} that is currently being displayed as key
    // preview.
    private final HashMap<Key,KeyPreviewView> mShowingKeyPreviewViews = new HashMap<>();

    private final KeyPreviewDrawParams mParams;

    public KeyPreviewChoreographer(final KeyPreviewDrawParams params) {
        mParams = params;
    }

    public KeyPreviewView getKeyPreviewView(final Key key, final ViewGroup placerView) {
        KeyPreviewView keyPreviewView = mShowingKeyPreviewViews.remove(key);
        if (keyPreviewView != null) {
            return keyPreviewView;
        }
        keyPreviewView = mFreeKeyPreviewViews.poll();
        if (keyPreviewView != null) {
            return keyPreviewView;
        }
        final Context context = placerView.getContext();
        keyPreviewView = new KeyPreviewView(context, null /* attrs */);
        placerView.addView(keyPreviewView, ViewLayoutUtils.newLayoutParam(placerView, 0, 0));
        return keyPreviewView;
    }

    public boolean isShowingKeyPreview(final Key key) {
        return mShowingKeyPreviewViews.containsKey(key);
    }

    public void clear() {
        for (final KeyPreviewView keyPreviewView : mShowingKeyPreviewViews.values()) {
            keyPreviewView.animate().cancel();
            removeFromParent(keyPreviewView);
        }
        for (final KeyPreviewView keyPreviewView : mFadingKeyPreviewViews) {
            keyPreviewView.animate().cancel();
            removeFromParent(keyPreviewView);
        }
        for (final KeyPreviewView keyPreviewView : mFreeKeyPreviewViews) {
            keyPreviewView.animate().cancel();
            removeFromParent(keyPreviewView);
        }
        mShowingKeyPreviewViews.clear();
        mFadingKeyPreviewViews.clear();
        mFreeKeyPreviewViews.clear();
    }

    public void dismissKeyPreview(final Key key) {
        if (key == null) {
            return;
        }
        final KeyPreviewView keyPreviewView = mShowingKeyPreviewViews.get(key);
        if (keyPreviewView == null) {
            return;
        }
        // Dismiss preview
        mShowingKeyPreviewViews.remove(key);
        keyPreviewView.animate().cancel();
        final Context context = keyPreviewView.getContext();
        if (KeyboardMotionSpec.isIos27(context) && KeyboardMotionSpec.enabled()
                && keyPreviewView.isAttachedToWindow()) {
            mFadingKeyPreviewViews.add(keyPreviewView);
            keyPreviewView.animate().alpha(0f).scaleX(.96f).scaleY(.96f)
                    .setDuration(KeyboardMotionSpec.keyPreviewExitMillis(context))
                    .setInterpolator(KeyboardMotionSpec.easing(context))
                    .withEndAction(() -> finishDismissing(keyPreviewView)).start();
        } else {
            finishDismissing(keyPreviewView);
        }
    }

    private void finishDismissing(final KeyPreviewView view) {
        mFadingKeyPreviewViews.remove(view);
        view.setTag(null);
        view.setVisibility(View.INVISIBLE);
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationY(0f);
        if (view.getParent() != null && !mFreeKeyPreviewViews.contains(view)) {
            mFreeKeyPreviewViews.add(view);
        }
    }

    public void placeAndShowKeyPreview(final Key key, final KeyboardIconsSet iconsSet,
            final KeyDrawParams drawParams, final int fullKeyboardViewWidth, final int[] keyboardOrigin,
            final ViewGroup placerView) {
        final KeyPreviewView keyPreviewView = getKeyPreviewView(key, placerView);
        placeKeyPreview(key, keyPreviewView, iconsSet, drawParams, fullKeyboardViewWidth, keyboardOrigin);
        showKeyPreview(key, keyPreviewView);
    }

    private void placeKeyPreview(final Key key, final KeyPreviewView keyPreviewView,
            final KeyboardIconsSet iconsSet, final KeyDrawParams drawParams,
            final int fullKeyboardViewWidth, final int[] originCoords) {
        final Colors colors = Settings.getValues().mColors;
        keyPreviewView.setPreviewBackgroundResource(getPreviewBackgroundResId(colors, keyPreviewView.getContext()));
        keyPreviewView.setPreviewVisual(key, iconsSet, drawParams);
        keyPreviewView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mParams.setGeometry(keyPreviewView);
        final int previewWidth = keyPreviewView.getMeasuredWidth();
        final int previewHeight = keyPreviewView.getMeasuredHeight();
        final int keyDrawWidth = key.getDrawWidth();
        // The key preview is horizontally aligned with the center of the visible part of the
        // parent key. If it doesn't fit in this {@link KeyboardView}, it is moved inward to fit and
        // the left/right background is used if such background is specified.
        final int keyPreviewPosition;
        int previewX = key.getDrawX() - (previewWidth - keyDrawWidth) / 2 + CoordinateUtils.x(originCoords);
        if (previewX < 0) {
            previewX = 0;
            keyPreviewPosition = KeyPreviewView.POSITION_LEFT;
        } else if (previewX > fullKeyboardViewWidth - previewWidth) {
            previewX = fullKeyboardViewWidth - previewWidth;
            keyPreviewPosition = KeyPreviewView.POSITION_RIGHT;
        } else {
            keyPreviewPosition = KeyPreviewView.POSITION_MIDDLE;
        }
        final boolean hasPopupKeys = (key.getPopupKeys() != null);
        keyPreviewView.setPreviewBackground(hasPopupKeys, keyPreviewPosition);

        final boolean isFloatingPreview = KeyboardMotionSpec.isIos27(keyPreviewView.getContext())
                || isCircularPreviewStyle(colors);
        if (isFloatingPreview) {
            keyPreviewView.setCircularPreviewBackgroundColor(
                    getCircularPreviewBackgroundColor(colors.get(ColorType.KEY_PREVIEW_BACKGROUND)));
        } else {
            colors.setBackground(keyPreviewView, ColorType.KEY_PREVIEW_BACKGROUND);
        }

        // The circular LXX preview has no tail padding, so float it above the key.
        final int previewY = isFloatingPreview
                ? key.getY() - previewHeight - (int) (4 * keyPreviewView.getResources().getDisplayMetrics().density)
                    + CoordinateUtils.y(originCoords)
                : key.getY() - previewHeight + key.getHeight() - mParams.mPreviewOffset
                    + CoordinateUtils.y(originCoords);

        ViewLayoutUtils.placeViewAt(keyPreviewView, previewX, previewY, previewWidth, previewHeight);
        keyPreviewView.setPivotX(previewWidth / 2.0f);
        keyPreviewView.setPivotY(previewHeight);
    }

    private int getPreviewBackgroundResId(final Colors colors, final Context context) {
        if (KeyboardMotionSpec.isIos27(context)) return R.drawable.keyboard_key_feedback_ios_motion;
        return isCircularPreviewStyle(colors)
                ? R.drawable.keyboard_key_feedback_circle
                : mParams.mPreviewBackgroundResId;
    }

    private static boolean isCircularPreviewStyle(final Colors colors) {
        final String themeStyle = colors.getThemeStyle();
        return KeyboardTheme.STYLE_ROUNDED.equals(themeStyle)
                || KeyboardTheme.STYLE_CIRCLE.equals(themeStyle)
                || KeyboardTheme.STYLE_DEFAULT.equals(themeStyle);
    }

    void showKeyPreview(final Key key, final KeyPreviewView keyPreviewView) {
        keyPreviewView.animate().cancel();
        keyPreviewView.setTag(key);
        keyPreviewView.setVisibility(View.VISIBLE);
        mShowingKeyPreviewViews.put(key, keyPreviewView);
        final Context context = keyPreviewView.getContext();
        if (KeyboardMotionSpec.isIos27(context) && KeyboardMotionSpec.enabled()
                && keyPreviewView.isAttachedToWindow()) {
            keyPreviewView.setAlpha(.35f);
            keyPreviewView.setScaleX(.94f);
            keyPreviewView.setScaleY(.90f);
            keyPreviewView.setTranslationY(2f * context.getResources().getDisplayMetrics().density);
            keyPreviewView.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(KeyboardMotionSpec.keyPreviewEnterMillis(context))
                    .setInterpolator(KeyboardMotionSpec.easing(context)).start();
        } else {
            keyPreviewView.setAlpha(1f);
            keyPreviewView.setScaleX(1f);
            keyPreviewView.setScaleY(1f);
            keyPreviewView.setTranslationY(0f);
        }
    }

    private static void removeFromParent(final View view) {
        if (view == null) {
            return;
        }
        final ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }
    }

    private static int getCircularPreviewBackgroundColor(final int themeColor) {
        final int opaqueThemeColor = Color.rgb(
                Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor));
        if (isLightColor(opaqueThemeColor)) {
            return blendColors(opaqueThemeColor, Color.WHITE,
                    CIRCULAR_PREVIEW_BACKGROUND_LIGHT_BLEND);
        }
        return blendColors(opaqueThemeColor, Color.BLACK, CIRCULAR_PREVIEW_BACKGROUND_DARK_BLEND);
    }

    private static int blendColors(final int fromColor, final int toColor, final float amount) {
        final float inverse = 1.0f - amount;
        return Color.rgb(
                Math.round(Color.red(fromColor) * inverse + Color.red(toColor) * amount),
                Math.round(Color.green(fromColor) * inverse + Color.green(toColor) * amount),
                Math.round(Color.blue(fromColor) * inverse + Color.blue(toColor) * amount));
    }

    private static boolean isLightColor(final int color) {
        final int red = Color.red(color);
        final int green = Color.green(color);
        final int blue = Color.blue(color);
        final double brightness = red * red * 0.241 + green * green * 0.691 + blue * blue * 0.068;
        return brightness >= 180.0 * 180.0;
    }
}
