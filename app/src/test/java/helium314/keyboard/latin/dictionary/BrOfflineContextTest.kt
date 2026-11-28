// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.dictionary

import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.Suggest
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.brain.BrOfflineContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class BrOfflineContextTest {
    private fun model(vararg rows: String) =
        BrOfflineContext.parse(sequenceOf(BrOfflineContext.VERSION, *rows))

    @Test
    fun longerContextBacksOffAndMixedLaneDoesNotLeakIntoEnglish() {
        val model = model(
            "en\tbirthday\tparty\t130",
            "en\thappy birthday\tto\t190",
            "mix\tbirthday\tbadhai\t170",
            "mix\tkal milte\thain\t255",
        )
        assertEquals("to", model.lookup(listOf("happy", "birthday"), false).first().surface)
        assertEquals("party", model.lookup(listOf("your", "birthday"), false).first().surface)
        assertEquals("badhai", model.lookup(listOf("birthday"), true).first().surface)
        assertEquals("hain", model.lookup(listOf("kal", "milte"), true).first().surface)
        assertTrue(model.lookup(listOf("kal", "milte"), false).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedRowsCannotProduceAPartialModel() {
        model(
            "en\tI think\twe're\t240",
            "mix\tkal milte\thain\t255",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateContextRejected() {
        model(
            "en\tthen\twe\t140",
            "en\tthen\tthey\t130",
            "mix\tchalo\tbhai\t255",
        )
    }

    @Test
    fun nativeAndImportedAmbiguousApostrophesCannotBecomeAutomaticInProse() {
        for (source in listOf(Dictionary.TYPE_MAIN, Dictionary.TYPE_BRR_PERSONAL, Dictionary.TYPE_USER_HISTORY)) {
            val info = word("we’re", SuggestedWordInfo.KIND_WHITELIST, source)
            assertTrue(Suggest.blocksOfflineReplacement("were", info, true))
            assertTrue(Suggest.blocksOfflineReplacement("Were", info, true))
            assertFalse(Suggest.blocksOfflineReplacement("we’re", info, true))
        }
        assertFalse(Suggest.blocksOfflineReplacement("were",
            word("we're", SuggestedWordInfo.KIND_SHORTCUT, Dictionary.TYPE_USER), true))
    }

    @Test
    fun automaticChoiceCannotRewriteTheTypedApostropheSurface() {
        val straight = word("we're", SuggestedWordInfo.KIND_CORRECTION, Dictionary.TYPE_MAIN)
        assertTrue(Suggest.blocksOfflineReplacement("we’re", straight, true))
        assertFalse(Suggest.blocksOfflineReplacement("we're", straight, true))
    }

    @Test
    fun manualAliasCannotCommitWhenContextBecomesIneligible() {
        val info = word("let's", SuggestedWordInfo.KIND_OFFLINE_ALIAS, Dictionary.TYPE_MAIN)
        assertTrue(Suggest.blocksOfflineReplacement("lets", info, true))
        assertTrue(Suggest.blocksOfflineReplacement("lets", info, false))
    }

    @Test
    fun nextWordAddsLicensedCandidateOnlyInCurrentEditorAndHonorsRemoval() {
        val model = model("en\tcold\tbrew\t255", "mix\tmilte\thain\t255")
        val context = NgramContext(NgramContext.WordInfo("cold"))
        val native = arrayListOf(word("water", SuggestedWordInfo.KIND_PREDICTION, Dictionary.TYPE_MAIN))
        fun rank(current: Boolean, allows: Boolean) = BrOfflineContext.rank(
            native, context, Locale.US, "", prediction = true, swipe = false, pinnedCount = 0,
            isCurrent = { current }, allowNewPrediction = { allows }, activeModel = model,
        )
        assertEquals(listOf("brew", "water"), rank(current = true, allows = true).map { it.mWord })
        assertEquals(native, rank(current = false, allows = true))
        assertEquals(native, rank(current = true, allows = false))
        assertEquals(listOf("water"), native.map { it.mWord })
    }

    @Test
    fun completedSwipeKeepsWinnerAndOnlyReordersExistingAlternatives() {
        val model = model("en\tcold\tbrew\t255", "mix\tmilte\thain\t255")
        val native = arrayListOf("water", "brow", "brew").mapTo(ArrayList()) {
            word(it, SuggestedWordInfo.KIND_CORRECTION, Dictionary.TYPE_MAIN)
        }
        val output = BrOfflineContext.rank(
            native, NgramContext(NgramContext.WordInfo("cold")), Locale.US, "",
            prediction = false, swipe = true, pinnedCount = 1,
            isCurrent = { true }, allowNewPrediction = { error("Swipe cannot add a word") },
            activeModel = model,
        )
        assertEquals(listOf("water", "brew", "brow"), output.map { it.mWord })
    }

    private fun word(word: String, kind: Int, source: String) = SuggestedWordInfo(
        word, "", 1, kind, Dictionary.PhonyDictionary(source),
        SuggestedWordInfo.NOT_AN_INDEX, SuggestedWordInfo.NOT_A_CONFIDENCE,
    )
}
