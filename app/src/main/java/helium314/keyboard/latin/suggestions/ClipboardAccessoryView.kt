// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.animation.ValueAnimator
import android.os.Build
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.animation.DecelerateInterpolator
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
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
    private var chromeRevealed = false
    private var separatorInk = Color.TRANSPARENT
    private val separatorPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
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
    val sideWidth: Int get() = dp(ClipboardAccessoryPolicy.CHROME_SLOT_DP) + paddingLeft + paddingRight

    fun configure(colors: Colors, content: CharSequence, imagePreview: Boolean) {
        fullLabel = content
        image = imagePreview
        val ink = colors.get(ColorType.SUGGESTION_AUTO_CORRECT)
        val lightInk = ColorUtils.calculateLuminance(ColorUtils.setAlphaComponent(ink, 255)) >= .35
        val pressed = if (lightInk) 0x18FFFFFF else 0x10000000
        separatorInk = ColorUtils.setAlphaComponent(ink, 35)
        paste.background = feedback(pressed)
        chrome.background = feedback(pressed)
        paste.setPadding(0, 0, 0, 0)
        chrome.setPaddingRelative(dp(11), 0, dp(11), 0)
        label.setTextColor(ink)
        chrome.setTextColor(ink)
        val medium = Typeface.create("sans-serif", Typeface.NORMAL)
        KeyboardTypeface.applyToTextView(label, content, medium)
        KeyboardTypeface.applyToTextView(chrome, chrome.text, medium)
        paste.contentDescription = resources.getString(R.string.board_accessory_paste_preview, content)
        label.text = content
        label.gravity = Gravity.CENTER
        label.textSize = 16f
        chrome.text = ""
        chrome.compoundDrawablePadding = 0
        gap.isVisible = false
        clipChildren = true
        clipToPadding = true
        label.setPaddingRelative(dp(if (image) 6 else 0), 0, 0, 0)
        preview.clipToOutline = true
        preview.background = GradientDrawable().apply {
            cornerRadius = dp(4).toFloat()
            setColor(Color.TRANSPARENT)
        }
    }

    private fun feedback(pressed: Int): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(pressed), null, GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(Color.WHITE)
        })

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        super.dispatchDraw(canvas)
        // Only a functional boundary between an action and the content.
        // Never draw decorative lines at either outer edge of the strip.
        val x = ClipboardAccessoryPolicy.chromeDividerX(
            !retired && chrome.isVisible, layoutDirection == View.LAYOUT_DIRECTION_RTL,
            chrome.left, chrome.right)
        if (x < 0) return
        separatorPaint.color = separatorInk
        separatorPaint.strokeWidth = resources.displayMetrics.density
        val cy = height / 2f
        val half = dp(11).toFloat()
        canvas.drawLine(x.toFloat(), cy - half, x.toFloat(), cy + half, separatorPaint)
    }

    fun setCompactMode(value: Boolean) {
        if (retired || compact == value) return
        compact = value
        val pasteParams = paste.layoutParams as LayoutParams
        pasteParams.width = 0
        pasteParams.weight = if (value) 0f else 1f
        paste.layoutParams = pasteParams
        paste.isVisible = !value
        val gapParams = gap.layoutParams as LayoutParams
        gapParams.width = 0
        gapParams.weight = if (value) 1f else 0f
        gap.layoutParams = gapParams
        gap.isVisible = value && chrome.isVisible
        val chromeParams = chrome.layoutParams as LayoutParams
        chromeParams.width = dp(ClipboardAccessoryPolicy.CHROME_SLOT_DP)
        chrome.layoutParams = chromeParams
        label.text = if (value) "" else fullLabel
        label.isVisible = !(value && image)
        label.gravity = Gravity.CENTER
        label.setPaddingRelative(dp(if (image) 6 else 0), 0, 0, 0)
        label.compoundDrawablePadding = if (value) 0 else dp(6)
        chrome.text = ""
        chrome.compoundDrawablePadding = 0
        chrome.setPaddingRelative(dp(11), 0, dp(11), 0)
        // The empty center has no click listener and lets prediction taps pass through.
    }

    /** Move only the accessory, leaving the keyboard and prediction surface stationary. */
    fun reveal() {
        if (retired || !chrome.isVisible || chromeRevealed) return
        chromeRevealed = true
        chrome.animate().cancel()
        val motion = Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()
        val direction = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) -1 else 1
        chrome.translationX = if (motion) direction * dp(34).toFloat() else 0f
        chrome.alpha = if (motion) 0f else 1f
        if (motion) chrome.animate().translationX(0f).alpha(1f)
            .setInterpolator(DecelerateInterpolator()).setDuration(210L).start()
        invalidate()
    }

    fun hideChrome() {
        val wasVisible = chrome.isVisible
        chrome.animate().cancel()
        chrome.isEnabled = false
        // Disable taps at the exact deadline, even if the icon is exiting.
        val motion = Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()
        if (wasVisible && chromeRevealed && !retired && motion && !compact) {
            val direction = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) -1 else 1
            chrome.animate().translationX(direction * dp(34).toFloat()).alpha(0f)
                .setDuration(160L).withEndAction {
                    if (!retired) finishChromeExit()
                }.start()
        } else finishChromeExit()
    }

    private fun finishChromeExit() {
        chrome.isVisible = false
        if (!compact) gap.isVisible = false
        onAccessoryChanged?.invoke()
        invalidate()
    }

    fun retire() {
        if (retired) return
        retired = true
        chrome.animate().cancel()
        animate().cancel()
        paste.isEnabled = false
        chrome.isEnabled = false
        visibility = GONE
        fullLabel = ""
        label.text = ""
        paste.contentDescription = null
        paste.setOnClickListener(null)
        paste.setOnLongClickListener(null)
        chrome.setOnClickListener(null)
        preview.setImageDrawable(null)
        onAccessoryChanged?.invoke()
    }


}
