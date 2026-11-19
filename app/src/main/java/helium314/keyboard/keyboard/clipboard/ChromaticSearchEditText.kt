// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.widget.EditText
import androidx.core.graphics.ColorUtils
import helium314.keyboard.keyboard.KeyboardTheme

/** No selection/background color is part of this policy: only opaque glyph ink. */
internal object ChromaticSearchInkPolicy {
    fun active(query: CharSequence?, hasMatches: Boolean): Boolean =
        hasMatches && !query.isNullOrBlank()

    fun colors(dark: Boolean): IntArray = if (dark) intArrayOf(
        0xFFFF69CA.toInt(), 0xFFC58AFF.toInt(), 0xFF62D9F8.toInt(),
        0xFF75ECC5.toInt(), 0xFFFFC578.toInt(), 0xFFFF69CA.toInt())
    else intArrayOf(
        0xFFB51C7E.toInt(), 0xFF713BBD.toInt(), 0xFF087F9F.toInt(),
        0xFF167F62.toInt(), 0xFF995A14.toInt(), 0xFFB51C7E.toInt())
}

/**
 * Animates the INK of the clipboard query, never a colored selection-shaped
 * rectangle behind it. The real EditText owns caret, selection, composing
 * spans, accessibility, and swipe-input editing throughout the effect.
 */
class ChromaticSearchEditText(context: Context, attrs: AttributeSet?) : EditText(context, attrs) {
    private val matrix = Matrix()
    private var shader: LinearGradient? = null
    private var shaderWidth = -1f
    private var shaderDark = false
    private var phase = 0f
    private var animator: ValueAnimator? = null
    private var matchActive = false

    fun showMatchedInk(active: Boolean) {
        // The query's chromatic ink is a SEARCH affordance, not a match
        // highlight. It also stays alive for queries with zero results.
        val shouldShow = ChromaticSearchInkPolicy.active(text, active)
        if (matchActive == shouldShow) return
        matchActive = shouldShow
        if (shouldShow) {
            updateAmbientGlow()
            startInk()
        } else {
            stopInk()
            setShadowLayer(0f, 0f, 0f, 0)
        }
        invalidate()
    }

    fun refreshInkTheme() {
        shader = null
        shaderWidth = -1f
        if (matchActive) {
            updateAmbientGlow()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!matchActive || text.isNullOrEmpty()) {
            super.onDraw(canvas)
            return
        }
        val dark = KeyboardTheme.isDarkThemeActive(context)
        val visibleWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1).toFloat()
        val measuredWidth = paint.measureText(text.toString()).coerceIn(1f, visibleWidth)
        if (shader == null || shaderDark != dark || shaderWidth != measuredWidth) {
            shaderDark = dark
            shaderWidth = measuredWidth
            shader = LinearGradient(-measuredWidth * 0.3f, 0f,
                measuredWidth * 1.3f, 0f, ChromaticSearchInkPolicy.colors(dark),
                null, Shader.TileMode.MIRROR)
        }
        val ink = shader
        if (ink == null) {
            super.onDraw(canvas)
            return
        }
        matrix.setTranslate(phase * measuredWidth * 0.60f, 0f)
        ink.setLocalMatrix(matrix)
        val previousShader = paint.shader
        try {
            paint.shader = ink
            super.onDraw(canvas)
        } finally {
            // Never leak the gradient into the normal text/hint render path.
            paint.shader = previousShader
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (matchActive) startInk()
    }

    override fun onDetachedFromWindow() {
        stopInk()
        super.onDetachedFromWindow()
    }

    private fun startInk() {
        if (animator != null || !isAttachedToWindow ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled())) return
        animator = ValueAnimator.ofFloat(-1f, 1f).apply {
            duration = 2_600L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                phase = it.animatedValue as Float
                updateAmbientGlow()
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    private fun stopInk() {
        animator?.cancel()
        animator = null
        phase = 0f
    }

    private fun updateAmbientGlow() {
        val dark = KeyboardTheme.isDarkThemeActive(context)
        val pink = if (dark) 0x5BD167EC else 0x4DB336A0
        val aqua = if (dark) 0x5B40C9D8 else 0x4D2589AE
        val color = ColorUtils.blendARGB(pink, aqua, (phase + 1f) * .5f)
        // A small, soft color spill around the letters influences the capsule
        // surface without creating a colored selection rectangle behind them.
        setShadowLayer(resources.displayMetrics.density * 3.8f, 0f, 0f, color)
    }
}
