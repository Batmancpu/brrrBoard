/*
 * Copyright (C) 2011 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.latin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.VelocityTracker;
import android.widget.FrameLayout;

import androidx.core.view.ViewKt;

import helium314.keyboard.accessibility.AccessibilityUtils;
import helium314.keyboard.keyboard.KeyboardSwitcher;
import helium314.keyboard.keyboard.MainKeyboardView;
import helium314.keyboard.latin.common.ColorType;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.suggestions.MoreSuggestionsView;
import helium314.keyboard.latin.suggestions.SuggestionStripView;
import kotlin.Unit;


public final class InputView extends FrameLayout {
    private final Rect mInputViewRect = new Rect();
    private MainKeyboardView mMainKeyboardView;
    private KeyboardTopPaddingForwarder mKeyboardTopPaddingForwarder;
    private MoreSuggestionsViewCanceler mMoreSuggestionsViewCanceler;
    private MotionEventForwarder<?, ?> mActiveForwarder;
    private View mBoardFrame;
    private SuggestionStripView mSuggestionStrip;
    private BoardFooterView mFooter;
    private LatinIME mOwner;
    private final int[] mStripLocation = new int[2];
    private boolean mMayDragToolbar;
    private boolean mDraggingToolbar;
    private float mToolbarDownX;
    private float mToolbarDownY;
    private VelocityTracker mToolbarVelocity;

    public InputView(final Context context, final AttributeSet attrs) {
        super(context, attrs, 0);
        setClipChildren(false);
        setClipToPadding(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final SuggestionStripView suggestionStripView =
                findViewById(R.id.suggestion_strip_view);
        mMainKeyboardView = findViewById(R.id.keyboard_view);
        mBoardFrame = findViewById(R.id.main_keyboard_frame);
        mSuggestionStrip = suggestionStripView;
        mFooter = findViewById(R.id.board_footer);
        mKeyboardTopPaddingForwarder = new KeyboardTopPaddingForwarder(
                mMainKeyboardView, suggestionStripView);
        mMoreSuggestionsViewCanceler = new MoreSuggestionsViewCanceler(
                mMainKeyboardView, suggestionStripView);
        ViewKt.doOnNextLayout(this, this::onNextLayout);
    }

    public void bindBoardControls(final LatinIME owner) {
        mOwner = owner;
        mFooter.bind(owner);
    }

    public void resetBoardGesture() {
        mMayDragToolbar = false;
        mDraggingToolbar = false;
        releaseToolbarVelocity();
        mFooter.reset();
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            releaseToolbarVelocity();
            mDraggingToolbar = false;
            mToolbarDownX = event.getRawX();
            mToolbarDownY = event.getRawY();
            mMayDragToolbar = mOwner != null && mSuggestionStrip.isShown()
                    && Settings.getValues().mToolbarSwipeDownToHide
                    && !AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled()
                    && !KeyboardSwitcher.getInstance().isResizeModeActive();
            if (mMayDragToolbar) {
                mSuggestionStrip.getLocationOnScreen(mStripLocation);
                final float x = mToolbarDownX - mStripLocation[0];
                final float y = mToolbarDownY - mStripLocation[1];
                mMayDragToolbar = x >= 0 && x < mSuggestionStrip.getWidth()
                        && y >= 0 && y < mSuggestionStrip.getHeight();
                if (mMayDragToolbar) {
                    mToolbarVelocity = VelocityTracker.obtain();
                    mToolbarVelocity.addMovement(event);
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE && mMayDragToolbar) {
            if (mToolbarVelocity != null) mToolbarVelocity.addMovement(event);
            final float dx = Math.abs(event.getRawX() - mToolbarDownX);
            final float dy = event.getRawY() - mToolbarDownY;
            final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (!mDraggingToolbar && dy > slop && dy > dx * 0.85f) {
                mDraggingToolbar = true;
                mOwner.cancelBoardDictation();
                final MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
            } else if (!mDraggingToolbar && (dy < -slop ||
                    (dx > slop * 2 && dx > Math.max(0f, dy) * 1.5f))) {
                mMayDragToolbar = false;
                releaseToolbarVelocity();
            }
            if (mDraggingToolbar) {
                // Let Android/ColorOS own the IME hide transition. Translating the
                // surface here causes a second movement and a blur flash on release.
                return true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_POINTER_DOWN) {
            mMayDragToolbar = false;
            if (mDraggingToolbar) {
                mDraggingToolbar = false;
                final float distance = Math.max(0f, event.getRawY() - mToolbarDownY);
                float velocity = 0f;
                if (mToolbarVelocity != null) {
                    mToolbarVelocity.addMovement(event);
                    mToolbarVelocity.computeCurrentVelocity(1000);
                    velocity = mToolbarVelocity.getYVelocity();
                }
                // A deliberate slow pull still travels halfway. A quick downward
                // flick can dismiss sooner without the abrupt reset seen on device.
                final float flingVelocity = 850f * getResources().getDisplayMetrics().density;
                final boolean dismiss = action == MotionEvent.ACTION_UP &&
                        (distance >= mBoardFrame.getHeight() * 0.5f ||
                        (distance >= ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2
                                && velocity > flingVelocity));
                releaseToolbarVelocity();
                if (dismiss) mOwner.onSwipeDownOnToolbar();
                return true;
            }
            releaseToolbarVelocity();
        }
        return super.dispatchTouchEvent(event);
    }

    private void releaseToolbarVelocity() {
        if (mToolbarVelocity != null) {
            mToolbarVelocity.recycle();
            mToolbarVelocity = null;
        }
    }

    public void setKeyboardTopPadding(final int keyboardTopPadding) {
        mKeyboardTopPaddingForwarder.setKeyboardTopPadding(keyboardTopPadding);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (mFooter != null && mMainKeyboardView != null) {
            final helium314.keyboard.keyboard.Keyboard keyboard = mMainKeyboardView.getKeyboard();
            final boolean typingFooter = keyboard != null && (keyboard.mId.isAlphaOrSymbolKeyboard()
                    || keyboard.mId.isNumberLayout())
                    && mMainKeyboardView.getVisibility() == VISIBLE;
            // Emoji and Clipboard used to lose the footer entirely. That made their
            // visible keyboard surface shorter than the alphabetic keyboard.
            final View emoji = findViewById(R.id.emoji_palettes_view);
            final View clipboard = findViewById(R.id.clipboard_history_view);
            final View toolbarPanel = findViewById(R.id.access_point_menu_view);
            final boolean emojiPanel = emoji != null && emoji.getVisibility() == VISIBLE;
            // Search hides the emoji pager but leaves the EmojiPalettesView
            // visible while hosting its alphabet keyboard. Never manually
            // record that GONE pager into the ABC/mic footer's live blur.
            final boolean emojiBrowse = emojiPanel
                    && !((helium314.keyboard.keyboard.emoji.EmojiPalettesView) emoji).isSearchMode();
            final boolean clipboardPanel = clipboard != null && clipboard.getVisibility() == VISIBLE;
            final boolean toolbarActive = toolbarPanel != null && toolbarPanel.getVisibility() == VISIBLE;
            final boolean panelFooter = emojiPanel
                    || clipboardPanel
                    // The access-point toolbar replaces the key rows but must keep
                    // the same bottom footer and IME window height as typing mode.
                    || toolbarActive;
            final boolean showFooter = typingFooter || panelFooter;
            mFooter.setPanelGlassSource(emojiBrowse ? findViewById(R.id.emoji_pager)
                    : clipboardPanel ? findViewById(R.id.clipboard_list)
                    : toolbarActive ? findViewById(R.id.access_point_scroll) : null,
                    emojiBrowse);
            // All three internal panels expose the same direct ABC return route.
            mFooter.render(showFooter, showFooter && Settings.getValues().mShowsVoiceInputKey,
                    FooterPanelPolicy.returnToAbc(emojiPanel, clipboardPanel, toolbarActive));
        }
        final View resizeOverlay = findViewById(R.id.keyboard_resize_overlay);
        final int overlayVisibility = resizeOverlay == null ? View.GONE : resizeOverlay.getVisibility();
        if (resizeOverlay != null && overlayVisibility != View.GONE) {
            resizeOverlay.setVisibility(View.GONE);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (resizeOverlay == null || overlayVisibility == View.GONE) {
            return;
        }
        resizeOverlay.setVisibility(overlayVisibility);
        final ViewGroup.LayoutParams params = resizeOverlay.getLayoutParams();
        final int overlayHeight = params != null && params.height > 0
                ? params.height
                : getMeasuredHeight();
        final int overlayWidthSpec = MeasureSpec.makeMeasureSpec(
                Math.max(0, getMeasuredWidth() - getPaddingLeft() - getPaddingRight()),
                MeasureSpec.EXACTLY);
        final int overlayHeightSpec = MeasureSpec.makeMeasureSpec(
                Math.max(1, overlayHeight),
                MeasureSpec.EXACTLY);
        resizeOverlay.measure(overlayWidthSpec, overlayHeightSpec);
    }

    @Override
    protected boolean dispatchHoverEvent(final MotionEvent event) {
        if (AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled()
                && mMainKeyboardView.isShowingPopupKeysPanel()) {
            // With accessibility mode on, discard hover events while a popup keys keyboard is shown.
            // The {@link PopupKeysKeyboard} receives hover events directly from the platform.
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent me) {
        if (KeyboardSwitcher.getInstance().isResizeModeActive()) {
            return false;
        }
        final Rect rect = mInputViewRect;
        getGlobalVisibleRect(rect);
        final int index = me.getActionIndex();
        final int x = (int)me.getX(index) + rect.left;
        final int y = (int)me.getY(index) + rect.top;

        // The touch events that hit the top padding of keyboard should be forwarded to
        // {@link SuggestionStripView}.
        if (mKeyboardTopPaddingForwarder.onInterceptTouchEvent(x, y, me)) {
            mActiveForwarder = mKeyboardTopPaddingForwarder;
            return true;
        }

        // To cancel {@link MoreSuggestionsView}, we should intercept a touch event to
        // {@link MainKeyboardView} and dismiss the {@link MoreSuggestionsView}.
        if (mMoreSuggestionsViewCanceler.onInterceptTouchEvent(x, y, me)) {
            mActiveForwarder = mMoreSuggestionsViewCanceler;
            return true;
        }

        mActiveForwarder = null;
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent me) {
        if (mActiveForwarder == null) {
            return super.onTouchEvent(me);
        }

        final Rect rect = mInputViewRect;
        getGlobalVisibleRect(rect);
        final int index = me.getActionIndex();
        final int x = (int)me.getX(index) + rect.left;
        final int y = (int)me.getY(index) + rect.top;
        return mActiveForwarder.onTouchEvent(x, y, me);
    }

    private Unit onNextLayout(View v) {
        refreshBoardFrameBackground();

        // Work around inset application being unreliable
        requestApplyInsets();
        return null;
    }

    public void refreshBoardFrameBackground() {
        if (mBoardFrame == null || Settings.getValues() == null) return;
        Settings.getValues().mColors.setBackground(mBoardFrame, ColorType.MAIN_BACKGROUND);
        mBoardFrame.invalidate();
    }

    /**
     * This class forwards series of {@link MotionEvent}s from <code>SenderView</code> to
     * <code>ReceiverView</code>.
     *
     * @param <SenderView> a {@link View} that may send a {@link MotionEvent} to <ReceiverView>.
     * @param <ReceiverView> a {@link View} that receives forwarded {@link MotionEvent} from
     *     <SenderView>.
     */
    private static abstract class
            MotionEventForwarder<SenderView extends View, ReceiverView extends View> {
        protected final SenderView mSenderView;
        protected final ReceiverView mReceiverView;

        protected final Rect mEventSendingRect = new Rect();
        protected final Rect mEventReceivingRect = new Rect();

        public MotionEventForwarder(final SenderView senderView, final ReceiverView receiverView) {
            mSenderView = senderView;
            mReceiverView = receiverView;
        }

        // Return true if a touch event of global coordinate x, y needs to be forwarded.
        protected abstract boolean needsToForward(final int x, final int y);

        // Translate global x-coordinate to <code>ReceiverView</code> local coordinate.
        protected int translateX(final int x) {
            return x - mEventReceivingRect.left;
        }

        // Translate global y-coordinate to <code>ReceiverView</code> local coordinate.
        protected int translateY(final int y) {
            return y - mEventReceivingRect.top;
        }

        /**
         * Callback when a {@link MotionEvent} is forwarded.
         * @param me the motion event to be forwarded.
         */
        protected void onForwardingEvent(final MotionEvent me) {}

        // Returns true if a {@link MotionEvent} is needed to be forwarded to
        // <code>ReceiverView</code>. Otherwise returns false.
        public boolean onInterceptTouchEvent(final int x, final int y, final MotionEvent me) {
            // Forwards a {link MotionEvent} only if both <code>SenderView</code> and
            // <code>ReceiverView</code> are visible.
            if (mSenderView.getVisibility() != View.VISIBLE ||
                    mReceiverView.getVisibility() != View.VISIBLE) {
                return false;
            }
            mSenderView.getGlobalVisibleRect(mEventSendingRect);
            if (!mEventSendingRect.contains(x, y)) {
                return false;
            }

            if (me.getActionMasked() == MotionEvent.ACTION_DOWN) {
                // If the down event happens in the forwarding area, successive
                // {@link MotionEvent}s should be forwarded to <code>ReceiverView</code>.
                return needsToForward(x, y);
            }

            return false;
        }

        // Returns true if a {@link MotionEvent} is forwarded to <code>ReceiverView</code>.
        // Otherwise returns false.
        public boolean onTouchEvent(final int x, final int y, final MotionEvent me) {
            mReceiverView.getGlobalVisibleRect(mEventReceivingRect);
            // Translate global coordinates to <code>ReceiverView</code> local coordinates.
            me.setLocation(translateX(x), translateY(y));
            mReceiverView.dispatchTouchEvent(me);
            onForwardingEvent(me);
            return true;
        }
    }

    /**
     * This class forwards {@link MotionEvent}s happened in the top padding of
     * {@link MainKeyboardView} to {@link SuggestionStripView}.
     */
    private static class KeyboardTopPaddingForwarder
            extends MotionEventForwarder<MainKeyboardView, SuggestionStripView> {
        private int mKeyboardTopPadding;

        public KeyboardTopPaddingForwarder(final MainKeyboardView mainKeyboardView,
                final SuggestionStripView suggestionStripView) {
            super(mainKeyboardView, suggestionStripView);
        }

        public void setKeyboardTopPadding(final int keyboardTopPadding) {
            mKeyboardTopPadding = keyboardTopPadding;
        }

        private boolean isInKeyboardTopPadding(final int y) {
            return y < mEventSendingRect.top + mKeyboardTopPadding;
        }

        @Override
        protected boolean needsToForward(final int x, final int y) {
            // Forwarding an event only when {@link MainKeyboardView} is visible.
            // Because the visibility of {@link MainKeyboardView} is controlled by its parent
            // view in {@link KeyboardSwitcher#setMainKeyboardFrame()}, we should check the
            // visibility of the parent view.
            final View mainKeyboardFrame = (View)mSenderView.getParent();
            return mainKeyboardFrame.getVisibility() == View.VISIBLE && isInKeyboardTopPadding(y);
        }

        @Override
        protected int translateY(final int y) {
            final int translatedY = super.translateY(y);
            if (isInKeyboardTopPadding(y)) {
                // The forwarded event should have coordinates that are inside of the target.
                return Math.min(translatedY, mEventReceivingRect.height() - 1);
            }
            return translatedY;
        }
    }

    /**
     * This class forwards {@link MotionEvent}s happened in the {@link MainKeyboardView} to
     * {@link SuggestionStripView} when the {@link MoreSuggestionsView} is showing.
     * {@link SuggestionStripView} dismisses {@link MoreSuggestionsView} when it receives any event
     * outside of it.
     */
    private static class MoreSuggestionsViewCanceler
            extends MotionEventForwarder<MainKeyboardView, SuggestionStripView> {
        public MoreSuggestionsViewCanceler(final MainKeyboardView mainKeyboardView,
                final SuggestionStripView suggestionStripView) {
            super(mainKeyboardView, suggestionStripView);
        }

        @Override
        protected boolean needsToForward(final int x, final int y) {
            return mReceiverView.isShowingMoreSuggestionPanel() && mEventSendingRect.contains(x, y);
        }

        @Override
        protected void onForwardingEvent(final MotionEvent me) {
            if (me.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mReceiverView.dismissMoreSuggestionsPanel();
            }
        }
    }
}
