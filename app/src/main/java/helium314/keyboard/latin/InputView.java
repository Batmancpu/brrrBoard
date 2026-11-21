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
import android.view.ViewTreeObserver;
import android.view.VelocityTracker;
import android.widget.FrameLayout;

import androidx.core.view.ViewKt;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import helium314.keyboard.accessibility.AccessibilityUtils;
import helium314.keyboard.keyboard.Key;
import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.keyboard.KeyboardSwitcher;
import helium314.keyboard.keyboard.MainKeyboardView;
import helium314.keyboard.keyboard.emoji.EmojiPageKeyboardView;
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
    private SlideConfirmationOverlay mSlideConfirmation;
    private MainKeyboardView mConfirmationKeyboard;
    private int mBoardAccessibilityBeforeConfirmation = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
    private LatinIME mOwner;
    private final int[] mStripLocation = new int[2];
    private boolean mMayDragToolbar;
    private boolean mDraggingToolbar;
    private boolean mDraggingExpandedToolbar;
    private float mToolbarDownX;
    private float mToolbarDownY;
    private VelocityTracker mToolbarVelocity;
    private ViewTreeObserver.OnPreDrawListener mEmojiGeometryProbe;

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
        mSlideConfirmation = new SlideConfirmationOverlay(getContext());
        mSlideConfirmation.setVisibility(GONE);
        addView(mSlideConfirmation, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ViewKt.doOnNextLayout(this, this::onNextLayout);
    }

    public void bindBoardControls(final LatinIME owner) {
        mOwner = owner;
        mFooter.bind(owner);
    }

    /** The footer remains the same physical layout while its controls slide away. */
    public void setFooterTrackpadMode(final boolean active) {
        if (mFooter != null) mFooter.setTrackpadMode(active);
    }

    public void resetBoardGesture() {
        cancelSlideConfirmation();
        mMayDragToolbar = false;
        mDraggingToolbar = false;
        mDraggingExpandedToolbar = false;
        releaseToolbarVelocity();
        mFooter.reset();
    }

    /** Drawn above the IME but aligned to the exact native spacebar bounds. */
    public void showSlideConfirmation(final MainKeyboardView source, final String label,
            final int accent, final Runnable onConfirmed) {
        if (source == null || mOwner == null || mSlideConfirmation == null) return;
        final Rect screen = new Rect();
        if (!source.getSpacebarBoundsOnScreen(screen)) return;
        cancelSlideConfirmation();
        final int[] rootLocation = new int[2];
        getLocationOnScreen(rootLocation);
        screen.offset(-rootLocation[0], -rootLocation[1]);
        mConfirmationKeyboard = source;
        source.setConfirmationVisualMode(true);
        if (mFooter != null) mFooter.setConfirmationMode(true);
        if (mBoardFrame != null) {
            mBoardAccessibilityBeforeConfirmation = mBoardFrame.getImportantForAccessibility();
            mBoardFrame.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        mSlideConfirmation.present(screen, label, accent,
                () -> { cancelSlideConfirmation(); onConfirmed.run(); return Unit.INSTANCE; },
                () -> { cancelSlideConfirmation(); return Unit.INSTANCE; });
    }

    public void cancelSlideConfirmation() {
        if (mSlideConfirmation != null) mSlideConfirmation.closeNow();
        if (mConfirmationKeyboard != null) {
            mConfirmationKeyboard.setConfirmationVisualMode(false);
            mConfirmationKeyboard = null;
        }
        if (mFooter != null) mFooter.setConfirmationMode(false);
        if (mBoardFrame != null && mBoardFrame.getImportantForAccessibility()
                == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
            mBoardFrame.setImportantForAccessibility(mBoardAccessibilityBeforeConfirmation);
    }

    public void refreshBoardPrivacy() {
        if (mFooter != null) mFooter.refreshPrivacyState();
    }

    /** One-shot, content-free device geometry for the unresolved ColorOS top band.
     * No editor identity, typed text, clipboard, audio, or Emoji usage is recorded.
     * A pre-draw sample observes the actual post-transition layout, not a stale
     * pre-measure height captured when the alphabet was still visible.
     */
    public void reportNextEmojiGeometry() {
        final ViewTreeObserver observer = getViewTreeObserver();
        if (!observer.isAlive()) return;
        if (mEmojiGeometryProbe != null) observer.removeOnPreDrawListener(mEmojiGeometryProbe);
        mEmojiGeometryProbe = new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                final ViewTreeObserver live = getViewTreeObserver();
                if (live.isAlive()) live.removeOnPreDrawListener(this);
                mEmojiGeometryProbe = null;
                final View emoji = findViewById(R.id.emoji_palettes_view);
                if (emoji == null || emoji.getVisibility() != VISIBLE) return true;
                final StringBuilder dimensions = new StringBuilder("geometry-only");
                appendGeometry(dimensions, "decor", getRootView());
                appendGeometry(dimensions, "inputArea", getRootView().findViewById(android.R.id.inputArea));
                appendGeometry(dimensions, "input", InputView.this);
                appendGeometry(dimensions, "frame", findViewById(R.id.main_keyboard_frame));
                appendGeometry(dimensions, "strip", findViewById(R.id.strip_container));
                appendGeometry(dimensions, "wrapper", findViewById(R.id.keyboard_view_wrapper));
                appendGeometry(dimensions, "emoji", emoji);
                appendGeometry(dimensions, "shell", findViewById(R.id.emoji_grid_shell));
                final View pager = findViewById(R.id.emoji_pager);
                appendGeometry(dimensions, "pager", pager);
                appendGeometry(dimensions, "categories", findViewById(R.id.emoji_tab_strip_container));
                final View firstRow = findFirstEmojiRow(pager);
                appendGeometry(dimensions, "firstEmojiRow", firstRow);
                appendFirstEmojiKeyBand(dimensions, firstRow);
                appendGeometry(dimensions, "footer", findViewById(R.id.board_footer));
                appendWindowInsets(dimensions, getRootView());
                dimensions.append(" orientation=")
                        .append(getResources().getConfiguration().orientation)
                        .append(" density=")
                        .append(getResources().getDisplayMetrics().density);
                final View frame = findViewById(R.id.main_keyboard_frame);
                if (frame != null) {
                    dimensions.append(" framePadding=")
                            .append(frame.getPaddingLeft()).append(',')
                            .append(frame.getPaddingTop()).append(',')
                            .append(frame.getPaddingRight()).append(',')
                            .append(frame.getPaddingBottom());
                }
                android.util.Log.i("BrrImeBounds", dimensions.toString());
                return true;
            }
        };
        observer.addOnPreDrawListener(mEmojiGeometryProbe);
    }

    /** Returns only the bounds of the visible row view; never reads Emoji text or keys. */
    private static View findFirstEmojiRow(final View pager) {
        if (pager == null) return null;
        final View row = pager.findViewById(R.id.emoji_keyboard_page);
        return row != null && row.getVisibility() == VISIBLE ? row : null;
    }

    /** Reports the drawn first-key band only; key labels and Emoji values are never inspected. */
    private static void appendFirstEmojiKeyBand(final StringBuilder report, final View row) {
        report.append(" firstKeyBand=");
        if (!(row instanceof EmojiPageKeyboardView)) {
            report.append("unavailable");
            return;
        }
        final Keyboard keyboard = ((EmojiPageKeyboardView) row).getKeyboard();
        if (keyboard == null || keyboard.getSortedKeys().isEmpty()) {
            report.append("unavailable");
            return;
        }
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        int left = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        for (final Key key : keyboard.getSortedKeys()) {
            if (key.getWidth() <= 0 || key.getHeight() <= 0) continue;
            final int keyTop = key.getY();
            if (keyTop < top) {
                top = keyTop;
                bottom = keyTop + key.getHeight();
                left = key.getX();
                right = key.getX() + key.getWidth();
            } else if (keyTop == top) {
                bottom = Math.max(bottom, keyTop + key.getHeight());
                left = Math.min(left, key.getX());
                right = Math.max(right, key.getX() + key.getWidth());
            }
        }
        if (top == Integer.MAX_VALUE) {
            report.append("empty");
            return;
        }
        final int[] location = new int[2];
        row.getLocationOnScreen(location);
        final int screenTop = location[1] + row.getPaddingTop() + top;
        report.append(location[0] + row.getPaddingLeft() + left).append(',')
                .append(screenTop).append(':').append(right - left).append('x')
                .append(bottom - top).append(" localTop=").append(top)
                .append(" viewPaddingTop=").append(row.getPaddingTop())
                .append(" keyboardTopPadding=").append(keyboard.mTopPadding)
                .append(" verticalGap=").append(keyboard.mVerticalGap);
    }

    private static void appendGeometry(final StringBuilder report, final String label,
            final View view) {
        report.append(' ').append(label).append('=');
        if (view == null) { report.append("absent"); return; }
        if (view.getVisibility() != VISIBLE) { report.append("hidden"); return; }
        final int[] location = new int[2];
        view.getLocationOnScreen(location);
        report.append(location[0]).append(',').append(location[1])
                .append(':').append(view.getWidth()).append('x').append(view.getHeight())
                .append(" measured=").append(view.getMeasuredWidth()).append('x')
                .append(view.getMeasuredHeight()).append(" localY=").append(view.getY())
                .append(" translationY=").append(view.getTranslationY())
                .append(" scrollY=").append(view.getScrollY())
                .append(" padding=").append(view.getPaddingLeft()).append(',')
                .append(view.getPaddingTop()).append(',').append(view.getPaddingRight())
                .append(',').append(view.getPaddingBottom());
        final View parent = view.getParent() instanceof View ? (View) view.getParent() : null;
        if (parent != null) {
            report.append(" parent=").append(parent.getWidth()).append('x')
                    .append(parent.getHeight()).append(" parentPadding=")
                    .append(parent.getPaddingLeft()).append(',').append(parent.getPaddingTop())
                    .append(',').append(parent.getPaddingRight()).append(',')
                    .append(parent.getPaddingBottom());
        }
        final ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            report.append(" layout=").append(params.width).append('x').append(params.height);
            if (params instanceof ViewGroup.MarginLayoutParams) {
                final ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
                report.append(" margins=").append(margins.leftMargin).append(',')
                        .append(margins.topMargin).append(',').append(margins.rightMargin)
                        .append(',').append(margins.bottomMargin);
            }
        }
    }

    private static void appendWindowInsets(final StringBuilder report, final View root) {
        final WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
        report.append(" windowInsets=");
        if (insets == null) {
            report.append("absent");
            return;
        }
        final androidx.core.graphics.Insets bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars());
        final androidx.core.graphics.Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
        final androidx.core.graphics.Insets gestures = insets.getInsets(
                WindowInsetsCompat.Type.systemGestures());
        report.append("bars:").append(bars.top).append(',').append(bars.bottom)
                .append(" ime:").append(ime.top).append(',').append(ime.bottom)
                .append(" gestures:").append(gestures.top).append(',').append(gestures.bottom);
    }

    @Override protected void onDetachedFromWindow() {
        if (mEmojiGeometryProbe != null) {
            final ViewTreeObserver observer = getViewTreeObserver();
            if (observer.isAlive()) observer.removeOnPreDrawListener(mEmojiGeometryProbe);
            mEmojiGeometryProbe = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            releaseToolbarVelocity();
            mDraggingToolbar = false;
            mDraggingExpandedToolbar = false;
            mToolbarDownX = event.getRawX();
            mToolbarDownY = event.getRawY();
            final View expandedToolbar = findViewById(R.id.access_point_menu_view);
            mDraggingExpandedToolbar = expandedToolbar != null && expandedToolbar.isShown();
            final View swipeArea = mDraggingExpandedToolbar ? expandedToolbar : mSuggestionStrip;
            final View toolbarScroll = mDraggingExpandedToolbar
                    ? findViewById(R.id.access_point_scroll) : null;
            mMayDragToolbar = mOwner != null && swipeArea != null && swipeArea.isShown()
                    && Settings.getValues().mToolbarSwipeDownToHide
                    && !AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled()
                    && !KeyboardSwitcher.getInstance().isResizeModeActive()
                    // A vertical dismiss pull is eligible only when there is no
                    // toolbar content above. The compact Toolbar rail scrolls
                    // horizontally, so vertical pulls remain available.
                    && (toolbarScroll == null || !toolbarScroll.canScrollVertically(-1));
            if (mMayDragToolbar) {
                swipeArea.getLocationOnScreen(mStripLocation);
                final float x = mToolbarDownX - mStripLocation[0];
                final float y = mToolbarDownY - mStripLocation[1];
                mMayDragToolbar = x >= 0 && x < swipeArea.getWidth()
                        && y >= 0 && y < swipeArea.getHeight();
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
                final float heightThreshold = mDraggingExpandedToolbar
                        ? Math.min(mBoardFrame.getHeight() * 0.5f,
                                96f * getResources().getDisplayMetrics().density)
                        : mBoardFrame.getHeight() * 0.5f;
                final boolean dismiss = action == MotionEvent.ACTION_UP &&
                        (distance >= heightThreshold ||
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
            final boolean clipboardBrowse = FooterGlassSourcePolicy.shouldSampleClipboard(
                    clipboardPanel, clipboardPanel && ((helium314.keyboard.keyboard.clipboard.ClipboardHistoryView) clipboard).isSearchMode());
            final boolean toolbarActive = toolbarPanel != null && toolbarPanel.getVisibility() == VISIBLE;
            final boolean panelFooter = emojiPanel
                    || clipboardPanel
                    // The access-point toolbar replaces the key rows but must keep
                    // the same bottom footer and IME window height as typing mode.
                    || toolbarActive;
            final boolean showFooter = typingFooter || panelFooter;
            // Clipboard's bottom glass now extends beneath this footer: do not
            // independently sample a second RenderNode across the same seam.
            mFooter.setPanelGlassSource(emojiBrowse ? findViewById(R.id.emoji_pager)
                    : toolbarActive ? findViewById(R.id.access_point_scroll) : null,
                    emojiBrowse);
            // All three internal panels expose the same direct ABC return route.
            mFooter.render(showFooter, showFooter && Settings.getValues().mShowsVoiceInputKey,
                    FooterPanelPolicy.returnToAbc(emojiPanel, clipboardPanel, toolbarActive),
                    toolbarActive);
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
