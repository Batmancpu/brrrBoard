// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.clipboard

import android.annotation.SuppressLint
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.EditText
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.KeyboardMotionSpec
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Outline
import android.os.Build
import android.os.Parcelable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.KeyboardActionListener
import helium314.keyboard.keyboard.KeyboardId
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.keyboard.MainKeyboardView
import helium314.keyboard.keyboard.emoji.EmojiPanelHeightMotion
import helium314.keyboard.keyboard.PanelMaterial
import helium314.keyboard.keyboard.PointerTracker
import helium314.keyboard.keyboard.internal.KeyDrawParams
import helium314.keyboard.keyboard.internal.KeyVisualAttributes
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.ClipboardHistoryManager
import helium314.keyboard.latin.BoardFooterView
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.Suggest
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.KeyboardWrapperView
import helium314.keyboard.latin.FrostedGlassHelper
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ResourceUtils
import helium314.keyboard.latin.utils.ToolbarMode
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.dpToPx
import helium314.keyboard.latin.utils.isDarkColor
import helium314.keyboard.latin.utils.prefs

@SuppressLint("CustomViewStyleable")
class ClipboardHistoryView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int = R.attr.clipboardHistoryViewStyle
) : LinearLayout(context, attrs, defStyle), View.OnClickListener,
    ClipboardDao.Listener, OnKeyEventListener,
    ClipboardHistoryRecyclerView.OnClipDismissListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val clipboardLayoutParams = ClipboardLayoutParams(context)
    private val pinIconId: Int
    private val keyBackgroundId: Int

    private lateinit var clipboardRecyclerView: ClipboardHistoryRecyclerView
    private lateinit var headerScrim: View
    private lateinit var placeholderView: TextView
    private lateinit var clearButton: ImageButton
    private lateinit var titleView: TextView
    private lateinit var clipboardAdapter: ClipboardAdapter
    private var undoBar: View? = null
    private var undoText: TextView? = null
    private var undoButton: TextView? = null
    private var lastDismissedEntry: ClipboardHistoryEntry? = null
    private var lastDismissedPosition: Int = -1
    private val footerUndoOwner = Any()
    private val hideUndoBarRunnable = Runnable { hideUndoBar() }
    private var searchMode = false
    private lateinit var searchHeader: View
    private lateinit var searchInput: EditText
    private lateinit var searchKeyboardHost: FrameLayout
    private lateinit var searchClearButton: ImageButton
    private lateinit var searchChromaticInput: ChromaticSearchEditText
    private var searchClearRevealed = false
    private var searchGestureGeneration = 0L
    private var clipboardRowKeyboard: MainKeyboardView? = null
    private var searchShifted = false
    private var normalListState: Parcelable? = null
    private var searchKeyboardClearance = 0
    private var heightMotion: ValueAnimator? = null
    private var animatedBodyHeight = -1
    private var pendingFromHeight = -1
    private var heightChangePending = false
    private var heightGeneration = 0L
    fun isSearchMode(): Boolean = searchMode


    lateinit var keyboardActionListener: KeyboardActionListener
    private lateinit var clipboardHistoryManager: ClipboardHistoryManager

    init {
        val clipboardViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.ClipboardHistoryView, defStyle, R.style.ClipboardHistoryView)
        pinIconId = clipboardViewAttr.getResourceId(R.styleable.ClipboardHistoryView_iconPinnedClip, 0)
        clipboardViewAttr.recycle()
        @SuppressLint("UseKtx") // suggestion does not work
        val keyboardViewAttr = context.obtainStyledAttributes(attrs, R.styleable.KeyboardView, defStyle, R.style.KeyboardView)
        keyBackgroundId = keyboardViewAttr.getResourceId(R.styleable.KeyboardView_keyBackground, 0)
        keyboardViewAttr.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val settings = Settings.getValues()
        val abcHeight = ResourceUtils.getKeyboardHeight(resources, settings)
        val persistentEmojiEnabled = context.prefs().getBoolean(Settings.PREF_PERSISTENT_EMOJI_ROW, helium314.keyboard.latin.settings.Defaults.PREF_PERSISTENT_EMOJI_ROW)
        val emojiRowHeight = if (persistentEmojiEnabled) (41 * resources.displayMetrics.density).toInt() else 0
        // This panel replaces the suggestion strip, while the shared footer now
        // stays visible. Count only the strip that the normal layout would have.
        val toolbarHeight = if (settings.mToolbarMode == ToolbarMode.HIDDEN) 0
            else resources.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
        val strip = rootView.findViewById<View>(R.id.strip_container)
        // Clipboard owns its fixed title, so no second alphabet strip belongs
        // above it (the same contract used by Emoji Browse).
        if (strip?.visibility == View.VISIBLE) strip.visibility = View.GONE
        val availableHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> MeasureSpec.getSize(heightMeasureSpec)
            else -> Int.MAX_VALUE
        }
        val normalHeight = helium314.keyboard.latin.SharedPanelGeometry.panelBodyHeight(
            abcHeight, emojiRowHeight, toolbarHeight,
            strip != null && strip.visibility == View.VISIBLE, availableHeight,
        )
        // Search should occupy the same native typing viewport + 160dp as
        // Expanded Emoji, even if the real alphabet is a few pixels taller
        // than ResourceUtils' nominal estimate on this device.
        val matchedEmojiHeight = helium314.keyboard.latin.SharedPanelGeometry.retainExistingPanelViewport(
            normalHeight, (parent as? KeyboardWrapperView)?.getLastTypingPanelViewportHeight() ?: 0,
            availableHeight, resources.getDimensionPixelSize(R.dimen.config_suggestions_strip_height))
        val targetHeight = if (searchMode)
            helium314.keyboard.latin.SharedPanelGeometry.emojiExpandedBodyHeight(
                matchedEmojiHeight, 160.dpToPx(resources), availableHeight)
            else normalHeight
        if (heightChangePending) {
            heightChangePending = false
            val from = pendingFromHeight
            pendingFromHeight = -1
            if (EmojiPanelHeightMotion.canAnimate(from, targetHeight, availableHeight,
                    isShown, heightMotionEnabled())) {
                // The old ValueAnimator remeasured the alphabet, RecyclerView,
                // glass RenderNode and entire IME tree 16 times per transition.
                // Keep one measured viewport for the duration and animate only
                // the visible clip/translation on the compositor path.
                animatedBodyHeight = if (from > targetHeight) from else -1
                val generation = heightGeneration
                post { launchHeightMotion(from, targetHeight, generation) }
            } else {
                animatedBodyHeight = -1
                clipBounds = null
            }
        }
        val finalHeight = if (animatedBodyHeight > 0)
            EmojiPanelHeightMotion.bound(animatedBodyHeight, availableHeight) else targetHeight
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY))
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), finalHeight)
        if (heightMotion != null && clipBounds != null) {
            clipBounds = Rect(0, clipBounds!!.top, measuredWidth, finalHeight)
        }
    }

    // Never animate an IME-internal layout height against ColorOS window/insets.
    // Endpoint geometry and the Android system keyboard show/hide remain intact.
    private fun heightMotionEnabled(): Boolean = false

    private fun prepareHeightMotion() {
        cancelHeightMotion()
    }

    private fun launchHeightMotion(from: Int, target: Int, generation: Long) {
        if (generation != heightGeneration || !isShown || !heightMotionEnabled()) {
            if (generation == heightGeneration) cancelHeightMotion()
            return
        }
        val distance = kotlin.math.abs(target - from)
        val growing = target > from
        val renderHeight = maxOf(from, target)
        val startClip = if (growing) distance else 0
        clipBounds = Rect(0, startClip, width, renderHeight)
        if (growing) {
            searchHeader.translationY = distance.toFloat()
            clipboardRecyclerView.translationY = distance.toFloat()
        }
        heightMotion = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = EmojiPanelHeightMotion.DURATION_MS
            interpolator = android.animation.TimeInterpolator(EmojiPanelHeightMotion::springProgress)
            addUpdateListener { motion ->
                if (generation == heightGeneration) {
                    val fraction = motion.animatedValue as Float
                    val top = if (growing) (distance * (1f - fraction)).toInt()
                        else (distance * fraction).toInt()
                    clipBounds = Rect(0, top.coerceIn(0, distance), width, renderHeight)
                    if (growing) {
                        val shift = distance * (1f - fraction)
                        searchHeader.translationY = shift
                        clipboardRecyclerView.translationY = shift
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (generation != heightGeneration) return
                    heightMotion = null
                    clipBounds = null
                    searchHeader.translationY = 0f
                    clipboardRecyclerView.translationY = 0f
                    animatedBodyHeight = -1
                    // Only collapsing needs a single final native viewport
                    // measurement. Growing measured the target immediately.
                    if (!growing) requestLayout()
                }
            })
            start()
        }
    }

    private fun cancelHeightMotion() {
        heightGeneration++
        pendingFromHeight = -1
        heightChangePending = false
        animatedBodyHeight = -1
        clipBounds = null
        if (this::searchHeader.isInitialized) searchHeader.translationY = 0f
        if (this::clipboardRecyclerView.isInitialized) clipboardRecyclerView.translationY = 0f
        heightMotion?.cancel()
        heightMotion = null
    }

    override fun onDetachedFromWindow() {
        cancelHeightMotion()
        super.onDetachedFromWindow()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initialize() { // needs to be delayed for access to ClipboardStrip, which is not a child of this view
        if (this::clipboardAdapter.isInitialized) return
        clipboardAdapter = ClipboardAdapter(clipboardLayoutParams, this).apply {
            itemBackgroundId = keyBackgroundId
            pinnedIconResId = pinIconId
        }
        // The header is a sibling of the RecyclerView. Never resolve it through
        // the RecyclerView's implicit `apply` receiver: that returns null and
        // crashes the entire IME when setAdapter() reports the empty state.
        headerScrim = findViewById(R.id.clipboard_header_scrim)
        placeholderView = findViewById(R.id.clipboard_empty_view)
        clipboardRecyclerView = findViewById<ClipboardHistoryRecyclerView>(R.id.clipboard_list).apply {
            val colCount = resources.getInteger(R.integer.config_clipboard_keyboard_col_count)
            layoutManager = StaggeredGridLayoutManager(colCount, StaggeredGridLayoutManager.VERTICAL)
            @Suppress("deprecation") // "no cache" should be fine according to warning in https://developer.android.com/reference/android/view/ViewGroup#setPersistentDrawingCache(int)
            persistentDrawingCache = PERSISTENT_NO_CACHE
            clipboardLayoutParams.setListProperties(this)
            placeholderView = this@ClipboardHistoryView.placeholderView
            clipDismissListener = this@ClipboardHistoryView
            onEmptyStateChanged = { isEmpty ->
                headerScrim.visibility =
                    if (isEmpty) View.INVISIBLE else View.VISIBLE
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    invalidatePanelGlass()
                }
            })
        }
        PanelMaterial.extendIntoFooterUnless(clipboardRecyclerView.parent as View,
            clipboardRecyclerView) { searchMode }
        clearButton = findViewById(R.id.clipboard_clear_button)
        titleView = findViewById(R.id.clipboard_title)
        undoBar = findViewById(R.id.clipboard_undo_bar)
        undoText = findViewById(R.id.clipboard_undo_text)
        undoButton = findViewById(R.id.clipboard_undo_button)
        setupSearchControls()

        findViewById<MainKeyboardView>(R.id.bottom_row_keyboard).addOnLayoutChangeListener {
                _, left, top, right, bottom, _, _, _, _ ->
            updateBottomLayerGeometry(bottom - top)
        }
    }

    private fun invalidatePanelGlass() {
        headerScrim.postInvalidateOnAnimation()
        findViewById<View>(R.id.clipboard_bottom_glass)?.postInvalidateOnAnimation()
        rootView.findViewById<BoardFooterView>(R.id.board_footer)?.invalidatePanelGlass()
    }

    private fun updateBottomLayerGeometry(rowHeight: Int) {
        if (rowHeight <= 0) return
        val extra = resources.getDimensionPixelSize(R.dimen.panel_content_bottom_extra)
        val reserved = rowHeight + extra +
            resources.getDimensionPixelSize(R.dimen.config_board_footer_height)
        val topInset = resources.getDimensionPixelSize(R.dimen.panel_content_top_inset)
        if (clipboardRecyclerView.paddingTop != topInset || clipboardRecyclerView.paddingBottom != reserved) {
            clipboardRecyclerView.setPadding(
                clipboardRecyclerView.paddingLeft, topInset,
                clipboardRecyclerView.paddingRight, reserved,
            )
        }
        val glass = findViewById<View>(R.id.clipboard_bottom_glass)
        (glass.layoutParams as FrameLayout.LayoutParams).let { params ->
            val desiredHeight = rowHeight + extra
            if (params.height != desiredHeight) {
                params.height = desiredHeight
                glass.layoutParams = params
            }
        }
        undoBar?.let { bar ->
            (bar.layoutParams as FrameLayout.LayoutParams).let { params ->
                val margin = rowHeight + 8.dpToPx(resources)
                if (params.bottomMargin != margin) {
                    params.bottomMargin = margin
                    bar.layoutParams = params
                }
            }
        }
    }

    private fun setupGlassLayers(colors: Colors) {
        PanelMaterial.apply(context, colors, headerScrim,
            findViewById(R.id.clipboard_bottom_glass), clipboardRecyclerView)
    }

    private fun setupUndoBar(colors: Colors) {
        val bar = undoBar ?: return
        val text = undoText
        val button = undoButton

        val isDark = KeyboardTheme.isDarkThemeActive(context)
        val pillRadius = 24.dpToPx(resources).toFloat()

        // Clip strictly to pill shape outline
        bar.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
            }
        }
        bar.clipToOutline = true

        // Consume touches inside the pill bounds so clicks never fall through to clipboard items beneath
        bar.setOnClickListener { /* Consume click */ }

        // Solid pill-shaped toast background without outline stroke
        val toastBgColor = if (isDark) {
            val funcBg = colors.get(ColorType.FUNCTIONAL_KEY_BACKGROUND)
            if (isDarkColor(funcBg)) ColorUtils.blendARGB(funcBg, Color.WHITE, 0.12f) else 0xFF2C2D30.toInt()
        } else {
            val funcBg = colors.get(ColorType.FUNCTIONAL_KEY_BACKGROUND)
            if (!isDarkColor(funcBg)) ColorUtils.blendARGB(funcBg, Color.BLACK, 0.08f) else 0xFFE9EAEC.toInt()
        }
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = pillRadius
            setColor(toastBgColor)
        }
        bar.background = bgDrawable

        // Text styling: pure white in dark mode, pure black in light mode
        text?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)

        // Solid accent pill button matching keyboard special keys
        val accentColor = colors.get(ColorType.SPECIAL_KEY_BACKGROUND)
        val buttonPill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 1000f
            setColor(accentColor)
        }
        val rippleColor = ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(if (isDark) Color.WHITE else Color.BLACK, 0x40)
        )
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 1000f
            setColor(Color.BLACK)
        }
        button?.background = RippleDrawable(rippleColor, buttonPill, mask)

        // Pure white text in dark mode, pure black in light mode
        button?.setTextColor(if (isDark) Color.WHITE else Color.BLACK)

        if (text != null) KeyboardTypeface.applyToTextView(text)
        if (button != null) KeyboardTypeface.applyToTextView(button)
    }

    private fun setupHeader(colors: Colors) {
        setupGlassLayers(colors)
        setupHeaderButton(clearButton, colors)
        
        val clearIcon = KeyboardIconsSet.instance.getNewDrawable(ToolbarKey.CLEAR_CLIPBOARD.name, context)
        clearButton.setImageDrawable(clearIcon)

        KeyboardTypeface.applyToTextView(
            titleView,
            titleView.text,
            android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD)
        )
        titleView.setTextColor(colors.get(ColorType.KEY_TEXT))
        titleView.setOnClickListener { /* Consume clicks */ }
        
        clearButton.setOnClickListener(this)
    }

    private fun setupHeaderButton(button: ImageButton, colors: Colors) {
        // Clipboard utilities use neutral modifier colors, never the action-key accent.
        button.imageTintList = ColorStateList.valueOf(colors.get(ColorType.FUNCTIONAL_KEY_TEXT))
        button.background = createHeaderButtonBackground(colors)
        button.scaleType = ImageView.ScaleType.CENTER
        button.setPadding(0, 0, 0, 0)
    }

    private fun createHeaderButtonBackground(colors: Colors): RippleDrawable {
        val buttonShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 13.dpToPx(resources).toFloat()
            colors.setColor(this, ColorType.FUNCTIONAL_KEY_BACKGROUND)
        }
        val rippleColor = ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(colors.get(ColorType.FUNCTIONAL_KEY_TEXT), 0x33)
        )
        val inset = 2.dpToPx(resources)
        val content = InsetDrawable(buttonShape, inset, inset, inset, inset)
        
        val maskShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 13.dpToPx(resources).toFloat()
            setColor(Color.BLACK)
        }
        val mask = InsetDrawable(maskShape, inset, inset, inset, inset)

        return RippleDrawable(
            rippleColor,
            content,
            mask
        )
    }

    /** Exactly the same semantic text/icon colors and round controls as Emoji Search. */
    private fun applySearchTheme(colors: Colors) {
        val textColor = colors.get(ColorType.TOOL_BAR_KEY)
        searchInput.setTextColor(textColor)
        searchChromaticInput.refreshInkTheme()
        searchInput.setHintTextColor(ColorUtils.setAlphaComponent(textColor, 0x80))
        val iconTint = ColorStateList.valueOf(colors.get(ColorType.FUNCTIONAL_KEY_TEXT))
        findViewById<ImageButton>(R.id.clipboard_search_back)?.apply {
            imageTintList = iconTint
            background = searchControlBackground(colors)
            scaleType = ImageView.ScaleType.CENTER
            setPadding(0, 0, 0, 0)
        }
        // A second opaque circle would look detached from the search capsule.
        // The clear glyph morphs out of the capsule itself as a light ripple.
        searchClearButton.imageTintList = iconTint
        searchClearButton.background = RippleDrawable(ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(colors.get(ColorType.FUNCTIONAL_KEY_TEXT), 0x35)),
            null, null)
        // Query matches color the actual glyphs, never a rectangle behind text.
    }

    private fun searchControlBackground(colors: Colors): RippleDrawable {
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors.setColor(this, ColorType.SPECIAL_KEY_BACKGROUND)
        }
        val inset = 3.dpToPx(resources)
        val content = InsetDrawable(circle, inset, inset, inset, inset)
        val mask = InsetDrawable(GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.BLACK)
        }, inset, inset, inset, inset)
        return RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(
            colors.get(ColorType.FUNCTIONAL_KEY_TEXT), 0x33)), content, mask)
    }

    private fun setupClipKey(params: KeyDrawParams) {
        clipboardAdapter.apply {
            itemBackgroundId = keyBackgroundId
            itemTypeFace = params.mTypeface
            itemTextColor = params.mTextColor
            itemTextSize = params.mLabelSize.toFloat()
        }
    }



    private fun setupBottomRowKeyboard(editorInfo: EditorInfo, listener: KeyboardActionListener) {
        val keyboardView = findViewById<MainKeyboardView>(R.id.bottom_row_keyboard)
        keyboardView.setKeyboardActionListener(object : KeyboardActionListener by listener {
            override fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {
                if (primaryCode == KeyCode.EMOJI_SEARCH) openSearch()
                else listener.onCodeInput(primaryCode, x, y, isKeyRepeat)
            }
        })
        clipboardRowKeyboard = keyboardView
        PointerTracker.switchTo(keyboardView)
        val kls = KeyboardLayoutSet.Builder.buildEmojiClipBottomRow(context, editorInfo)
        val keyboard = kls.getKeyboard(KeyboardId.ELEMENT_CLIPBOARD_BOTTOM_ROW)
        keyboardView.setKeyboard(keyboard)
    }

    fun setHardwareAcceleratedDrawingEnabled(enabled: Boolean) {
        if (!enabled) return
        // TODO: Should use LAYER_TYPE_SOFTWARE when hardware acceleration is off?
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun startClipboardHistory(
            historyManager: ClipboardHistoryManager,
            keyVisualAttr: KeyVisualAttributes?,
            editorInfo: EditorInfo,
            keyboardActionListener: KeyboardActionListener
    ) {
        cancelHeightMotion()
        this.keyboardActionListener = keyboardActionListener
        clipboardHistoryManager = historyManager
        initialize()
        val settings = Settings.getInstance()
        setupHeader(settings.current.mColors)
        setupUndoBar(settings.current.mColors)
        historyManager.prepareClipboardHistory()
        historyManager.setHistoryChangeListener(this)
        clipboardAdapter.clipboardHistoryManager = historyManager
        if (searchMode) closeSearch()
        clipboardAdapter.setSearchQuery("")

        val params = KeyDrawParams()
        params.updateParams(clipboardLayoutParams.bottomRowKeyboardHeight, keyVisualAttr)
        KeyboardTypeface.customTypeface()?.let { params.mTypeface = it }
        setupClipKey(params)
        setupBottomRowKeyboard(editorInfo, keyboardActionListener)

        placeholderView.apply {
            KeyboardTypeface.applyToTextView(this)
            setTextColor(settings.current.mColors.get(ColorType.KEY_TEXT))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        }
        clipboardRecyclerView.apply {
            adapter = clipboardAdapter
            val keyboardWidth = ResourceUtils.getKeyboardWidth(context, settings.current)
            val recyclerParams = layoutParams as FrameLayout.LayoutParams
            recyclerParams.width = keyboardWidth
            recyclerParams.gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = recyclerParams

            // set side padding
            val keyboardAttr = context.obtainStyledAttributes(
                null, R.styleable.Keyboard, R.attr.keyboardStyle, R.style.Keyboard)
            val leftPadding = (keyboardAttr.getFraction(R.styleable.Keyboard_keyboardLeftPadding,
                keyboardWidth, keyboardWidth, 0f)
                    * settings.current.mSidePaddingScale).toInt()
            val rightPadding =  (keyboardAttr.getFraction(R.styleable.Keyboard_keyboardRightPadding,
                keyboardWidth, keyboardWidth, 0f)
                    * settings.current.mSidePaddingScale).toInt()
            keyboardAttr.recycle()
            setPadding(leftPadding, paddingTop, rightPadding, paddingBottom)
        }
        updateBottomLayerGeometry(findViewById<View>(R.id.bottom_row_keyboard).height)


    }

    fun stopClipboardHistory() {
        cancelHeightMotion()
        if (searchMode) closeSearch()
        hideUndoBar()
        removeCallbacks(hideUndoBarRunnable)
        undoBar?.removeCallbacks(hideUndoBarRunnable)
        lastDismissedEntry = null
        lastDismissedPosition = -1
        if (!this::clipboardAdapter.isInitialized) return
        // Do not retain transient Clipboard pixels in the offscreen GPU list.
        PanelMaterial.apply(context, Settings.getValues().mColors, headerScrim,
            findViewById(R.id.clipboard_bottom_glass), null)
        clipboardRecyclerView.adapter = null
        clipboardHistoryManager.setHistoryChangeListener(null)
        clipboardAdapter.clipboardHistoryManager = null
    }

    override fun onClipDismissed(entry: ClipboardHistoryEntry, originalPosition: Int) {
        showUndoBar(entry, originalPosition)
    }

    private fun showUndoBar(entry: ClipboardHistoryEntry, position: Int) {
        lastDismissedEntry = entry
        lastDismissedPosition = position

        removeCallbacks(hideUndoBarRunnable)
        val footer = rootView.findViewById<BoardFooterView>(R.id.board_footer)
        if (footer != null && footer.isShown) {
            // The old panel overlay floated above the card grid and sometimes
            // occluded its live glass. The owner requested the temporary Undo
            // action in the vacant fixed footer, between ABC and mic.
            undoBar?.animate()?.cancel()
            undoBar?.visibility = View.GONE
            footer.showContextAction(footerUndoOwner,
                R.string.board_footer_action_removed, R.string.undo) {
                restoreLastDismissedEntry()
            }
            postDelayed(hideUndoBarRunnable, 6_000L)
            return
        }

        val bar = undoBar ?: return
        val text = undoText
        val button = undoButton

        removeCallbacks(hideUndoBarRunnable)
        bar.removeCallbacks(hideUndoBarRunnable)
        bar.animate().cancel()

        text?.text = context.getString(R.string.clipboard_clip_deleted)
        button?.setOnClickListener {
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, it, HapticEvent.KEY_PRESS)
            restoreLastDismissedEntry()
        }

        if (bar.visibility != View.VISIBLE) bar.alpha = 0f
        bar.visibility = View.VISIBLE
        bar.animate().alpha(1f).setDuration(180).start()
        bar.postDelayed(hideUndoBarRunnable, 10000)
    }

    private fun restoreLastDismissedEntry() {
        val clip = lastDismissedEntry
        if (clip != null) {
            val restoredPos = clipboardHistoryManager.restoreEntry(clip, lastDismissedPosition)
            if (restoredPos >= 0) {
                clipboardAdapter.notifyItemInserted(restoredPos)
                clipboardRecyclerView.smoothScrollToPosition(restoredPos)
            }
        }
        hideUndoBar()
    }

    private fun hideUndoBar() {
        removeCallbacks(hideUndoBarRunnable)
        rootView.findViewById<BoardFooterView>(R.id.board_footer)
            ?.clearContextAction(footerUndoOwner)
        // The modern footer path intentionally never inflates/displays the
        // legacy floating bar. Expiry must still discard the clipboard entry
        // reference even if that optional view was never created.
        lastDismissedEntry = null
        lastDismissedPosition = -1
        val bar = undoBar ?: return
        bar.removeCallbacks(hideUndoBarRunnable)
        bar.animate().cancel()
        if (bar.visibility == View.VISIBLE) {
            bar.animate().alpha(0f).setDuration(180).withEndAction {
                bar.visibility = View.GONE
            }.start()
        }
    }


    private fun setupSearchControls() {
        searchHeader = findViewById(R.id.clipboard_search_header)
        searchInput = findViewById(R.id.clipboard_search_text)
        searchKeyboardHost = findViewById(R.id.clipboard_search_keyboard_host)
        searchClearButton = findViewById(R.id.clipboard_search_clear)
        searchChromaticInput = searchInput as ChromaticSearchEditText
        searchKeyboardHost.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            if (searchMode) updateSearchKeyboardClearance(bottom - top)
        }
        searchInput.setShowSoftInputOnFocus(false)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                morphSearchClear(query.isNotEmpty())
                if (::clipboardAdapter.isInitialized && ::clipboardRecyclerView.isInitialized) {
                    clipboardAdapter.setSearchQuery(query)
                    clipboardRecyclerView.scrollToPosition(0)
                    showSearchMatch(query.isNotBlank())
                    if (query.isNotBlank() && clipboardAdapter.itemCount > 0)
                        KeyboardMotionSpec.revealSearchResults(clipboardRecyclerView)
                }
            }
        })
        findViewById<ImageButton>(R.id.clipboard_search_back).setOnClickListener { closeSearch() }
        searchClearButton.setOnClickListener {
            searchInput.setText("")
        }
    }

    /** The X comes from the same pill, with only compositor properties moving. */
    private fun morphSearchClear(show: Boolean) {
        if (show == searchClearRevealed) return
        searchClearRevealed = show
        searchClearButton.animate().cancel()
        searchClearButton.isEnabled = show
        if (show) {
            searchClearButton.visibility = View.VISIBLE
            if (!KeyboardMotionSpec.enabled()) {
                searchClearButton.alpha = 1f
                searchClearButton.scaleX = 1f
                searchClearButton.scaleY = 1f
            } else {
                searchClearButton.translationX = 8.dpToPx(resources).toFloat()
                searchClearButton.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .translationX(0f).setDuration(170).start()
            }
        } else if (!KeyboardMotionSpec.enabled()) {
            searchClearButton.visibility = View.INVISIBLE
            searchClearButton.alpha = 0f
            searchClearButton.scaleX = 0.55f
            searchClearButton.scaleY = 0.55f
        } else {
            searchClearButton.animate().alpha(0f).scaleX(0.55f).scaleY(0.55f)
                .translationX(8.dpToPx(resources).toFloat()).setDuration(140)
                .withEndAction {
                    if (!searchClearRevealed) searchClearButton.visibility = View.INVISIBLE
                }.start()
        }
    }

    /** Chromatic query glyphs, with normal selection and an uncolored capsule. */
    private fun showSearchMatch(hasQuery: Boolean) {
        searchChromaticInput.showMatchedInk(hasQuery)
    }

    private fun applySearchResultsLayout(search: Boolean) {
        val list = clipboardRecyclerView
        if (search) {
            normalListState = list.layoutManager?.onSaveInstanceState()
            clipboardAdapter.horizontalSearchRow = true
            list.horizontalSearchMode = true
            list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        } else {
            clipboardAdapter.horizontalSearchRow = false
            list.horizontalSearchMode = false
            val columns = resources.getInteger(R.integer.config_clipboard_keyboard_col_count)
            val manager = StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
            list.layoutManager = manager
            normalListState?.let { manager.onRestoreInstanceState(it) }
            normalListState = null
        }
        (list.layoutParams as FrameLayout.LayoutParams).let { params ->
            // Search owns the entire gap between its fixed 48dp field and the
            // REAL alphabet top. A fixed 96dp card lane left a dead band on
            // the Reno10 even though the outer Search body was expanded.
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            params.topMargin = if (search) 48.dpToPx(resources) else 0
            params.bottomMargin = if (search) searchKeyboardClearance else 0
            params.gravity = if (search) Gravity.TOP else Gravity.CENTER_HORIZONTAL
            list.layoutParams = params
        }
        val top = if (search) 0 else resources.getDimensionPixelSize(R.dimen.panel_content_top_inset)
        list.setPadding(list.paddingLeft, top, list.paddingRight,
            if (search) 0 else list.paddingBottom)
        (placeholderView.layoutParams as FrameLayout.LayoutParams).let { params ->
            params.gravity = if (search) Gravity.TOP else Gravity.CENTER
            params.topMargin = if (search) 48.dpToPx(resources) else 0
            params.bottomMargin = if (search) searchKeyboardClearance else 0
            params.height = if (search) ViewGroup.LayoutParams.MATCH_PARENT
                else ViewGroup.LayoutParams.WRAP_CONTENT
            placeholderView.layoutParams = params
        }
        clipboardAdapter.notifyDataSetChanged()
    }

    /** Dock the horizontal cards to the measured alphabet, including resize/rotation. */
    private fun updateSearchKeyboardClearance(measuredKeyboardHeight: Int) {
        if (!searchMode || measuredKeyboardHeight <= 0
                || searchKeyboardClearance == measuredKeyboardHeight) return
        searchKeyboardClearance = measuredKeyboardHeight
        for (view in arrayOf(clipboardRecyclerView, placeholderView)) {
            val params = view.layoutParams as FrameLayout.LayoutParams
            if (params.bottomMargin != measuredKeyboardHeight) {
                params.bottomMargin = measuredKeyboardHeight
                view.layoutParams = params
            }
        }
    }

    private fun openSearch() {
        if (searchMode || !::clipboardHistoryManager.isInitialized) return
        searchGestureGeneration++
        val switcher = KeyboardSwitcher.getInstance()
        val typing = switcher.mainKeyboardView ?: return
        prepareHeightMotion()
        searchMode = true
        searchShifted = false
        applySearchTheme(Settings.getValues().mColors)
        applySearchResultsLayout(true)
        searchHeader.visibility = View.VISIBLE
        titleView.visibility = View.INVISIBLE
        clipboardRowKeyboard?.visibility = View.GONE
        findViewById<View>(R.id.clipboard_bottom_glass).visibility = View.GONE
        searchKeyboardHost.visibility = View.VISIBLE
        placeholderView.setText(R.string.clipboard_search_no_matches)
        (typing.parent as? ViewGroup)?.removeView(typing)
        searchKeyboardHost.addView(typing, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        updateSearchKeyboardClearance(typing.height)
        typing.visibility = View.VISIBLE
        switcher.setAlphabetKeyboard()
        typing.setKeyboardActionListener(searchTypingListener())
        PointerTracker.switchTo(typing)
        searchInput.requestFocus()
        searchInput.setSelection(searchInput.length())
        searchKeyboardHost.requestLayout()
        requestLayout()
    }

    private fun closeSearch(animate: Boolean = true) {
        if (!searchMode) return
        KeyboardMotionSpec.reset(clipboardRecyclerView)
        searchGestureGeneration++
        searchChromaticInput.showMatchedInk(false)
        if (animate) prepareHeightMotion() else cancelHeightMotion()
        searchMode = false
        searchHeader.visibility = View.GONE
        titleView.visibility = View.VISIBLE
        searchInput.setText("")
        clipboardAdapter.setSearchQuery("")
        searchKeyboardClearance = 0
        applySearchResultsLayout(false)
        placeholderView.setText(R.string.clipboard_empty_state)
        val switcher = KeyboardSwitcher.getInstance()
        val typing = switcher.mainKeyboardView
        if (typing != null && typing.parent === searchKeyboardHost) {
            searchKeyboardHost.removeView(typing)
            (switcher.wrapperView as? ViewGroup)?.addView(typing, 0)
            typing.setKeyboardActionListener(keyboardActionListener)
            typing.visibility = View.GONE
        }
        searchKeyboardHost.visibility = View.GONE
        clipboardRowKeyboard?.let {
            it.visibility = View.VISIBLE
            PointerTracker.switchTo(it)
            updateBottomLayerGeometry(it.height)
        }
        findViewById<View>(R.id.clipboard_bottom_glass).visibility = View.VISIBLE
        searchInput.clearFocus()
        requestLayout()
    }

    /** Called only from the separate ABC footer, never by Search typing keys. */
    fun exitSearchForAlphabet() {
        if (searchMode) closeSearch(animate = false)
    }

    /** Shared footer ABC switches once, immediately, with no 260 ms hold. */
    fun collapseSearchForFooterAlphabet(switchToAlphabet: Runnable): Boolean {
        if (!searchMode) return false
        closeSearch(animate = false)
        if (!searchMode) switchToAlphabet.run()
        return true
    }

    private fun insertSearchText(value: String) {
        val edit = searchInput.text ?: return
        val start = searchInput.selectionStart.coerceAtLeast(0)
        val end = searchInput.selectionEnd.coerceAtLeast(start)
        edit.replace(start, end, value)
        searchInput.setSelection(start + value.length)
    }

    private fun deleteSearchCharacter() {
        val edit = searchInput.text ?: return
        val start = searchInput.selectionStart.coerceAtLeast(0)
        val end = searchInput.selectionEnd.coerceAtLeast(start)
        if (end > start) { edit.delete(start, end); return }
        if (start > 0) {
            val previous = Character.offsetByCodePoints(edit, start, -1)
            edit.delete(previous, start)
        }
    }

    private fun searchTypingListener(): KeyboardActionListener {
        val base = keyboardActionListener
        return object : KeyboardActionListener by base {
            override fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {
                if (!searchMode) return
                val switcher = KeyboardSwitcher.getInstance()
                when (primaryCode) {
                    KeyCode.DELETE -> deleteSearchCharacter()
                    KeyCode.SHIFT -> {
                        searchShifted = !searchShifted
                        if (searchShifted) switcher.setAlphabetManualShiftedKeyboard()
                        else switcher.setAlphabetKeyboard()
                        switcher.mainKeyboardView?.setKeyboardActionListener(this)
                    }
                    KeyCode.SYMBOL -> {
                        switcher.setSymbolsKeyboard()
                        switcher.mainKeyboardView?.setKeyboardActionListener(this)
                    }
                    KeyCode.SYMBOL_ALPHA, KeyCode.ALPHA -> {
                        searchShifted = false
                        switcher.setAlphabetKeyboard()
                        switcher.mainKeyboardView?.setKeyboardActionListener(this)
                    }
                    KeyCode.NUMPAD -> {
                        switcher.setNumpadKeyboard()
                        switcher.mainKeyboardView?.setKeyboardActionListener(this)
                    }
                    Constants.CODE_ENTER -> searchInput.requestFocus()
                    else -> if (primaryCode >= Constants.CODE_SPACE)
                        insertSearchText(String(Character.toChars(primaryCode)))
                }
            }
            override fun onTextInput(text: String) { if (searchMode) insertSearchText(text) }
            override fun onKeyDown(keyCode: Int, keyEvent: KeyEvent): Boolean =
                searchInput.dispatchKeyEvent(keyEvent)
            override fun onKeyUp(keyCode: Int, keyEvent: KeyEvent): Boolean =
                searchInput.dispatchKeyEvent(keyEvent)
            // Decode the gesture against the same native dictionary as Emoji/GIF
            // Search, but commit only into this private filter EditText. Never
            // forward a batch event to the host application's InputConnection.
            override fun onStartBatchInput() {
                if (searchMode) searchInput.isCursorVisible = true
            }
            override fun onUpdateBatchInput(batchPointers: helium314.keyboard.latin.common.InputPointers) = Unit
            override fun onEndBatchInput(batchPointers: helium314.keyboard.latin.common.InputPointers) {
                if (!searchMode) return
                val session = searchGestureGeneration
                val keyboard = KeyboardSwitcher.getInstance().mainKeyboardView?.keyboard ?: return
                getLatinIme()?.getKlipySearchGestureSuggestion(batchPointers, keyboard,
                    object : Suggest.OnGetSuggestedWordsCallback {
                        override fun onGetSuggestedWords(words: SuggestedWords?) {
                            val text = words?.takeUnless { it.isEmpty }?.getWord(0)
                                ?.takeIf { it.isNotBlank() } ?: return
                            post {
                                if (ClipboardSearchGestureGate.accepts(searchMode,
                                    session, searchGestureGeneration,
                                    isAttachedToWindow, searchInput.hasFocus())) {
                                    insertSearchText(text)
                                }
                            }
                        }
                    })
            }
            override fun onCancelBatchInput() { searchGestureGeneration++ }
        }
    }

    private fun getLatinIme(): LatinIME? {
        var current: Context = context
        while (true) {
            if (current is LatinIME) return current
            current = (current as? ContextWrapper)?.baseContext ?: return null
        }
    }

    override fun onClick(view: View) {
        if (view === clearButton) {
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(KeyCode.NOT_SPECIFIED, this, HapticEvent.KEY_PRESS)
            clipboardHistoryManager.clearHistory()
        }
    }

    override fun onKeyDown(clipId: Long) {
        keyboardActionListener.onPressKey(KeyCode.NOT_SPECIFIED, 0, true, HapticEvent.KEY_PRESS)
    }

    override fun onKeyUp(clipId: Long) {
        val clipContent = clipboardHistoryManager.getHistoryEntryContent(clipId)
        val outcome = clipContent?.let(clipboardHistoryManager::pasteHistoryEntry)
        if (outcome == helium314.keyboard.latin.ClipboardHistoryPasteOutcome.TEXT && clipContent != null) {
            keyboardActionListener.onTextInput(clipContent.text)
        }
        keyboardActionListener.onReleaseKey(KeyCode.NOT_SPECIFIED, false)
        if (outcome != null && outcome != helium314.keyboard.latin.ClipboardHistoryPasteOutcome.REJECTED &&
            Settings.getValues().mAlphaAfterClipHistoryEntry)
            keyboardActionListener.onCodeInput(KeyCode.ALPHA, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
    }

    override fun onClipInserted(position: Int) {
        if (clipboardAdapter.isFiltered) { clipboardAdapter.refreshSearchResults(); return }
        clipboardAdapter.notifyItemInserted(position)
        clipboardRecyclerView.smoothScrollToPosition(position)
    }

    override fun onClipsRemoved(position: Int, count: Int) {
        if (clipboardAdapter.isFiltered) { clipboardAdapter.refreshSearchResults(); return }
        clipboardAdapter.notifyItemRangeRemoved(position, count)
    }

    override fun onClipMoved(oldPosition: Int, newPosition: Int) {
        if (clipboardAdapter.isFiltered) { clipboardAdapter.refreshSearchResults(); return }
        clipboardAdapter.notifyItemMoved(oldPosition, newPosition)
        clipboardAdapter.notifyItemChanged(newPosition)
        if (newPosition < oldPosition) clipboardRecyclerView.smoothScrollToPosition(newPosition)
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        // The setting can only be changed from a settings screen, but adding it to this listener seems necessary: https://github.com/HeliBorg/HeliBoard/pull/1903#issuecomment-3478424606
        if (::clipboardHistoryManager.isInitialized && key == Settings.PREF_CLIPBOARD_HISTORY_PINNED_FIRST) {
            // Ensure settings are reloaded first
            Settings.getInstance().onSharedPreferenceChanged(prefs, key)
            clipboardHistoryManager.sortHistoryEntries()
            clipboardAdapter.notifyDataSetChanged()
        }
    }
}
