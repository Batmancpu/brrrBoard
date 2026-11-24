// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** All methods are confined to the capture worker. No model or IPC on AudioRecord. */
internal class BrrBrainBlockAssembler(
    private val infer: (ByteArray, ByteArray) -> BrrBrainScoredBlock?,
    private val failed: () -> Unit,
    private val emit: (ByteArray, BrrBrainScoredBlock?) -> Unit,
) {
    private val block = BrrBrainStreamWire.BLOCK_BYTES
    private val look = BrrBrainStreamWire.LOOK_BYTES
    private val buffer = ByteArray(block + look)
    private var used = 0
    private var first = true
    private var disabled = false
    private var closed = false
    private data class Packet(val raw: ByteArray, val scored: BrrBrainScoredBlock, val actual: Int) {
        fun wipe() { raw.fill(0); scored.wipe() }
    }
    private var history: Packet? = null
    private var previous: Packet? = null

    fun append(pcm: ByteArray, count: Int) {
        check(!closed)
        require(count in 0..pcm.size && count % 2 == 0)
        var pos = 0
        while (pos < count) {
            if (disabled) {
                val n = minOf(block, count-pos)
                emit(pcm.copyOfRange(pos,pos+n), null)
                pos += n
            } else {
                val n = minOf(buffer.size-used,count-pos)
                pcm.copyInto(buffer,used,pos,pos+n)
                used += n; pos += n
                if (used == buffer.size) processBlock()
            }
        }
    }
    private fun processBlock() {
        val actual = minOf(used,block)
        check(actual > 0)
        // Only first packet contains the initial hop; every later packet feeds
        // new samples after the already consumed 16 ms of lookahead.
        val raw = buffer.copyOf(block)
        val input = buffer.copyOfRange(if (first) 0 else look,buffer.size)
        val response = try { infer(raw,input) } catch (_: Exception) { null }
            finally { input.fill(0) }
        first = false
        val remaining = used-actual
        buffer.copyInto(buffer,0,actual,used)
        buffer.fill(0,remaining)
        used = remaining
        val good = response != null && response.clean.size == block &&
            response.rawScore.size == block/1024 && response.cleanScore.size == block/1024 &&
            response.rawScore.all { it.isFinite() && it in 0f..1f } &&
            response.cleanScore.all { it.isFinite() && it in 0f..1f }
        if (good) {
            val current = Packet(raw,checkNotNull(response),actual)
            emitPrevious(current)
            previous = current
        } else {
            response?.wipe()
            emitPrevious(null)
            history?.wipe(); history = null
            previous = null
            disabled = true
            failed()
            emit(raw.copyOf(actual),null)
            raw.fill(0)
            if (used > 0) {
                emit(buffer.copyOf(used),null)
                buffer.fill(0); used=0
            }
        }
    }
    private fun emitPrevious(next: Packet?) {
        val current = previous ?: return
        val parts = listOfNotNull(history,current,next)
        val raw = ByteArray(parts.size*block)
        val clean = ByteArray(raw.size)
        val scores = FloatArray(raw.size/1024)
        val cleanScores = FloatArray(scores.size)
        var offset = 0
        for (part in parts) {
            part.raw.copyInto(raw,offset)
            part.scored.clean.copyInto(clean,offset)
            part.scored.rawScore.copyInto(scores,offset/1024)
            part.scored.cleanScore.copyInto(cleanScores,offset/1024)
            offset += block
        }
        val start = if (history == null) 0 else block
        var enhanced: ByteArray? = null
        try {
            // Include preceding and following blocks for quiet onset/offset
            // protection. Apply speech gain later on the full ASR batch.
            enhanced = try {
                ProtectedVoiceAudio.protect(raw,clean,scores,cleanScores,gainEnabled=false)
            } catch (_: Exception) { null }
            val selected = enhanced
            val n = (current.actual + 1023)/1024
            val result = if (selected == null) null else BrrBrainScoredBlock(
                selected.copyOfRange(start,start+current.actual),
                current.scored.rawScore.copyOf(n),current.scored.cleanScore.copyOf(n))
            emit(current.raw.copyOf(current.actual), result)
        } finally {
            raw.fill(0); clean.fill(0); scores.fill(0f); cleanScores.fill(0f)
            enhanced?.fill(0)
        }
        history?.wipe()
        history = current
        previous = null
    }
    fun finish() {
        if (closed) return
        while (used>0) processBlock()
        emitPrevious(null)
        discard()
    }
    fun discard() {
        if (closed) return
        closed = true; buffer.fill(0); used=0
        history?.wipe(); history=null
        previous?.wipe(); previous=null
    }
}

/** Synchronized with the raw chunker's overlap: no independent cut or dropped sample. */
internal class AlignedVoiceBuffer {
    private val pcm = ByteArray(PrivateVoiceChunker.MAX_BYTES+BrrBrainStreamWire.BLOCK_BYTES)
    private val flags = ByteArray(pcm.size/2)
    private var used = 0
    fun append(raw: ByteArray, scored: BrrBrainScoredBlock?) {
        require(raw.size%2==0 && used+raw.size<=pcm.size)
        val source = scored?.clean ?: raw
        require(source.size==raw.size)
        source.copyInto(pcm,used)
        for (i in 0 until raw.size/2) {
            val f = i/ProtectedVoiceAudio.HOP
            flags[used/2+i] = if (scored==null) 0 else {
                val a=scored.rawScore[f]; val b=scored.cleanScore[f]
                (KNOWN or (if (maxOf(a,b)>=.55f) SPEECH else 0) or
                    (if (maxOf(a,b)>=.25f) WEAK else 0) or
                    (if (a>=.55f) RAW_STRONG else 0)).toByte()
            }
        }
        used += raw.size
    }
    fun activity(from: Int,to: Int): Boolean? {
        require(from>=0 && to<=used && from<=to)
        var unknown=false; var weak=0
        for (i in from/2 until to/2) {
            val bits=flags[i].toInt()
            if (bits and SPEECH != 0) return true
            if (bits and KNOWN == 0) unknown=true
            if (bits and WEAK != 0) weak++
        }
        if (unknown) return null
        return weak >= (to-from)/4
    }
    fun copyPrefix(count: Int): ByteArray {
        require(count in 0..used)
        return pcm.copyOf(count)
    }
    fun boundaryPenalty(from: Int, to: Int): Double {
        var speech=0; var weak=0
        for (i in from/2 until to/2) {
            val bits=flags[i].toInt()
            if (bits and SPEECH != 0) speech++
            else if (bits and WEAK != 0) weak++
        }
        return (speech*30000.0+weak*5000.0)/((to-from)/2).coerceAtLeast(1)
    }
    fun take(raw: ByteArray): BrrCleanResult? {
        val count=raw.size
        require(count in 0..used)
        var known=false; var run=0; var evidence=false
        val score=FloatArray((count+1023)/1024)
        for (i in 0 until count/2) {
            val bits=flags[i].toInt()
            if (bits and KNOWN != 0) known=true
            if (bits and SPEECH != 0) score[i/512]=1f
            if (bits and RAW_STRONG != 0) { if (++run>=1024) evidence=true } else run=0
        }
        val result=if (known) {
            val aligned=pcm.copyOf(count)
            try {
                val gained=ProtectedVoiceAudio.protect(aligned,aligned,score,score)
                BrrCleanResult(compactDigitalSilence(raw,gained),evidence)
            } catch (_: Exception) { null }
            finally { aligned.fill(0) }
        } else null
        score.fill(0f)
        val drop=(count-PrivateVoiceChunker.OVERLAP_BYTES).coerceAtLeast(0)
        val remaining=used-drop
        pcm.copyInto(pcm,0,drop,used); pcm.fill(0,remaining,used)
        flags.copyInto(flags,0,drop/2,used/2); flags.fill(0,remaining/2,used/2)
        used=remaining
        return result
    }
    // Only remove long outer digital-zero runs also classified as non-speech
    // by both models. Preserve natural internal pauses and the raw retry buffer.
    // Never trim ordinary low-level, nonzero speech on a probability alone.
    private fun compactDigitalSilence(raw: ByteArray, enhanced: ByteArray): ByteArray {
        fun quiet(i: Int): Boolean {
            val bits=flags[i].toInt()
            return bits and KNOWN != 0 && bits and WEAK == 0 &&
                ProtectedVoiceAudio.sample(raw,i)==0
        }
        val total=raw.size/2
        var start=0; var end=total
        while (start<total && quiet(start)) start++
        while (end>start && quiet(end-1)) end--
        val trimStart=if (start>=16000) (start-5120).coerceAtLeast(0) else 0
        val trimEnd=if (total-end>=16000) (end+4096).coerceAtMost(total) else total
        if (trimStart==0 && trimEnd==total) return enhanced
        // An entirely silent batch still supplies a small, valid ASR window.
        val lo=trimStart.coerceAtMost((total-4096).coerceAtLeast(0))
        val hi=trimEnd.coerceAtLeast(lo+minOf(4096,total)).coerceAtMost(total)
        val compact=enhanced.copyOfRange(lo*2,hi*2)
        enhanced.fill(0)
        return compact
    }
    fun clear() { pcm.fill(0); flags.fill(0); used=0 }
    companion object {
        private const val KNOWN=1
        private const val SPEECH=2
        private const val WEAK=4
        private const val RAW_STRONG=8
    }
}
