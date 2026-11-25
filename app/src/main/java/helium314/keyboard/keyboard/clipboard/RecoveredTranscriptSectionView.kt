// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.latin.R
import helium314.keyboard.latin.voice.RecoveredTranscript

/** A single temporary card above the regular clipboard grid, not a saved/pinnable clip. */
class RecoveredTranscriptSectionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setPadding(dp(10), dp(4), dp(10), dp(6))
    }

    fun bind(items: List<RecoveredTranscript>, onInsert: (Long) -> Unit,
            onRemove: (Long) -> Unit) {
        removeAllViews()
        val item = items.firstOrNull()
        visibility = if (item == null) GONE else VISIBLE
        if (item == null) return
        val dark = KeyboardTheme.isDarkThemeActive(context)
        val ink = if (dark) Color.WHITE else 0xFF202126.toInt()
        val accent = if (dark) 0xFFFF8A8E.toInt() else 0xFFB92D39.toInt()
        val card = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(if (dark) 0xD92D3039.toInt() else 0xE8F2F3F7.toInt())
            }
            isClickable = true
            isFocusable = true
            contentDescription = context.getString(R.string.board_voice_recovery_insert) + ". " + item.text.take(120)
            setOnClickListener { onInsert(item.id) }
            setOnLongClickListener { onRemove(item.id); true }
        }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(43), dp(7))
        }
        body.addView(TextView(context).apply {
            text = context.getString(R.string.board_voice_recovery_title)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(accent)
            KeyboardTypeface.applyToTextView(this)
        })
        body.addView(TextView(context).apply {
            text = item.text.take(320)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(ink)
            KeyboardTypeface.applyToTextView(this)
        })
        card.addView(body, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val mic = ImageView(context).apply {
            setImageResource(R.drawable.ic_board_mic)
            imageTintList = ColorStateList.valueOf(accent)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                gradientType = GradientDrawable.RADIAL_GRADIENT
                colors = intArrayOf(if (dark) 0x60FF424D else 0x35FF424D, Color.TRANSPARENT)
                gradientRadius = dp(18).toFloat()
            }
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        card.addView(mic, LayoutParams(dp(36), dp(36), Gravity.TOP or Gravity.RIGHT).apply {
            topMargin = dp(2)
            rightMargin = dp(4)
        })
        addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
