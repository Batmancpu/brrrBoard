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
    private val canonical = ComponentName("net.devemperor.dictate",
        "dev.patrickgold.florisboard.dictate.recognition.DictateRecognitionService")
    private val other = ComponentName("com.example.other", "com.example.other.Recognizer")

    @Test fun selectedDictateRecognitionServiceIsUsedExactly() {
        val resolved = resolveDictateServiceSelection(dictate.flattenToString(), listOf(dictate, other))
        assertEquals(dictate, resolved.component)
        assertNull(resolved.failure)
    }

    @Test fun colorOsDifferentSelectionStillUsesVerifiedDictateRecognizer() {
        val resolved = resolveDictateServiceSelection(other.flattenToString(), listOf(other, canonical))
        assertEquals(canonical, resolved.component)
        assertNull(resolved.failure)
    }

    @Test fun absentSettingCanUseOneVerifiedDictateRecognizer() {
        assertEquals(dictate, resolveDictateServiceSelection(null, listOf(dictate)).component)
        assertEquals(canonical, resolveDictateServiceSelection("broken", listOf(dictate, canonical)).component)
    }

    @Test fun missingMalformedAndDifferentProviderCannotBindToAnUnrelatedRecognizer() {
        assertEquals(DictateServiceSelection.Failure.NO_SELECTION,
            resolveDictateServiceSelection(null, listOf(other)).failure)
        assertEquals(DictateServiceSelection.Failure.MALFORMED,
            resolveDictateServiceSelection("bad component", listOf(other)).failure)
        assertEquals(DictateServiceSelection.Failure.WRONG_PROVIDER,
            resolveDictateServiceSelection(other.flattenToString(), listOf(other)).failure)
        assertEquals(DictateServiceSelection.Failure.SERVICE_MISSING,
            resolveDictateServiceSelection(dictate.flattenToString(), emptyList()).failure)
        assertNull(resolveDictateServiceSelection(other.flattenToString(), listOf(other)).component)
    }

    @Test fun ambiguousDictateServicesNeverGuessAnUnverifiedComponent() {
        val second = ComponentName("net.devemperor.dictate", "net.devemperor.dictate.SecondRecognizer")
        assertNull(resolveDictateServiceSelection(null, listOf(dictate, second)).component)
        assertEquals(DictateServiceSelection.Failure.NO_SELECTION,
            resolveDictateServiceSelection(null, listOf(dictate, second)).failure)
    }
}
