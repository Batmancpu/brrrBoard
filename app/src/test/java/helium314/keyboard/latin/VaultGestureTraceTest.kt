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
    @Test fun acceptsNaturalCornerAndSpeedVariationButRejectsMirror() {
        val a = shape()
        val xs = FloatArray(25) { if (it < 11) it * 4.8f else (24 - it) * 3.5f }
        val ys = FloatArray(25) { if (it < 11) it * 2.5f else 26f - (it - 10) * .9f }
        val b = requireNotNull(VaultGestureTrace.normalize(xs, ys, xs.size, 1f))
        assertTrue(VaultGestureTrace.matches(a, b))
        assertNotNull(VaultGestureTrace.average(listOf(a, b, a)))
        val mirrored = a.copyOf().also { for (i in 1 until it.size step 2) it[i] = -it[i] }
        assertFalse(VaultGestureTrace.matches(a, mirrored))
        val invalid = a.copyOf().also { it[0] = Float.NaN }
        assertFalse(VaultGestureTrace.matches(a, invalid))
    }
    @Test fun majorityEnrollmentRejectsUnrelatedOrReversedTraces() {
        val a = shape(); val b = shape(true)
        assertTrue(VaultGestureTrace.matchesEnrollment(listOf(a, a, b), a))
        assertFalse(VaultGestureTrace.matchesEnrollment(listOf(a, a, b), b))
        assertFalse(VaultGestureTrace.matchesEnrollment(listOf(a, a), a))
    }
    @Test fun oneStrokeAllowsSmallWristAngleChangesButStillRequiresConsensus() {
        val enrolled = shape()
        val xs = FloatArray(25) { if (it < 13) it * 4f else (24 - it) * 4f }
        val ys = FloatArray(25) { if (it < 13) it * 2f else 24f - (it - 12) * 1f }
        val angle = 8.0 * Math.PI / 180.0
        val rotatedX = FloatArray(xs.size) { (xs[it] * kotlin.math.cos(angle) - ys[it] * kotlin.math.sin(angle)).toFloat() }
        val rotatedY = FloatArray(xs.size) { (xs[it] * kotlin.math.sin(angle) + ys[it] * kotlin.math.cos(angle)).toFloat() }
        val candidate = requireNotNull(VaultGestureTrace.normalize(rotatedX, rotatedY, xs.size, 1f))
        assertTrue(VaultGestureTrace.matchesEnrollment(listOf(enrolled, enrolled, enrolled), candidate))
        val reverse = shape(true)
        assertFalse(VaultGestureTrace.matchesEnrollment(listOf(enrolled, enrolled, enrolled), reverse))
        val mirrored = enrolled.copyOf().also { for (i in 1 until it.size step 2) it[i] = -it[i] }
        assertFalse(VaultGestureTrace.matchesEnrollment(listOf(enrolled, enrolled, enrolled), mirrored))
        assertFalse(VaultGestureTrace.matchesEnrollment(listOf(enrolled, reverse, reverse), candidate))
    }
    @Test fun diagonalDragIsNotASecretPattern() {
        val x = FloatArray(20) { it * 4f }
        val y = FloatArray(20) { it * 3f }
        assertNull(VaultGestureTrace.normalize(x, y, x.size, 1f))
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
