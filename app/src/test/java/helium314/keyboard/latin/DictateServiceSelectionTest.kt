// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DictateServiceSelectionTest {
    private val dictate = ComponentName("net.devemperor.dictate", "net.devemperor.dictate.TestRecognizer")
    private val other = ComponentName("com.example.other", "com.example.other.Recognizer")

    @Test fun selectedDictateRecognitionServiceIsUsedExactly() {
        val resolved = resolveDictateServiceSelection(dictate.flattenToString(), listOf(dictate, other))
        assertEquals(dictate, resolved.component)
        assertNull(resolved.failure)
    }

    @Test fun missingMalformedAndDifferentProviderCannotBindOrLaunchFallback() {
        assertEquals(DictateServiceSelection.Failure.NO_SELECTION,
            resolveDictateServiceSelection(null, listOf(dictate)).failure)
        assertEquals(DictateServiceSelection.Failure.MALFORMED,
            resolveDictateServiceSelection("bad component", listOf(dictate)).failure)
        assertEquals(DictateServiceSelection.Failure.WRONG_PROVIDER,
            resolveDictateServiceSelection(other.flattenToString(), listOf(other)).failure)
        assertEquals(DictateServiceSelection.Failure.SERVICE_MISSING,
            resolveDictateServiceSelection(dictate.flattenToString(), emptyList()).failure)
        assertNull(resolveDictateServiceSelection(other.flattenToString(), listOf(other)).component)
    }
}
