// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.content.Context
import android.graphics.Canvas
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import kotlin.math.abs

/**
 * Ordinary TextView with an optional, short-lived visual effect in its own
 * slot. Text, accessibility labels, and clickable tags update before begin().
 * No outgoing word, transparent letters, or frame-by-frame text measurement.
 */
internal class MorphingSuggestionTextView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    TextView(context, attrs, defStyle) {

    private data class Motion(val plan: SuggestionGlyphPlan, val ink: TextPaint)

    private var motion: Motion? = null
    private var progress = 1f

    fun snapshot(): SuggestionGlyphFrame? {
        val label = text.toString()
        if (!canRender(label)) return null
        val running = motion
        if (running != null && running.plan.word == label) {
            return SuggestionGlyphFrame(label, FloatArray(label.length) { index ->
                SuggestionGlyphMotion.x(running.plan, index, progress)
            })
        }
        val ink = safePaint() ?: return null
        if (!canDrawByGlyph(label, ink)) return null
        return SuggestionGlyphFrame(label, positions(label, ink))
    }

    fun begin(previous: SuggestionGlyphFrame?): Boolean {
        val label = text.toString()
        if (previous == null || !canRender(label)) return false
        val ink = safePaint() ?: return false
        if (!canDrawByGlyph(label, ink)) return false
        val availableWidth = width - compoundPaddingLeft - compoundPaddingRight
        if (ink.measureText(label) >= availableWidth - 1f) return false
        val plan = SuggestionGlyphMotion.plan(previous, label, positions(label, ink),
            resources.displayMetrics.density * 8f) ?: return false

        motion = Motion(plan, ink)
        progress = 0f
        invalidate()
        return true
    }

    fun advance(fraction: Float) {
        if (motion == null) return
        progress = SuggestionGlyphMotion.eased(fraction)
        invalidate()
    }

    fun end() {
        if (motion == null) return
        motion = null
        progress = 1f
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw) end()
    }

    override fun onDraw(canvas: Canvas) {
        val running = motion
        if (running == null || !TextUtils.equals(text, running.plan.word) ||
            compoundDrawables.any { it != null }) {
            if (running != null) end()
            super.onDraw(canvas)
            return
        }

        val plan = running.plan
        val ink = running.ink
        ink.color = currentTextColor
        val middle = (compoundPaddingLeft + width - compoundPaddingRight) / 2f
        val baselineY = baseline.toFloat()
        val clip = canvas.save()
        canvas.clipRect(compoundPaddingLeft, 0, width - compoundPaddingRight, height)
        for (index in plan.word.indices) {
            val x = middle + SuggestionGlyphMotion.x(plan, index, progress)
            val scale = SuggestionGlyphMotion.scale(plan, index, progress)
            if (scale == 1f) {
                canvas.drawText(plan.word, index, index + 1, x, baselineY, ink)
            } else {
                val save = canvas.save()
                canvas.scale(scale, scale, x, baselineY)
                canvas.drawText(plan.word, index, index + 1, x, baselineY, ink)
                canvas.restoreToCount(save)
            }
        }
        canvas.restoreToCount(clip)
    }

    private fun canRender(label: String): Boolean =
        width > 0 && layoutDirection != View.LAYOUT_DIRECTION_RTL &&
            SuggestionGlyphMotion.supported(label) &&
            compoundDrawables.none { it != null }

    /** Native text shaping wins whenever separate glyph painting could shift ink. */
    private fun canDrawByGlyph(label: String, ink: TextPaint): Boolean {
        val lower = label.lowercase()
        if (listOf("ff", "fi", "fl", "st").any { it in lower }) return false
        var singleWidth = 0f
        for (character in label) singleWidth += ink.measureText(character.toString())
        return abs(singleWidth - ink.measureText(label)) <= .5f
    }

    private fun safePaint(): TextPaint? {
        val ink = TextPaint(paint)
        val styled = text as? Spanned ?: return ink
        for (span in styled.getSpans(0, styled.length, CharacterStyle::class.java)) {
            if (styled.getSpanStart(span) != 0 || styled.getSpanEnd(span) != styled.length)
                return null
            when (span) {
                is StyleSpan -> ink.typeface = android.graphics.Typeface.create(ink.typeface, span.style)
                is UnderlineSpan -> ink.isUnderlineText = true
                else -> return null
            }
        }
        return ink
    }

    /** Positions are relative to the slot center and calculated once per word. */
    private fun positions(word: String, ink: TextPaint): FloatArray {
        val advances = FloatArray(word.length)
        ink.getTextWidths(word, advances)
        val result = FloatArray(word.length)
        var cursor = -ink.measureText(word) / 2f
        for (index in word.indices) {
            result[index] = cursor
            cursor += advances[index]
        }
        return result
    }
}
