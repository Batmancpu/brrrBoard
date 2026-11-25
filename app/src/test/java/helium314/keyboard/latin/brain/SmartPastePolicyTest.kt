package helium314.keyboard.latin.brain

import org.junit.Test
import org.junit.Assert.*

class SmartPastePolicyTest {
    private val policy = SmartPastePolicy
    @Test fun wholeTextRemainsOriginalInGeneralEditor() {
        val raw = "Send this: https://example.com/watch?v=x&utm_source=a"
        val c = policy.classify(raw)
        val d = policy.decide(c, SmartPastePolicy.Destination.GENERAL, 0, "", false, 0, false)!!
        assertEquals(raw,raw.substring(d.candidate.start,d.candidate.end))
        val url = policy.decide(c, SmartPastePolicy.Destination.URL, 0, "", false, 0, false)!!
        assertEquals("https://example.com/watch?v=x&utm_source=a",raw.substring(url.candidate.start,url.candidate.end))
    }
    @Test fun ambiguousStructuredEntitiesAreRejected() {
        val c = policy.classify("a@example.com and b@example.org")
        assertNull(policy.decide(c, SmartPastePolicy.Destination.EMAIL,0,"",false,0,false))
        assertNull(policy.decide(c, SmartPastePolicy.Destination.PHONE,0,"",false,0,false))
    }
    @Test fun freshnessAndIntent() {
        val c = policy.classify("https://example.com/")
        assertTrue(policy.decide(c, SmartPastePolicy.Destination.GENERAL,0,"",false,0,false)!!.prominent)
        assertTrue(policy.intent(SmartPastePolicy.Kind.LINK,"Here is the link: "))
        assertTrue(policy.intent(SmartPastePolicy.Kind.PHONE,"mera number hai"))
        assertNull(policy.decide(c,SmartPastePolicy.Destination.GENERAL,60_000,"",false,0,false))
        assertNotNull(policy.decide(c,SmartPastePolicy.Destination.URL,60_000,"",false,0,false))
    }
    @Test fun otpNeverOrdinaryCandidate() {
        assertTrue(policy.classify("Your OTP is 123456").isEmpty())
        assertTrue(policy.classify("123456").isEmpty())
        val c = policy.copiedOtp("Your OTP is 123456")!!
        assertEquals(SmartPastePolicy.Kind.OTP,c.kind)
        assertNull(policy.copiedOtp("OTP 123456 and OTP 654321"))
        assertNull(policy.copiedOtp("https://example.com/?token=123456"))
        assertNull(policy.decide(listOf(c),SmartPastePolicy.Destination.OTP,60_000,"",false,0,false))
    }
    @Test fun learningIsBoundedAndNeverLearnsOtp() {
        SmartPasteLearning.reset()
        repeat(100) { SmartPasteLearning.record(SmartPastePolicy.Kind.LINK,SmartPastePolicy.Destination.GENERAL,true) }
        assertTrue(SmartPasteLearning.bias(SmartPastePolicy.Kind.LINK,SmartPastePolicy.Destination.GENERAL) in 1..5)
        repeat(100) { SmartPasteLearning.record(SmartPastePolicy.Kind.OTP,SmartPastePolicy.Destination.OTP,true) }
        assertEquals(0,SmartPasteLearning.bias(SmartPastePolicy.Kind.OTP,SmartPastePolicy.Destination.OTP))
        SmartPasteLearning.reset()
        assertEquals(0,SmartPasteLearning.bias(SmartPastePolicy.Kind.LINK,SmartPastePolicy.Destination.GENERAL))
    }
}
