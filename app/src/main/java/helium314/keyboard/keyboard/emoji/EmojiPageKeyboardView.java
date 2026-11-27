/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.keyboard.emoji;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import helium314.keyboard.keyboard.internal.KeyDrawParams;
import helium314.keyboard.keyboard.MotionContinuity;
import helium314.keyboard.latin.KeyboardMotionSpec;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import helium314.keyboard.keyboard.PopupTextView;
import helium314.keyboard.latin.utils.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;

import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import helium314.keyboard.accessibility.AccessibilityUtils;
import helium314.keyboard.accessibility.KeyboardAccessibilityDelegate;
import helium314.keyboard.keyboard.Key;
import helium314.keyboard.keyboard.KeyDetector;
import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.keyboard.KeyboardView;
import helium314.keyboard.keyboard.PopupKeysKeyboard;
import helium314.keyboard.keyboard.PopupKeysKeyboardView;
import helium314.keyboard.keyboard.PopupKeysPanel;
import helium314.keyboard.keyboard.internal.PopupKeySpec;
import helium314.keyboard.latin.R;
import helium314.keyboard.latin.common.CoordinateUtils;
import helium314.keyboard.latin.settings.Settings;

import java.util.WeakHashMap;

/**
 * This is an extended {@link KeyboardView} class that hosts an emoji page keyboard.
 * Multi-touch unsupported. No gesture support.
 */
public final class EmojiPageKeyboardView extends KeyboardView implements
        PopupKeysPanel.Controller {
    private static final String TAG = "EmojiPageKeyboardView";
    // This state is used only by the search results view, not browsing pages.
    private final EmojiSearchGlyphMotion mSearchGlyphMotion = new EmojiSearchGlyphMotion();
    private float mSearchDrawProgress = 1f;
    private final SearchResultTouchGate mSearchTouchGate = new SearchResultTouchGate();

    void setSearchInteractionPending(final boolean pending) {
        mSearchTouchGate.setPending(pending);
        if (pending) cancelSearchInteraction();
    }

    /** Invalidate only the Search row's old gesture, never a browsing-page gesture. */
    void cancelSearchInteraction() {
        if (!mSearchTouchGate.managed()) return;
        mSearchTouchGate.invalidate();
        releaseCurrentKey(false);
        cancelLongPress();
        dismissPopupKeysPanel();
        mPointerId = MotionEvent.INVALID_POINTER_ID;
        if (getParent() != null) disallowParentInterceptTouchEvent(false);
    }

    void prepareSearchGlyphMotion(final boolean preserve) {
        mSearchGlyphMotion.capture(preserve ? getKeyboard() : null,mSearchDrawProgress);
    }

    void finishSearchGlyphMotion() {
        if (!KeyboardMotionSpec.isIos27(getContext()) || !KeyboardMotionSpec.optionalMotion(getContext())) {
            cancelSearchGlyphMotion();
            return;
        }
        mSearchGlyphMotion.retarget(getKeyboard(),SystemClock.uptimeMillis());
        mSearchDrawProgress = 0f;
        invalidateAllKeys();
    }

    void cancelSearchGlyphMotion() {
        if (mSearchGlyphMotion.clear()) invalidateAllKeys();
        mSearchDrawProgress = 1f;
    }

    @Override protected void onDraw(@NonNull final Canvas canvas) {
        if (mSearchGlyphMotion.active()) {
            if (!isShown() || !KeyboardMotionSpec.optionalMotion(getContext())) cancelSearchGlyphMotion();
            else {
                mSearchDrawProgress = mSearchGlyphMotion.progress(SystemClock.uptimeMillis());
                if (mSearchDrawProgress >= 1f) mSearchGlyphMotion.clear();
                invalidateAllKeys();
            }
        }
        super.onDraw(canvas);
        if (mSearchGlyphMotion.active()) postInvalidateOnAnimation();
    }

    @Override protected void onDrawKeyTopVisuals(@NonNull final Key key,@NonNull final Canvas canvas,
            @NonNull final Paint paint,@NonNull final KeyDrawParams params) {
        final EmojiSearchGlyphMotion.Track track = mSearchGlyphMotion.track(key.getLabel());
        if (track == null) {
            super.onDrawKeyTopVisuals(key,canvas,paint,params);
            return;
        }
        final float p = MotionContinuity.smooth(mSearchDrawProgress);
        final int originalAlpha = params.mAnimAlpha;
        final float cx = key.getDrawWidth() * .5f, cy = key.getHeight() * .5f;
        final int clip = canvas.save();
        // Keep every visual inside its current slot and never create a second touch target.
        canvas.clipRect(0f,0f,key.getDrawWidth(),key.getHeight());
        if (track.departing != null && p < .6f) {
            final int out = canvas.save();
            final float exit = MotionContinuity.smooth(p / .6f);
            final float scale = MotionContinuity.lerp(track.departingScale,.35f,exit);
            canvas.scale(scale,scale,cx,cy);
            params.mAnimAlpha = Math.round(originalAlpha * track.departingAlpha * (1f-exit));
            super.onDrawKeyTopVisuals(track.departing,canvas,paint,params);
            canvas.restoreToCount(out);
        }
        canvas.translate(track.offset * (1f-p),0f);
        final float scale = MotionContinuity.lerp(track.scale,1f,p);
        canvas.scale(scale,scale,cx,cy);
        params.mAnimAlpha = Math.round(originalAlpha * MotionContinuity.lerp(track.alpha,1f,p));
        super.onDrawKeyTopVisuals(key,canvas,paint,params);
        params.mAnimAlpha = originalAlpha;
        canvas.restoreToCount(clip);
    }
    private static final boolean LOG = false;
    private static final long KEY_PRESS_DELAY_TIME = 250;  // msec
    private static final long KEY_RELEASE_DELAY_TIME = 30;  // msec

    private static final EmojiViewCallback EMPTY_EMOJI_VIEW_CALLBACK = new EmojiViewCallback() {
        @Override
        public void onPressKey(final Key key) {}
        @Override
        public void onReleaseKey(final Key key) {}
        @Override
        public String getDescription(String emoji) {
            return null;
        }
    };

    private EmojiViewCallback mEmojiViewCallback = EMPTY_EMOJI_VIEW_CALLBACK;
    private final KeyDetector mKeyDetector = new KeyDetector();
    private KeyboardAccessibilityDelegate<EmojiPageKeyboardView> mAccessibilityDelegate;

    // Touch inputs
    private int mPointerId = MotionEvent.INVALID_POINTER_ID;
    private int mLastX, mLastY;
    private Key mCurrentKey;
    private Runnable mPendingKeyDown;
    private Runnable mPendingLongPress;
    private final Handler mHandler;

    // More keys keyboard
    private final View mPopupKeysKeyboardContainer;
    private final PopupTextView mDescriptionView;
    private final PopupKeysKeyboardView mPopupKeysKeyboardView;
    private final WeakHashMap<Key, Keyboard> mPopupKeysKeyboardCache = new WeakHashMap<>();
    private final boolean mConfigShowPopupKeysKeyboardAtTouchedPoint;
    // More keys panel (used by popup keys keyboard view)
    // TODO: Consider extending to support multiple popup keys panels
    private PopupKeysPanel mPopupKeysPanel;
    private PopupWindow mPopupWindow;

    public EmojiPageKeyboardView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.keyboardViewStyle);
    }

    public EmojiPageKeyboardView(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);
        mHandler = new Handler();

        final TypedArray keyboardViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.MainKeyboardView, defStyle, R.style.MainKeyboardView);
        final int popupKeysKeyboardLayoutId = keyboardViewAttr.getResourceId(
                R.styleable.MainKeyboardView_popupKeysKeyboardLayout, 0);
        mConfigShowPopupKeysKeyboardAtTouchedPoint = keyboardViewAttr.getBoolean(
                R.styleable.MainKeyboardView_showPopupKeysKeyboardAtTouchedPoint, false);
        keyboardViewAttr.recycle();

        final LayoutInflater inflater = LayoutInflater.from(getContext());
        mPopupKeysKeyboardContainer = inflater.inflate(popupKeysKeyboardLayoutId, null);
        mDescriptionView = mPopupKeysKeyboardContainer.findViewById(R.id.description_view);
        mPopupKeysKeyboardView = mPopupKeysKeyboardContainer.findViewById(R.id.popup_keys_keyboard_view);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final Keyboard keyboard = getKeyboard();
        if (keyboard instanceof DynamicGridKeyboard) {
            final int width = keyboard.mOccupiedWidth + getPaddingLeft() + getPaddingRight();
            final int occupiedHeight = ((DynamicGridKeyboard) keyboard).getOccupiedHeight();
            final int height = occupiedHeight + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(width, height);
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setHardwareAcceleratedDrawingEnabled(final boolean enabled) {
        super.setHardwareAcceleratedDrawingEnabled(enabled);
    }

    @Override
    protected void onDrawKeyBackground(@NonNull final Key key, @NonNull final Canvas canvas,
            @NonNull final Drawable background) {
        // Emoji glyphs already carry their own shape/color. Drawing key bubbles behind them looks
        // inconsistent across emoji fonts, so the emoji page keeps the keys visually transparent.
    }

    public void setEmojiViewCallback(final EmojiViewCallback emojiViewCallback) {
        mEmojiViewCallback = emojiViewCallback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKeyboard(@NonNull final Keyboard keyboard) {
        super.setKeyboard(keyboard);
        mKeyDetector.setKeyboard(keyboard, 0 /* correctionX */, 0 /* correctionY */);
        mPopupKeysKeyboardCache.clear();
        if (AccessibilityUtils.Companion.getInstance().isAccessibilityEnabled()) {
            if (mAccessibilityDelegate == null) {
                mAccessibilityDelegate = new KeyboardAccessibilityDelegate<>(this, mKeyDetector);
            }
            mAccessibilityDelegate.setKeyboard(keyboard);
        } else {
            mAccessibilityDelegate = null;
        }
    }

    @Nullable
    private PopupKeysPanel showPopupKeysKeyboard(@NonNull final Key key) {
        mPopupKeysKeyboardView.setVisibility(GONE);
        final PopupKeySpec[] popupKeys = key.getPopupKeys();
        if (popupKeys == null) {
            return null;
        }
        Keyboard popupKeysKeyboard = mPopupKeysKeyboardCache.get(key);
        if (popupKeysKeyboard == null) {
            final PopupKeysKeyboard.Builder builder = new PopupKeysKeyboard.Builder(
                    getContext(), key, getKeyboard(), false, 0, 0, newLabelPaint(key));
            popupKeysKeyboard = builder.build();
            mPopupKeysKeyboardCache.put(key, popupKeysKeyboard);
        }

        mPopupKeysKeyboardView.setKeyboard(popupKeysKeyboard);
        mPopupKeysKeyboardView.setVisibility(VISIBLE);
        return mPopupKeysKeyboardView;
    }

    private void dismissPopupKeysPanel() {
        if (isShowingPopupKeysPanel()) {
            mPopupKeysPanel.dismissPopupKeysPanel();
        }
    }

    public boolean isShowingPopupKeysPanel() {
        return mPopupKeysPanel != null;
    }

    @Override
    public void setLayoutGravity(int layoutGravity) {
        var layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = layoutGravity;
        mPopupKeysKeyboardContainer.setLayoutParams(layoutParams);
        mDescriptionView.setLayoutParams(layoutParams);
    }

    @Override
    public void onShowPopupKeysPanel(final PopupKeysPanel panel) {
        onDismissPopupKeysPanel();
        showFloatingPopup(panel);
        mPopupKeysPanel = panel;
    }

    private void showFloatingPopup(final PopupKeysPanel panel) {
        final View container = panel.getContainerView();
        final View rootView = getRootView();
        final int popupWidth = container.getMeasuredWidth();
        final int popupHeight = container.getMeasuredHeight();
        final int popupX = getClampedPopupX(rootView, Math.round(container.getX()), popupWidth);
        final int popupY = Math.round(container.getY());
        if (container.getParent() instanceof ViewGroup) {
            ((ViewGroup) container.getParent()).removeView(container);
        }
        container.setX(0f);
        container.setY(0f);

        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
        final PopupWindow window = new PopupWindow(
                container,
                popupWidth,
                popupHeight,
                false
        );
        window.setTouchable(true);
        window.setOutsideTouchable(false);
        window.setClippingEnabled(false);
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

        mPopupWindow = window;
        window.setOnDismissListener(() -> {
            if (mPopupWindow == window) {
                mPopupWindow = null;
            }
        });
        window.showAtLocation(rootView, Gravity.NO_GRAVITY, popupX, popupY);
        KeyboardMotionSpec.revealLongPressPopup(container);
    }

    private int getClampedPopupX(final View rootView, final int popupX, final int popupWidth) {
        final int[] rootCoords = CoordinateUtils.newInstance();
        rootView.getLocationInWindow(rootCoords);
        final int minX = CoordinateUtils.x(rootCoords);
        final int maxX = Math.max(minX, minX + rootView.getWidth() - popupWidth);
        return Math.max(minX, Math.min(maxX, popupX));
    }

    @Override
    public void onDismissPopupKeysPanel() {
        if (isShowingPopupKeysPanel()) {
            if (mPopupWindow != null) {
                mPopupWindow.dismiss();
                mPopupWindow = null;
            }
            mPopupKeysPanel.removeFromParent();
            mPopupKeysPanel = null;
        }
    }

    @Override
    public void onCancelPopupKeysPanel() {
        if (isShowingPopupKeysPanel()) {
            dismissPopupKeysPanel();
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(final AccessibilityEvent event) {
        // Don't populate accessibility event with all Emoji keys.
        return true;
    }

    private int getLongPressTimeout() {
        return Settings.getValues().mKeyLongpressTimeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onHoverEvent(final MotionEvent event) {
        final KeyboardAccessibilityDelegate<EmojiPageKeyboardView> accessibilityDelegate = mAccessibilityDelegate;
        if (accessibilityDelegate != null && AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled()) {
            return accessibilityDelegate.onHoverEvent(event);
        }
        return super.onHoverEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent e) {
        final int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (!mSearchTouchGate.begin()) return true;
            cancelSearchGlyphMotion();
        } else if (!mSearchTouchGate.continuationAllowed()) return true;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
            mSearchTouchGate.invalidate();
        return switch (action) {
            case MotionEvent.ACTION_DOWN -> {
                mPointerId = e.getPointerId(0);
                yield onDown(e);
            }
            case MotionEvent.ACTION_UP -> onUp(e);
            case MotionEvent.ACTION_MOVE -> onMove(e);
            case MotionEvent.ACTION_CANCEL -> onCancel(e);
            default -> false;
        };
    }

    private Key getKey(final int x, final int y) {
        return mKeyDetector.detectHitKey(x, y);
    }

    private void onLongPressed(final Key key) {
        if (isShowingPopupKeysPanel()) {
            return;
        }

        if (key == null) {
            if (LOG) Log.d(TAG, "Long press ignored because detected key is null");
            return;
        }

        var descriptionPanel = showDescription(key);
        final PopupKeysPanel popupKeysPanel = showPopupKeysKeyboard(key);

        final int x = mLastX;
        final int y = mLastY;
        if (popupKeysPanel != null || descriptionPanel != null) {
            mPopupKeysKeyboardContainer.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            final int[] lastCoords = CoordinateUtils.newCoordinateArray(1, x, y);
            // The popup keys keyboard is usually horizontally aligned with the center of the parent key.
            // If showPopupKeysKeyboardAtTouchedPoint is true and the key preview is disabled, the more
            // keys keyboard is placed at the touch point of the parent key.
            final int pointX = mConfigShowPopupKeysKeyboardAtTouchedPoint
                    ? CoordinateUtils.x(lastCoords)
                    : key.getX() + key.getWidth() / 2;
            final int pointY = key.getY() - getKeyboard().mVerticalGap;
            (popupKeysPanel != null? popupKeysPanel : descriptionPanel)
                            .showPopupKeysPanel(this, this, pointX, pointY, mEmojiViewCallback);
        }

        if (popupKeysPanel != null) {
            final int translatedX = popupKeysPanel.translateX(x);
            final int translatedY = popupKeysPanel.translateY(y);
            popupKeysPanel.onDownEvent(translatedX, translatedY, mPointerId, 0 /* nor used for now */);
            // No need of re-allowing parent later as we don't
            // want any scroll to append during this entire input.
            disallowParentInterceptTouchEvent(true);
        }
    }

    private PopupKeysPanel showDescription(Key key) {
        mDescriptionView.setVisibility(GONE);
        var description = mEmojiViewCallback.getDescription(key.getLabel());
        if (description == null) {
            return null;
        }

        mDescriptionView.setText(description);
        mDescriptionView.setKeyDrawParams(key, getKeyDrawParams());
        mDescriptionView.setVisibility(VISIBLE);
        return mDescriptionView;
    }

    private void registerPress(final Key key) {
        // Do not trigger key-down effect right now in case this is actually a fling action.
        mPendingKeyDown = () -> callListenerOnPressKey(key);
        mHandler.postDelayed(mPendingKeyDown, KEY_PRESS_DELAY_TIME);
    }

    private void registerLongPress(final Key key) {
        mPendingLongPress = () -> onLongPressed(key);
        mHandler.postDelayed(mPendingLongPress, getLongPressTimeout());
    }

    void callListenerOnReleaseKey(final Key releasedKey, final boolean withKeyRegistering) {
        releasedKey.onReleased();
        invalidateKey(releasedKey);
        if (withKeyRegistering) {
            mEmojiViewCallback.onReleaseKey(releasedKey);
        }
    }

    void callListenerOnPressKey(final Key pressedKey) {
        mPendingKeyDown = null;
        pressedKey.onPressed();
        invalidateKey(pressedKey);
        mEmojiViewCallback.onPressKey(pressedKey);
    }

    public void releaseCurrentKey(final boolean withKeyRegistering) {
        mHandler.removeCallbacks(mPendingKeyDown);
        mPendingKeyDown = null;
        final Key currentKey = mCurrentKey;
        if (currentKey == null) {
            return;
        }
        callListenerOnReleaseKey(currentKey, withKeyRegistering);
        mCurrentKey = null;
    }

    public void cancelLongPress() {
        mHandler.removeCallbacks(mPendingLongPress);
        mPendingLongPress = null;
    }

    public boolean onDown(final MotionEvent e) {
        final int x = (int) e.getX();
        final int y = (int) e.getY();
        final Key key = getKey(x, y);
        releaseCurrentKey(false /* withKeyRegistering */);
        mCurrentKey = key;
        if (key == null) {
            return false;
        }
        registerPress(key);

        registerLongPress(key);

        mLastX = x;
        mLastY = y;
        return true;
    }

    public boolean onUp(final MotionEvent e) {
        final int x = (int) e.getX();
        final int y = (int) e.getY();
        final Key key = getKey(x, y);
        final Runnable pendingKeyDown = mPendingKeyDown;
        final Key currentKey = mCurrentKey;
        releaseCurrentKey(false /* withKeyRegistering */);

        final boolean isShowingPopupKeysPanel = isShowingPopupKeysPanel();
        if (isShowingPopupKeysPanel) {
            final long eventTime = e.getEventTime();
            final int translatedX = mPopupKeysPanel.translateX(x);
            final int translatedY = mPopupKeysPanel.translateY(y);
            mPopupKeysPanel.onUpEvent(translatedX, translatedY, mPointerId, eventTime);
            dismissPopupKeysPanel();
        } else if (key == currentKey && pendingKeyDown != null) {
            pendingKeyDown.run();
            // Trigger key-release event a little later so that a user can see visual feedback.
            // A Search result can be replaced before a delayed release fires.
            if (mSearchTouchGate.managed()) callListenerOnReleaseKey(key, true);
            else mHandler.postDelayed(() -> callListenerOnReleaseKey(key, true), KEY_RELEASE_DELAY_TIME);
        } else if (key != null) {
            callListenerOnReleaseKey(key, true /* withRegistering */);
        }

        cancelLongPress();
        return true;
    }

    public boolean onCancel(final MotionEvent e) {
        releaseCurrentKey(false);
        dismissPopupKeysPanel();
        cancelLongPress();
        return true;
    }

    public boolean onMove(final MotionEvent e) {
        final int x = (int)e.getX();
        final int y = (int)e.getY();
        final Key key = getKey(x, y);
        final boolean isShowingPopupKeyboard = isShowingPopupKeysPanel() && mPopupKeysKeyboardView.getVisibility() == VISIBLE;

        // Touched key has changed, release previous key's callbacks and
        // re-register them for the new key.
        if (key != mCurrentKey && !isShowingPopupKeyboard) {
            releaseCurrentKey(false);
            mCurrentKey = key;
            cancelLongPress();
            if (isShowingPopupKeysPanel()) {
                onCancelPopupKeysPanel();
            }
            if (key == null) {
                return false;
            }
            registerPress(key);
            registerLongPress(key);
        }

        if (isShowingPopupKeyboard) {
            final long eventTime = e.getEventTime();
            final int translatedX = mPopupKeysPanel.translateX(x);
            final int translatedY = mPopupKeysPanel.translateY(y);
            mPopupKeysPanel.onMoveEvent(translatedX, translatedY, mPointerId, eventTime);
        }

        mLastX = x;
        mLastY = y;
        return true;
    }

    private void disallowParentInterceptTouchEvent(final boolean disallow) {
        final ViewParent parent = getParent();
        if (parent == null) {
            Log.w(TAG, "Cannot disallow touch event interception, no parent found.");
            return;
        }
        parent.requestDisallowInterceptTouchEvent(disallow);
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelSearchInteraction();
        cancelSearchGlyphMotion();
        onDismissPopupKeysPanel();
        super.onDetachedFromWindow();
    }
}
