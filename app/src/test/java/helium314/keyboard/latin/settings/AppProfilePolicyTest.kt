package helium314.keyboard.latin.settings
import org.junit.Assert.*
import org.junit.Test

class AppProfilePolicyTest {
    @Test fun defaultsAndSparseOverrides() {
        val a = AppProfileStore.Profile(autocorrect=false,numberRow=true)
        val b = AppProfileStore.Profile()
        assertFalse(a.autocorrect(true))
        assertTrue(a.numberRow(false))
        assertTrue(a.suggestions(true))
        assertTrue(b.autocorrect(true))
        assertFalse(b.numberRow(false))
    }
    @Test fun optionalTypingOverridesInheritIndependently() {
        val p = AppProfileStore.Profile(autoCap = false, doubleSpacePeriod = true)
        assertFalse(p.autoCap(true))
        assertTrue(p.doubleSpacePeriod(false))
        assertTrue(AppProfileStore.Profile().autoCap(true))
        assertFalse(AppProfileStore.Profile().doubleSpacePeriod(false))
        assertTrue(p.privateMode(true))
    }
    @Test fun learnedPreferenceNeverOverridesExplicitSetting() {
        val learned = AppProfileStore.Profile(automaticAutocorrectOff = true)
        assertFalse(learned.autocorrect(true))
        assertTrue(learned.copy(autocorrect = true).autocorrect(false))
        assertFalse(learned.copy(autocorrect = false).autocorrect(true))
    }
    @Test fun boundedEvidenceCannotCrossEditorOrExpiry() {
        val window = AppProfileRejectionWindow()
        repeat(5) { assertFalse(window.record(1,100L + it)) }
        assertFalse(window.record(2,200L))
        repeat(4) { assertFalse(window.record(2,201L + it)) }
        assertFalse(window.record(2,30_200L))
        repeat(4) { assertFalse(window.record(2,30_201L + it)) }
        assertTrue(window.record(2,30_210L))
        assertFalse(window.record(2,30_211L))
    }
    @Test fun privacyRevocationClearsPartialRejectionEvidence() {
        val window = AppProfileRejectionWindow()
        repeat(5) { assertFalse(window.record(4, 100L + it)) }

        // LatinIME clears the same bounded window when its current editor becomes ineligible.
        window.clear()
        repeat(5) { assertFalse(window.record(4, 200L + it)) }
        assertTrue(window.record(4, 205L))
    }
    @Test fun encryptedProfileRejectsTampering() {
        val key = javax.crypto.spec.SecretKeySpec(ByteArray(32) { 7 }, "AES")
        val input = "example.profile".toByteArray()
        val one = AppProfileStore.seal(key,input)
        val two = AppProfileStore.seal(key,input)
        assertFalse(one.contentEquals(two))
        assertArrayEquals(input,AppProfileStore.open(key,one))
        one[one.lastIndex] = (one.last().toInt() xor 1).toByte()
        try { AppProfileStore.open(key,one); fail("tampering accepted") }
        catch (_: java.security.GeneralSecurityException) { }
    }
    @Test fun privacyCanOnlyTighten() {
        assertTrue(AppProfileStore.Profile().privateMode(true))
        assertFalse(AppProfileStore.Profile().privateMode(false))
        assertTrue(AppProfileStore.Profile(privateMode=true).privateMode(false))
    }
}
