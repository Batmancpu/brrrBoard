/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.keyboard.internal;

import android.content.res.TypedArray;

import helium314.keyboard.latin.R;
import helium314.keyboard.latin.common.ColorType;
import helium314.keyboard.latin.settings.Settings;

/**
 * This class holds parameters to control how a gesture trail is drawn and animated on the screen.
 * <p>
 * On the other hand, {@link GestureStrokeDrawingParams} class controls how each gesture stroke is
 * sampled and interpolated. This class controls how those gesture strokes are displayed as a
 * gesture trail and animated on the screen.
 */
final class GestureTrailDrawingParams {
    private static final int FADEOUT_START_DELAY_FOR_DEBUG = 2000; // millisecond
    private static final int FADEOUT_DURATION_FOR_DEBUG = 200; // millisecond

    public int mTrailColor;
    /** The visual renderer changes; native swipe recognition keeps its existing thresholds. */
    // The user can switch motion profiles without recreating the IME view.
    public boolean mIosMotion;
    public float mTrailStartWidth;
    public float mTrailEndWidth;
    public final float mTrailBodyRatio;
    public final boolean mTrailShadowEnabled;
    public final float mTrailShadowRatio;
    public final int mFadeoutStartDelay;
    public final int mFadeoutDuration;
    public int mUpdateInterval;

    private final int mConfiguredTrailColor;
    private final int mConfiguredKeyTextColor;
    private final float mConfiguredStartWidth;
    private final float mConfiguredEndWidth;
    private final int mConfiguredUpdateInterval;
    private final float mDensity;

    public final int mTrailLingerDuration;

    public GestureTrailDrawingParams(final TypedArray mainKeyboardViewAttr, final boolean iosMotion) {
        mConfiguredTrailColor = Settings.getValues().mColors.get(ColorType.GESTURE_TRAIL);
        mConfiguredKeyTextColor = Settings.getValues().mColors.get(ColorType.KEY_TEXT);
        mDensity = mainKeyboardViewAttr.getResources().getDisplayMetrics().density;
        mConfiguredStartWidth = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_gestureTrailStartWidth, 0.0f);
        mConfiguredEndWidth = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_gestureTrailEndWidth, 0.0f);
        mConfiguredUpdateInterval = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_gestureTrailUpdateInterval, 0);
        setIosMotion(iosMotion);
        final int PERCENTAGE_INT = 100;
        mTrailBodyRatio = (float)mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_gestureTrailBodyRatio, PERCENTAGE_INT)
                / (float)PERCENTAGE_INT;
        final int trailShadowRatioInt = mainKeyboardViewAttr.getInt(
                R.styleable.MainKeyboardView_gestureTrailShadowRatio, 0);
        mTrailShadowEnabled = (trailShadowRatioInt > 0);
        mTrailShadowRatio = (float)trailShadowRatioInt / (float)PERCENTAGE_INT;
        mFadeoutStartDelay = GestureTrailDrawingPoints.DEBUG_SHOW_POINTS
                ? FADEOUT_START_DELAY_FOR_DEBUG
                : mainKeyboardViewAttr.getInt(
                        R.styleable.MainKeyboardView_gestureTrailFadeoutStartDelay, 0);
        mFadeoutDuration = GestureTrailDrawingPoints.DEBUG_SHOW_POINTS
                ? FADEOUT_DURATION_FOR_DEBUG
                : Settings.getValues().mGestureTrailFadeoutDuration;
        mTrailLingerDuration = mFadeoutStartDelay + mFadeoutDuration;
    }

    /** Keyboard view is reused across preset switches: restore ALL old visual values. */
    public void setIosMotion(final boolean iosMotion) {
        mIosMotion = iosMotion;
        mTrailColor = GestureTrailVisualPolicy.color(iosMotion,
                mConfiguredTrailColor, mConfiguredKeyTextColor);
        mTrailStartWidth = GestureTrailVisualPolicy.startWidth(
                iosMotion, mConfiguredStartWidth, mDensity);
        mTrailEndWidth = GestureTrailVisualPolicy.endWidth(
                iosMotion, mConfiguredEndWidth, mDensity);
        mUpdateInterval = GestureTrailVisualPolicy.updateInterval(
                iosMotion, mConfiguredUpdateInterval);
    }
}
