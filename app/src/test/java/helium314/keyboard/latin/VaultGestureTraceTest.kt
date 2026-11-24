package helium314.keyboard.latin

import org.junit.Assert.*
import org.junit.Test

class VaultGestureTraceTest {
    private fun shape(reverse: Boolean = false): FloatArray {
        val xs = FloatArray(25) { if (it < 13) it * 4f else (24 - it) * 4f }
        val ys = FloatArray(25) { if (it < 13) it * 2f else 24f - (it - 12) * 1f }
        if (reverse) { xs.reverse(); ys.reverse() }
        return requireNotNull(VaultGestureTrace.normalize(xs, ys, xs.size, 1f))
    }
    @Test fun consistentEnrollmentAndDifferentDirection() {
        val a = shape(); val b = shape(true)
        assertTrue(VaultGestureTrace.matches(a, a))
        assertFalse(VaultGestureTrace.matches(a, b))
        assertNotNull(VaultGestureTrace.average(listOf(a, a, a)))
        assertNull(VaultGestureTrace.average(listOf(a, b, a)))
    }
    @Test fun rejectsFlatAndNonFiniteInput() {
        assertNull(VaultGestureTrace.normalize(FloatArray(20) { it.toFloat() }, FloatArray(20), 20, 1f))
        assertNull(VaultGestureTrace.normalize(FloatArray(20) { Float.NaN }, FloatArray(20), 20, 1f))
    }
    @Test fun onlyMaskedTextPasswordIsEligible() {
        val text = android.text.InputType.TYPE_CLASS_TEXT
        assertTrue(VaultGestureStore.isMaskedPassword(text or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertTrue(VaultGestureStore.isMaskedPassword(text or android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
        assertFalse(VaultGestureStore.isMaskedPassword(text or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD))
        assertFalse(VaultGestureStore.isMaskedPassword(text))
    }
}
