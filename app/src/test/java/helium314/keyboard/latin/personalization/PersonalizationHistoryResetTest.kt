// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.personalization

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** B15: forgetting learned history must not erase static or explicitly saved words. */
@RunWith(RobolectricTestRunner::class)
class PersonalizationHistoryResetTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun clearingHistoryPreservesStaticAndExplicitPersonalDictionaries() {
        val dictionaryFiles = temporaryFolder.newFolder("dictionaries")
        val learnedHistory = File(dictionaryFiles, "UserHistoryDictionary.en_US.dict")
        val learnedHistoryBackup = File(dictionaryFiles, "UserHistoryDictionary.en_GB.dict")
        val staticSeed = File(dictionaryFiles, "brrPersonal_v1.en_US.dict")
        val explicitPersonalWords = File(dictionaryFiles, "UserBinaryDictionary.en_US.dict")
        val unrelatedFile = File(dictionaryFiles, "settings.txt")
        listOf(learnedHistory, learnedHistoryBackup, staticSeed, explicitPersonalWords, unrelatedFile)
            .forEach { it.writeText("synthetic-test-data") }

        val base = ApplicationProvider.getApplicationContext<Context>()
        val testContext = object : ContextWrapper(base) {
            override fun getFilesDir(): File = dictionaryFiles
        }
        PersonalizationHelper.removeAllUserHistoryDictionaries(testContext)

        assertFalse(learnedHistory.exists(), "Learned history must be removed")
        assertFalse(learnedHistoryBackup.exists(), "All learned locales must be removed")
        assertTrue(staticSeed.exists(), "Bundled brrPersonal cache must be retained")
        assertTrue(explicitPersonalWords.exists(), "Explicit user words must be retained")
        assertTrue(unrelatedFile.exists(), "Other application data must be retained")
    }
}
