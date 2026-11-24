// SPDX-License-Identifier: MIT
// Copyright (c) 2026 marmalade-stt contributors.
// Adapted from upstream blob 437b863cb8fb18fee5be78162030325f0dcfeda2.
package helium314.keyboard.latin.voice

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Whisper log-mel features for the smart-turn v3 model (#43): 8 s of 16 kHz
 * mono in, `[80][800]` out.
 *
 * A port of marmalade-stt-cli's `whisper_features.py`, which is pipecat's
 * `smart_turn/_whisper_features.py` (BSD-2-Clause, Copyright (c) 2024–2026
 * Daily, https://github.com/pipecat-ai/pipecat), itself a numpy transcription
 * of transformers' `WhisperFeatureExtractor` (Apache-2.0, Copyright The
 * HuggingFace Inc. team). Modified: pipecat cut it down to what smart-turn
 * needs, and we ported that to Kotlin (NOTICE.md).
 * The steps: the waveform normalised to zero mean and unit variance (in
 * float32, as the reference), centred 400-sample frames every 160 samples
 * (reflect padding) under a periodic Hann window, their power spectra, 80
 * Slaney mel bins, log10 floored at 1e-10 and then 8 decades below the
 * loudest bin, and (x + 4) / 4. That gives 801 frames; the last is dropped.
 * Computed in doubles like numpy; `WhisperFeaturesTest` holds it to the
 * Python output.
 *
 * Thread-safe: the tables are read-only and every call has its own buffers.
 */
object WhisperFeatures {
    const val SAMPLE_RATE = 16_000

    /** The model's fixed window: 8 s. */
    const val N_SAMPLES = 128_000
    const val N_MELS = 80
    const val N_FRAMES = 800

    private const val N_FFT = 400
    private const val HOP_LENGTH = 160
    private const val N_FREQS = N_FFT / 2 + 1
    private const val MEL_FLOOR = 1e-10
    private const val NORM_VARIANCE_EPS = 1e-7f

    /** Decades of power kept below the clip's loudest bin (80 dB). */
    private const val LOG_RANGE = 8.0

    /** Periodic Hann (torch.hann_window, not np.hanning's symmetric one). */
    private val window = DoubleArray(N_FFT) { 0.5 - 0.5 * cos(2.0 * PI * it / N_FFT) }

    private val fft = MixedRadixFft(N_FFT)

    /**
     * transformers' Slaney-normalised mel filterbank (0-8 kHz), stored sparse:
     * each triangle covers a few of the 201 bins, so filter m weighs
     * `power[melFirstBin[m] + j]` by `melWeights[m][j]`.
     */
    private val melFirstBin = IntArray(N_MELS)
    private val melWeights: Array<DoubleArray>

    init {
        val logStep = 27.0 / ln(6.4)
        fun hzToMel(f: Double) = if (f >= 1000.0) 15.0 + ln(f / 1000.0) * logStep else 3.0 * f / 200.0
        fun melToHz(m: Double) = if (m >= 15.0) 1000.0 * exp(ln(6.4) / 27.0 * (m - 15.0)) else 200.0 * m / 3.0

        // np.linspace(mel(0), mel(8000), 82): index * step, the last point exact.
        val melMax = hzToMel(SAMPLE_RATE / 2.0)
        val melStep = melMax / (N_MELS + 1)
        val filterFreqs = DoubleArray(N_MELS + 2) { melToHz(if (it == N_MELS + 1) melMax else it * melStep) }
        val binHz = (SAMPLE_RATE / 2).toDouble() / (N_FREQS - 1)
        melWeights = Array(N_MELS) { m ->
            val enorm = 2.0 / (filterFreqs[m + 2] - filterFreqs[m])
            val dense = DoubleArray(N_FREQS) { k ->
                val f = k * binHz
                val down = -(filterFreqs[m] - f) / (filterFreqs[m + 1] - filterFreqs[m])
                val up = (filterFreqs[m + 2] - f) / (filterFreqs[m + 2] - filterFreqs[m + 1])
                max(0.0, min(down, up)) * enorm
            }
            val first = dense.indexOfFirst { it != 0.0 }
            val last = dense.indexOfLast { it != 0.0 }
            melFirstBin[m] = first
            dense.copyOfRange(first, last + 1)
        }
    }

    /**
     * The model's view of [audio]: its last 8 s, zero-padded at the START when
     * shorter, so the end of the turn always sits at the end of the window
     * (the CLI's `SmartTurnDetector._window`).
     */
    fun lastWindow(audio: FloatArray): FloatArray =
        if (audio.size >= N_SAMPLES) {
            audio.copyOfRange(audio.size - N_SAMPLES, audio.size)
        } else {
            FloatArray(N_SAMPLES).also { audio.copyInto(it, destinationOffset = N_SAMPLES - audio.size) }
        }

    /**
     * Features for [audio], `[N_MELS][N_FRAMES]`. Hand over [lastWindow]'s
     * output: other lengths are zero-padded or cut at the END, as the reference
     * extractor does, which is not what the model wants for a turn.
     */
    fun logMel(audio: FloatArray): Array<FloatArray> {
        val x = audio.copyOf(N_SAMPLES)

        // Zero mean and unit variance on the waveform, in float32 like numpy.
        var sum = 0.0
        for (v in x) sum += v
        val mean = (sum / N_SAMPLES).toFloat()
        var squares = 0.0
        for (v in x) {
            val d = (v - mean).toDouble()
            squares += d * d
        }
        val std = sqrt((squares / N_SAMPLES).toFloat() + NORM_VARIANCE_EPS)

        // Centred frames: reflect-pad half a frame each side (the edge sample isn't repeated).
        val half = N_FFT / 2
        val padded = DoubleArray(N_SAMPLES + N_FFT)
        for (i in 0 until N_SAMPLES) padded[half + i] = ((x[i] - mean) / std).toDouble()
        for (i in 0 until half) {
            padded[half - 1 - i] = padded[half + 1 + i]
            padded[half + N_SAMPLES + i] = padded[half + N_SAMPLES - 2 - i]
        }

        val frame = DoubleArray(N_FFT)
        val re = DoubleArray(N_FFT)
        val im = DoubleArray(N_FFT)
        val scratch = DoubleArray(fft.scratchSize)
        val logSpec = DoubleArray(N_MELS * N_FRAMES) // [mel][frame]
        var top = Double.NEGATIVE_INFINITY
        // The reference computes one more frame and drops it before taking the maximum.
        for (t in 0 until N_FRAMES) {
            val start = t * HOP_LENGTH
            for (n in 0 until N_FFT) frame[n] = padded[start + n] * window[n]
            fft.transform(frame, re, im, scratch)
            for (m in 0 until N_MELS) {
                val weights = melWeights[m]
                val first = melFirstBin[m]
                var energy = 0.0
                for (j in weights.indices) {
                    val k = first + j
                    energy += weights[j] * (re[k] * re[k] + im[k] * im[k])
                }
                val l = log10(max(energy, MEL_FLOOR))
                logSpec[m * N_FRAMES + t] = l
                if (l > top) top = l
            }
        }
        val floor = top - LOG_RANGE
        val result = Array(N_MELS) { m ->
            FloatArray(N_FRAMES) { t -> ((max(logSpec[m * N_FRAMES + t], floor) + 4.0) / 4.0).toFloat() }
        }
        x.fill(0f)
        padded.fill(0.0)
        frame.fill(0.0)
        re.fill(0.0)
        im.fill(0.0)
        scratch.fill(0.0)
        logSpec.fill(0.0)
        return result
    }
}

/**
 * Forward DFT of [n] real samples by recursive mixed-radix Cooley-Tukey
 * (decimation in time, kissfft's scheme). Whisper's frames are 400 = 4·4·5·5
 * samples, and padding them to a power of two would change the spectrum.
 * Holds only read-only tables; callers bring the output and scratch arrays.
 */
private class MixedRadixFft(private val n: Int) {
    /** Radices, 4s first, then 2, 3, 5, … (kissfft's order). */
    private val factors: IntArray = buildList {
        var rest = n
        var p = 4
        while (rest > 1) {
            while (rest % p != 0) p = when (p) { 4 -> 2; 2 -> 3; else -> p + 2 }
            add(p)
            rest /= p
        }
    }.toIntArray()

    private val maxFactor = factors.max()

    /** Doubles of scratch [transform] needs. */
    val scratchSize = 2 * maxFactor

    /** e^(-2πik/n) for k in 0 until n. */
    private val twiddleRe = DoubleArray(n) { cos(2.0 * PI * it / n) }
    private val twiddleIm = DoubleArray(n) { -sin(2.0 * PI * it / n) }

    /** [re] + i·[im] (each [n] long) ← the DFT of the real [input]. */
    fun transform(input: DoubleArray, re: DoubleArray, im: DoubleArray, scratch: DoubleArray) {
        step(input, 0, 1, 0, re, im, 0, scratch)
    }

    /**
     * The DFT of `input[inOffset + j * stride]`, j < n / stride, into
     * `re/im[outOffset ...]`: [factors]`[level]` sub-DFTs of every p-th sample,
     * then one radix-p butterfly per output index.
     */
    private fun step(
        input: DoubleArray, inOffset: Int, stride: Int, level: Int,
        re: DoubleArray, im: DoubleArray, outOffset: Int, scratch: DoubleArray,
    ) {
        val p = factors[level]
        val m = n / stride / p
        if (m == 1) {
            for (r in 0 until p) {
                re[outOffset + r] = input[inOffset + r * stride]
                im[outOffset + r] = 0.0
            }
        } else {
            for (r in 0 until p) step(input, inOffset + r * stride, stride * p, level + 1, re, im, outOffset + r * m, scratch)
        }
        val tIm = maxFactor
        val pStep = n / p
        for (k in 0 until m) {
            // Sub-DFT r's k-th output, times W_(m·p)^(r·k) = W_n^(r·k·stride); r·k·stride < n.
            for (r in 0 until p) {
                val a = outOffset + r * m + k
                val w = r * k * stride
                scratch[r] = re[a] * twiddleRe[w] - im[a] * twiddleIm[w]
                scratch[tIm + r] = re[a] * twiddleIm[w] + im[a] * twiddleRe[w]
            }
            // Output k + q·m is their p-point DFT at q: W_p^(r·q) = W_n^(r·q·n/p mod n).
            for (q in 0 until p) {
                var sr = scratch[0]
                var si = scratch[tIm]
                var w = 0
                for (r in 1 until p) {
                    w += q * pStep
                    if (w >= n) w -= n
                    sr += scratch[r] * twiddleRe[w] - scratch[tIm + r] * twiddleIm[w]
                    si += scratch[r] * twiddleIm[w] + scratch[tIm + r] * twiddleRe[w]
                }
                re[outOffset + k + q * m] = sr
                im[outOffset + k + q * m] = si
            }
        }
    }
}
