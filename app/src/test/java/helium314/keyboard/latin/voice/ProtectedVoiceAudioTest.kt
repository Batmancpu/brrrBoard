// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.*
import org.junit.Test

class ProtectedVoiceAudioTest {
    private val h = ProtectedVoiceAudio.HOP
    private fun pcm(frames: Int, amplitude: (Int) -> Int) = ByteArray(frames*h*2).also { out ->
        for(i in 0 until out.size/2) {
            val v = amplitude(i/h) * if(i%2==0) 1 else -1
            out[i*2] = v.toByte()
            out[i*2+1] = (v shr 8).toByte()
        }
    }
    @Test fun quietSpeechRecoversButNoiseDoesNot() {
        val raw = pcm(50) { if(it in 20..24) 600 else 2000 }
        val clean = pcm(50) { if(it in 20..24) 20 else 10 }
        val r = FloatArray(50) { if(it in 20..24) .9f else .04f }
        val c = FloatArray(50) { .03f }
        val out = ProtectedVoiceAudio.protect(raw,clean,r,c)
        assertEquals(raw.size,out.size)
        assertTrue(ProtectedVoiceAudio.sample(out,22*h+h/2) >= 600)
        assertEquals(10, ProtectedVoiceAudio.sample(out,2*h+h/2))
    }
    @Test fun noiseOnlyDoesNotGetGainOrRawBlend() {
        val raw = pcm(20) { 4000 }
        val clean = pcm(20) { 30 }
        assertArrayEquals(clean, ProtectedVoiceAudio.protect(raw,clean,FloatArray(20),FloatArray(20)))
    }
    @Test fun cleanSpeechIsNotReplacedByNoise() {
        val raw = pcm(20) { 8000 }
        val clean = pcm(20) { 2000 }
        val out = ProtectedVoiceAudio.protect(raw,clean,FloatArray(20){.1f},FloatArray(20){.95f})
        assertTrue(ProtectedVoiceAudio.sample(out,h/2) in 2000..2600)
    }
    @Test fun scoreWindowsIncludeRealPreviousContextAndTailPadding() {
        val audio = pcm(2) { if(it==0) 512 else 1024 }.copyOf(2*h*2-2)
        var calls = 0
        val scores = ProtectedVoiceAudio.scores(audio) { samples ->
            assertEquals(576,samples.size)
            if(calls==0) assertEquals(0f,samples[0],0f)
            else {
                assertEquals(512/32768f,samples[0],0f)
                assertEquals(0f,samples.last(),0f)
            }
            calls++; .5f
        }
        assertEquals(2,calls)
        assertEquals(2,scores.size)
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidModelScoreCannotSilentlyDiscardSpeech() {
        val raw = pcm(1) { 100 }
        ProtectedVoiceAudio.protect(raw,raw,floatArrayOf(Float.NaN),floatArrayOf(0f))
    }
    @Test fun clippedAndOddTailStayBoundedAndAligned() {
        val raw = pcm(4) { 32767 }.copyOf(4*h*2-2)
        val clean = pcm(4) { 30 }.copyOf(raw.size)
        val out = ProtectedVoiceAudio.protect(raw,clean,FloatArray(4){.9f},FloatArray(4){0f})
        assertEquals(raw.size,out.size)
        assertTrue(ProtectedVoiceAudio.sample(out,2*h) in 30000..32767)
    }
}
