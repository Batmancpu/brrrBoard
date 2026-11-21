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
 * Ordinary TextView with a finite, interruptible replacement transition.
 * Its actual text, TalkBack label and click tag always describe the NEW word.
 * Shaped scripts use native TextView ink with a tiny presentation-only settle.
 */
internal class MorphingSuggestionTextView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    TextView(context, attrs, defStyle) {

    private data class Motion(val word: String, val plan: SuggestionGlyphPlan?,
                              val ink: TextPaint?)

    private var motion: Motion? = null
    private var progress = 1f

    fun snapshot(): SuggestionGlyphFrame? {
        val label = text.toString()
        if (width <= 0 || label.isBlank()) return null
        val running = motion
        if (running?.plan != null && running.word == label) {
            return SuggestionGlyphFrame(label, FloatArray(label.length) { index ->
                SuggestionGlyphMotion.x(running.plan, index, progress)
            })
        }
        val ink = if (canRender(label)) safePaint() else null
        if (ink == null || !canDrawByGlyph(label, ink))
            return SuggestionGlyphFrame(label, FloatArray(0))
        return SuggestionGlyphFrame(label, positions(label, ink))
    }

    fun begin(previous: SuggestionGlyphFrame?): Boolean {
        val label = text.toString()
        if (previous?.word == label || label.isBlank() || width <= 0) return false
        val ink = if (canRender(label)) safePaint() else null
        val availableWidth = width - compoundPaddingLeft - compoundPaddingRight
        val canUseGlyphs = ink != null && previous != null &&
            previous.positions.size == previous.word.length &&
            canDrawByGlyph(label, ink) && ink.measureText(label) < availableWidth - 1f
        val plan = if (canUseGlyphs) SuggestionGlyphMotion.plan(
            previous, label, positions(label, ink!!), resources.displayMetrics.density * 10f)
        else null
        // Always settle a changed label. Complex fonts, ligatures, Emoji, RTL
        // and unusually wide suggestions retain native text shaping instead.
        motion = Motion(label, plan, if (plan == null) null else ink)
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
        // Chip labels use WRAP_CONTENT, so each changed word can resize its
        // TextView during the same transition. Its ink coordinates are relative
        // to the slot center; a positive resize must not cancel glyph motion.
        if (w <= 0 || h <= 0) end() else invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val running = motion
        if (running == null || !TextUtils.equals(text, running.word) ||
            compoundDrawables.any { it != null }) {
            if (running != null) end()
            super.onDraw(canvas)
            return
        }

        val plan = running.plan
        val ink = running.ink
        if (plan == null || ink == null) {
            // Canvas-only movement: native TextView continues shaping ligatures,
            // RTL and Emoji; no text alpha, click-target or accessibility delay.
            val save = canvas.save()
            canvas.translate(0f, (1f - progress) * resources.displayMetrics.density * 2.5f)
            super.onDraw(canvas)
            canvas.restoreToCount(save)
            return
        }
        ink.color = currentTextColor
        val middle = (compoundPaddingLeft + width - compoundPaddingRight) / 2f
        val baselineY = baseline.toFloat()
        val clip = canvas.save()
        canvas.clipRect(compoundPaddingLeft, 0, width - compoundPaddingRight, height)
        val opaqueAlpha = ink.alpha
        for (index in plan.outgoingWord.indices) {
            val fading = SuggestionGlyphMotion.outgoingAlpha(plan, index, progress)
            if (fading <= 0f) continue
            ink.alpha = (opaqueAlpha * fading).toInt().coerceIn(0, 255)
            canvas.drawText(plan.outgoingWord, index, index + 1,
                middle + plan.outgoingPositions[index], baselineY, ink)
        }
        ink.alpha = opaqueAlpha
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
        ink.alpha = opaqueAlpha
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
