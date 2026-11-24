// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.roundToInt

/**
 * Sample-aligned, non-destructive enhancement. The speech scores are evidence,
 * not calibrated confidence or speaker identity. No sample is ever removed.
 * Both score tracks must be finite; the caller falls back to original PCM otherwise.
 */
internal object ProtectedVoiceAudio {
    const val HOP = 512 // 32 ms at 16 kHz
    const val CONTEXT = 64
    private const val PRE = 10 // 320 ms
    private const val POST = 8 // 256 ms
    private const val STRONG = .55f
    private const val WEAK = .25f

    fun sample(pcm: ByteArray, index: Int): Int =
        ((pcm[index * 2 + 1].toInt() shl 8) or (pcm[index * 2].toInt() and 255)).toShort().toInt()

    fun frames(pcm: ByteArray): Int = (pcm.size / 2 + HOP - 1) / HOP

    /** Reuse a single model session with reset between the raw and clean passes. */
    fun scores(pcm: ByteArray, infer: (FloatArray) -> Float): FloatArray {
        require(pcm.size % 2 == 0)
        val window = FloatArray(HOP + CONTEXT)
        val probabilities = FloatArray(frames(pcm))
        try {
            for (f in probabilities.indices) {
                for (j in window.indices) {
                    val i = f * HOP + j - CONTEXT
                    window[j] = if (i in 0 until pcm.size / 2) sample(pcm, i) / 32768f else 0f
                }
                probabilities[f] = infer(window)
                require(probabilities[f].isFinite() && probabilities[f] in 0f..1f)
            }
            return probabilities
        } finally { window.fill(0f) }
    }

    fun protect(raw: ByteArray, clean: ByteArray, rawScore: FloatArray, cleanScore: FloatArray): ByteArray {
        require(raw.size == clean.size && raw.size % 2 == 0)
        val n = frames(raw)
        require(rawScore.size == n && cleanScore.size == n)
        require(rawScore.all { it.isFinite() && it in 0f..1f } &&
            cleanScore.all { it.isFinite() && it in 0f..1f })
        val speech = BooleanArray(n)
        // Both paths may rescue speech. Dilation protects quiet phonemes and
        // speech onset/offset; it does not declare every padded frame speech.
        for (f in 0 until n) {
            if (maxOf(rawScore[f], cleanScore[f]) >= STRONG)
                for (j in maxOf(0, f - PRE)..minOf(n - 1, f + POST)) speech[j] = true
        }
        val mix = FloatArray(n)
        var speechEnergy = 0.0
        var speechSamples = 0
        var peak = 0
        for (f in 0 until n) {
            val start = f * HOP
            val end = minOf(start + HOP, raw.size / 2)
            var a = 0.0
            var b = 0.0
            for (i in start until end) {
                val r = sample(raw, i).toDouble()
                val c = sample(clean, i).toDouble()
                a += r * r
                b += c * c
            }
            val ratio = if (a > 0.0) b / a else 1.0
            val rawEvidence = rawScore[f]
            val cleanEvidence = cleanScore[f]
            mix[f] = when {
                // Neural disagreement plus attenuation, not loudness alone.
                rawEvidence >= STRONG && cleanEvidence < WEAK && ratio < .25 -> 1f
                rawEvidence >= STRONG && rawEvidence - cleanEvidence >= .20f && ratio < .5 -> .65f
                speech[f] && rawEvidence >= WEAK && cleanEvidence < WEAK && ratio < .25 -> .60f
                // Never sacrifice weak phonemes between recognized syllables.
                cleanEvidence >= STRONG && rawEvidence < WEAK -> 0f
                speech[f] && ratio < .10 -> .25f
                speech[f] -> .08f
                else -> 0f
            }
            // Measure only confidently speech-like frames for gain control.
            if (maxOf(rawEvidence, cleanEvidence) >= STRONG) {
                for (i in start until end) {
                    val v = sample(clean, i) * (1f - mix[f]) + sample(raw, i) * mix[f]
                    speechEnergy += v.toDouble() * v
                    speechSamples++
                }
            }
            for (i in start until end) peak = maxOf(peak, abs(sample(raw,i)), abs(sample(clean,i)))
        }
        // Modest speech-aware, batch-constant gain. Never amplify a noise-only
        // batch; no gain on near-silent signals; no peak clipping.
        val rms = if (speechSamples > 0) sqrt(speechEnergy / speechSamples) else 0.0
        val gain = if (speechSamples >= HOP * 3 && rms >= 32.0 && peak > 0)
            minOf(1.6, maxOf(1.0, 1500.0 / rms), maxOf(1.0, 31000.0 / peak)).toFloat()
        else 1f
        val result = ByteArray(raw.size)
        for (i in 0 until raw.size / 2) {
            val f = i / HOP
            // Interpolate between frame centres, avoiding clicks at boundaries.
            val position = (i % HOP) / HOP.toFloat()
            val left = if (position < .5f) mix[maxOf(0, f - 1)] else mix[f]
            val right = if (position < .5f) mix[f] else mix[minOf(n - 1, f + 1)]
            val t = if (position < .5f) position + .5f else position - .5f
            val alpha = left + (right - left) * t
            val gainLeft = if (speech[if (position < .5f) maxOf(0, f - 1) else f]) gain else 1f
            val gainRight = if (speech[if (position < .5f) f else minOf(n - 1, f + 1)]) gain else 1f
            val localGain = gainLeft + (gainRight - gainLeft) * t
            val value = ((sample(clean,i) * (1f - alpha) + sample(raw,i) * alpha) * localGain)
                .roundToInt().coerceIn(-32768,32767)
            result[2*i] = value.toByte()
            result[2*i+1] = (value shr 8).toByte()
        }
        return result
    }
}
