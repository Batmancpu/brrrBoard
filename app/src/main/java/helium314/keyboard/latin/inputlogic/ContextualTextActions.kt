// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic

import java.text.BreakIterator
import java.util.Locale

/** Pure bounded transformations. No host, network, storage or clipboard access. */
object ContextualTextActions {
    const val MAX_CHARS = 2048
    enum class Action(val label: String) {
        UPPER("Uppercase"), LOWER("Lowercase"), TITLE("Title case"), SENTENCE("Sentence case"),
        TRIM_SPACES("Trim extra spaces"), REMOVE_EMPTY("Remove empty lines"), JOIN("Join lines"),
        DEDUP("Remove duplicate lines"), SORT("Sort lines"), BULLETS("Bullet list"), QUOTE("Quote lines")
    }
    private fun firstUpper(value: String, locale: Locale): String {
        var i = 0
        while (i < value.length) {
            val cp = value.codePointAt(i)
            val next = i + Character.charCount(cp)
            if (Character.isLetter(cp)) return value.substring(0,i) +
                value.substring(i,next).uppercase(locale) + value.substring(next)
            i = next
        }
        return value
    }
    private fun applyBoundaries(value: String, locale: Locale, sentence: Boolean): String {
        val iterator = if (sentence) BreakIterator.getSentenceInstance(locale) else BreakIterator.getWordInstance(locale)
        iterator.setText(value)
        val out = StringBuilder(value.length)
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            out.append(firstUpper(value.substring(start,end).lowercase(locale),locale))
            start = end; end = iterator.next()
        }
        return out.toString()
    }
    private fun lineSeparator(value: String): String? {
        val crlf = value.contains("\r\n")
        val rest = value.replace("\r\n","")
        val cr = rest.contains('\r')
        val lf = rest.contains('\n')
        if (listOf(crlf,cr,lf).count { it } > 1) return null
        return if (crlf) "\r\n" else if (cr) "\r" else "\n"
    }
    fun transform(value: String, action: Action, locale: Locale): String? {
        if (value.isEmpty() || value.length > MAX_CHARS) return null
        val result = when (action) {
            Action.UPPER -> value.uppercase(locale)
            Action.LOWER -> value.lowercase(locale)
            Action.TITLE -> applyBoundaries(value,locale,false)
            Action.SENTENCE -> applyBoundaries(value,locale,true)
            Action.TRIM_SPACES -> {
                val sep = lineSeparator(value) ?: return null
                value.split(sep).joinToString(sep) { it.replace(Regex("[ \\t]+"), " ").trim(' ','\t') }
            }
            Action.JOIN -> value.replace(Regex("(?:\\r\\n|\\r|\\n)+"), " ").replace(Regex("[ \\t]+")," ").trim()
            else -> {
                val sep = lineSeparator(value) ?: return null
                val trailing = value.endsWith(sep)
                val lines = value.removeSuffix(if(trailing) sep else "").split(sep)
                val mapped = when(action) {
                    Action.REMOVE_EMPTY -> lines.filter { it.isNotBlank() }
                    Action.DEDUP -> lines.distinct()
                    Action.SORT -> lines.sortedWith(java.text.Collator.getInstance(locale))
                    Action.BULLETS -> lines.map { if(it.isBlank()) it else "• $it" }
                    Action.QUOTE -> lines.map { if(it.isBlank()) it else "> $it" }
                    else -> return null
                }
                mapped.joinToString(sep) + if (trailing && mapped.isNotEmpty()) sep else ""
            }
        }
        return result.takeIf { it.length <= MAX_CHARS && it != value }
    }
}
