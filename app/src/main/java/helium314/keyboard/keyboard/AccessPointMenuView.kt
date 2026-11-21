// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.ClipData
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.DragShadowBuilder
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.core.graphics.drawable.DrawableCompat
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.BoardFooterView
import helium314.keyboard.latin.R
import helium314.keyboard.latin.SharedPanelGeometry
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.ResourceUtils
import helium314.keyboard.latin.utils.TOOLBAR_DRAG_CLIP_LABEL
import helium314.keyboard.latin.utils.ToolbarDragSource
import helium314.keyboard.latin.utils.ToolbarDragState
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.ToolbarMode
import helium314.keyboard.latin.utils.getCodeForToolbarKey
import helium314.keyboard.latin.utils.getEnabledToolbarKeys
import helium314.keyboard.latin.utils.getStringResourceOrName
import helium314.keyboard.latin.utils.getToolbarKeyFromDragData
import helium314.keyboard.latin.utils.isAvailableInToolbarUi
import helium314.keyboard.latin.utils.removePinnedKey
import helium314.keyboard.latin.utils.startDragAndDropCompat

class AccessPointMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var keyboardActionListener: KeyboardActionListener = KeyboardActionListener.EMPTY_LISTENER
    private val grid: GridLayout
    private val scroll: HorizontalScrollView
    private var accessDropPreviewView: View? = null
    private var accessDropPreviewIndex = -1
    private var ownGridDragIndex = -1
    private val dragTargetScreenLocation = IntArray(2)
    private val gridScreenLocation = IntArray(2)
    private val dragAutoScrollHandler = Handler(Looper.getMainLooper())
    private var dragAutoScrollDirection = 0
    private var dragAutoScrollX = 0f
    private val dragAutoScroll = object : Runnable {
        override fun run() {
            val direction = dragAutoScrollDirection
            if (direction == 0 || !scroll.isLaidOut || grid.width <= 0) return
            val maxScroll = (grid.width - scroll.width + scroll.paddingLeft + scroll.paddingRight)
                .coerceAtLeast(0)
            val edge = (48f * resources.displayMetrics.density).coerceAtLeast(1f)
            val penetration = if (direction < 0) {
                (edge - dragAutoScrollX).coerceIn(0f, edge)
            } else {
                (dragAutoScrollX - (scroll.width - edge)).coerceIn(0f, edge)
            }
            val frameStep = ToolbarDragAutoScrollPolicy.frameStepPx(
                resources.displayMetrics.density, penetration / edge)
            val nextScroll = (scroll.scrollX + direction * frameStep).coerceIn(0, maxScroll)
            if (nextScroll == scroll.scrollX) {
                stopDragAutoScroll()
                return
            }
            scroll.scrollTo(nextScroll, 0)
            dragAutoScrollHandler.postDelayed(this, 16L)
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.access_point_menu, this, true)
        grid = findViewById(R.id.access_point_grid)
        scroll = findViewById(R.id.access_point_scroll)
        scroll.isHorizontalScrollBarEnabled = false
        scroll.isSmoothScrollingEnabled = true
        PanelMaterial.extendIntoFooter(scroll.parent as View, scroll)
        scroll.setOnScrollChangeListener { _, _, _, _, _ ->
            findViewById<View>(R.id.access_point_header_scrim)?.postInvalidateOnAnimation()
            findViewById<View>(R.id.access_point_bottom_glass)?.postInvalidateOnAnimation()
            rootView.findViewById<BoardFooterView>(R.id.board_footer)?.invalidatePanelGlass()
        }
        val dropPinnedKeyListener = View.OnDragListener { target, event -> onPinnedKeyDropFromStrip(target, event) }
        setOnDragListener(dropPinnedKeyListener)
        grid.setOnDragListener(dropPinnedKeyListener)
        scroll.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> getToolbarKeyFromDragData(
                    event.localState, event.clipData) != null
                DragEvent.ACTION_DRAG_LOCATION -> {
                    updateDragAutoScroll(event.x)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DROP, DragEvent.ACTION_DRAG_ENDED -> {
                    stopDragAutoScroll()
                    true
                }
                else -> false
            }
        }
    }

    fun setKeyboardActionListener(listener: KeyboardActionListener) {
        keyboardActionListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val settings = Settings.getValues()
        val abcHeight = ResourceUtils.getKeyboardHeight(resources, settings)
        val emojiRowHeight = if (context.prefs().getBoolean(
                Settings.PREF_PERSISTENT_EMOJI_ROW,
                helium314.keyboard.latin.settings.Defaults.PREF_PERSISTENT_EMOJI_ROW))
            (41 * resources.displayMetrics.density).toInt() else 0
        val accessoryHeight = if (settings.mToolbarMode == ToolbarMode.HIDDEN) 0 else
            resources.getDimensionPixelSize(R.dimen.config_suggestions_strip_height)
        val strip = rootView.findViewById<View>(R.id.strip_container)
        val availableHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> MeasureSpec.getSize(heightMeasureSpec)
            else -> Int.MAX_VALUE
        }
        val finalHeight = SharedPanelGeometry.panelBodyHeight(abcHeight, emojiRowHeight,
            accessoryHeight, strip != null && strip.visibility == View.VISIBLE, availableHeight)
        val constrainedHeightSpec = MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, constrainedHeightSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), finalHeight)
    }

    fun populateMenu() {
        grid.removeAllViews()
        scroll.scrollTo(0, 0)
        val colors = Settings.getValues().mColors
        PanelMaterial.apply(context, colors,
            findViewById(R.id.access_point_header_scrim),
            findViewById(R.id.access_point_bottom_glass), scroll)
        findViewById<TextView>(R.id.access_point_title).also {
            KeyboardTypeface.applyToTextView(it)
            it.setTextColor(colors.get(ColorType.KEY_TEXT))
        }
        val prefs = context.prefs()
        // The full panel replaces the pinned suggestion strip; show all tools
        // here, including entries previously omitted as pinned/persistent.
        val enabledKeys = getEnabledToolbarKeys(prefs).filter {
            it.isAvailableInToolbarUi()
        }

        val inflater = LayoutInflater.from(context)
        for (key in enabledKeys) {
            val tile = inflater.inflate(R.layout.menu_tile_item, grid, false)
            tile.tag = key
            try {
                val iconView = tile.findViewById<ImageButton>(R.id.menu_tile_icon)
                // The Toolbar is a continuous icon rail; large theme-colored
                // keycaps waste room and visually fragment the panel.
                iconView.setBackgroundResource(android.R.color.transparent)
                val labelView = tile.findViewById<TextView>(R.id.menu_tile_label)

                var drawable: android.graphics.drawable.Drawable? = null
                try {
                    drawable = KeyboardIconsSet.instance.getNewDrawable(key.name, context)
                } catch (e: Exception) {
                    android.util.Log.e("AccessPointMenuView", "Failed to load drawable for ${key.name}", e)
                }
                if (drawable == null) {
                    drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_settings_default)
                }

                val keyboardTextColor = Settings.getValues().mColors.get(helium314.keyboard.latin.common.ColorType.KEY_TEXT)
                applyKeyTextIcon(iconView, drawable, keyboardTextColor)

                labelView.text = key.name.lowercase().getStringResourceOrName("", context)
                labelView.setTextColor(keyboardTextColor)
                KeyboardTypeface.applyToTextView(labelView)

                // Icon is non-interactive; touch is handled by parent tile
                iconView.isClickable = false
                iconView.isFocusable = false
            } catch (e: android.content.res.Resources.NotFoundException) {
                android.util.Log.e("AccessPointMenuView", "Resource not found for tile ${key.name}", e)
            }

            // Whole tile is the touch target, not just the icon
            tile.setOnClickListener {
                AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.NOT_SPECIFIED, tile, HapticEvent.KEY_PRESS)
                val code = getCodeForToolbarKey(key)
                if (code != helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.UNSPECIFIED) {
                    keyboardActionListener.onCodeInput(code, Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE, false)
                }
                if (code != helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.CLIPBOARD &&
                    code != helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.AI_TOOLS &&
                    code != helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.EMOJI &&
                    code != helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.NUMPAD &&
                    code != helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.GIFS &&
                    code != helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.STICKERS &&
                    code != helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.RESIZE_KEYBOARD) {
                    KeyboardSwitcher.getInstance().setAlphabetKeyboard()
                }
            }

            tile.setOnLongClickListener { v ->
                AudioAndHapticFeedbackManager.getInstance().performHapticFeedback(v, HapticEvent.KEY_LONG_PRESS)
                val clipData = ClipData.newPlainText(TOOLBAR_DRAG_CLIP_LABEL, key.name)
                val shadow = DragShadowBuilder(v)
                v.visibility = View.INVISIBLE
                v.startDragAndDropCompat(clipData, shadow, ToolbarDragState(key, ToolbarDragSource.ACCESS_POINT_MENU, v))
                true
            }

            tile.setOnDragListener { v, event ->
                val dragState = event.localState as? ToolbarDragState
                val draggedView = dragState?.sourceView ?: event.localState as? View
                val ownGridDraggedView = draggedView?.takeIf {
                    dragState?.source == ToolbarDragSource.ACCESS_POINT_MENU && it.parent === grid
                }
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> getToolbarKeyFromDragData(event.localState, event.clipData) != null
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        if (ownGridDraggedView != null && ownGridDraggedView != v) {
                            previewOwnGridMove(ownGridDraggedView, accessDropIndexFromEvent(v, event))
                        } else if (dragState?.source == ToolbarDragSource.PINNED_ROW) {
                            previewPinnedKeyInAccessMenu(dragState.key,
                                accessDropIndexFromEvent(v, event))
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_LOCATION -> {
                        updateDragAutoScroll(event.x + grid.left + v.left - scroll.scrollX)
                        if (ownGridDraggedView != null && ownGridDraggedView != v) {
                            previewOwnGridMove(ownGridDraggedView,
                                accessDropIndexFromEvent(v, event))
                        }
                        if (dragState?.source == ToolbarDragSource.PINNED_ROW) {
                            previewPinnedKeyInAccessMenu(dragState.key,
                                accessDropIndexFromEvent(v, event))
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        v.alpha = 1.0f
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        stopDragAutoScroll()
                        if (ownGridDraggedView != null) {
                            grid.post {
                                for (i in 0 until grid.childCount) {
                                    grid.getChildAt(i).alpha = 1.0f
                                }
                                ownGridDraggedView.visibility = View.VISIBLE
                                saveToolbarKeyOrder(grid)
                            }
                        } else if (dragState?.source == ToolbarDragSource.PINNED_ROW) {
                            commitPinnedKeyDropToAccess(dragState.key)
                        } else {
                            grid.post {
                                for (i in 0 until grid.childCount) {
                                    grid.getChildAt(i).alpha = 1.0f
                                }
                            }
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        stopDragAutoScroll()
                        grid.post {
                            for (i in 0 until grid.childCount) {
                                grid.getChildAt(i).alpha = 1.0f
                            }
                            if (draggedView != null) {
                                draggedView.visibility = View.VISIBLE
                            }
                            ownGridDragIndex = -1
                            if (dragState?.source == ToolbarDragSource.PINNED_ROW) {
                                clearAccessDropPreview()
                            }
                        }
                        true
                    }
                    else -> false
                }
            }

            grid.addView(tile)
        }
    }

    private fun onPinnedKeyDropFromStrip(target: View, event: DragEvent): Boolean {
        val dragState = event.localState as? ToolbarDragState
        if (event.action == DragEvent.ACTION_DRAG_LOCATION && dragState != null) {
            target.getLocationOnScreen(dragTargetScreenLocation)
            scroll.getLocationOnScreen(gridScreenLocation)
            updateDragAutoScroll(event.x + dragTargetScreenLocation[0] - gridScreenLocation[0])
        }
        if (dragState?.source != ToolbarDragSource.PINNED_ROW) return false
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> {
                alpha = 0.92f
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                previewPinnedKeyInAccessMenu(dragState.key, accessDropIndexFromEvent(target, event))
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                alpha = 1.0f
                true
            }
            DragEvent.ACTION_DROP -> {
                stopDragAutoScroll()
                alpha = 1.0f
                commitPinnedKeyDropToAccess(dragState.key)
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                stopDragAutoScroll()
                alpha = 1.0f
                clearAccessDropPreview()
                true
            }
            else -> true
        }
    }

    private fun updateDragAutoScroll(pointerXInViewport: Float) {
        dragAutoScrollX = pointerXInViewport
        val edge = 48f * resources.displayMetrics.density
        val maxScroll = (grid.width - scroll.width + scroll.paddingLeft + scroll.paddingRight)
            .coerceAtLeast(0)
        val direction = ToolbarDragAutoScrollPolicy.direction(pointerXInViewport,
            scroll.width, edge, scroll.scrollX, maxScroll)
        if (direction == dragAutoScrollDirection) return
        dragAutoScrollHandler.removeCallbacks(dragAutoScroll)
        dragAutoScrollDirection = direction
        if (direction != 0) dragAutoScrollHandler.post(dragAutoScroll)
    }

    private fun stopDragAutoScroll() {
        dragAutoScrollDirection = 0
        dragAutoScrollHandler.removeCallbacks(dragAutoScroll)
    }

    override fun onDetachedFromWindow() {
        stopDragAutoScroll()
        super.onDetachedFromWindow()
    }

    private fun commitPinnedKeyDropToAccess(key: ToolbarKey) {
        removePinnedKey(context.prefs(), key)
        accessDropPreviewView?.let { if (it.parent === grid) saveToolbarKeyOrder(grid) }
        clearAccessDropPreview()
        populateMenu()
        AudioAndHapticFeedbackManager.getInstance().performHapticFeedback(this, HapticEvent.KEY_PRESS)
    }

    private fun previewPinnedKeyInAccessMenu(key: ToolbarKey, index: Int) {
        val preview = accessDropPreviewView ?: createAccessDropPreviewTile(key).also {
            accessDropPreviewView = it
        }
        val targetIndex = index.coerceIn(0, grid.childCount)
        if (preview.parent === grid && accessDropPreviewIndex == targetIndex) return
        accessDropPreviewIndex = targetIndex
        if (preview.parent !== grid) {
            grid.addView(preview, targetIndex)
            return
        }
        moveGridChildToIndex(preview, targetIndex)
    }

    private fun clearAccessDropPreview() {
        accessDropPreviewView?.let {
            if (it.parent === grid) grid.removeView(it)
        }
        accessDropPreviewView = null
        accessDropPreviewIndex = -1
    }

    private fun previewOwnGridMove(child: View, index: Int) {
        val targetIndex = index.coerceIn(0, grid.childCount - 1)
        if (ownGridDragIndex == targetIndex) return
        ownGridDragIndex = targetIndex
        moveGridChildToIndex(child, targetIndex)
    }

    private fun moveGridChildToIndex(child: View, index: Int) {
        val currentIndex = grid.indexOfChild(child)
        val targetIndex = index.coerceIn(0, grid.childCount - 1)
        if (currentIndex < 0 || currentIndex == targetIndex) return
        grid.removeView(child)
        grid.addView(child, targetIndex)
    }

    private fun accessDropIndexFromEvent(target: View, event: DragEvent): Int {
        target.getLocationOnScreen(dragTargetScreenLocation)
        grid.getLocationOnScreen(gridScreenLocation)
        val xInGrid = event.x + dragTargetScreenLocation[0] - gridScreenLocation[0]
        val yInGrid = event.y + dragTargetScreenLocation[1] - gridScreenLocation[1]
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            if (xInGrid >= child.left.toFloat() && xInGrid <= child.right.toFloat()
                && yInGrid >= child.top.toFloat() && yInGrid <= child.bottom.toFloat()
            ) {
                return i
            }
        }
        return grid.childCount
    }

    private fun createAccessDropPreviewTile(key: ToolbarKey): View {
        val tile = LayoutInflater.from(context).inflate(R.layout.menu_tile_item, grid, false)
        tile.tag = key
        tile.alpha = 0.42f
        tile.isClickable = false
        tile.isFocusable = false
        val iconView = tile.findViewById<ImageButton>(R.id.menu_tile_icon)
        val labelView = tile.findViewById<TextView>(R.id.menu_tile_label)
        val keyboardTextColor = Settings.getValues().mColors.get(helium314.keyboard.latin.common.ColorType.KEY_TEXT)
        applyKeyTextIcon(iconView, KeyboardIconsSet.instance.getNewDrawable(key.name, context), keyboardTextColor)
        labelView.text = key.name.lowercase().getStringResourceOrName("", context)
        labelView.setTextColor(keyboardTextColor)
        KeyboardTypeface.applyToTextView(labelView)
        return tile
    }

    private fun saveToolbarKeyOrder(grid: GridLayout) {
        val prefs = context.prefs()
        val allPrefString = prefs.getString(Settings.PREF_TOOLBAR_KEYS, helium314.keyboard.latin.utils.defaultToolbarPref) ?: return
        val allEntries = allPrefString.split(Constants.Separators.ENTRY).toMutableList()

        val reorderedEnabled = mutableListOf<ToolbarKey>()
        for (i in 0 until grid.childCount) {
            val tile = grid.getChildAt(i)
            val key = tile.tag as? ToolbarKey
            if (key != null) {
                reorderedEnabled.add(key)
            }
        }

        val newEntries = mutableListOf<String>()
        for (key in reorderedEnabled) {
            newEntries.add("${key.name}${Constants.Separators.KV}true")
        }
        for (entry in allEntries) {
            val name = entry.substringBefore(Constants.Separators.KV)
            if (reorderedEnabled.none { it.name == name }) {
                newEntries.add(entry)
            }
        }
        prefs.edit().putString(Settings.PREF_TOOLBAR_KEYS, newEntries.joinToString(Constants.Separators.ENTRY)).commit()
        Settings.getInstance().onSharedPreferenceChanged(prefs, Settings.PREF_TOOLBAR_KEYS)
    }

    fun updateThemeColors(colors: helium314.keyboard.latin.common.Colors) {
        for (i in 0 until grid.childCount) {
            val tile = grid.getChildAt(i)
            val iconView = tile.findViewById<ImageButton>(R.id.menu_tile_icon)
            iconView.setBackgroundResource(android.R.color.transparent)

            val labelView = tile.findViewById<TextView>(R.id.menu_tile_label)
            val keyboardTextColor = colors.get(helium314.keyboard.latin.common.ColorType.KEY_TEXT)
            labelView.setTextColor(keyboardTextColor)
            KeyboardTypeface.applyToTextView(labelView)

            val drawable = iconView.drawable
            if (drawable != null) {
                applyKeyTextIcon(iconView, drawable, keyboardTextColor)
            }
        }
    }

    private fun applyKeyTextIcon(iconView: ImageButton, icon: Drawable?, keyboardTextColor: Int) {
        if (icon == null) {
            iconView.setImageDrawable(null)
            return
        }
        val tintedIcon = DrawableCompat.wrap(icon.mutate())
        DrawableCompat.setTint(tintedIcon, keyboardTextColor)
        DrawableCompat.setTintMode(tintedIcon, PorterDuff.Mode.SRC_IN)
        iconView.clearColorFilter()
        iconView.imageTintMode = PorterDuff.Mode.SRC_IN
        iconView.imageTintList = ColorStateList.valueOf(keyboardTextColor)
        iconView.setImageDrawable(tintedIcon)
    }
}
