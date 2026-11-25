// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/** Bounded whole-copy event parser. Never guesses a numeric date order or a timezone. */
object CalendarAccessoryPolicy {
    data class Draft(val title: String?, val beginMillis: Long, val endMillis: Long, val allDay: Boolean)
    private val months = listOf("january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december")
    private val month = "(?:" + months.joinToString("|") { it + "|" + it.take(3) } + ")"
    private val date = "(?:[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{1,2} $month [0-9]{4}|$month [0-9]{1,2},? [0-9]{4}|today|tomorrow)"
    private val time = "(?:[0-9]{1,2}(?::[0-9]{2})?\\s*(?:am|pm)|[0-9]{2}:[0-9]{2})"
    private val event = Regex("(?i)^(?:(.{1,80}?) (?:on )?)?($date)(?: at ($time)(?:\\s*(?:to|[-–])\\s*($time))?)?$")
    private val iso = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
    private val dayFirst = Regex("(?i)^([0-9]{1,2}) ($month) ([0-9]{4})$")
    private val monthFirst = Regex("(?i)^($month) ([0-9]{1,2}),? ([0-9]{4})$")
    private val clock = Regex("(?i)^([0-9]{1,2})(?::([0-9]{2}))?\\s*(am|pm)?$")
    private val titleShape = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N} &'().,-]{0,79}$")
    private val negation = Regex("(?i)\\b(?:cancelled|canceled|cancel|postponed|not|maybe|tentative|instead|until|before|after|from|if)\\b")
    private fun monthNumber(value: String): Int =
        months.indexOfFirst { it.startsWith(value.lowercase(Locale.ROOT)) } + 1
    private fun parseDate(value: String, today: LocalDate): LocalDate? = when (value.lowercase(Locale.ROOT)) {
        "today" -> today
        "tomorrow" -> today.plusDays(1)
        else -> {
            if (iso.matches(value)) LocalDate.parse(value)
            else dayFirst.matchEntire(value)?.let { LocalDate.of(it.groupValues[3].toInt(),
                monthNumber(it.groupValues[2]), it.groupValues[1].toInt()) }
                ?: monthFirst.matchEntire(value)?.let { LocalDate.of(it.groupValues[3].toInt(),
                    monthNumber(it.groupValues[1]), it.groupValues[2].toInt()) }
        }
    }
    private fun parseTime(value: String): LocalTime? {
        val m = clock.matchEntire(value) ?: return null
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].ifEmpty { "0" }.toInt()
        val meridiem = m.groupValues[3].lowercase(Locale.ROOT)
        return if (meridiem.isNotEmpty()) {
            if (h !in 1..12) null else LocalTime.of(h % 12 + if (meridiem == "pm") 12 else 0, min)
        } else {
            if (m.groupValues[2].isEmpty() || m.groupValues[1].length != 2) null else LocalTime.of(h, min)
        }
    }
    private fun instant(local: LocalDateTime, zone: ZoneId): Long? {
        // DST gap or repeated wall time: do not guess which instant was meant.
        val offset = zone.rules.getValidOffsets(local).singleOrNull() ?: return null
        return local.toInstant(offset).toEpochMilli()
    }
    fun choose(text: String, today: LocalDate, zone: ZoneId): Draft? {
        if (text.length !in 1..180 || text.any { it.isISOControl() } ||
            text != text.trim() || negation.containsMatchIn(text)) return null
        val m = event.matchEntire(text) ?: return null
        val title = m.groupValues[1].ifBlank { null }
        // A title is allowed only when it is separated by "on", or is
        // followed by an explicit time. Do not interpret arbitrary prose.
        if (title != null && (!titleShape.matches(title) ||
            (m.groupValues[3].isEmpty() && !text.startsWith("$title on ", ignoreCase = true)))) return null
        if (title != null && Regex("(?i)\\b(?:today|tomorrow|am|pm|at|on|mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\\b").containsMatchIn(title)) return null
        return try {
            val day = parseDate(m.groupValues[2], today) ?: return null
            if (day.year !in 1900..2099) return null
            val startText = m.groupValues[3]
            if (startText.isEmpty()) {
                // UTC midnight is Android Calendar's all-day event convention.
                Draft(title, day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                    day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(), true)
            } else {
                val start = day.atTime(parseTime(startText) ?: return null)
                val endText = m.groupValues[4]
                val end = if (endText.isEmpty()) start.plusHours(1)
                    else day.atTime(parseTime(endText) ?: return null)
                if (!end.isAfter(start)) return null
                val begin = instant(start, zone) ?: return null
                val finish = instant(end, zone) ?: return null
                if (finish <= begin) null else Draft(title, begin, finish, false)
            }
        } catch (_: DateTimeException) { null }
    }
}
