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
    @Test fun privacyCanOnlyTighten() {
        assertTrue(AppProfileStore.Profile().privateMode(true))
        assertFalse(AppProfileStore.Profile().privateMode(false))
        assertTrue(AppProfileStore.Profile(privateMode=true).privateMode(false))
    }
}
