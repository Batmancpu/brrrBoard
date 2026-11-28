package helium314.keyboard.latin
import org.junit.Assert.*
import org.junit.Test
class VisualPowerPolicyTest {
    @Test fun thresholdAndCharging() {
        assertTrue(VisualPowerPolicy.lowBattery(19, false))
        assertTrue(VisualPowerPolicy.lowBattery(0, false))
        assertFalse(VisualPowerPolicy.lowBattery(20, false))
        assertFalse(VisualPowerPolicy.lowBattery(19, true))
        assertFalse(VisualPowerPolicy.lowBattery(-1, false))
    }
}
