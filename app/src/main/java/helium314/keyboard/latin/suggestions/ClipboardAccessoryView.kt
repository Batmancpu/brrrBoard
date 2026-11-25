// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.animation.ValueAnimator
import android.os.Build
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ScaleDrawable
import android.view.animation.DecelerateInterpolator
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors

/** Pure presentation; the manager retains all original clipboard and editor receipts. */
class ClipboardAccessoryView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val paste by lazy { findViewById<View>(R.id.clipboard_suggestion_paste) }
    private val label by lazy { findViewById<TextView>(R.id.clipboard_suggestion_text) }
    private val preview by lazy { findViewById<ImageView>(R.id.clipboard_suggestion_preview) }
    private val chrome by lazy { findViewById<TextView>(R.id.clipboard_suggestion_chrome) }
    private val close by lazy { findViewById<ImageButton>(R.id.clipboard_suggestion_close) }
    private val gap by lazy { findViewById<View>(R.id.clipboard_suggestion_gap) }
    private var fullLabel: CharSequence = ""
    private var image = false
    var retired = false
        private set
    var compact = false
        private set
    var onAccessoryChanged: (() -> Unit)? = null
    var prefersProminent = true
    var isVerificationCode = false
    private var intentEmphasized = false
    private var capsuleMotion: ValueAnimator? = null
    private val capsuleFills = ArrayList<ScaleDrawable>(3)
    val hasLiveChrome: Boolean get() = !retired && chrome.isVisible && chrome.isEnabled

    fun updateIntentEmphasis(value: Boolean) {
        if (retired || intentEmphasized == value || isVerificationCode) return
        intentEmphasized = value
        // Keep geometry and word tap targets stable; use a restrained state highlight.
        paste.isSelected = value
        paste.alpha = if (value) 1f else .94f
        if (value && compact) reveal()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    val sideWidth: Int get() = dp(42)

    fun configure(colors: Colors, content: CharSequence, imagePreview: Boolean) {
        fullLabel = content
        image = imagePreview
        val ink = colors.get(ColorType.SUGGESTION_AUTO_CORRECT)
        val lightInk = ColorUtils.calculateLuminance(ColorUtils.setAlphaComponent(ink, 255)) >= .35
        val fill = if (lightInk) 0x24FFFFFF else 0xA6FFFFFF.toInt()
        val pressed = if (lightInk) 0x28FFFFFF else 0x19000000
        val edge = if (lightInk) 0x1AFFFFFF else 0x82FFFFFF.toInt()
        capsuleFills.clear()
        paste.background = capsule(fill, pressed, edge)
        chrome.background = capsule(fill, pressed, edge)
        close.background = capsule(Color.TRANSPARENT, pressed, Color.TRANSPARENT)
        paste.setPadding(0, 0, 0, 0)
        chrome.setPaddingRelative(dp(9), 0, dp(10), 0)
        close.setPadding(dp(9), dp(9), dp(9), dp(9))
        label.setTextColor(ink)
        chrome.setTextColor(ink)
        close.setColorFilter(ink)
        close.imageAlpha = 166
        val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        KeyboardTypeface.applyToTextView(label, content, medium)
        KeyboardTypeface.applyToTextView(chrome, chrome.text, medium)
        paste.contentDescription = resources.getString(R.string.board_accessory_paste_preview, content)
        label.text = content
        if (image) label.setPaddingRelative(dp(4), 0, dp(8), 0)
        preview.clipToOutline = true
        preview.background = GradientDrawable().apply {
            cornerRadius = dp(6).toFloat()
            setColor(Color.TRANSPARENT)
        }
    }

    private fun capsule(fill: Int, pressed: Int, edge: Int): RippleDrawable {
        fun shape(color: Int, stroke: Boolean) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(100).toFloat()
            setColor(color)
            if (stroke && edge != Color.TRANSPARENT) setStroke(dp(1).coerceAtLeast(1), edge)
        }
        val vertical = dp(4)
        val scaled = ScaleDrawable(InsetDrawable(shape(fill, true), 0, vertical, 0, vertical),
            Gravity.CENTER, 1f, 0f).apply { level = 10000 }
        capsuleFills.add(scaled)
        return RippleDrawable(ColorStateList.valueOf(pressed), scaled,
            InsetDrawable(shape(Color.WHITE, false), 0, vertical, 0, vertical)).apply { level = 10000 }
    }

    fun setCompactMode(value: Boolean) {
        if (retired || compact == value) return
        compact = value
        val pasteParams = paste.layoutParams as LayoutParams
        pasteParams.width = if (value) dp(38) else 0
        pasteParams.weight = if (value) 0f else 1f
        paste.layoutParams = pasteParams
        val gapParams = gap.layoutParams as LayoutParams
        gapParams.width = if (value) 0 else dp(6)
        gapParams.weight = if (value) 1f else 0f
        gap.layoutParams = gapParams
        gap.isVisible = value || chrome.isVisible
        val chromeParams = chrome.layoutParams as LayoutParams
        chromeParams.width = if (value) dp(38) else LayoutParams.WRAP_CONTENT
        chrome.layoutParams = chromeParams
        label.text = if (value) "" else fullLabel
        label.isVisible = !(value && image)
        label.gravity = if (value) Gravity.CENTER else Gravity.CENTER_VERTICAL or Gravity.START
        label.setPaddingRelative(dp(if (value) 10 else if (image) 4 else 10), 0, dp(if (value) 10 else 8), 0)
        label.compoundDrawablePadding = if (value) 0 else dp(6)
        chrome.text = if (value) "" else resources.getString(R.string.board_chrome_label)
        chrome.compoundDrawablePadding = if (value) 0 else dp(5)
        chrome.setPaddingRelative(dp(if (value) 10 else 9), 0, dp(10), 0)
        close.isVisible = !value
        // The empty center has no click listener and lets prediction taps pass through.
    }

    /** Morph capsule fills inside their final bounds. No per-frame layout or blur recreation. */
    fun reveal() {
        animate().cancel()
        capsuleMotion?.cancel()
        if (retired || (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled())) {
            capsuleFills.forEach { it.level = 10000 }
            alpha = 1f
            return
        }
        alpha = .84f
        animate().alpha(1f).setDuration(140L).start()
        capsuleMotion = ValueAnimator.ofInt(7900,10000).apply {
            duration = 150L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                if (!retired) capsuleFills.forEach { it.level = animation.animatedValue as Int }
            }
            start()
        }
    }

    fun hideChrome() {
        val wasVisible = chrome.isVisible
        chrome.isEnabled = false
        chrome.isVisible = false
        if (!compact) gap.isVisible = false
        onAccessoryChanged?.invoke()
        // Chrome itself vanishes at the deadline; only the surviving capsule morphs.
        if (wasVisible && !retired) reveal()
    }

    fun retire() {
        if (retired) return
        retired = true
        capsuleMotion?.cancel()
        capsuleMotion = null
        animate().cancel()
        paste.isEnabled = false
        chrome.isEnabled = false
        close.isEnabled = false
        visibility = GONE
        fullLabel = ""
        label.text = ""
        paste.contentDescription = null
        paste.setOnClickListener(null)
        paste.setOnLongClickListener(null)
        chrome.setOnClickListener(null)
        close.setOnClickListener(null)
        preview.setImageDrawable(null)
        onAccessoryChanged?.invoke()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!retired && !compact && chrome.isVisible) {
            val narrow = MeasureSpec.getSize(widthMeasureSpec) < dp(250)
            val target = if (narrow) "" else resources.getString(R.string.board_chrome_label)
            if (chrome.text.toString() != target) {
                chrome.text = target
                chrome.compoundDrawablePadding = if (narrow) 0 else dp(5)
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
