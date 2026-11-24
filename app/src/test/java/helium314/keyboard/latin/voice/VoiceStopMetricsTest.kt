package helium314.keyboard.latin.voice
import org.junit.Assert.*
import org.junit.Test
class VoiceStopMetricsTest {
    @Test fun relativeStagesAndFirstPartialOnly() {
        VoiceStopMetrics.begin(true)
        VoiceStopMetrics.stop(100)
        VoiceStopMetrics.stop(110)
        VoiceStopMetrics.captureReleased(140)
        VoiceStopMetrics.drainPartial(180)
        VoiceStopMetrics.drainPartial(210)
        VoiceStopMetrics.resultReady(230)
        VoiceStopMetrics.dispatchReturned(240)
        assertEquals(VoiceStopMetrics.Snapshot(40,80,130,140),VoiceStopMetrics.snapshot())
        VoiceStopMetrics.begin(false)
        VoiceStopMetrics.stop(100)
        VoiceStopMetrics.resultReady(200)
        assertEquals(VoiceStopMetrics.Snapshot(-1,-1,-1,-1),VoiceStopMetrics.snapshot())
    }
}
