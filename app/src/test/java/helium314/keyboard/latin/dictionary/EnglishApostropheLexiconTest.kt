// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.dictionary

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EnglishApostropheLexiconTest {
    private fun entry(word: String) = EnglishApostropheLexicon.Entry(word, 72)
    @Test fun existingWordsAndTypographicVariantsAreNeverReweightedOrDuplicated() {
        val entries = listOf(entry("don't"), entry("can't"), entry("I'm"), entry("won't"),
            entry("we're"), entry("could've"), entry("they'll"))
        val existing = setOf("don't", "can’t", "i'm", "were", "theyll")
        val plan = EnglishApostropheLexicon.plan(entries) { it in existing }
        assertEquals(listOf("won't", "we're", "could've", "they'll"), plan.missing.map { it.word })
        assertEquals(setOf("dont", "couldve"), plan.malformedAliases)
    }

    @Test fun ambiguousValidSpellingsAreNotNewForcedReplacements() {
        for ((typed, candidate) in listOf("were" to "we're", "well" to "we'll",
            "ill" to "I'll", "its" to "it's", "cant" to "can't", "wont" to "won’t",
            "shell" to "she'll", "id" to "I'd")) {
            assertTrue(EnglishApostropheLexicon.isAmbiguousOmission(typed, candidate))
        }
        assertFalse(EnglishApostropheLexicon.isAmbiguousOmission("dont", "don't"))
        assertFalse(EnglishApostropheLexicon.isAmbiguousOmission("couldve", "could've"))
        assertFalse(EnglishApostropheLexicon.isAmbiguousOmission("well", "welcome"))
    }

    @Test fun reviewedAssetIsBoundedAndContainsOnlyCanonicalContractions() {
        val file = listOf(File("src/main/assets/brrPersonal_english_contractions.tsv"),
            File("app/src/main/assets/brrPersonal_english_contractions.tsv")).first { it.isFile }
        val entries = file.bufferedReader().use { EnglishApostropheLexicon.parse(it.lineSequence()) }
        assertEquals(66, entries.size)
        val words = entries.map { it.word }.toSet()
        assertTrue(words.containsAll(listOf("don't", "can't", "I'm", "I'll", "I've", "we're",
            "couldn't", "should've", "didn't", "isn't", "haven't", "o'clock", "ma'am")))
        assertTrue(entries.all { it.frequency in 32..96 })
    }

    @Test fun manifestRejectsMalformedTokensDuplicatesAndAggressiveWeights() {
        for (line in listOf("dont\t72", "'quote\t72", "dogs'\t72", "don’t\t72",
            "don't\t255", "don't\t72\tshortcut", "abc def\t72")) {
            assertTrue(runCatching { EnglishApostropheLexicon.parse(sequenceOf(line)) }.isFailure)
        }
        assertTrue(runCatching {
            EnglishApostropheLexicon.parse(sequenceOf("don't\t72", "don't\t48"))
        }.isFailure)
    }
}
