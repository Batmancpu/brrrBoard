// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic

import helium314.keyboard.latin.EditorSessionGeneration
import helium314.keyboard.latin.SuggestedWords
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.suggestions.SuggestionPrefixPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.ArrayList

/** Synthetic policy replay only; it does not claim dictionary-quality or device-latency results. */
@RunWith(RobolectricTestRunner::class)
class QuickTypeReplayCorpusTest {
    private data class Scenario(
        val partition: String,
        val prefixes: List<String>,
        val nativeCandidates: List<String>,
    )

    private data class Request(
        val editor: Long,
        val request: Long,
        val prefix: String,
        val selectionStart: Int,
        val selectionEnd: Int,
    )

    private val corpus = listOf(
        Scenario("English", listOf("t", "th", "the", "ther"),
            listOf("there", "their", "then", "office")),
        Scenario("Indian English", listOf("r", "re", "rev"),
            listOf("revert", "review", "really", "office")),
        Scenario("Romanized Hinglish", listOf("a", "aa", "aau"),
            listOf("aaunga", "aaj", "aap", "office")),
    )

    private fun nativeSuggestions(words: List<String>): SuggestedWords {
        val infos = ArrayList(words.map { word ->
            SuggestedWordInfo(word, "", 400, SuggestedWordInfo.KIND_PREDICTION,
                Dictionary.DICTIONARY_HARDCODED, SuggestedWordInfo.NOT_AN_INDEX,
                SuggestedWordInfo.NOT_A_CONFIDENCE)
        })
        return SuggestedWords(infos, null, null, false, false, false,
            SuggestedWords.INPUT_STYLE_PREDICTION, SuggestedWords.NOT_A_SEQUENCE_NUMBER)
    }

    private fun isCurrent(expected: Request, current: Request): Boolean =
        SuggestionDeliveryPolicy.isCurrent(expected.editor, current.editor,
            expected.request, current.request, expected.prefix, current.prefix,
            expected.selectionStart, current.selectionStart,
            expected.selectionEnd, current.selectionEnd)

    @Test fun corpusPlaybackKeepsPreviewsPrefixSafeAcrossLanguagePartitions() {
        for (scenario in corpus) {
            val native = nativeSuggestions(scenario.nativeCandidates)
            for (prefix in scenario.prefixes) {
                val preview = SuggestionPrefixPreview.forTypedPrefix(native, prefix, false)
                assertEquals("${scenario.partition}: literal slot", prefix, preview.getWord(0))
                assertFalse("${scenario.partition}: preview must not autocorrect", preview.mWillAutoCorrect)
                for (index in 1 until preview.size()) {
                    assertTrue("${scenario.partition}: stale candidate ${preview.getWord(index)} for $prefix",
                        preview.getWord(index).startsWith(prefix, ignoreCase = true))
                }
            }
        }
    }

    @Test fun rapidTypingPauseSpaceBackspaceAndEditorSwitchRejectOldResults() {
        val editorSession = EditorSessionGeneration()
        var requestId = 1L
        var current = Request(editorSession.current(), requestId, "t", 1, 1)

        // Rapid typing changes the prefix before the older native result returns.
        val firstKey = current
        current = Request(editorSession.current(), ++requestId, "th", 2, 2)
        assertFalse(isCurrent(firstKey, current))

        // A quiet pause leaves the latest request valid; space and Backspace each
        // create a newer composing state and revoke the prior click targets.
        val afterPause = current
        assertTrue(isCurrent(afterPause, current))
        current = Request(editorSession.current(), ++requestId, "the ", 4, 4)
        assertFalse(isCurrent(afterPause, current))
        val afterSpace = current
        current = Request(editorSession.current(), ++requestId, "the", 3, 3)
        assertFalse(isCurrent(afterSpace, current))

        // Switching fields invalidates all queued results even if the text/caret match.
        val beforeSwitch = current
        val newEditor = editorSession.invalidate()
        current = Request(newEditor, ++requestId, "the", 3, 3)
        assertFalse(isCurrent(beforeSwitch, current))
    }
}
