// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.clipboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.content.res.ColorStateList
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.load
import coil.request.CachePolicy
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.ClipboardImageHistoryClip
import helium314.keyboard.latin.ClipboardHistoryManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.KeyBackgroundUtils
import helium314.keyboard.latin.settings.Settings
import androidx.core.graphics.ColorUtils

class ClipboardAdapter(
       val clipboardLayoutParams: ClipboardLayoutParams,
       val keyEventListener: OnKeyEventListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** Request only: the parent shows a real slide-to-confirm before any deletion. */
    var onClearRequested: (() -> Unit)? = null

    var clipboardHistoryManager: ClipboardHistoryManager? = null
    private var filterQuery: String = ""
    private var filteredIds: List<Long> = emptyList()
    var horizontalSearchRow: Boolean = false

    val isFiltered: Boolean get() = filterQuery.isNotEmpty()

    /** Ephemeral in-memory IDs only; no extra clipboard text copies, no upload. */
    fun setSearchQuery(query: String) {
        filterQuery = query.trim().lowercase(java.util.Locale.ROOT)
        refreshSearchResults()
    }

    fun refreshSearchResults() {
        val manager = clipboardHistoryManager
        filteredIds = if (filterQuery.isEmpty() || manager == null) emptyList() else
            ClipboardSearchPolicy.matchingIds(filterQuery,
                (0 until manager.getHistorySize()).mapNotNull { manager.getHistoryEntry(it) })
        notifyDataSetChanged()
    }

    var pinnedIconResId = 0
    var itemBackgroundId = 0
    var itemTypeFace: Typeface? = null
    var itemTextColor = 0
    var itemTextSize = 0f

    companion object {
        private const val VIEW_TYPE_TEXT = 0
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_CLEAR = 2
        private const val ROUNDED_ENTRY_RADIUS_DP = 16f
    }

    /** Moving high-contrast rainbow ink below sharp white match letters. */
    private class RainbowMatchSpan(private val density: Float) : ReplacementSpan() {
        private val inset = 3f * density
        private val shaderMatrix = android.graphics.Matrix()
        private var cachedWidth = -1f
        private var cachedShader: LinearGradient? = null
        override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int,
                fm: Paint.FontMetricsInt?): Int =
            kotlin.math.ceil(paint.measureText(text, start, end) + 2f * inset).toInt()

        override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
                x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            val width = paint.measureText(text, start, end) + 2f * inset
            val rect = RectF(x + density,
                y + paint.fontMetrics.ascent - 2f * density,
                x + width - density, y + paint.fontMetrics.descent + 2f * density)
            val oldColor = paint.color
            val oldShader = paint.shader
            val oldStyle = paint.style
            val oldStroke = paint.strokeWidth
            val oldBold = paint.isFakeBoldText
            try {
                paint.shader = null
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f * density
                paint.color = 0xA65D55CE.toInt()
                canvas.drawRoundRect(rect, 6f * density, 6f * density, paint)
                paint.style = Paint.Style.FILL
                // Shader output is multiplied by Paint alpha. The previous
                // translucent border color (0x66 alpha) had made the rainbow
                // highlighter look like a faint gray wash. Restore full alpha.
                paint.color = Color.WHITE
                if (cachedShader == null || cachedWidth != rect.width()) {
                    cachedWidth = rect.width()
                    cachedShader = LinearGradient(-cachedWidth, 0f, cachedWidth * 2f, 0f,
                        intArrayOf(0xFFBA2FFF.toInt(), 0xFFFA4BA9.toInt(),
                            0xFF3ABBE9.toInt(), 0xFF38D2B1.toInt(), 0xFFB565FF.toInt()),
                        null, Shader.TileMode.MIRROR)
                }
                // The matched card's live foreground invalidates this span;
                // no TextWatcher or query/recycler relayout is involved.
                val travel = (android.os.SystemClock.uptimeMillis() % 5200L) / 5200f
                shaderMatrix.setTranslate(rect.left + (travel * 2f - 1f) * rect.width(), rect.top)
                cachedShader?.setLocalMatrix(shaderMatrix)
                paint.shader = cachedShader
                canvas.drawRoundRect(rect, 6f * density, 6f * density, paint)
                paint.shader = null
                paint.color = Color.WHITE
                paint.isFakeBoldText = true
                canvas.drawText(text, start, end, x + inset, y.toFloat(), paint)
            } finally {
                paint.color = oldColor
                paint.shader = oldShader
                paint.style = oldStyle
                paint.strokeWidth = oldStroke
                paint.isFakeBoldText = oldBold
            }
        }
    }

    private fun emphasizedPreview(text: String, query: String, density: Float): CharSequence {
        val window = ClipboardSearchPreview.window(text, query)
        if (!window.hasMatch) return window.text
        return SpannableStringBuilder(window.text).apply {
            setSpan(RainbowMatchSpan(density),
                window.matchStart, window.matchEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (hasClearAction() && position == (clipboardHistoryManager?.getHistorySize() ?: 0))
            return VIEW_TYPE_CLEAR
        val entry = getItem(position)
        val imageClip = entry?.text?.let { ClipboardHistoryManager.decodeImageHistoryClip(it) }
            ?.takeIf { it.uri.scheme == "content" }
        return if (imageClip != null) VIEW_TYPE_IMAGE else VIEW_TYPE_TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_CLEAR) {
            val density = parent.resources.displayMetrics.density
            val colors = Settings.getValues().mColors
            val label = TextView(parent.context).apply {
                text = "Clear recent clipboard  ›"
                contentDescription = "Clear unpinned clipboard history. Confirmation required."
                setTextColor(colors.get(ColorType.FUNCTIONAL_KEY_TEXT))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = android.view.Gravity.CENTER
                minHeight = (52f * density).toInt()
                background = GradientDrawable().apply {
                    cornerRadius = 20f * density
                    setColor(ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_BACKGROUND), 0x6A))
                    setStroke((density).toInt().coerceAtLeast(1),
                        ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_TEXT), 0x3B))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { onClearRequested?.invoke() }
            }
            label.layoutParams = StaggeredGridLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                isFullSpan = true
                topMargin = (8f * density).toInt()
                bottomMargin = (12f * density).toInt()
            }
            return ClearHolder(label)
        }
        val layoutId = if (viewType == VIEW_TYPE_IMAGE) {
            R.layout.clipboard_entry_image_key
        } else {
            R.layout.clipboard_entry_key
        }
        val view = LayoutInflater.from(parent.context)
                .inflate(layoutId, parent, false)
        return ViewHolder(parent, view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) holder.setContent(getItem(position))
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is ClearHolder)
            (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
        (holder.itemView.foreground as? ClipboardRainbowGlowDrawable)?.start()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        (holder.itemView.foreground as? ClipboardRainbowGlowDrawable)?.stop()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder.itemView.foreground as? ClipboardRainbowGlowDrawable)?.stop()
        if (holder is ViewHolder) holder.clearImage()
        super.onViewRecycled(holder)
    }

    private fun getItem(position: Int): ClipboardHistoryEntry? {
        val manager = clipboardHistoryManager ?: return null
        return if (isFiltered) filteredIds.getOrNull(position)?.let(manager::getHistoryEntryContent)
               else manager.getHistoryEntry(position)
    }

    private fun hasClearAction(): Boolean = !isFiltered && clipboardHistoryManager?.let { manager ->
        (0 until manager.getHistorySize()).any { manager.getHistoryEntry(it)?.isPinned == false }
    } == true

    override fun getItemCount() = if (isFiltered) filteredIds.size else
        (clipboardHistoryManager?.getHistorySize() ?: 0) + if (hasClearAction()) 1 else 0

    class ClearHolder(view: View) : RecyclerView.ViewHolder(view)

    inner class ViewHolder(
            val parent: ViewGroup,
            view: View
    ) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {

        private val pinnedIconView: ImageView
        private val imageView: ImageView
        private val contentView: TextView?

        init {
            view.apply {
                setOnClickListener(this@ViewHolder)
                setOnTouchListener(this@ViewHolder)
                setOnLongClickListener(this@ViewHolder)
                setBackgroundResource(itemBackgroundId)
                isHapticFeedbackEnabled = false
            }
            val colors = Settings.getValues().mColors
            // All supported themes share one quiet card silhouette; the glass
            // fill lets the keyboard material show through instead of a heavy
            // dark Android-style rectangle.
            view.background = createRoundedClipboardEntryBackground(view)
            pinnedIconView = view.findViewById<ImageView>(R.id.clipboard_entry_pinned_icon).apply {
                visibility = View.GONE
                setImageResource(pinnedIconResId)
                // The enclosing card exposes a labelled pin/unpin action instead.
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            imageView = view.findViewById(R.id.clipboard_entry_image)
            configureImagePreviewCorners()
            contentView = view.findViewById<TextView>(R.id.clipboard_entry_content)?.apply {
                KeyboardTypeface.applyToTextView(this, null, itemTypeFace ?: android.graphics.Typeface.DEFAULT)
                setTextColor(itemTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize)
            }
            clipboardLayoutParams.setItemProperties(view)
            pinnedIconView.imageTintList = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_TEXT), 0xC4))
        }

        private fun createRoundedClipboardEntryBackground(view: View): StateListDrawable {
            val colors = Settings.getValues().mColors
            val isDark = KeyboardTheme.isDarkThemeActive(view.context)
            val rawSurface = colors.get(ColorType.MAIN_BACKGROUND)
            val baseSurface = if (Color.alpha(rawSurface) < 24) {
                if (isDark) 0xFF24252B.toInt() else 0xFFF0F1F4.toInt()
            } else ColorUtils.setAlphaComponent(rawSurface, 255)
            val shade = if (isDark) Color.WHITE else Color.BLACK
            val normalColor = ColorUtils.setAlphaComponent(
                ColorUtils.blendARGB(baseSurface, shade, if (isDark) 0.11f else 0.045f),
                if (isDark) 206 else 198,
            )
            val pressedColor = ColorUtils.setAlphaComponent(
                ColorUtils.blendARGB(baseSurface, shade, if (isDark) 0.18f else 0.09f), 230)
            val radius = ROUNDED_ENTRY_RADIUS_DP * view.resources.displayMetrics.density
            val outline = ColorUtils.setAlphaComponent(shade, if (isDark) 35 else 18)
            val border = view.resources.displayMetrics.density.coerceAtLeast(1f).toInt()
            return StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), roundedRectDrawable(pressedColor, radius, outline, border))
                addState(intArrayOf(android.R.attr.state_selected), roundedRectDrawable(pressedColor, radius, outline, border))
                addState(intArrayOf(), roundedRectDrawable(normalColor, radius, outline, border))
            }
        }

        private fun roundedRectDrawable(color: Int, radius: Float, outline: Int, border: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
            setStroke(border, outline)
        }

        private fun configureImagePreviewCorners() {
            val density = imageView.resources.displayMetrics.density
            val margin = (imageView.layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
            val radius = (ROUNDED_ENTRY_RADIUS_DP * density - margin).coerceAtLeast(0f)
            imageView.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
            imageView.clipToOutline = true
        }

        fun setContent(historyEntry: ClipboardHistoryEntry?) {
            itemView.tag = historyEntry?.id
            itemView.isEnabled = historyEntry != null
            val imageClip = historyEntry?.text?.let { ClipboardHistoryManager.decodeImageHistoryClip(it) }
                ?.takeIf { it.uri.scheme == "content" }

            (itemView.foreground as? ClipboardRainbowGlowDrawable)?.stop()
            val glow = if (horizontalSearchRow && filterQuery.isNotBlank()
                    && historyEntry != null && imageClip == null)
                ClipboardRainbowGlowDrawable(itemView.resources.displayMetrics.density, false) else null
            itemView.foreground = glow
            if (itemView.isAttachedToWindow) glow?.start()

            // Adjust StaggeredGridLayoutManager parameters so it stays in one column
            val lp = itemView.layoutParams
            if (lp is StaggeredGridLayoutManager.LayoutParams) {
                lp.isFullSpan = false
            }

            if (imageClip != null) {
                imageView.isGone = false
                imageView.load(imageClip.uri) {
                    crossfade(false)
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                }
                imageView.contentDescription = itemView.context.getString(R.string.clipboard_image_preview)
                contentView?.text = imageClip.label
                contentView?.let { KeyboardTypeface.applyToTextView(it, imageClip.label, itemTypeFace ?: android.graphics.Typeface.DEFAULT) }

                // Bound thumbnails so a landscape panel or a single image cannot
                // occupy the entire history viewport.
                if (horizontalSearchRow) {
                    if (lp != null) {
                        lp.width = searchCardWidth()
                        lp.height = searchCardHeight()
                        itemView.layoutParams = lp
                    }
                } else if (parent.width > 0) {
                    val layoutManager = (parent as? RecyclerView)?.layoutManager
                    val spanCount = when (layoutManager) {
                        is StaggeredGridLayoutManager -> layoutManager.spanCount
                        else -> 2
                    }
                    if (lp != null) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                        lp.height = ClipboardCardGeometry.previewHeightPx(
                            parent.width,
                            parent.paddingLeft + parent.paddingRight,
                            spanCount,
                            parent.resources.displayMetrics.density,
                        )
                        itemView.layoutParams = lp
                    }
                } else {
                    if (lp != null) {
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                        lp.height = ClipboardCardGeometry.previewHeightPx(
                            0, 0, 1, parent.resources.displayMetrics.density,
                        )
                        itemView.layoutParams = lp
                    }
                }
            } else {
                imageView.isGone = true
                // Cancels a pending Coil request before a recycled card is reused.
                clearImage()
                val text = historyEntry?.text?.let {
                    // A damaged legacy image marker must never be shown as raw serialized data.
                    if (ClipboardImageHistoryClip.hasImageHistoryPrefix(it))
                        itemView.context.getString(R.string.clipboard_image_unavailable)
                    else if (horizontalSearchRow && filterQuery.isNotBlank())
                        // Build the visible window around the actual hit, not
                        // the first 1,000 characters of a long copied note.
                        it
                    else it.take(1000)
                }
                val visibleText = if (horizontalSearchRow && text != null
                        && filterQuery.isNotBlank())
                    emphasizedPreview(text, filterQuery, itemView.resources.displayMetrics.density)
                    else text
                contentView?.text = visibleText
                contentView?.let { KeyboardTypeface.applyToTextView(it, visibleText,
                    itemTypeFace ?: android.graphics.Typeface.DEFAULT) }

                contentView?.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    if (horizontalSearchRow) itemTextSize * 1.07f else itemTextSize)

                contentView?.maxLines = if (horizontalSearchRow) 6 else 4
                // Search shows one horizontally scrollable row of bounded
                // cards; full original clip text is retained for insertion.
                if (lp != null) {
                    lp.width = if (horizontalSearchRow) searchCardWidth()
                        else ViewGroup.LayoutParams.MATCH_PARENT
                    lp.height = if (horizontalSearchRow) searchCardHeight()
                        else ViewGroup.LayoutParams.WRAP_CONTENT
                    itemView.layoutParams = lp
                }
            }
            pinnedIconView.visibility = if (historyEntry?.isPinned == true) View.VISIBLE else View.GONE
            val pinAction = if (historyEntry?.isPinned == true) R.string.clipboard_unpin_item else R.string.clipboard_pin_item
            ViewCompat.replaceAccessibilityAction(
                itemView,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
                itemView.context.getString(pinAction),
            ) { _, _ ->
                val clipId = itemView.tag as? Long ?: return@replaceAccessibilityAction false
                clipboardHistoryManager?.toggleClipPinned(clipId)
                true
            }
        }

        private fun searchCardWidth(): Int {
            val screen = if (parent.width > 0) parent.width else
                helium314.keyboard.latin.utils.ResourceUtils.getKeyboardWidth(
                    parent.context, Settings.getValues())
            val density = itemView.resources.displayMetrics.density
            return (screen * 0.55f).toInt().coerceIn((120 * density).toInt(), (220 * density).toInt())
        }

        // Horizontal cards flex with the Search viewport instead of fixing a
        // tiny 80dp island above the alphabet. The source clip is unmodified.
        private fun searchCardHeight(): Int = ViewGroup.LayoutParams.MATCH_PARENT

        fun clearImage() {
            imageView.load(null)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                val id = view.tag as? Long ?: return false
                keyEventListener.onKeyDown(id)
            }
            return false
        }

        override fun onClick(view: View) {
            val id = view.tag as? Long ?: return
            keyEventListener.onKeyUp(id)
        }

        override fun onLongClick(view: View): Boolean {
            val id = view.tag as? Long ?: return false
            clipboardHistoryManager?.toggleClipPinned(id)
            return true
        }
    }
}
