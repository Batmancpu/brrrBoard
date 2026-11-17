/*
 * Copyright (C) 2013 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.keyboard.emoji;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.content.res.ColorStateList;
import androidx.core.graphics.ColorUtils;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import helium314.keyboard.event.HapticEvent;
import helium314.keyboard.keyboard.Key;
import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.keyboard.KeyboardActionListener;
import helium314.keyboard.keyboard.KeyboardId;
import helium314.keyboard.keyboard.KeyboardLayoutSet;
import helium314.keyboard.keyboard.KeyboardTheme;
import helium314.keyboard.keyboard.KeyboardSwitcher;
import helium314.keyboard.keyboard.KeyboardView;
import helium314.keyboard.keyboard.MainKeyboardView;
import helium314.keyboard.keyboard.PanelMaterial;
import helium314.keyboard.keyboard.PointerTracker;
import helium314.keyboard.keyboard.internal.KeyDrawParams;
import helium314.keyboard.keyboard.internal.KeyVisualAttributes;
import helium314.keyboard.keyboard.internal.KeyboardBuilder;
import helium314.keyboard.keyboard.internal.KeyboardParams;
import helium314.keyboard.keyboard.internal.keyboard_parser.EmojiParserKt;
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode;
import helium314.keyboard.latin.AudioAndHapticFeedbackManager;
import helium314.keyboard.latin.LatinIME;
import helium314.keyboard.latin.dictionary.Dictionary;
import helium314.keyboard.latin.dictionary.DictionaryFactory;
import helium314.keyboard.latin.R;
import helium314.keyboard.latin.RichInputMethodManager;
import helium314.keyboard.latin.RichInputMethodSubtype;
import helium314.keyboard.latin.Suggest;
import helium314.keyboard.latin.SuggestedWords;
import helium314.keyboard.latin.SingleDictionaryFacilitator;
import helium314.keyboard.latin.common.ColorType;
import helium314.keyboard.latin.common.Colors;
import helium314.keyboard.latin.common.Constants;
import helium314.keyboard.latin.common.InputPointers;
import helium314.keyboard.latin.common.StringUtilsKt;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.settings.SettingsValues;
import helium314.keyboard.latin.utils.DictionaryInfoUtils;
import helium314.keyboard.latin.utils.KtxKt;
import helium314.keyboard.latin.utils.Log;
import helium314.keyboard.latin.utils.ResourceUtils;
import helium314.keyboard.latin.utils.ToolbarMode;


import java.util.List;
import java.util.ArrayList;

import static helium314.keyboard.latin.common.Constants.NOT_A_COORDINATE;

/**
 * View class to implement Emoji palettes.
 * The Emoji keyboard consists of group of views layout/emoji_palettes_view.
 * <ol>
 * <li> Emoji category tabs.
 * <li> Delete button.
 * <li> Emoji keyboard pages that can be scrolled by swiping horizontally or by selecting a tab.
 * <li> Back to main keyboard button and enter button.
 * </ol>
 * Because of the above reasons, this class doesn't extend {@link KeyboardView}.
 */
public final class EmojiPalettesView extends LinearLayout
        implements View.OnClickListener, EmojiViewCallback {
    private static volatile SingleDictionaryFacilitator sDictionaryFacilitator;
    private static volatile SingleDictionaryFacilitator sSearchDictionaryFacilitator;

    private boolean initialized = false;
    private final Colors mColors;
    private final EmojiLayoutParams mEmojiLayoutParams;
    private LinearLayout mTabStrip;
    private EmojiCategoryPageIndicatorView mEmojiCategoryPageIndicatorView;
    private KeyboardActionListener mKeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER;
    private KeyboardActionListener mMainKeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER;
    private final EmojiCategory mEmojiCategory;
    private ViewPager2 mPager;
    private EmojiCategoryPagerAdapter mCategoryPagerAdapter;
    private MainKeyboardView mBottomRowKeyboard;
    private View mSearchBarContainer;
    private ImageButton mSearchBackButton;
    private EditText mSearchEditText;
    private ImageButton mSearchClearButton;
    private HorizontalScrollView mSearchResultsScrollView;
    private EmojiPageKeyboardView mSearchResultsView;
    private TextView mSearchEmptyView;
    private View mSearchKeyboardPlaceholder;
    private DynamicGridKeyboard mSearchResultsKeyboard;
    private KeyboardParams mSearchKeyboardParams;
    private int mSearchResultsKeyboardWidth;
    private int mSearchBaseResultsColumnCount = 1;
    private float mSearchKeyWidth;
    private float mSearchKeyHeight;
    private boolean mSearchMode = false;
    private boolean mUpdatingSearchText = false;
    private String mSearchQuery = "";
    private int mCurrentSearchKeyboardElementId = KeyboardId.ELEMENT_ALPHABET;
    private LinearLayout mContextSmartSection;
    private LinearLayout mContextSmartRow;
    private boolean mHasContextSmartPicks;
    private boolean mCompactContextPanel;
    private EditorInfo mContextSmartEditor;
    private boolean mEmojiGridScrollActive;
    private final SparseIntArray mCategoryScrollRows = new SparseIntArray();

    public EmojiPalettesView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.emojiPalettesViewStyle);
    }

    public EmojiPalettesView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        mColors = Settings.getValues().mColors;
        final KeyboardLayoutSet.Builder builder = new KeyboardLayoutSet.Builder(context, null);
        final Resources res = context.getResources();
        mEmojiLayoutParams = new EmojiLayoutParams(res);
        builder.setSubtype(RichInputMethodSubtype.Companion.getEmojiSubtype());
        builder.setKeyboardGeometry(ResourceUtils.getKeyboardWidth(context, Settings.getValues()),
                mEmojiLayoutParams.getEmojiKeyboardHeight());
        final KeyboardLayoutSet layoutSet = builder.build();
        final TypedArray emojiPalettesViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.EmojiPalettesView, defStyle, R.style.EmojiPalettesView);
        mEmojiCategory = new EmojiCategory(context, layoutSet, emojiPalettesViewAttr);
        emojiPalettesViewAttr.recycle();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final Resources res = getContext().getResources();
        final helium314.keyboard.latin.settings.SettingsValues settings = Settings.getValues();
        final int abcHeight = ResourceUtils.getKeyboardHeight(res, settings);
        final boolean persistentEmojiEnabled = helium314.keyboard.latin.utils.KtxKt.prefs(getContext()).getBoolean(Settings.PREF_PERSISTENT_EMOJI_ROW, helium314.keyboard.latin.settings.Defaults.PREF_PERSISTENT_EMOJI_ROW);
        final int emojiRowHeight = persistentEmojiEnabled ? (int) (41 * res.getDisplayMetrics().density) : 0;
        // The normal alphabetic keyboard has a suggestion strip above its keys.
        // If the emoji category strip is hidden, reclaim that exact height inside
        // the emoji panel instead. Both modes now retain the shared bottom footer.
        final View strip = getRootView().findViewById(R.id.strip_container);
        final int accessoryHeight = settings.mToolbarMode == ToolbarMode.HIDDEN ? 0
                : res.getDimensionPixelSize(R.dimen.config_suggestions_strip_height);
        final int availableHeight = switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec);
            default -> Integer.MAX_VALUE;
        };
        int finalHeight = helium314.keyboard.latin.SharedPanelGeometry.panelBodyHeight(
                abcHeight, emojiRowHeight, accessoryHeight,
                strip != null && strip.getVisibility() == View.VISIBLE, availableHeight);
        if (mSearchMode) {
            // Search is a complete typing keyboard plus one result row and a
            // search field. The fixed-height grid mode cannot accommodate it.
            final View searchBar = findViewById(R.id.emoji_search_bar_container);
            final View results = findViewById(R.id.emoji_search_results_scroll);
            final int barHeight = searchBar == null ? 0 : searchBar.getLayoutParams().height;
            final int resultHeight = results == null ? 0 : results.getLayoutParams().height;
            finalHeight = helium314.keyboard.latin.SharedPanelGeometry.emojiSearchBodyHeight(
                    abcHeight, barHeight, resultHeight, finalHeight, availableHeight);
        }
        // The Smart row must not sacrifice the two visible Emoji rows in a
        // short landscape IME. The keyboard shell height itself never grows.
        final boolean compact = finalHeight < (int) (260 * res.getDisplayMetrics().density);
        if (mCompactContextPanel != compact) {
            mCompactContextPanel = compact;
            updateContextSmartVisibility();
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
        final int requestedWidth = ResourceUtils.getKeyboardWidth(getContext(), settings)
                + getPaddingLeft() + getPaddingRight();
        final int availableWidth = switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec);
            default -> Integer.MAX_VALUE;
        };
        final int width = Math.min(requestedWidth, availableWidth);
        if (mEmojiCategoryPageIndicatorView != null) {
            mEmojiCategoryPageIndicatorView.mWidth = width;
        }
        setMeasuredDimension(width, finalHeight);
    }

    private void addTab(final LinearLayout host, final int categoryId) {
        final TabImageView iconView = new TabImageView(getContext());
        mColors.setBackground(iconView, ColorType.STRIP_BACKGROUND);
        mColors.setColor(iconView, ColorType.EMOJI_CATEGORY);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        final int iconPadding = (int) (6 * getContext().getResources().getDisplayMetrics().density);
        iconView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
        iconView.setImageResource(mEmojiCategory.getCategoryTabIcon(categoryId));
        iconView.setContentDescription(mEmojiCategory.getAccessibilityDescription(categoryId));
        iconView.setTag((long) categoryId); // use long for simple difference to int used for key codes
        host.addView(iconView);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        iconView.setOnClickListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void initialize() { // needs to be delayed for access to EmojiTabStrip, which is not a child of this view
        if (initialized) return;
        mEmojiCategory.initialize();
        final View tabStripView = KeyboardSwitcher.getInstance().getEmojiTabStrip();
        mTabStrip = (LinearLayout) tabStripView.findViewById(R.id.emoji_tab_strip);
        for (final EmojiCategory.CategoryProperties properties : mEmojiCategory.getShownCategories()) {
            addTab(mTabStrip, properties.mCategoryId);
        }

        mPager = findViewById(R.id.emoji_pager);
        mPager.setOffscreenPageLimit(1);
        mCategoryScrollRows.put(mEmojiCategory.getCurrentCategoryId(),
                mEmojiCategory.getCurrentCategoryPageId() * Math.max(1, mEmojiCategory.getRowsPerPage()));
        mCategoryPagerAdapter = new EmojiCategoryPagerAdapter(mEmojiCategory, this,
                mCategoryScrollRows, new EmojiCategoryPagerAdapter.RowScrollCallback() {
                    @Override public void onCategoryRowScrolled(final int categoryId, final int firstRow) {
                        invalidatePanelGlass();
                        if (categoryId == mEmojiCategory.getCurrentCategoryId()) {
                            final int page = firstRow / Math.max(1, mEmojiCategory.getRowsPerPage());
                            if (page != mEmojiCategory.getCurrentCategoryPageId())
                                mEmojiCategory.setCurrentCategoryPageId(page);
                        }
                    }
                    @Override public void onRowScrollStateChanged(final int categoryId, final int state) {
                        if (categoryId != mEmojiCategory.getCurrentCategoryId()) return;
                        mEmojiGridScrollActive = state != RecyclerView.SCROLL_STATE_IDLE;
                        if (!mEmojiGridScrollActive) updateContextSmartVisibility();
                    }
                });
        mPager.setAdapter(mCategoryPagerAdapter);
        mPager.setCurrentItem(Math.max(0, mCategoryPagerAdapter.indexOf(
                mEmojiCategory.getCurrentCategoryId())), false);
        PanelMaterial.extendIntoFooter(findViewById(R.id.emoji_grid_shell), mPager);
        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageScrolled(final int position, final float positionOffset,
                    final int positionOffsetPixels) {
                invalidatePanelGlass();
            }
            @Override public void onPageSelected(final int position) {
                setCurrentCategoryId(mCategoryPagerAdapter.categoryAt(position), true);
            }
        });

        mEmojiLayoutParams.setEmojiListProperties(findViewById(R.id.emoji_grid_shell));
        mEmojiCategoryPageIndicatorView = findViewById(R.id.emoji_category_page_id_view);
        mEmojiCategoryPageIndicatorView.setVisibility(View.GONE);
        mEmojiLayoutParams.setCategoryPageIdViewProperties(mEmojiCategoryPageIndicatorView);
        mBottomRowKeyboard = findViewById(R.id.bottom_row_keyboard);
        mBottomRowKeyboard.addOnLayoutChangeListener((v, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom <= top) return;
            final View glass = findViewById(R.id.emoji_bottom_glass);
            final int height = bottom - top + (int) (8 * getResources().getDisplayMetrics().density);
            final ViewGroup.LayoutParams params = glass.getLayoutParams();
            if (params.height != height) {
                params.height = height;
                glass.setLayoutParams(params);
            }
        });
        mContextSmartSection = findViewById(R.id.emoji_context_smart_section);
        mContextSmartRow = findViewById(R.id.emoji_context_smart_row);
        final TextView smartTitle = findViewById(R.id.emoji_context_smart_title);
        final TextView recentLabel = findViewById(R.id.emoji_context_recent_label);
        final int labelColor = ColorUtils.setAlphaComponent(mColors.get(ColorType.TOOL_BAR_KEY), 0xC0);
        if (smartTitle != null) smartTitle.setTextColor(labelColor);
        if (recentLabel != null) recentLabel.setTextColor(labelColor);
        setupInlineSearchViews();
        setCurrentCategoryId(mEmojiCategory.getCurrentCategoryId(), true);
        initialized = true;
    }

    private void invalidatePanelGlass() {
        final View header = findViewById(R.id.emoji_header_scrim);
        final View bottom = findViewById(R.id.emoji_bottom_glass);
        if (header != null) header.postInvalidateOnAnimation();
        if (bottom != null) bottom.postInvalidateOnAnimation();
        final helium314.keyboard.latin.BoardFooterView footer =
                getRootView().findViewById(R.id.board_footer);
        if (footer != null) footer.invalidatePanelGlass();
    }

    private void setupInlineSearchViews() {
        mSearchBarContainer = findViewById(R.id.emoji_search_bar_container);
        mSearchBackButton = findViewById(R.id.emoji_search_back_button);
        mSearchEditText = findViewById(R.id.emoji_search_edit_text);
        mSearchClearButton = findViewById(R.id.emoji_search_clear_button);
        mSearchResultsScrollView = findViewById(R.id.emoji_search_results_scroll);
        mSearchResultsView = findViewById(R.id.emoji_search_results_view);
        mSearchEmptyView = findViewById(R.id.emoji_search_empty);
        mSearchKeyboardPlaceholder = findViewById(R.id.emoji_search_keyboard_placeholder);

        setupSearchResultKeyboard();
        setupSearchResultsScrolling();
        setupSearchField();
        setupSearchClickListeners();
        applySearchTheme();
    }

    private void setupSearchResultKeyboard() {
        setupSearchResultKeyboard(ResourceUtils.getKeyboardWidth(getContext(), Settings.getValues()));
    }

    private void setupSearchResultKeyboard(final int keyboardWidth) {
        if (mSearchResultsView == null) {
            return;
        }
        mSearchResultsKeyboardWidth = keyboardWidth;
        final int baseWidth = ResourceUtils.getKeyboardWidth(getContext(), Settings.getValues());
        final KeyboardLayoutSet layoutSet = new KeyboardLayoutSet.Builder(getContext(), null)
                .setSubtype(RichInputMethodSubtype.Companion.getEmojiSubtype())
                .setKeyboardGeometry(baseWidth, mEmojiLayoutParams.getEmojiKeyboardHeight())
                .build();
        mSearchResultsKeyboard = DynamicGridKeyboard.ofPagedRowCount(KtxKt.prefs(getContext()),
                layoutSet.getKeyboard(KeyboardId.ELEMENT_EMOJI_RECENTS), 1,
                KeyboardId.ELEMENT_EMOJI_CATEGORY16, keyboardWidth, baseWidth);
        if (keyboardWidth == baseWidth) {
            mSearchBaseResultsColumnCount = Math.max(1, mSearchResultsKeyboard.getOccupiedColumnCount());
        }
        final KeyboardBuilder builder = new KeyboardBuilder(getContext(), new KeyboardParams());
        builder.load(mSearchResultsKeyboard.mId);
        mSearchKeyboardParams = builder.mParams;
        final kotlin.Pair<Float, Float> keyDimensions =
                EmojiParserKt.getEmojiKeyDimensions(mSearchKeyboardParams, getContext());
        mSearchKeyWidth = keyDimensions.getFirst();
        mSearchKeyHeight = keyDimensions.getSecond();

        mSearchResultsView.setKeyboard(mSearchResultsKeyboard);
        mSearchResultsView.setEmojiViewCallback(this);
        mSearchResultsView.setVisibility(mSearchMode ? View.VISIBLE : View.GONE);
        mColors.setBackground(mSearchResultsView, ColorType.MAIN_BACKGROUND);
        mSearchResultsView.setPadding(0, 3, 0, 3);
    }

    private void setupSearchResultsScrolling() {
        if (mSearchResultsScrollView == null) {
            return;
        }
        mSearchResultsScrollView.setSmoothScrollingEnabled(true);
        mSearchResultsScrollView.setHorizontalScrollBarEnabled(false);
    }

    private int getSearchResultsKeyboardWidth(final int resultCount) {
        final int baseWidth = ResourceUtils.getKeyboardWidth(getContext(), Settings.getValues());
        final int columnsPerScreen = Math.max(1, mSearchBaseResultsColumnCount);
        final int maxResultsPerScreen = columnsPerScreen;
        final int pages = Math.max(1, (resultCount + maxResultsPerScreen - 1) / maxResultsPerScreen);
        return baseWidth * pages;
    }

    private void setupSearchField() {
        mSearchEditText.setShowSoftInputOnFocus(false);
        mSearchEditText.setSingleLine(true);
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {}

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {}

            @Override
            public void afterTextChanged(final Editable s) {
                if (mUpdatingSearchText) {
                    return;
                }
                mSearchQuery = s == null ? "" : s.toString();
                updateSearchQueryUi();
                searchEmoji(mSearchQuery);
            }
        });
        mSearchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            mSearchEditText.setCursorVisible(mSearchMode && hasFocus);
        });
    }

    private void setupSearchClickListeners() {
        if (mSearchBackButton != null) {
            mSearchBackButton.setOnClickListener(v -> {
                performTapFeedback(v);
                exitSearchMode(false);
            });
        }
        if (mSearchClearButton != null) {
            mSearchClearButton.setOnClickListener(v -> {
                performTapFeedback(v);
                setSearchText("", 0);
                focusSearchField(false);
            });
        }
        if (mSearchEditText != null) {
            mSearchEditText.setOnClickListener(v -> focusSearchField(false));
        }
    }

    private void applySearchTheme() {
        if (mSearchBarContainer != null) {
            mColors.setBackground(mSearchBarContainer, ColorType.STRIP_BACKGROUND);
        }
        if (mSearchBackButton != null) {
            mColors.setColor(mSearchBackButton, ColorType.FUNCTIONAL_KEY_TEXT);
            mSearchBackButton.setBackground(createBackButtonBackground(mColors));
            mSearchBackButton.setPadding(0, 0, 0, 0);
            mSearchBackButton.setScaleType(ImageView.ScaleType.CENTER);
        }
        if (mSearchEditText != null) {
            final int toolBarColor = mColors.get(ColorType.TOOL_BAR_KEY);
            mSearchEditText.setTextColor(toolBarColor);
            mSearchEditText.setHintTextColor((toolBarColor & 0x00FFFFFF) | 0x80000000);
        }
        final ImageView searchIcon = findViewById(R.id.emoji_search_icon);
        if (searchIcon != null) {
            mColors.setColor(searchIcon, ColorType.TOOL_BAR_KEY);
        }
        if (mSearchClearButton != null) {
            mColors.setColor(mSearchClearButton, ColorType.TOOL_BAR_KEY);
        }
    }

    /**
     * Called from {@link EmojiPageKeyboardView} through {@link android.view.View.OnClickListener}
     * interface to handle non-canceled touch-up events from View-based elements such as the space
     * bar.
     */
    @Override
    public void onClick(View v) {
        final Object tag = v.getTag();
        if (tag instanceof Long) {
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_PRESS);
            final int categoryId = ((Long) tag).intValue();
            if (categoryId != mEmojiCategory.getCurrentCategoryId()) {
                setCurrentCategoryId(categoryId, false);
                updateEmojiCategoryPageIdView();
            }
        }
    }

    /**
     * Called from {@link EmojiPageKeyboardView} through {@link EmojiViewCallback}
     * interface to handle touch events from non-View-based elements such as Emoji buttons.
     */
    @Override
    public void onPressKey(final Key key) {
        final int code = key.getCode();
        mKeyboardActionListener.onPressKey(code, 0, true, HapticEvent.KEY_PRESS);
    }

    /**
     * Called from {@link EmojiPageKeyboardView} through {@link EmojiViewCallback}
     * interface to handle touch events from non-View-based elements such as Emoji buttons.
     * This may be called without any prior call to {@link EmojiViewCallback#onPressKey(Key)}.
     */
    @Override
    public void onReleaseKey(final Key key) {
        addRecentKey(key);
        final int code = key.getCode();
        if (code == KeyCode.MULTIPLE_CODE_POINTS) {
            mKeyboardActionListener.onTextInput(key.getOutputText());
        } else {
            mKeyboardActionListener.onCodeInput(code, NOT_A_COORDINATE, NOT_A_COORDINATE, false);
        }
        mKeyboardActionListener.onReleaseKey(code, false);
        if (Settings.getValues().mAlphaAfterEmojiInEmojiView)
            mKeyboardActionListener.onCodeInput(KeyCode.ALPHA, NOT_A_COORDINATE, NOT_A_COORDINATE, false);
    }

    @Override
    public String getDescription(String emoji) {
        final SingleDictionaryFacilitator facilitator = sDictionaryFacilitator;
        if (facilitator == null) {
            return null;
        }

        var wordProperty = facilitator.getWordProperty(EmojiParserKt.getEmojiNeutralVersion(emoji));
        if (wordProperty == null || ! wordProperty.mHasShortcuts) {
            return null;
        }

        return wordProperty.mShortcutTargets.get(0).mWord;
    }

    public void setHardwareAcceleratedDrawingEnabled(final boolean enabled) {
        if (!enabled) return;
        // TODO: Should use LAYER_TYPE_SOFTWARE when hardware acceleration is off?
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void startEmojiPalettes(final KeyVisualAttributes keyVisualAttr,
               final EditorInfo editorInfo, final KeyboardActionListener keyboardActionListener) {
        initialize();
        PanelMaterial.apply(getContext(), Settings.getValues().mColors,
                findViewById(R.id.emoji_header_scrim),
                findViewById(R.id.emoji_bottom_glass), mPager);
        getRecentsKeyboard().flushPendingRecentKeys();
        updateAdaptiveEmojiOrder();
        mMainKeyboardActionListener = keyboardActionListener;
        mKeyboardActionListener = keyboardActionListener;
        if (mSearchMode) {
            exitSearchMode(false, false);
        }
        setSearchText("", 0);
        updateContextSmartPicks(editorInfo);

        // Recents can grow between openings; rebuild the virtual columns after
        // flushing pending keys without discarding the saved category/page.
        if (mCategoryPagerAdapter != null) mCategoryPagerAdapter.refresh();
        setCurrentCategoryId(mEmojiCategory.getCurrentCategoryId(), true);

        setupBottomRowKeyboard(editorInfo, keyboardActionListener);
        final KeyDrawParams params = new KeyDrawParams();
        params.updateParams(mEmojiLayoutParams.getBottomRowKeyboardHeight(), keyVisualAttr);
        setupSidePadding();
        initDictionaryFacilitator();
    }

    public boolean isSearchMode() {
        return mSearchMode;
    }

    public void enterSearchMode() {
        initialize();

        if (mSearchMode) {
            focusSearchField(true);
            return;
        }
        mSearchMode = true;
        updateContextSmartVisibility();
        mCurrentSearchKeyboardElementId = KeyboardId.ELEMENT_ALPHABET;

        mPager.setVisibility(View.GONE);
        findViewById(R.id.emoji_grid_shell).setVisibility(View.GONE);
        mEmojiCategoryPageIndicatorView.setVisibility(View.GONE);
        if (mBottomRowKeyboard != null) {
            mBottomRowKeyboard.setVisibility(View.GONE);
        }
        final View tabStripView = KeyboardSwitcher.getInstance().getEmojiTabStrip();
        if (tabStripView != null) {
            tabStripView.setVisibility(View.GONE);
        }
        revealWithoutFlash(mSearchBarContainer);
        if (mSearchResultsScrollView != null) {
            revealWithoutFlash(mSearchResultsScrollView);
        }
        if (mSearchEmptyView != null) mSearchEmptyView.setVisibility(View.GONE);
        mSearchResultsView.setVisibility(View.VISIBLE);

        KeyboardSwitcher.getInstance().notifyEnteringSearchMode();
        swapKeyboardToEmojiSearch(true, false);
        updateSearchKeyboard();
        focusSearchField(true);
        updateSearchQueryUi();
        searchEmoji(mSearchQuery);
        requestLayout();
    }

    public void exitSearchMode(final boolean clearQuery) {
        exitSearchMode(clearQuery, true);
    }

    public void exitSearchMode(final boolean clearQuery, final boolean restoreViews) {

        if (!mSearchMode && !clearQuery) {
            return;
        }
        if (clearQuery) {
            setSearchText("", 0);
        }
        mSearchMode = false;
        updateContextSmartVisibility();
        if (mSearchEditText != null) {
            mSearchEditText.setCursorVisible(false);

            mSearchEditText.clearFocus();
        }
        if (mSearchBarContainer != null) {
            mSearchBarContainer.setVisibility(View.GONE);
        }
        if (mSearchResultsScrollView != null) {
            mSearchResultsScrollView.setVisibility(View.GONE);
        }
        if (mSearchEmptyView != null) mSearchEmptyView.setVisibility(View.GONE);
        if (mSearchResultsView != null) {
            mSearchResultsView.setVisibility(View.GONE);
        }
        if (mPager != null) {
            revealWithoutFlash(mPager);
        }
        findViewById(R.id.emoji_grid_shell).setVisibility(View.VISIBLE);
        if (mEmojiCategoryPageIndicatorView != null) {
            mEmojiCategoryPageIndicatorView.setVisibility(View.GONE);
        }
        if (mBottomRowKeyboard != null) {
            revealWithoutFlash(mBottomRowKeyboard);
        }
        swapKeyboardToEmojiSearch(false, restoreViews);
        if (restoreViews) {
            restoreEmojiCategoryStrip();
        }
        final MainKeyboardView mainKeyboardView = KeyboardSwitcher.getInstance().getMainKeyboardView();
        if (mainKeyboardView != null) {
            mainKeyboardView.setKeyboardActionListener(mMainKeyboardActionListener);
        }
        PointerTracker.switchTo(mBottomRowKeyboard != null ? mBottomRowKeyboard : mainKeyboardView);
        KeyboardSwitcher.getInstance().logTypingListenerInvariant(
                "EmojiSearch.exit", true /* logWhenOk */);
        requestLayout();
        if (restoreViews) {
            post(this::restoreEmojiCategoryStrip);
        }
    }

    public boolean handleBackPress() {
        if (mSearchMode) {
            exitSearchMode(false);
            return true;
        }
        return false;
    }

    private void restoreEmojiCategoryStrip() {
        final KeyboardSwitcher switcher = KeyboardSwitcher.getInstance();
        final View stripContainer = switcher.getStripContainer();
        final View tabStripView = switcher.getEmojiTabStrip();
        if (stripContainer != null) {
            revealWithoutFlash(stripContainer);
        }
        if (tabStripView != null) {
            revealWithoutFlash(tabStripView);
        }
    }

    private void revealWithoutFlash(final View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        if (isAnimationsDisabled()) {
            view.setAlpha(1f);
        } else {
            view.setAlpha(0f);
            view.animate().alpha(1f).setDuration(PanelMaterial.TRANSITION_MS).start();
        }
    }



    private void swapKeyboardToEmojiSearch(final boolean enter, final boolean hideKeyboard) {
        final KeyboardSwitcher switcher = KeyboardSwitcher.getInstance();
        final MainKeyboardView keyboardView = switcher.getMainKeyboardView();
        final View wrapperView = switcher.getWrapperView();
        if (keyboardView == null || !(wrapperView instanceof ViewGroup) || mSearchKeyboardPlaceholder == null) {
            return;
        }
        final ViewGroup wrapper = (ViewGroup) wrapperView;
        final ViewParent currentParent = keyboardView.getParent();
        if (enter) {
            keyboardView.animate().cancel();
            keyboardView.setAlpha(1f);
            if (currentParent != this) {
                if (currentParent instanceof ViewGroup) {
                    ((ViewGroup) currentParent).removeView(keyboardView);
                }
                final int index = indexOfChild(mSearchKeyboardPlaceholder);
                addView(keyboardView, index >= 0 ? index : getChildCount());
            }
            keyboardView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            keyboardView.setVisibility(View.VISIBLE);
            switcher.getStripContainer().setVisibility(View.GONE);
        } else if (currentParent == this) {
            removeView(keyboardView);
            wrapper.addView(keyboardView, 0);
            keyboardView.setAlpha(1f);
            if (hideKeyboard) {
                keyboardView.setVisibility(View.GONE);
                switcher.getStripContainer().setVisibility(View.GONE);
            }
        }
    }

    private void updateSearchKeyboard() {
        final KeyboardSwitcher switcher = KeyboardSwitcher.getInstance();
        final MainKeyboardView keyboardView = switcher.getMainKeyboardView();
        if (keyboardView == null) {
            return;
        }

        keyboardView.setKeyboardActionListener(mSearchKeyboardActionListener);
        PointerTracker.switchTo(keyboardView);
        switch (mCurrentSearchKeyboardElementId) {
            case KeyboardId.ELEMENT_ALPHABET:
                switcher.setAlphabetKeyboard();
                break;
            case KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED:
                switcher.setAlphabetManualShiftedKeyboard();
                break;
            case KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED:
                switcher.setAlphabetShiftLockedKeyboard();
                break;
            case KeyboardId.ELEMENT_SYMBOLS:
                switcher.setSymbolsKeyboard();
                break;
            case KeyboardId.ELEMENT_SYMBOLS_SHIFTED:
                switcher.setSymbolsShiftedKeyboard();
                break;
            case KeyboardId.ELEMENT_NUMPAD:
                switcher.setNumpadKeyboard();
                break;
            default:
                mCurrentSearchKeyboardElementId = KeyboardId.ELEMENT_ALPHABET;
                switcher.setAlphabetKeyboard();
                break;
        }
        keyboardView.setKeyboardActionListener(mSearchKeyboardActionListener);
        PointerTracker.switchTo(keyboardView);
    }

    private void performTapFeedback(final View view) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                KeyCode.NOT_SPECIFIED, view, HapticEvent.KEY_PRESS);
    }

    private void focusSearchField(final boolean moveCursorToEnd) {
        if (mSearchEditText == null) {
            return;
        }

        mSearchEditText.requestFocus();
        mSearchEditText.setCursorVisible(mSearchMode);
        if (moveCursorToEnd) {
            mSearchEditText.setSelection(mSearchEditText.getText().length());
        }
    }

    private void updateSearchQueryUi() {
        if (mSearchClearButton != null) {
            mSearchClearButton.setVisibility(mSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (mSearchEditText != null) {
            mSearchEditText.setCursorVisible(mSearchMode && mSearchEditText.hasFocus());
        }
    }

    private void setSearchText(final String text, final int selection) {
        mSearchQuery = text == null ? "" : text;
        if (mSearchEditText != null) {
            final int safeSelection = Math.max(0, Math.min(selection, mSearchQuery.length()));
            if (!mSearchQuery.contentEquals(mSearchEditText.getText())) {
                mUpdatingSearchText = true;
                try {
                    mSearchEditText.getText().replace(0, mSearchEditText.getText().length(), mSearchQuery);
                } finally {
                    mUpdatingSearchText = false;
                }
            }
            mSearchEditText.setSelection(safeSelection);
        }
        updateSearchQueryUi();
        searchEmoji(mSearchQuery);
    }

    private void searchEmoji(final String text) {
        if (mSearchResultsKeyboard == null || mSearchResultsView == null) {
            return;
        }
        initSearchDictionaryFacilitator();
        final ArrayList<String> emojis = new ArrayList<>();
        final SingleDictionaryFacilitator facilitator = sSearchDictionaryFacilitator;
        if (facilitator != null) {
            final List<String> tokens = StringUtilsKt.splitOnWhitespace(text == null ? "" : text);
            for (final SuggestedWords.SuggestedWordInfo suggestion : facilitator.getSuggestions(tokens)) {
                if (suggestion.isEmoji()) {
                    emojis.add(EmojiParserKt.getEmojiDefaultVersion(suggestion.getWord()));
                }
            }
        }

        final int keyboardWidth = getSearchResultsKeyboardWidth(emojis.size());
        if (keyboardWidth != mSearchResultsKeyboardWidth) {
            setupSearchResultKeyboard(keyboardWidth);
        }
        mSearchResultsKeyboard.removeAllKeys();
        for (final String emoji : emojis) {
            final String popupSpec = EmojiParserKt.getEmojiPopupSpec(emoji);
            final Key.KeyParams keyParams = new Key.KeyParams(
                    emoji,
                    EmojiParserKt.getCode(emoji),
                    popupSpec != null ? EmojiParserKt.EMOJI_HINT_LABEL : null,
                    popupSpec,
                    Key.LABEL_FLAGS_FONT_NORMAL,
                    mSearchKeyboardParams);
            keyParams.mAbsoluteWidth = mSearchKeyWidth;
            keyParams.mAbsoluteHeight = mSearchKeyHeight;
            mSearchResultsKeyboard.addKeyLast(keyParams.createKey());
        }
        if (mSearchEmptyView != null) {
            final boolean noMatches = text != null && !text.isBlank() && emojis.isEmpty();
            mSearchEmptyView.setVisibility(mSearchMode && noMatches ? View.VISIBLE : View.GONE);
            if (mSearchResultsScrollView != null)
                mSearchResultsScrollView.setVisibility(mSearchMode && noMatches ? View.GONE : View.VISIBLE);
        }
        if (mSearchResultsScrollView != null) {
            mSearchResultsScrollView.scrollTo(0, 0);
        }
        mSearchResultsView.invalidate();
        mSearchResultsView.requestLayout();
    }

    private static synchronized void initSearchDictionaryFacilitator(final Context context) {
        final var locale = RichInputMethodManager.getInstance().getCurrentSubtype().getLocale();
        final SingleDictionaryFacilitator facilitator = sSearchDictionaryFacilitator;
        if (facilitator != null && facilitator.isForLocale(locale)) {
            return;
        }
        if (sSearchDictionaryFacilitator != null) {
            sSearchDictionaryFacilitator.closeDictionaries();
            sSearchDictionaryFacilitator = null;
        }
        try {
            final var dictFile = DictionaryInfoUtils.getCachedDictForLocaleAndType(
                    locale, Dictionary.TYPE_EMOJI, context);
            final var dictionary = dictFile != null ? DictionaryFactory.getDictionary(dictFile, locale) : null;
            if (dictionary != null) {
                sSearchDictionaryFacilitator = new SingleDictionaryFacilitator(dictionary);
            }
        } catch (Exception e) {
            Log.e("EmojiPalettesView", "Failed to load emoji search dictionary", e);
        }
    }

    private void initSearchDictionaryFacilitator() {
        initSearchDictionaryFacilitator(getContext());
    }

    private int searchSelectionStart() {
        return mSearchEditText == null ? 0 : Math.max(0, Math.min(mSearchEditText.getSelectionStart(), mSearchEditText.length()));
    }

    private int searchSelectionEnd() {
        return mSearchEditText == null ? 0 : Math.max(0, Math.min(mSearchEditText.getSelectionEnd(), mSearchEditText.length()));
    }

    private int searchSelectionMin() {
        return Math.min(searchSelectionStart(), searchSelectionEnd());
    }

    private int searchSelectionMax() {
        return Math.max(searchSelectionStart(), searchSelectionEnd());
    }

    private void syncSearchQueryFromField() {
        if (mSearchEditText == null) {
            return;
        }
        mSearchQuery = mSearchEditText.getText().toString();
        updateSearchQueryUi();
        searchEmoji(mSearchQuery);
    }

    private void insertSearchText(final String text) {
        if (text == null || text.isEmpty() || mSearchEditText == null) {
            return;
        }
        final Editable editable = mSearchEditText.getText();
        final int start = searchSelectionMin();
        final int end = searchSelectionMax();
        editable.replace(start, end, text);
        mSearchEditText.setSelection(start + text.length());
        syncSearchQueryFromField();
    }

    private void deleteSearchTextBeforeCursor() {
        if (mSearchEditText == null) {
            return;
        }
        final Editable editable = mSearchEditText.getText();
        final int start = searchSelectionMin();
        final int end = searchSelectionMax();
        if (start != end) {
            editable.delete(start, end);
            mSearchEditText.setSelection(start);
            syncSearchQueryFromField();
            return;
        }
        if (start <= 0) {
            return;
        }
        final int deleteFrom = editable.toString().offsetByCodePoints(start, -1);
        editable.delete(deleteFrom, start);
        mSearchEditText.setSelection(deleteFrom);
        syncSearchQueryFromField();
    }

    private void moveSearchCursorByCodePoints(final int delta) {
        if (delta == 0 || mSearchEditText == null) {
            return;
        }
        final String text = mSearchEditText.getText().toString();
        if (text.isEmpty()) {
            return;
        }
        final int start = searchSelectionMin();
        final int end = searchSelectionMax();
        final int newPosition;
        if (start != end) {
            newPosition = delta < 0 ? start : end;
        } else {
            try {
                newPosition = text.offsetByCodePoints(start, delta);
            } catch (IndexOutOfBoundsException e) {
                mSearchEditText.setSelection(delta < 0 ? 0 : text.length());
                updateSearchQueryUi();
                return;
            }
        }
        mSearchEditText.setSelection(Math.max(0, Math.min(newPosition, text.length())));
        updateSearchQueryUi();
    }

    private static boolean isSearchWordCodePoint(final int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '_' || codePoint == '-';
    }

    private int findSearchWordBoundary(final int position, final boolean forward) {
        final String text = mSearchEditText.getText().toString();
        int index = Math.max(0, Math.min(position, text.length()));
        if (forward) {
            while (index < text.length() && !isSearchWordCodePoint(text.codePointAt(index))) {
                index = text.offsetByCodePoints(index, 1);
            }
            while (index < text.length() && isSearchWordCodePoint(text.codePointAt(index))) {
                index = text.offsetByCodePoints(index, 1);
            }
            return index;
        }
        while (index > 0 && !isSearchWordCodePoint(text.codePointBefore(index))) {
            index = text.offsetByCodePoints(index, -1);
        }
        while (index > 0 && isSearchWordCodePoint(text.codePointBefore(index))) {
            index = text.offsetByCodePoints(index, -1);
        }
        return index;
    }

    private void moveSearchCursorByWord(final boolean forward) {
        if (mSearchEditText == null) {
            return;
        }
        final int start = searchSelectionMin();
        final int end = searchSelectionMax();
        final int anchor = start == end ? start : (forward ? end : start);
        mSearchEditText.setSelection(findSearchWordBoundary(anchor, forward));
        updateSearchQueryUi();
    }

    private void selectSearchWord() {
        if (mSearchEditText == null || mSearchEditText.length() == 0) {
            return;
        }
        int cursor = searchSelectionEnd();
        final String text = mSearchEditText.getText().toString();
        if (cursor == text.length() && cursor > 0 && isSearchWordCodePoint(text.codePointBefore(cursor))) {
            cursor = text.offsetByCodePoints(cursor, -1);
        }
        while (cursor < text.length() && !isSearchWordCodePoint(text.codePointAt(cursor))) {
            cursor = text.offsetByCodePoints(cursor, 1);
        }
        if (cursor >= text.length()) {
            return;
        }
        final int start = findSearchWordBoundary(cursor, false);
        final int end = findSearchWordBoundary(cursor, true);
        if (start < end) {
            mSearchEditText.setSelection(start, end);
        }
    }

    private void copySearchText(final boolean selectionOnly) {
        if (mSearchEditText == null) {
            return;
        }
        final int start = selectionOnly ? searchSelectionMin() : 0;
        final int end = selectionOnly ? searchSelectionMax() : mSearchEditText.length();
        if (start == end) {
            if (selectionOnly && mSearchEditText.length() > 0) {
                copyTextToClipboard(mSearchEditText.getText().toString());
            }
            return;
        }
        copyTextToClipboard(mSearchEditText.getText().subSequence(start, end));
    }

    private void cutSearchSelection() {
        if (mSearchEditText == null) {
            return;
        }
        final int start = searchSelectionMin();
        final int end = searchSelectionMax();
        if (start == end) {
            return;
        }
        copyTextToClipboard(mSearchEditText.getText().subSequence(start, end));
        mSearchEditText.getText().delete(start, end);
        mSearchEditText.setSelection(start);
        syncSearchQueryFromField();
    }

    private void copyTextToClipboard(final CharSequence text) {
        if (text == null || text.length() == 0) {
            return;
        }
        final ClipboardManager clipboard =
                (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("emoji_search", text));
        }
    }

    private void pasteIntoSearchField() {
        final ClipboardManager clipboard =
                (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip() || clipboard.getPrimaryClip() == null
                || clipboard.getPrimaryClip().getItemCount() == 0) {
            return;
        }
        final CharSequence text = clipboard.getPrimaryClip().getItemAt(0).coerceToText(getContext());
        if (text != null) {
            insertSearchText(text.toString());
        }
    }

    private void dispatchSearchShortcut(final int keyCode, final int metaState) {
        if (mSearchEditText == null) {
            return;
        }
        final long now = SystemClock.uptimeMillis();
        mSearchEditText.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState));
        mSearchEditText.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState));
        syncSearchQueryFromField();
    }

    private void handleSearchCodeInput(final int primaryCode, final int x, final int y, final boolean isKeyRepeat) {
        switch (primaryCode) {
            case KeyCode.DELETE:
                deleteSearchTextBeforeCursor();
                return;
            case Constants.CODE_ENTER:
                syncSearchQueryFromField();
                focusSearchField(false);
                return;
            case KeyCode.ARROW_LEFT:
                moveSearchCursorByCodePoints(-1);
                return;
            case KeyCode.ARROW_RIGHT:
                moveSearchCursorByCodePoints(1);
                return;
            case KeyCode.WORD_LEFT:
                moveSearchCursorByWord(false);
                return;
            case KeyCode.WORD_RIGHT:
                moveSearchCursorByWord(true);
                return;
            case KeyCode.MOVE_START_OF_LINE:
            case KeyCode.MOVE_START_OF_PAGE:
            case KeyCode.PAGE_UP:
                if (mSearchEditText != null) {
                    mSearchEditText.setSelection(0);
                    updateSearchQueryUi();
                }
                return;
            case KeyCode.MOVE_END_OF_LINE:
            case KeyCode.MOVE_END_OF_PAGE:
            case KeyCode.PAGE_DOWN:
                if (mSearchEditText != null) {
                    mSearchEditText.setSelection(mSearchEditText.length());
                    updateSearchQueryUi();
                }
                return;
            case KeyCode.CLIPBOARD_SELECT_ALL:
                if (mSearchEditText != null) {
                    mSearchEditText.selectAll();
                }
                return;
            case KeyCode.CLIPBOARD_SELECT_WORD:
                selectSearchWord();
                return;
            case KeyCode.CLIPBOARD_COPY:
                copySearchText(true);
                return;
            case KeyCode.CLIPBOARD_COPY_ALL:
                copySearchText(false);
                return;
            case KeyCode.CLIPBOARD_CUT:
                cutSearchSelection();
                return;
            case KeyCode.CLIPBOARD_PASTE:
                pasteIntoSearchField();
                return;
            case KeyCode.UNDO:
                dispatchSearchShortcut(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON);
                return;
            case KeyCode.REDO:
                dispatchSearchShortcut(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON);
                return;
            case KeyCode.SHIFT:
                mCurrentSearchKeyboardElementId = switch (mCurrentSearchKeyboardElementId) {
                    case KeyboardId.ELEMENT_ALPHABET -> KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED;
                    case KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED -> KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED;
                    case KeyboardId.ELEMENT_ALPHABET_SHIFT_LOCKED -> KeyboardId.ELEMENT_ALPHABET;
                    case KeyboardId.ELEMENT_SYMBOLS -> KeyboardId.ELEMENT_SYMBOLS_SHIFTED;
                    case KeyboardId.ELEMENT_SYMBOLS_SHIFTED -> KeyboardId.ELEMENT_SYMBOLS;
                    default -> KeyboardId.ELEMENT_ALPHABET;
                };
                updateSearchKeyboard();
                return;
            case KeyCode.SYMBOL:
            case KeyCode.SYMBOL_ALPHA:
                mCurrentSearchKeyboardElementId =
                        (mCurrentSearchKeyboardElementId == KeyboardId.ELEMENT_SYMBOLS
                                || mCurrentSearchKeyboardElementId == KeyboardId.ELEMENT_SYMBOLS_SHIFTED)
                                ? KeyboardId.ELEMENT_ALPHABET : KeyboardId.ELEMENT_SYMBOLS;
                updateSearchKeyboard();
                return;
            case KeyCode.ALPHA:
                mCurrentSearchKeyboardElementId = KeyboardId.ELEMENT_ALPHABET;
                updateSearchKeyboard();
                return;
            case KeyCode.NUMPAD:
                mCurrentSearchKeyboardElementId = KeyboardId.ELEMENT_NUMPAD;
                updateSearchKeyboard();
                return;
            case KeyCode.EMOJI:
                exitSearchMode(false);
                return;
            default:
                break;
        }

        if (primaryCode >= Constants.CODE_SPACE) {
            insertSearchText(new String(Character.toChars(primaryCode)));
            if (mCurrentSearchKeyboardElementId == KeyboardId.ELEMENT_ALPHABET_MANUAL_SHIFTED) {
                mCurrentSearchKeyboardElementId = KeyboardId.ELEMENT_ALPHABET;
                updateSearchKeyboard();
            }
            return;
        }

        mMainKeyboardActionListener.onCodeInput(primaryCode, x, y, isKeyRepeat);
    }

    private LatinIME getLatinIME() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof LatinIME) {
                return (LatinIME) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private final KeyboardActionListener mSearchKeyboardActionListener = new KeyboardActionListener.Adapter() {
        @Override
        public void onPressKey(final int primaryCode, final int repeatCount, final boolean isSinglePointer,
                final HapticEvent hapticEvent) {
            mMainKeyboardActionListener.onPressKey(primaryCode, repeatCount, isSinglePointer, hapticEvent);
        }

        @Override
        public void onLongPressKey(final int primaryCode) {
            mMainKeyboardActionListener.onLongPressKey(primaryCode);
        }

        @Override
        public void onReleaseKey(final int primaryCode, final boolean withSliding) {
            mMainKeyboardActionListener.onReleaseKey(primaryCode, withSliding);
        }

        @Override
        public boolean onKeyDown(final int keyCode, final KeyEvent keyEvent) {
            return mSearchEditText != null && mSearchEditText.dispatchKeyEvent(keyEvent);
        }

        @Override
        public boolean onKeyUp(final int keyCode, final KeyEvent keyEvent) {
            return mSearchEditText != null && mSearchEditText.dispatchKeyEvent(keyEvent);
        }

        @Override
        public void onCodeInput(final int primaryCode, final int x, final int y, final boolean isKeyRepeat) {
            handleSearchCodeInput(primaryCode, x, y, isKeyRepeat);
        }

        @Override
        public void onTextInput(final String text) {
            insertSearchText(text);
        }

        @Override
        public void onStartBatchInput() {
            focusSearchField(false);
        }

        @Override
        public void onEndBatchInput(final InputPointers batchPointers) {
            if (batchPointers == null) {
                return;
            }
            final LatinIME latinIME = getLatinIME();
            final MainKeyboardView keyboardView = KeyboardSwitcher.getInstance().getMainKeyboardView();
            final Keyboard keyboard = keyboardView == null ? null : keyboardView.getKeyboard();
            if (latinIME == null || keyboard == null) {
                return;
            }
            latinIME.getKlipySearchGestureSuggestion(batchPointers, keyboard, new Suggest.OnGetSuggestedWordsCallback() {
                @Override
                public void onGetSuggestedWords(final SuggestedWords suggestedWords) {
                    if (suggestedWords == null || suggestedWords.isEmpty()) {
                        return;
                    }
                    final String word = suggestedWords.getWord(0);
                    if (word != null && !word.isBlank()) {
                        post(() -> insertSearchText(word));
                    }
                }
            });
        }

        @Override
        public void onCancelBatchInput() {
            updateSearchQueryUi();
        }

        @Override
        public boolean onHorizontalSpaceSwipe(final int steps) {
            moveSearchCursorByCodePoints(steps);
            return true;
        }

        @Override
        public void onMoveDeletePointer(final int steps) {
            if (mSearchEditText == null) {
                return;
            }
            final int cursorEnd = searchSelectionEnd();
            final int cursorStart = searchSelectionStart();
            final int newStart = Math.max(0, Math.min(cursorStart + steps, mSearchEditText.length()));
            if (newStart <= cursorEnd) {
                mSearchEditText.setSelection(newStart, cursorEnd);
            }
        }

        @Override
        public void onUpWithDeletePointerActive() {
            if (mSearchEditText == null) {
                return;
            }
            final int start = searchSelectionMin();
            final int end = searchSelectionMax();
            if (start < end) {
                mSearchEditText.getText().delete(start, end);
                mSearchEditText.setSelection(start);
                syncSearchQueryFromField();
            }
        }

        @Override
        public boolean toggleNumpad(final boolean withSliding, final boolean forceReturnToAlpha) {
            mCurrentSearchKeyboardElementId = KeyboardId.ELEMENT_NUMPAD;
            updateSearchKeyboard();
            return true;
        }
    };

    void addRecentKey(final Key key) {
        if (Settings.getValues().mIncognitoModeEnabled) {
            // We do not want to log recent keys while being in incognito
            return;
        }
        final String emojiStr = key.getOutputText() != null ? key.getOutputText() : (key.getCode() > 0 ? new String(Character.toChars(key.getCode())) : null);
        if (emojiStr != null) {
            AdaptiveEmojiEngine.recordEmojiUsage(getContext(), emojiStr);
        }
        // Defer visible recents reordering until the panel is closed to avoid RecyclerView
        // rebinds/layout shifts while the user is tapping emojis.
        if (getVisibility() == VISIBLE) {
            getRecentsKeyboard().addPendingKey(key);
            return;
        }
        getRecentsKeyboard().addKeyFirst(key);
        if (initialized && mCategoryPagerAdapter != null) mCategoryPagerAdapter.refresh();
    }

    public void addRecentEmoji(final String emoji) {
        if (Settings.getValues().mIncognitoModeEnabled) {
            return;
        }
        final Key emojiKey = mEmojiCategory.findEmojiKey(emoji);
        if (emojiKey != null) {
            // addRecentKey updates the adaptive count and recent list exactly once.
            addRecentKey(emojiKey);
        }
    }

    public static class AdaptiveEmojiEngine {
        private static final String PREF_ADAPTIVE_METADATA = "pref_adaptive_emoji_metadata";
        private static final String PREF_FAST_ROW_CACHE = "pref_adaptive_emoji_fast_row_cache";
        private static final String PREF_FAST_ROW_LAST_REFRESH = "pref_adaptive_emoji_fast_row_last_refresh";
        private static final long FAST_ROW_REFRESH_INTERVAL_MILLIS = 12L * 60L * 60L * 1000L;
        private static final int MAX_FAST_ROW_EMOJI_COUNT = 48;
        private static final double RECENCY_DECAY_SECONDS = 7.0 * 24.0 * 60.0 * 60.0;

        public static void recordEmojiUsage(@NonNull final Context context, @NonNull final String emoji) {
            new Thread(() -> {
                synchronized (AdaptiveEmojiEngine.class) {
                    try {
                        final android.content.SharedPreferences prefs = helium314.keyboard.latin.utils.KtxKt.prefs(context);
                        final String jsonStr = prefs.getString(PREF_ADAPTIVE_METADATA, "{}");
                        org.json.JSONObject root;
                        try {
                            root = new org.json.JSONObject(jsonStr);
                        } catch (Exception e) {
                            root = new org.json.JSONObject();
                        }

                        final long now = System.currentTimeMillis();
                        org.json.JSONObject record = root.optJSONObject(emoji);
                        if (record == null) {
                            record = new org.json.JSONObject();
                            try {
                                record.put("lastUsed", now);
                                record.put("freq", 1);
                                record.put("burst", 1);
                                root.put(emoji, record);
                            } catch (Exception ignored) {}
                        } else {
                            long lastUsed = record.optLong("lastUsed", now);
                            int freq = record.optInt("freq", 0);
                            int burst = record.optInt("burst", 0);

                            if (now - lastUsed < 300_000L) { // 5 minutes window for burst
                                burst++;
                            } else {
                                burst = 1; // reset burst if outside window
                            }
                            freq++;
                            try {
                                record.put("lastUsed", now);
                                record.put("freq", freq);
                                record.put("burst", burst);
                                root.put(emoji, record);
                            } catch (Exception ignored) {}
                        }

                        prefs.edit()
                                .putString(PREF_ADAPTIVE_METADATA, root.toString())
                                .remove(PREF_FAST_ROW_CACHE)
                                .remove(PREF_FAST_ROW_LAST_REFRESH)
                                .apply();
                    } catch (Exception e) {
                        Log.e("AdaptiveEmojiEngine", "Failed to record emoji usage asynchronously", e);
                    }
                }
            }).start();
        }

        public static boolean shouldRefreshFastRow(@NonNull final Context context) {
            final android.content.SharedPreferences prefs = helium314.keyboard.latin.utils.KtxKt.prefs(context);
            final long now = System.currentTimeMillis();
            return shouldRefreshFastRow(prefs, readCachedFastRow(prefs), now);
        }

        public static java.util.List<String> getFastRowEmojis(@NonNull final Context context) {
            return getFastRowEmojis(context, 10);
        }

        public static java.util.List<String> getFastRowEmojis(@NonNull final Context context, final int maxCount) {
            synchronized (AdaptiveEmojiEngine.class) {
                final android.content.SharedPreferences prefs = helium314.keyboard.latin.utils.KtxKt.prefs(context);
                final long now = System.currentTimeMillis();
                java.util.List<String> ranked = readCachedFastRow(prefs);
                if (shouldRefreshFastRow(prefs, ranked, now)) {
                    ranked = getRankedEmojis(context);
                    final java.util.ArrayList<Object> cacheObjects = new java.util.ArrayList<>();
                    cacheObjects.addAll(ranked);
                    prefs.edit()
                            .putString(PREF_FAST_ROW_CACHE, helium314.keyboard.latin.utils.JsonUtils.listToJsonStr(cacheObjects))
                            .putLong(PREF_FAST_ROW_LAST_REFRESH, now)
                            .apply();
                }
                final int resultCount = Math.max(0, Math.min(maxCount, ranked.size()));
                return new java.util.ArrayList<>(ranked.subList(0, resultCount));
            }
        }

        private static boolean shouldRefreshFastRow(final android.content.SharedPreferences prefs,
                final java.util.List<String> cached, final long now) {
            final long lastRefresh = prefs.getLong(PREF_FAST_ROW_LAST_REFRESH, 0L);
            return cached.isEmpty()
                    || lastRefresh <= 0L
                    || now < lastRefresh
                    || now - lastRefresh >= FAST_ROW_REFRESH_INTERVAL_MILLIS;
        }

        private static java.util.List<String> readCachedFastRow(final android.content.SharedPreferences prefs) {
            final String jsonStr = prefs.getString(PREF_FAST_ROW_CACHE, "");
            final java.util.List<Object> cachedObjects = helium314.keyboard.latin.utils.JsonUtils.jsonStrToList(jsonStr);
            final java.util.ArrayList<String> cached = new java.util.ArrayList<>();
            for (final Object o : cachedObjects) {
                if (o instanceof String) {
                    cached.add((String) o);
                }
            }
            return cached;
        }

        private static java.util.List<String> getRankedEmojis(@NonNull final Context context) {
            final android.content.SharedPreferences prefs = helium314.keyboard.latin.utils.KtxKt.prefs(context);
            final String jsonStr = prefs.getString(PREF_ADAPTIVE_METADATA, "{}");
            org.json.JSONObject root;
            try {
                root = new org.json.JSONObject(jsonStr);
            } catch (Exception e) {
                root = new org.json.JSONObject();
            }

            final long now = System.currentTimeMillis();
            final java.util.ArrayList<EmojiScore> list = new java.util.ArrayList<>();
            final java.util.Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                final String emoji = keys.next();
                final org.json.JSONObject record = root.optJSONObject(emoji);
                if (record != null) {
                    final long lastUsed = record.optLong("lastUsed", now);
                    final int freq = record.optInt("freq", 1);
                    final int burst = record.optInt("burst", 1);

                    // 1. Frequency grows sublinearly so one emoji cannot dominate forever.
                    double freqScore = Math.log1p(freq) * 30.0;

                    // 2. Exponential Recency decay factor. The fast row should feel stable across days.
                    long deltaSec = (now - lastUsed) / 1000L;
                    if (deltaSec < 0) deltaSec = 0;
                    double decay = Math.exp(-deltaSec / RECENCY_DECAY_SECONDS);
                    double recencyScore = decay * 100.0;

                    // 3. Burst factor (active 15-minute window for burst boosts)
                    double burstBoost = (now - lastUsed < 900_000L) ? Math.min(burst, 5) * 20.0 : 0.0;

                    // Total Score
                    double score = recencyScore + burstBoost + freqScore;
                    list.add(new EmojiScore(emoji, score));
                }
            }

            // Fallback / Baseline integration
            final String strRecent = prefs.getString(Settings.PREF_EMOJI_RECENT_KEYS, helium314.keyboard.latin.settings.Defaults.PREF_EMOJI_RECENT_KEYS);
            final java.util.List<Object> recentKeys = helium314.keyboard.latin.utils.JsonUtils.jsonStrToList(strRecent);
            for (final Object o : recentKeys) {
                String emoji = null;
                if (o instanceof Integer) {
                    emoji = new String(Character.toChars((Integer) o));
                } else if (o instanceof String) {
                    emoji = (String) o;
                }
                if (emoji != null) {
                    boolean found = false;
                    for (final EmojiScore es : list) {
                        if (es.emoji.equals(emoji)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        list.add(new EmojiScore(emoji, 10.0)); // baseline score for existing recents
                    }
                }
            }

            final String[] defaultEmojis = { "😂", "🙏", "😍", "👍", "😭", "🥺", "🤣", "❤️", "✨", "🔥" };
            for (final String defEmoji : defaultEmojis) {
                boolean found = false;
                for (final EmojiScore es : list) {
                    if (es.emoji.equals(defEmoji)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    list.add(new EmojiScore(defEmoji, 5.0));
                }
            }

            java.util.Collections.sort(list, (a, b) -> Double.compare(b.score, a.score));

            final java.util.ArrayList<String> result = new java.util.ArrayList<>();
            for (final EmojiScore es : list) {
                result.add(es.emoji);
                if (result.size() >= MAX_FAST_ROW_EMOJI_COUNT) {
                    break;
                }
            }
            return result;
        }

        private static class EmojiScore {
            final String emoji;
            final double score;
            EmojiScore(String emoji, double score) {
                this.emoji = emoji;
                this.score = score;
            }
        }
    }

    private void setupBottomRowKeyboard(final EditorInfo editorInfo, final KeyboardActionListener keyboardActionListener) {
        MainKeyboardView keyboardView = findViewById(R.id.bottom_row_keyboard);
        keyboardView.setKeyboardActionListener(keyboardActionListener);
        PointerTracker.switchTo(keyboardView);
        final KeyboardLayoutSet kls = KeyboardLayoutSet.Builder.buildEmojiClipBottomRow(getContext(), editorInfo);
        final Keyboard keyboard = kls.getKeyboard(KeyboardId.ELEMENT_EMOJI_BOTTOM_ROW);
        keyboardView.setKeyboard(keyboard);
    }

    private void setupSidePadding() {
        final SettingsValues sv = Settings.getValues();
        final int keyboardWidth = ResourceUtils.getKeyboardWidth(getContext(), sv);
        final TypedArray keyboardAttr = getContext().obtainStyledAttributes(
                null, R.styleable.Keyboard, R.attr.keyboardStyle, R.style.Keyboard);
        final float leftPadding = keyboardAttr.getFraction(R.styleable.Keyboard_keyboardLeftPadding,
                keyboardWidth, keyboardWidth, 0f) * sv.mSidePaddingScale;
        final float rightPadding =  keyboardAttr.getFraction(R.styleable.Keyboard_keyboardRightPadding,
                keyboardWidth, keyboardWidth, 0f) * sv.mSidePaddingScale;
        keyboardAttr.recycle();
        if (mCategoryPagerAdapter != null)
            mCategoryPagerAdapter.setHorizontalPadding((int) Math.max(leftPadding, rightPadding));
        mEmojiCategoryPageIndicatorView.setPadding(
                (int) leftPadding,
                mEmojiCategoryPageIndicatorView.getPaddingTop(),
                (int) rightPadding,
                mEmojiCategoryPageIndicatorView.getPaddingBottom()
        );
        // setting width does not do anything, so we have some workaround in EmojiCategoryPageIndicatorView
    }

    public void stopEmojiPalettes() {
        if (!initialized) return;

        if (mSearchMode) {
            exitSearchMode(false, false);
        }
        getRecentsKeyboard().flushPendingRecentKeys();
        // Never retain even a reviewed trigger or selection across editors.
        mHasContextSmartPicks = false;
        mContextSmartEditor = null;
        if (mContextSmartRow != null) mContextSmartRow.removeAllViews();
        updateContextSmartVisibility();
    }

    private DynamicGridKeyboard getRecentsKeyboard() {
        return mEmojiCategory.getKeyboard(EmojiCategory.ID_RECENTS, 0);
    }

    /** Ephemeral, reviewed Smart picks. Actual saved Recents remain in their grid. */
    private void updateContextSmartPicks(final EditorInfo editorInfo) {
        mHasContextSmartPicks = false;
        mContextSmartEditor = null;
        if (mContextSmartRow == null) return;
        mContextSmartRow.removeAllViews();
        if (!EmojiContextEligibility.isEligible(editorInfo,
                Settings.getValues().mIncognitoModeEnabled)) {
            updateContextSmartVisibility();
            return;
        }
        final LatinIME ime = findHostingIme(getContext());
        if (ime == null || ime.getCurrentInputEditorInfo() != editorInfo) {
            updateContextSmartVisibility();
            return;
        }
        final InputConnection connection = ime.getCurrentInputConnection();
        if (connection == null) {
            updateContextSmartVisibility();
            return;
        }
        final CharSequence beforeCursor;
        try {
            beforeCursor = connection.getTextBeforeCursor(160, 0);
        } catch (RuntimeException unavailableEditor) {
            updateContextSmartVisibility();
            return;
        }
        if (beforeCursor == null || beforeCursor.length() == 0) {
            updateContextSmartVisibility();
            return;
        }
        // User text exists only within this stack frame, never in preferences.
        final List<EmojiContextScorer.Candidate> candidates =
                EmojiContextScorer.rank(beforeCursor.toString(), false);
        final float density = getResources().getDisplayMetrics().density;
        for (final EmojiContextScorer.Candidate candidate : candidates) {
            final Key key = mEmojiCategory.findEmojiKey(candidate.emoji);
            if (key == null) continue;
            final TextView cell = new TextView(getContext());
            cell.setText(candidate.emoji);
            cell.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 28);
            cell.setGravity(android.view.Gravity.CENTER);
            cell.setContentDescription(getResources().getString(
                    R.string.emoji_context_accessibility, candidate.matchedTrigger, candidate.emoji));
            cell.setFocusable(true);
            cell.setClickable(true);
            final int margin = Math.round(4 * density);
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, Math.round(48 * density), 1f);
            params.setMargins(margin, 0, margin, 0);
            final GradientDrawable glass = new GradientDrawable();
            glass.setColor(ColorUtils.setAlphaComponent(
                    mColors.get(ColorType.SPECIAL_KEY_BACKGROUND), 0x70));
            glass.setCornerRadius(14 * density);
            final ColorStateList press = ColorStateList.valueOf(ColorUtils.setAlphaComponent(
                    mColors.get(ColorType.TOOL_BAR_KEY), 0x2A));
            cell.setBackground(new RippleDrawable(press, glass, null));
            cell.setOnClickListener(view -> {
                final LatinIME activeIme = findHostingIme(getContext());
                if (activeIme == null || activeIme.getCurrentInputEditorInfo() != mContextSmartEditor
                        || !EmojiContextEligibility.isEligible(mContextSmartEditor,
                                Settings.getValues().mIncognitoModeEnabled)) return;
                // Use the normal Emoji key path for insertion and user Recents.
                onPressKey(key);
                onReleaseKey(key);
            });
            mContextSmartRow.addView(cell, params);
            mHasContextSmartPicks = true;
        }
        if (mHasContextSmartPicks) mContextSmartEditor = editorInfo;
        updateContextSmartVisibility();
    }

    private void updateContextSmartVisibility() {
        if (mContextSmartSection == null) return;
        // Never resize the scrolling viewport underneath an active finger.
        if (mEmojiGridScrollActive && mHasContextSmartPicks && !mSearchMode) return;
        final boolean visible = mHasContextSmartPicks && !mSearchMode
                && !mCompactContextPanel
                && mEmojiCategory.getCurrentCategoryId() == EmojiCategory.ID_RECENTS;
        mContextSmartSection.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private static LatinIME findHostingIme(final Context context) {
        // The palette can be inflated with an application/theme context rather
        // than the service itself; LatinIME clears this reference on destroy.
        final LatinIME active = LatinIME.getInstance();
        if (active != null) return active;
        Context current = context;
        for (int depth = 0; current != null && depth < 12; depth++) {
            if (current instanceof LatinIME) return (LatinIME) current;
            if (!(current instanceof ContextWrapper)) return null;
            final Context next = ((ContextWrapper) current).getBaseContext();
            if (next == current) return null;
            current = next;
        }
        return null;
    }

    private void updateAdaptiveEmojiOrder() {
        if (Settings.getValues().mIncognitoModeEnabled) {
            return;
        }
        final DynamicGridKeyboard recentsKeyboard = getRecentsKeyboard();
        final int maxRankedCount = Math.min(recentsKeyboard.getMaxKeyCount(),
                Math.max(1, recentsKeyboard.getOccupiedColumnCount() * 3));
        recentsKeyboard.prioritizeKeys(
                AdaptiveEmojiEngine.getFastRowEmojis(getContext(), maxRankedCount),
                maxRankedCount);
        if (initialized && mCategoryPagerAdapter != null) mCategoryPagerAdapter.refresh();
    }

    public void setKeyboardActionListener(final KeyboardActionListener listener) {
        mKeyboardActionListener = listener;
    }

    private void updateEmojiCategoryPageIdView() {
    }

    private void setCurrentCategoryId(final int categoryId, final boolean fromScroll) {
        if (!fromScroll && mPager != null && mCategoryPagerAdapter != null) {
            final int index = mCategoryPagerAdapter.indexOf(categoryId);
            if (index >= 0 && index != mPager.getCurrentItem()) {
                // ViewPager2 moves both complete category pages during drag;
                // it handles incomplete gestures and always snaps at rest.
                mPager.setCurrentItem(index, !isAnimationsDisabled());
                return;
            }
        }
        final int oldCategoryId = mEmojiCategory.getCurrentCategoryId();
        if (fromScroll || oldCategoryId != categoryId) {
            if (oldCategoryId == EmojiCategory.ID_RECENTS && !fromScroll) {
                getRecentsKeyboard().flushPendingRecentKeys();
                if (mCategoryPagerAdapter != null) mCategoryPagerAdapter.refresh();
            }

            mEmojiCategory.setCurrentCategoryId(categoryId);
            final int targetRow = mCategoryScrollRows.get(categoryId, 0);
            mEmojiCategory.setCurrentCategoryPageId(targetRow / Math.max(1, mEmojiCategory.getRowsPerPage()));

            final View old = mTabStrip.findViewWithTag((long) oldCategoryId);
            final View current = mTabStrip.findViewWithTag((long) categoryId);

            if (old instanceof TabImageView) {
                ((TabImageView) old).setSelectedCategory(false, 0);
                Settings.getValues().mColors.setColor((ImageView) old, ColorType.EMOJI_CATEGORY);
            }
            if (current instanceof TabImageView) {
                android.util.TypedValue typedValue = new android.util.TypedValue();
                getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
                int accentColor = KeyboardTheme.nearestAppleAccent(typedValue.data);
                ((TabImageView) current).setSelectedCategory(true, accentColor);

                // Set contrasting color for the icon so it's visible on the accent circle
                double y = (299 * android.graphics.Color.red(accentColor) + 587 * android.graphics.Color.green(accentColor) + 114 * android.graphics.Color.blue(accentColor)) / 1000.0;
                int contrastColor = y >= 128 ? android.graphics.Color.BLACK : android.graphics.Color.WHITE;
                ((TabImageView) current).setColorFilter(contrastColor, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
        updateContextSmartVisibility();
    }

    private boolean isAnimationsDisabled() {
        return android.provider.Settings.Global.getFloat(getContext().getContentResolver(),
                                                         android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0.0f;
    }

    public void clearKeyboardCache() {
        if (!initialized) {
            return;
        }

        mEmojiCategory.clearKeyboardCache();
        setupSearchResultKeyboard();
        searchEmoji(mSearchQuery);
        if (mCategoryPagerAdapter != null) mCategoryPagerAdapter.refresh();
        closeDictionaryFacilitator();
    }

    private void initDictionaryFacilitator() {
        if (Settings.getValues().mShowEmojiDescriptions) {
            final var locale = RichInputMethodManager.getInstance().getCurrentSubtype().getLocale();
            final SingleDictionaryFacilitator facilitator = sDictionaryFacilitator;
            if (facilitator == null || ! facilitator.isForLocale(locale)) {
                closeDictionaryFacilitator();
                // Load asynchronously in a background thread to prevent blocking the UI thread on emoji keyboard start
                new Thread(() -> {
                    try {
                        var dictFile = DictionaryInfoUtils.getCachedDictForLocaleAndType(locale, Dictionary.TYPE_EMOJI, getContext());
                        var dictionary = dictFile != null ? DictionaryFactory.getDictionary(dictFile, locale) : null;
                        if (dictionary != null) {
                            synchronized (EmojiPalettesView.class) {
                                var currentLocale = RichInputMethodManager.getInstance().getCurrentSubtype().getLocale();
                                if (locale.equals(currentLocale)) {
                                    if (sDictionaryFacilitator != null) {
                                        sDictionaryFacilitator.closeDictionaries();
                                    }
                                    sDictionaryFacilitator = new SingleDictionaryFacilitator(dictionary);
                                } else {
                                    dictionary.close();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("EmojiPalettesView", "Failed to load emoji dictionary asynchronously", e);
                    }
                }).start();
            }
        } else {
            closeDictionaryFacilitator();
        }
    }

    public static synchronized void closeDictionaryFacilitator() {
        if (sDictionaryFacilitator != null) {
            sDictionaryFacilitator.closeDictionaries();
            sDictionaryFacilitator = null;
        }
        if (sSearchDictionaryFacilitator != null) {
            sSearchDictionaryFacilitator.closeDictionaries();
            sSearchDictionaryFacilitator = null;
        }
    }

    private RippleDrawable createBackButtonBackground(final Colors colors) {
        final GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(android.graphics.Color.WHITE);
        colors.setColor(circle, ColorType.SPECIAL_KEY_BACKGROUND);

        final ColorStateList rippleColor = ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(colors.get(ColorType.FUNCTIONAL_KEY_TEXT), 0x33)
        );

        final float density = getContext().getResources().getDisplayMetrics().density;
        final int horizontalInset = (int) (3 * density);
        final int verticalInset = (int) (3 * density);

        final InsetDrawable content = new InsetDrawable(
            circle, horizontalInset, verticalInset, horizontalInset, verticalInset
        );

        return new RippleDrawable(
            rippleColor, content, content.getConstantState() != null ? content.getConstantState().newDrawable().mutate() : null
        );
    }

    private static class TabImageView extends ImageView {
        private final Paint mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean mIsSelectedCategory = false;

        public TabImageView(Context context) {
            super(context);
            // Start faded by default
            setAlpha(0.45f);
        }

        public void setSelectedCategory(boolean selected, int accentColor) {
            mIsSelectedCategory = selected;
            mCirclePaint.setColor(accentColor);
            if (selected) {
                setAlpha(1.0f);
            } else {
                setAlpha(0.45f);
                // restore normal unselected tint via clearColorFilter
                clearColorFilter();
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (mIsSelectedCategory) {
                float radius = Math.min(getWidth(), getHeight()) * 0.48f;
                canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, mCirclePaint);
            }
            super.onDraw(canvas);
        }
    }
}
