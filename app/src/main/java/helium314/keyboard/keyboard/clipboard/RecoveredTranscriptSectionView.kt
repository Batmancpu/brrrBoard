// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.latin.FooterIndicatorPolicy
import helium314.keyboard.latin.R
import helium314.keyboard.latin.voice.RecoveredTranscript
import java.util.Date

/** A separate, fixed-height rail: it never participates in ordinary clip swipes or pinning. */
class RecoveredTranscriptSectionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    private val title = TextView(context)
    private val row = LinearLayout(context)
    private val scroller = HorizontalScrollView(context)

    init {
        orientation = VERTICAL
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        title.apply {
            text = context.getString(R.string.board_recovered_transcripts)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(15), 0, dp(12), dp(4))
            gravity = Gravity.CENTER_VERTICAL
            KeyboardTypeface.applyToTextView(this)
        }
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, dp(23)))
        scroller.apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            setPadding(dp(10), 0, dp(10), dp(4))
        }
        row.orientation = HORIZONTAL
        scroller.addView(row, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        addView(scroller, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun bind(items: List<RecoveredTranscript>, onInsert: (Long) -> Unit,
            onRemove: (Long) -> Unit) {
        visibility = if (items.isEmpty()) GONE else VISIBLE
        row.removeAllViews()
        if (items.isEmpty()) return
        val dark = KeyboardTheme.isDarkThemeActive(context)
        val ink = if (dark) Color.WHITE else 0xFF202126.toInt()
        val accent = FooterIndicatorPolicy.voiceTint(dark, offline = true)
        title.setTextColor(accent)
        items.forEach { item ->
            val card = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), dp(5), dp(8), dp(6))
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(if (dark) 0xD92D3039.toInt() else 0xE8F2F3F7.toInt())
                    setStroke(dp(1), ColorUtils.setAlphaComponent(accent, if (dark) 190 else 145))
                }
                isClickable = true
                isFocusable = true
                contentDescription = context.getString(R.string.board_recovered_transcript_insert_a11y,
                    formattedTime(item.timestamp)) + ". " + item.text.take(120)
                setOnClickListener { onInsert(item.id) }
            }
            val header = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val time = TextView(context).apply {
                text = formattedTime(item.timestamp)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextColor(accent)
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
            }
            header.addView(time, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            val remove = TextView(context).apply {
                text = "×"
                gravity = Gravity.CENTER
                setTextColor(ColorUtils.setAlphaComponent(ink, 210))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                contentDescription = context.getString(R.string.board_recovered_transcript_remove_a11y)
                isClickable = true
                isFocusable = true
                minWidth = dp(32)
                minHeight = dp(30)
                setOnClickListener { onRemove(item.id) }
            }
            header.addView(remove, LayoutParams(dp(32), dp(30)))
            card.addView(header, LayoutParams(LayoutParams.MATCH_PARENT, dp(30)))
            val preview = TextView(context).apply {
                text = item.text.take(180)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(ink)
                KeyboardTypeface.applyToTextView(this)
            }
            card.addView(preview, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            row.addView(card, LayoutParams(dp(186), LayoutParams.MATCH_PARENT).apply {
                marginEnd = dp(8)
            })
        }
    }

    private fun formattedTime(timestamp: Long): String {
        val date = Date(timestamp)
        return DateFormat.getDateFormat(context).format(date) + " · " +
            DateFormat.getTimeFormat(context).format(date)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
