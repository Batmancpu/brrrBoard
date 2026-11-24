// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class BrrBrainStreamingTest {
    private val block=BrrBrainStreamWire.BLOCK_BYTES
    private val look=BrrBrainStreamWire.LOOK_BYTES
    private fun waveform(n: Int)=ByteArray(n) { i -> (i*31+i/13).toByte() }
    private fun scored(raw: ByteArray, a: Float=.8f, b: Float=.8f)=
        BrrBrainScoredBlock(raw.copyOf(),FloatArray((raw.size+1023)/1024){a},
            FloatArray((raw.size+1023)/1024){b})

    @Test fun arbitraryCaptureFramesAndFinalPartialKeepAllSamplesExactlyOnce() {
        val input=waveform(block*3+1354)
        val emitted=ByteArrayOutputStream()
        val fed=ByteArrayOutputStream()
        var calls=0
        val stream=BrrBrainBlockAssembler(
            infer={ raw, fresh ->
                assertEquals(block,raw.size)
                assertEquals(block+(if(calls++==0)look else 0),fresh.size)
                fed.write(fresh)
                scored(raw)
            },
            failed={ fail("unexpected stream failure") },
            emit={ raw, enhanced ->
                val output=checkNotNull(enhanced)
                assertArrayEquals(raw,output.clean)
                emitted.write(raw)
                raw.fill(0); output.wipe()
            })
        for(i in input.indices step 1746) {
            val end=minOf(i+1746,input.size)
            stream.append(input.copyOfRange(i,end),end-i)
        }
        stream.finish()
        assertArrayEquals(input,emitted.toByteArray())
        val nativeInput=fed.toByteArray()
        assertArrayEquals(input,nativeInput.copyOf(input.size))
        assertTrue(nativeInput.drop(input.size).all { it==0.toByte() })
        assertEquals(4,calls)
    }

    @Test fun childFailureEmitsPendingAndAllFutureRawAudioInOrder() {
        val input=waveform(block*4+2222)
        val emitted=ByteArrayOutputStream()
        var calls=0; var failures=0; var protectedBlocks=0
        val stream=BrrBrainBlockAssembler(
            infer={ raw,_ -> if(++calls==2)null else scored(raw) },
            failed={ failures++ },
            emit={ raw, enhanced ->
                emitted.write(raw)
                if(enhanced!=null)protectedBlocks++
                raw.fill(0); enhanced?.wipe()
            })
        stream.append(input,input.size)
        stream.finish()
        assertArrayEquals(input,emitted.toByteArray())
        assertEquals(2,calls)
        assertEquals(1,failures)
        assertEquals(1,protectedBlocks)
    }

    @Test fun cancelNeverEmitsBufferedAudio() {
        val input=waveform(block)
        var emissions=0
        val stream=BrrBrainBlockAssembler({ raw,_->scored(raw) },{},
            { raw,enhanced->emissions++;raw.fill(0);enhanced?.wipe() })
        stream.append(input,input.size)
        stream.discard()
        stream.finish()
        assertEquals(0,emissions)
    }

    @Test fun bothVadPathsProtectBoundariesAndOverlapStaysAligned() {
        val aligned=AlignedVoiceBuffer()
        val raw=waveform(block)
        val clean=raw.copyOf().also { it[1]=99 }
        val result=BrrBrainScoredBlock(clean,FloatArray(20){.03f},FloatArray(20){.9f})
        aligned.append(raw,result)
        assertEquals(true,aligned.activity(0,3200))
        assertTrue(aligned.boundaryPenalty(0,3200)>0)
        val count=16000
        val first=checkNotNull(aligned.take(raw.copyOf(count)))
        assertFalse(first.rawSpeechEvidence)
        assertEquals(count,first.pcm.size)
        val drop=count-PrivateVoiceChunker.OVERLAP_BYTES
        assertArrayEquals(clean.copyOfRange(drop,block),aligned.copyPrefix(block-drop))
        aligned.clear()
        aligned.append(raw,null)
        assertNull(aligned.activity(0,3200))
        assertNull(aligned.take(raw))
        aligned.clear()
    }

    @Test fun compactionOnlyRemovesConfirmedDigitalSilenceAtOuterEdges() {
        val n=PrivateVoiceChunker.BYTES_PER_SECOND*4
        val raw=ByteArray(n)
        val from=PrivateVoiceChunker.BYTES_PER_SECOND*3/2
        val to=n-from
        for(i in from until to step 2) { raw[i]=20;raw[i+1]=1 }
        val scores=FloatArray((n+1023)/1024){ i ->
            if(i*1024 in from-1024..to) .95f else .01f }
        val aligned=AlignedVoiceBuffer()
        aligned.append(raw,BrrBrainScoredBlock(raw.copyOf(),scores,scores.copyOf()))
        val compact=checkNotNull(aligned.take(raw))
        assertTrue(compact.pcm.size<raw.size)
        assertTrue(compact.rawSpeechEvidence)
        assertEquals((to-from)/2,(0 until compact.pcm.size/2).count {
            ProtectedVoiceAudio.sample(compact.pcm,it)!=0 })
        aligned.clear()
        val faint=ByteArray(n).also { for(i in it.indices step 2)it[i]=1 }
        aligned.append(faint,scored(faint,.01f,.01f))
        assertEquals(n,checkNotNull(aligned.take(faint)).pcm.size)
        aligned.clear()
    }
}
