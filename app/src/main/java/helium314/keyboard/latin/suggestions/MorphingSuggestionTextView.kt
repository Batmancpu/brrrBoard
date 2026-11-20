// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.content.Context
import android.graphics.Canvas
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView

/**
 * Draws each suggestion character exactly once in its own unchanged TextView/hitbox.
 * Replacing the word or an interrupted morph never creates duplicate TextViews,
 * overlays, stale click targets, or a frame-by-frame layout request.
 */
internal class MorphingSuggestionTextView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    TextView(context, attrs, defStyle) {

    private var tracks: List<SuggestionGlyphTrack>? = null
    private var fraction = 1f
    private var framePaint: TextPaint? = null
    private var trackWidths = FloatArray(0)

    fun snapshot(): List<SuggestionGlyphSnapshot> {
        val running = tracks
        if (running != null) return running.map { track ->
            val eased = SuggestionGlyphMotion.progress(track, fraction)
            SuggestionGlyphSnapshot(track.character,
                SuggestionGlyphMotion.interpolate(track.fromX, track.toX, eased),
                SuggestionGlyphMotion.interpolate(track.fromOpacity, track.toOpacity, eased),
                SuggestionGlyphMotion.interpolate(track.fromScale, track.toScale, eased))
        }
        val value = text.toString()
        if (layoutDirection == View.LAYOUT_DIRECTION_RTL ||
            !SuggestionGlyphMotion.supported(value)) return emptyList()
        val drawing = styledPaint()
        val offsets = glyphOffsets(value, drawing)
        return value.indices.map { index ->
            SuggestionGlyphSnapshot(value[index], offsets[index])
        }
    }

    fun begin(previous: List<SuggestionGlyphSnapshot>, newValue: String): Boolean {
        if (layoutDirection == View.LAYOUT_DIRECTION_RTL ||
            previous.isEmpty() || !SuggestionGlyphMotion.supported(newValue) ||
            previous.any { it.character !in ' '..'~' }) return false
        val styled = text as? Spanned
        if (styled != null && styled.getSpans(0, styled.length, CharacterStyle::class.java)
                .any { styled.getSpanStart(it) != 0 || styled.getSpanEnd(it) != styled.length }) {
            // Preserve mixed styling and host-provided presentation through TextView.
            return false
        }
        val drawing = styledPaint()
        val offsets = glyphOffsets(newValue, drawing)
        val next = SuggestionGlyphMotion.plan(previous, newValue, offsets)
        if (next.isEmpty()) return false
        framePaint = drawing
        tracks = next
        trackWidths = FloatArray(next.size) { drawing.measureText(next[it].ink) }
        fraction = 0f
        invalidate()
        return true
    }

    fun advance(progress: Float) {
        fraction = progress.coerceIn(0f, 1f)
        invalidate()
    }

    fun end() {
        tracks = null
        framePaint = null
        trackWidths = FloatArray(0)
        fraction = 1f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val current = tracks
        val drawing = framePaint
        if (current == null || drawing == null) {
            super.onDraw(canvas)
            return
        }
        val center = (paddingLeft + width - paddingRight) / 2f
        val baseline = this.baseline.toFloat()
        drawing.color = currentTextColor
        val opaqueAlpha = drawing.alpha
        for (index in current.indices) {
            val track = current[index]
            val eased = SuggestionGlyphMotion.progress(track, fraction)
            val opacity = SuggestionGlyphMotion.interpolate(track.fromOpacity, track.toOpacity, eased)
            if (opacity <= 0.005f) continue
            val x = center + SuggestionGlyphMotion.interpolate(track.fromX, track.toX, eased)
            val scale = SuggestionGlyphMotion.interpolate(track.fromScale, track.toScale, eased)
            drawing.alpha = (opaqueAlpha * opacity).toInt().coerceIn(0, 255)
            val save = canvas.save()
            if (scale != 1f) canvas.scale(scale, scale,
                x + trackWidths[index] / 2f, baseline)
            canvas.drawText(track.ink, x, baseline, drawing)
            canvas.restoreToCount(save)
        }
        drawing.alpha = opaqueAlpha
    }

    private fun styledPaint(): TextPaint {
        val drawing = TextPaint(paint)
        val chars = text
        if (chars is Spanned) {
            val all = chars.length
            var style = 0
            for (span in chars.getSpans(0, all, StyleSpan::class.java)) {
                if (chars.getSpanStart(span) == 0 && chars.getSpanEnd(span) == all)
                    style = style or span.style
            }
            if (style != 0) drawing.typeface = android.graphics.Typeface.create(drawing.typeface, style)
            for (span in chars.getSpans(0, all, UnderlineSpan::class.java)) {
                if (chars.getSpanStart(span) == 0 && chars.getSpanEnd(span) == all)
                    drawing.isUnderlineText = true
            }
        }
        drawing.isAntiAlias = true
        drawing.color = currentTextColor
        return drawing
    }

    private fun glyphOffsets(value: String, drawing: TextPaint): List<Float> {
        val widths = FloatArray(value.length)
        drawing.getTextWidths(value, widths)
        val halfWidth = drawing.measureText(value) / 2f
        var cursor = -halfWidth
        return value.indices.map { index ->
            val result = cursor
            cursor += widths[index]
            result
        }
    }
}
