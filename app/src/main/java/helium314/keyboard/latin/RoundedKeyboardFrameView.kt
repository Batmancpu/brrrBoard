// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Insets
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.emoji.EmojiPalettesView
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs

class RoundedKeyboardFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val clipPath = Path()
    private val outlineClipPath = Path()
    private val topHighlightPath = Path()
    private val highlightShaders = android.util.SparseArray<LinearGradient>()
    private var highlightShaderWidth = -1
    private val exclusivePanelIds = intArrayOf(
        R.id.emoji_palettes_view, R.id.clipboard_history_view, R.id.klipy_palettes_view,
        R.id.ai_writing_tools_view, R.id.access_point_menu_view)
    private val outerHeaderIds = intArrayOf(
        R.id.persistent_emoji_row_scroll, R.id.persistent_emoji_row_divider, R.id.strip_container)
    private val topHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // Butt caps avoid bright end bulbs where the rim meets side highlights.
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.ROUND
        strokeWidth = resources.displayMetrics.density
    }
    private val clipRect = RectF()
    private var lastWidth = -1
    private var lastHeight = -1
    private var lastRadiusPx = -1f

    private var cachedRadiusPx = -1f
    // An IME window sized from a wrap-content Emoji child can shrink upward
    // after the alphabet disappears, leaving the *host app* visible as a band
    // above the category tabs. Retain the complete, actually measured typing
    // frame (including its rows/footer/inset), never a guessed dp offset.
    private val nativeTypingFrame = KeyboardViewportHeightMemory()
    private val staticDustOverlay = StaticSparkleDustOverlay()
    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Settings.PREF_KEYBOARD_CORNER_RADIUS) {
            cachedRadiusPx = -1f
            invalidateOutline()
            postInvalidateOnAnimation()
        } else if (key == Settings.PREF_KEYBOARD_TOP_HIGHLIGHT_STYLE) {
            // The actual IME stays open: re-render only its edge reflection.
            postInvalidateOnAnimation()
        }
    }

    init {
        // Keep the navigation bar inset after the footer instead of inserting it
        // between the last key row and the footer inside KeyboardWrapperView.
        fitsSystemWindows = true
        clipChildren = false
        clipToPadding = false
        // Also clip via HWUI so hardware-layered descendants respect the rounded keyboard shape.
        // Android 13+ supports path outlines, which lets us keep only the top corners rounded.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val radiusPx = if (cachedRadiusPx >= 0f) cachedRadiusPx else keyboardCornerRadiusPx()
                    if (view.width <= 0 || view.height <= 0 || radiusPx <= 0f) {
                        outline.setRect(0, 0, view.width, view.height)
                        return
                    }
                    outlineClipPath.reset()
                    outlineClipPath.addRoundRect(
                        0f,
                        0f,
                        view.width.toFloat(),
                        view.height.toFloat(),
                        floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx, 0f, 0f, 0f, 0f),
                        Path.Direction.CW
                    )
                    outline.setPath(outlineClipPath)
                }
            }
            clipToOutline = true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Exclusive panels own their own headers. Suppress all native-typing
        // header slots BEFORE LinearLayout measures its preceding children.
        // A child-level hide is too late to prevent a one-pass empty band,
        // especially when switching directly between two non-typing panels.
        // Ordinary alphabet/symbol layouts keep their existing header policy.
        val emoji = findViewById<View>(R.id.emoji_palettes_view)
        val exclusivePanelVisible = exclusivePanelIds.any { findViewById<View>(it)?.visibility == View.VISIBLE }
        if (exclusivePanelVisible) {
            for (id in outerHeaderIds) {
                findViewById<View>(id)?.let { header ->
                    if (header.visibility != View.GONE) header.visibility = View.GONE
                }
            }
        }
        // Preserve the measured key body; only the optional header shrinks.
        // Keep the strip attached so a late provider response can expand it.
        val stripContainer = findViewById<View>(R.id.strip_container) as? android.widget.FrameLayout
        val strip = stripContainer?.findViewById<View>(R.id.suggestion_strip_view) as? helium314.keyboard.latin.suggestions.SuggestionStripView
        if (stripContainer != null && strip != null) {
            val compact = !exclusivePanelVisible && stripContainer.visibility == View.VISIBLE &&
                strip.visibility == View.VISIBLE && strip.shouldCollapseUnusedStrip()
            val fullHeight = resources.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
            val density = resources.displayMetrics.density
            val compactHeight = AccessoryHeaderGeometry.topClearancePx(keyboardCornerRadiusPx(), density)
            val targetContainerHeight = if (compact) compactHeight else fullHeight
            val targetStripHeight = if (compact) 0 else LayoutParams.MATCH_PARENT
            if (stripContainer.layoutParams.height != targetContainerHeight)
                stripContainer.layoutParams.height = targetContainerHeight
            if (strip.layoutParams.height != targetStripHeight)
                strip.layoutParams.height = targetStripHeight
            if (stripContainer.clipChildren != compact) stripContainer.clipChildren = compact
            if (stripContainer.clipToPadding != compact) stripContainer.clipToPadding = compact
            val accessibility = if (compact) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            if (strip.importantForAccessibility != accessibility)
                strip.importantForAccessibility = accessibility
        }
        if (emoji?.visibility == View.VISIBLE) {
            val currentWidth = MeasureSpec.getSize(widthMeasureSpec)
            val footer = findViewById<View>(R.id.board_footer)
            val footerHeight = footer?.takeIf { it.visibility == View.VISIBLE }?.let { view ->
                view.layoutParams?.height?.takeIf { it > 0 } ?: view.measuredHeight
            } ?: 0
            val bodyFromNativeFrame = SharedPanelGeometry.bodyFromMeasuredTypingFrame(
                nativeTypingFrame.heightForWidth(currentWidth),
                paddingTop + paddingBottom, footerHeight)
            (emoji as? EmojiPalettesView)?.setTypingFrameBodyHint(currentWidth, bodyFromNativeFrame)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val alphabet = findViewById<View>(R.id.keyboard_view)
        val wrapper = findViewById<View>(R.id.keyboard_view_wrapper)
        if (emoji?.visibility != View.VISIBLE && alphabet?.visibility == View.VISIBLE &&
            alphabet.parent === wrapper) {
            nativeTypingFrame.observe(measuredHeight, true, measuredWidth)
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val navigationBottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        } else {
            insets.systemWindowInsetBottom
        }
        setPadding(paddingLeft, paddingTop, paddingRight, navigationBottomInset)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets.Builder(insets)
                .setInsets(WindowInsets.Type.navigationBars(), Insets.NONE)
                .setSystemWindowInsets(Insets.of(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    0
                ))
                .build()
        } else {
            insets.replaceSystemWindowInsets(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                0
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.prefs().registerOnSharedPreferenceChangeListener(prefListener)
        cachedRadiusPx = -1f
        invalidateOutline()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.prefs().unregisterOnSharedPreferenceChangeListener(prefListener)
        staticDustOverlay.clear()
        highlightShaders.clear()
        highlightShaderWidth = -1
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Rebuild outline as soon as the IME view gets its real size.
        lastWidth = -1
        lastHeight = -1
        invalidateOutline()
        postInvalidateOnAnimation()
    }

    override fun draw(canvas: Canvas) {
        if (cachedRadiusPx < 0f) {
            cachedRadiusPx = keyboardCornerRadiusPx()
        }
        val radiusPx = cachedRadiusPx
        if (radiusPx <= 0f || width <= 0 || height <= 0) {
            super.draw(canvas)
            return
        }

        updateClipPath(radiusPx)
        val saveCount = canvas.save()
        canvas.clipPath(clipPath)
        super.draw(canvas)
        canvas.restoreToCount(saveCount)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (shouldDrawStaticDust()) {
            staticDustOverlay.draw(
                canvas,
                width,
                height,
                staticDustAlpha(),
                staticSparkleColors(),
                staticSparkleUseScreenBlend()
            )
        } else {
            staticDustOverlay.clear()
        }
        super.dispatchDraw(canvas)
        drawTopGlassHighlight(canvas)
    }

    private fun drawTopGlassHighlight(canvas: Canvas) {
        if (!runCatching { Settings.getValues()?.mColors?.isFrosted == true }.getOrDefault(false)
            || width <= 0 || height <= 0) return

        val radius = (if (cachedRadiusPx >= 0f) cachedRadiusPx else keyboardCornerRadiusPx())
            .coerceAtMost(width / 2f)
        val density = resources.displayMetrics.density
        if (radius <= density) return

        val isDark = KeyboardTheme.isDarkThemeActive(context)
        val baseAlpha = if (isDark) 0x52 else 0x30
        val style = (KeyboardTheme.livePreviewHighlightStyle ?: context.prefs().getInt(
            Settings.PREF_KEYBOARD_TOP_HIGHLIGHT_STYLE,
            Defaults.PREF_KEYBOARD_TOP_HIGHLIGHT_STYLE
        )).coerceIn(0, 3)

        if (highlightShaderWidth != width) {
            highlightShaders.clear()
            highlightShaderWidth = width
        }
        // Every style is an OPEN reflection, never a border around the slab.
        // Fade out before the very left/right edges so the specular reflection
        // cannot form the former bright vertical corner streak.
        fun reflection(path: Path, lineDp: Float, alpha: Int, softSides: Boolean = true) {
            val a = alpha.coerceIn(0, 255)
            val transparent = ColorUtils.setAlphaComponent(Color.WHITE, 0)
            val peak = ColorUtils.setAlphaComponent(Color.WHITE, a)
            val shoulder = ColorUtils.setAlphaComponent(Color.WHITE, (a * 0.7f).toInt())
            topHighlightPaint.strokeWidth = lineDp * density
            topHighlightPaint.strokeCap = Paint.Cap.BUTT
            // The shader already contains the requested opacity gradient.
            // Leaving Paint.color alpha at "a" would multiply opacity twice
            // and wash out the first-ever highlight (especially in light mode).
            topHighlightPaint.color = if (softSides) Color.WHITE else peak
            topHighlightPaint.shader = if (softSides) highlightShaders[a] ?: LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                intArrayOf(transparent, shoulder, peak, peak, shoulder, transparent),
                floatArrayOf(0f, 0.04f, 0.13f, 0.87f, 0.96f, 1f),
                Shader.TileMode.CLAMP
            ).also { highlightShaders.put(a, it) } else null
            canvas.drawPath(path, topHighlightPaint)
            topHighlightPaint.shader = null
        }

        val edge = density / 2f
        topHighlightPath.reset()
        when (style) {
            0 -> {
                // Original Lift (DEFAULT): first-ever quadratic glint from
                // a271e5a. Only its outer tips fade, keeping the slab illusion
                // without turning the reflection into a closed perimeter.
                topHighlightPath.moveTo(edge, radius)
                topHighlightPath.quadTo(edge, edge, radius, edge)
                topHighlightPath.lineTo(width - radius, edge)
                topHighlightPath.quadTo(width - edge, edge, width - edge, radius)
                reflection(topHighlightPath, 1f, baseAlpha)
            }
            1 -> {
                // Soft Horizon: a wide, low-energy top glint without side arcs.
                topHighlightPath.moveTo(width * 0.10f, 1.4f * density)
                topHighlightPath.lineTo(width * 0.90f, 1.4f * density)
                reflection(topHighlightPath, 2.8f, (baseAlpha * 0.64f).toInt())
            }
            2 -> {
                // Studio Glints: a quiet full sheen plus an offset specular stroke.
                topHighlightPath.moveTo(width * 0.13f, edge)
                topHighlightPath.lineTo(width * 0.87f, edge)
                reflection(topHighlightPath, 0.85f, (baseAlpha * 0.75f).toInt())
                topHighlightPath.reset()
                topHighlightPath.moveTo(width * 0.29f, 2.2f * density)
                topHighlightPath.lineTo(width * 0.68f, 2.2f * density)
                reflection(topHighlightPath, 1.4f, (baseAlpha * 0.46f).toInt())
            }
            else -> {
                // Quiet Edge: barely-there centered lift, no curved side streaks.
                topHighlightPath.moveTo(width * 0.23f, edge)
                topHighlightPath.lineTo(width * 0.77f, edge)
                reflection(topHighlightPath, 0.8f, (baseAlpha * 0.65f).toInt())
            }
        }
    }

    private fun keyboardCornerRadiusPx(): Float {
        val radiusDp = Settings.readKeyboardCornerRadius(context.prefs()).coerceIn(
                Settings.KEYBOARD_CORNER_RADIUS_MIN_DP,
                Settings.KEYBOARD_CORNER_RADIUS_MAX_DP
            )
        return radiusDp * resources.displayMetrics.density
    }

    private fun shouldDrawStaticDust(): Boolean {
        if (Defaults.LIMIT_EXPENSIVE_RENDERING) return false
        val isFrosted = runCatching { Settings.getValues()?.mColors?.isFrosted == true }.getOrDefault(false)
        return isFrosted && staticDustEnabled()
    }

    private fun staticDustEnabled(): Boolean {
        return KeyboardTheme.livePreviewValues?.dustEnabled
            ?: context.prefs().getBoolean(Settings.PREF_FROSTED_DUST_ENABLED, Defaults.PREF_FROSTED_DUST_ENABLED)
    }

    private fun staticDustAlpha(): Float {
        KeyboardTheme.livePreviewValues?.dustAlpha?.let {
            return renderSparkleAlpha(it)
        }

        val prefs = context.prefs()
        val alpha = if (KeyboardTheme.isDarkThemeActive(context)) {
            prefs.getFloat(
                Settings.PREF_FROSTED_DUST_ALPHA_NIGHT,
                prefs.getFloat(Settings.PREF_FROSTED_DUST_ALPHA, Defaults.PREF_FROSTED_DUST_ALPHA_NIGHT)
            )
        } else {
            prefs.getFloat(Settings.PREF_FROSTED_DUST_ALPHA, Defaults.PREF_FROSTED_DUST_ALPHA)
        }
        return renderSparkleAlpha(alpha)
    }

    private fun renderSparkleAlpha(alpha: Float): Float {
        val isDark = KeyboardTheme.isDarkThemeActive(context)
        val sliderAlpha = alpha.coerceIn(MIN_SPARKLE_ALPHA, MAX_SPARKLE_ALPHA)
        if (isDark) {
            return sliderAlpha
        }

        val progress = (sliderAlpha - MIN_SPARKLE_ALPHA) / (MAX_SPARKLE_ALPHA - MIN_SPARKLE_ALPHA)
        return MIN_SPARKLE_ALPHA + progress * (MAX_LIGHT_THEME_SPARKLE_ALPHA - MIN_SPARKLE_ALPHA)
            .coerceIn(MIN_SPARKLE_ALPHA, MAX_LIGHT_THEME_SPARKLE_ALPHA)
    }

    private fun staticSparkleColors(): IntArray {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return intArrayOf(
                Color.rgb(255, 244, 210),
                Color.rgb(165, 232, 255),
                Color.rgb(220, 180, 255),
                Color.rgb(255, 176, 122),
                Color.rgb(160, 195, 255)
            )
        }

        val isDark = KeyboardTheme.isDarkThemeActive(context)
        return intArrayOf(
            ContextCompat.getColor(context, android.R.color.system_accent1_100),
            ContextCompat.getColor(context, android.R.color.system_accent2_100),
            ContextCompat.getColor(context, android.R.color.system_accent3_100),
            ContextCompat.getColor(context, android.R.color.system_accent1_200),
            ContextCompat.getColor(context, android.R.color.system_accent2_200)
        ).map { polishSparkleColor(it, isDark) }.toIntArray()
    }

    private fun staticSparkleUseScreenBlend(): Boolean {
        return true
    }

    private fun polishSparkleColor(color: Int, isDark: Boolean): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[1] = (hsl[1] * if (isDark) 1.4f else 1.15f).coerceIn(if (isDark) 0.72f else 0.48f, 1f)
        hsl[2] = if (isDark) hsl[2].coerceIn(0.76f, 0.95f) else hsl[2].coerceIn(0.84f, 0.97f)
        val polished = ColorUtils.HSLToColor(hsl)
        return if (isDark) polished else ColorUtils.blendARGB(polished, Color.WHITE, 0.28f)
    }

    private fun updateClipPath(radiusPx: Float) {
        if (lastWidth == width && lastHeight == height && lastRadiusPx == radiusPx) {
            return
        }

        lastWidth = width
        lastHeight = height
        lastRadiusPx = radiusPx
        clipRect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(
            clipRect,
            floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx, 0f, 0f, 0f, 0f),
            Path.Direction.CW
        )
    }

    private companion object {
        private const val MIN_SPARKLE_ALPHA = 1f
        private const val MAX_SPARKLE_ALPHA = 10f
        private const val MAX_LIGHT_THEME_SPARKLE_ALPHA = 20f
    }
}
