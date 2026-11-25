// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import helium314.keyboard.latin.ClipboardEntityExtractor
import helium314.keyboard.latin.brain.SmartPastePolicy
import helium314.keyboard.latin.inputlogic.ContextualTextActions
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId

/** Explicit whole-copy actions; mixed copies may open only a guarded entity picker. */
object ContextualAccessoryPolicy {
    enum class Kind { DIAL, MAIL, MAP, CLEAN_TEXT, CASE, CALENDAR, TRACK, EXTRACT }

    /** Only specific bounded entities; no raw content is retained or learned. */
    fun extractCandidates(text: String, candidates: List<SmartPastePolicy.Candidate> = SmartPastePolicy.classify(text)):
        List<SmartPastePolicy.Candidate> {
        if (ClipboardEntityExtractor.excludeFromAutomaticHistory(text)) return emptyList()
        return candidates.drop(1).filter { c ->
            c.start >= 0 && c.end <= text.length && c.end - c.start in 1..512 &&
                when (c.kind) {
                    SmartPastePolicy.Kind.PHONE -> {
                        val part = text.substring(c.start, c.end)
                        val digits = part.count { it in '0'..'9' }
                        digits in 10..15 && (part.startsWith("+") || part.any { it in " ()-" })
                    }
                    SmartPastePolicy.Kind.EMAIL, SmartPastePolicy.Kind.ADDRESS, SmartPastePolicy.Kind.DATE,
                    SmartPastePolicy.Kind.TRACKING, SmartPastePolicy.Kind.IDENTIFIER,
                    SmartPastePolicy.Kind.LINK -> true
                    else -> false
                }
        }.distinctBy { it.start to it.end }.sortedWith(compareBy<SmartPastePolicy.Candidate> {
            when (it.kind) {
                SmartPastePolicy.Kind.EMAIL, SmartPastePolicy.Kind.LINK, SmartPastePolicy.Kind.TRACKING -> 0
                SmartPastePolicy.Kind.PHONE, SmartPastePolicy.Kind.ADDRESS, SmartPastePolicy.Kind.DATE -> 1
                else -> 2
            }
        }.thenBy { it.start }).take(8)
    }
    data class Action(val kind: Kind, val value: String, val carrier: String? = null,
        val calendar: CalendarAccessoryPolicy.Draft? = null)
    fun choose(text: String, destination: SmartPastePolicy.Destination,
        today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Action? {
        if (destination != SmartPastePolicy.Destination.GENERAL &&
            destination != SmartPastePolicy.Destination.SEARCH) return null
        if (text.isBlank() || text.length > 2048 ||
            ClipboardEntityExtractor.excludeFromAutomaticHistory(text)) return null
        ParcelAccessoryPolicy.choose(text)?.let { return Action(Kind.TRACK, it.url, it.carrier) }
        CalendarAccessoryPolicy.choose(text, today, zone)?.let { return Action(Kind.CALENDAR, "", calendar = it) }
        val candidates = SmartPastePolicy.classify(text)
        val whole = candidates.firstOrNull() ?: return null
        return when (whole.kind) {
            SmartPastePolicy.Kind.EMAIL -> if (text.length <= 254) Action(Kind.MAIL, text) else null
            SmartPastePolicy.Kind.PHONE -> {
                val number = text.filter { it in '0'..'9' || it == '+' }
                if (number.matches(Regex("\\+?[0-9]{10,15}")) &&
                    (text.startsWith("+") || text.any { it == ' ' || it == '(' || it == '-' }))
                    Action(Kind.DIAL, number) else null
            }
            SmartPastePolicy.Kind.ADDRESS -> if (text.length <= 160) Action(Kind.MAP, text) else null
            SmartPastePolicy.Kind.DATE -> null
            SmartPastePolicy.Kind.TRACKING -> null
            SmartPastePolicy.Kind.TEXT -> {
                if (destination == SmartPastePolicy.Destination.GENERAL && extractCandidates(text, candidates).isNotEmpty())
                    return Action(Kind.EXTRACT, "")
                val prose = text.length <= 512 && text.count { it == ' ' } >= 2 &&
                    text.all { it.isLetter() || it == ' ' || it in ".,!?'-" } &&
                    text.any { it.isUpperCase() } && text.none { it.isLowerCase() }
                if (destination == SmartPastePolicy.Destination.GENERAL && prose)
                    return ContextualTextActions.transform(text, ContextualTextActions.Action.SENTENCE,
                        Locale.ROOT)?.let { Action(Kind.CASE, it) }
                if (candidates.size != 1 || destination != SmartPastePolicy.Destination.GENERAL ||
                    text.any { it == '\n' || it == '\r' || it in "{}<>=" } ||
                    !Regex("\\p{L}{2,} +\\p{L}{2,}").containsMatchIn(text) ||
                    !Regex(" {2,}").containsMatchIn(text)) null
                else ContextualTextActions.transform(text, ContextualTextActions.Action.TRIM_SPACES,
                    Locale.ROOT)?.let { Action(Kind.CLEAN_TEXT, it) }
            }
            else -> null
        }
    }
}
