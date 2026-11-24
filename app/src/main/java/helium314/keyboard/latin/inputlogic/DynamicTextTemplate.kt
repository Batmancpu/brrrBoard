// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Explicit snippets only; no executable expressions, automatic expansion or clipboard reads. */
object DynamicTextTemplate {
    const val MAX_TEMPLATES = 12
    const val MAX_NAME = 48
    const val MAX_BODY = 512
    data class Template(val name: String, val body: String)
    data class Rendered(val text: String, val cursor: Int)

    fun valid(template: Template): Boolean =
        template.name.isNotBlank() && template.name.length <= MAX_NAME &&
        template.name.none { it.isISOControl() } &&
        template.body.isNotBlank() && template.body.length <= MAX_BODY &&
        template.body.indexOf("{cursor}") == template.body.lastIndexOf("{cursor}") &&
        render(template.body, "2000-01-01", "12:00") != null

    fun validList(templates: List<Template>): Boolean =
        templates.size <= MAX_TEMPLATES && templates.all(::valid) &&
        templates.map { it.name.trim().lowercase(Locale.ROOT) }.distinct().size == templates.size

    fun render(body: String, date: String, time: String): Rendered? {
        if (body.length !in 1..MAX_BODY || date.length > 64 || time.length > 64) return null
        val out = StringBuilder(body.length)
        var cursor = -1
        var i = 0
        while (i < body.length) {
            when {
                body.startsWith("{date}", i) -> { out.append(date); i += 6 }
                body.startsWith("{time}", i) -> { out.append(time); i += 6 }
                body.startsWith("{cursor}", i) -> {
                    if (cursor >= 0) return null
                    cursor = out.length; i += 8
                }
                else -> { out.append(body[i]); i++ }
            }
            if (out.length > MAX_BODY) return null
        }
        if (out.isEmpty()) return null
        // Never split a surrogate pair even if malformed input surrounds a marker.
        if (cursor > 0 && cursor < out.length &&
            Character.isHighSurrogate(out[cursor-1]) && Character.isLowSurrogate(out[cursor])) return null
        return Rendered(out.toString(), if (cursor < 0) out.length else cursor)
    }
    fun renderNow(template: Template, locale: Locale, now: Date = Date()): Rendered? =
        render(template.body, DateFormat.getDateInstance(DateFormat.SHORT,locale).format(now),
            DateFormat.getTimeInstance(DateFormat.SHORT,locale).format(now))
}
