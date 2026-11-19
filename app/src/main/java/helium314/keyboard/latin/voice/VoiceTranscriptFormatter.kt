// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import java.util.Locale

/**
 * Tiny, deterministic English casing pass. The recognizer remains authoritative on
 * which words were actually spoken: this never changes phonetics, punctuation,
 * numbers, whitespace, or word boundaries and never reads personal dictionaries.
 *
 * Do not add phonetic or two-word substitutions here. The keyboard's existing
 * dictionary and Emoji suggestions run normally after transcript insertion.
 */
internal object VoiceTranscriptFormatter {
    private val canonicalWords = mapOf(
        "android" to "Android",
        "brrboard" to "brrBoard",
        "brrspeak" to "brrSpeak",
        "github" to "GitHub",
        "kotlin" to "Kotlin",
        "obtainium" to "Obtainium",
    )
    // Exclude URLs/email addresses without treating ordinary sentence-ending periods
    // as part of a domain. For example, "android." is a word, "android.com" is not.
    private val tokens = Regex("(?<![\\p{L}\\p{N}.@])[A-Za-z]+(?![\\p{L}\\p{N}@]|\\.[A-Za-z0-9])")

    fun format(text: String): String = tokens.replace(text) { match ->
        canonicalWords[match.value.lowercase(Locale.ROOT)] ?: match.value
    }
}
