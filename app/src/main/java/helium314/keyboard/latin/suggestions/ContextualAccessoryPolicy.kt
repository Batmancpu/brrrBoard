// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import helium314.keyboard.latin.ClipboardEntityExtractor
import helium314.keyboard.latin.brain.SmartPastePolicy
import helium314.keyboard.latin.inputlogic.ContextualTextActions
import java.util.Locale

/** Explicit actions only for an unambiguous whole copy. No mixed-message extraction. */
object ContextualAccessoryPolicy {
    enum class Kind { DIAL, MAIL, MAP, CLEAN_TEXT, CASE, CALENDAR, TRACK, EXTRACT }
    data class Action(val kind: Kind, val value: String)
    fun choose(text: String, destination: SmartPastePolicy.Destination): Action? {
        if (destination != SmartPastePolicy.Destination.GENERAL &&
            destination != SmartPastePolicy.Destination.SEARCH) return null
        if (text.isBlank() || text.length > 2048 ||
            ClipboardEntityExtractor.excludeFromAutomaticHistory(text)) return null
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
            SmartPastePolicy.Kind.DATE -> Action(Kind.CALENDAR, text)
            SmartPastePolicy.Kind.TRACKING ->
                if (text.matches(Regex("(?i)1Z[A-Z0-9]{16}"))) Action(Kind.TRACK, text.uppercase(Locale.ROOT)) else null
            SmartPastePolicy.Kind.TEXT -> {
                if (destination == SmartPastePolicy.Destination.GENERAL && candidates.drop(1).any {
                        it.kind != SmartPastePolicy.Kind.OTP && it.kind != SmartPastePolicy.Kind.RICH &&
                        it.end - it.start in 1..512
                    }) return Action(Kind.EXTRACT, "")
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
