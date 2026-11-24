// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic

import java.util.Locale

/** Bounded literal replacement only: no regex, no clipboard, no host access. */
object LiteralTextReplacement {
    const val MAX_RULES = 12
    const val MAX_NAME = 48
    const val MAX_FIND = 128
    const val MAX_REPLACEMENT = 512
    data class Rule(val name: String, val find: String, val replacement: String,
                    val matchCase: Boolean = true, val wholeWord: Boolean = false, val all: Boolean = true)
    data class Result(val text: String, val count: Int)
    fun validRules(rules: List<Rule>): Boolean = rules.size <= MAX_RULES &&
        rules.map { it.name.trim().lowercase(Locale.ROOT) }.distinct().size == rules.size &&
        rules.all { it.name.isNotBlank() && it.name.length <= MAX_NAME && it.name.none(Char::isISOControl) &&
            it.find.length in 1..MAX_FIND && it.replacement.length <= MAX_REPLACEMENT &&
            validUnicode(it.name) && validUnicode(it.find) && validUnicode(it.replacement) }
    fun validUnicode(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val c = s[i++]
            if (Character.isHighSurrogate(c)) {
                if (i >= s.length || !Character.isLowSurrogate(s[i++])) return false
            } else if (Character.isLowSurrogate(c)) return false
        }
        return true
    }
    private fun word(cp: Int): Boolean = Character.isLetterOrDigit(cp) ||
        cp == 0x200C || cp == 0x200D || when (Character.getType(cp)) {
            Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(), Character.CONNECTOR_PUNCTUATION.toInt() -> true
            else -> false
        }
    fun replace(source: String, find: String, replacement: String,
                matchCase: Boolean, wholeWord: Boolean, all: Boolean): Result? {
        val cap = ContextualTextActions.MAX_CHARS
        if (source.length !in 1..cap || find.length !in 1..MAX_FIND ||
            replacement.length > MAX_REPLACEMENT ||
            !validUnicode(source) || !validUnicode(find) || !validUnicode(replacement)) return null
        val out = StringBuilder(source.length)
        var i = 0
        var count = 0
        while (i < source.length) {
            val end = i + find.length
            val endBoundary = end >= source.length || !Character.isLowSurrogate(source[end])
            val match = end <= source.length && endBoundary &&
                source.regionMatches(i,find,0,find.length,ignoreCase=!matchCase) &&
                (!wholeWord || ((i == 0 || !word(source.codePointBefore(i))) &&
                    (end == source.length || !word(source.codePointAt(end)))))
            if (match && (all || count == 0)) {
                out.append(replacement)
                count++
                i = end
            } else {
                val cp = source.codePointAt(i)
                out.appendCodePoint(cp)
                i += Character.charCount(cp)
            }
            if (out.length > cap) return null
        }
        return if (count == 0 || out.toString() == source) null else Result(out.toString(),count)
    }
}
