// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.snippets

/** Content-free, monotonic eligibility. One fixed deadline per rule and app visit. */
internal enum class SnippetCadence { SESSION, DAILY, KEYBOARD }
internal data class SnippetRule(
    val id: Long, val fingerprint: String, val title: String = "Saved snippet",
    val packages: Set<String> = emptySet(), val allApps: Boolean = false,
    val cadence: SnippetCadence = SnippetCadence.DAILY,
    val durationSeconds: Int = 120, val emptyAgain: Boolean = true,
    val newLine: Boolean = true, val enabled: Boolean = true,
    val usedDay: Int = 0, val days: Int = 127,
    val startHour: Int = 0, val endHour: Int = 24
) {
    fun matches(pkg: String) = enabled && (allApps || pkg in packages)
    fun scheduled(dayOfWeek: Int, hour: Int): Boolean {
        if (dayOfWeek !in 1..7 || hour !in 0..23) return false
        val overnight = startHour > endHour
        val startDay = if (overnight && hour < endHour) (if (dayOfWeek == 1) 7 else dayOfWeek - 1)
            else dayOfWeek
        return days and (1 shl (startDay - 1)) != 0 &&
            (if (startHour < endHour) hour in startHour until endHour
            else if (overnight) hour >= startHour || hour < endHour else true)
    }
    fun valid(): Boolean = id > 0 && fingerprint.matches(Regex("[0-9a-f]{64}")) &&
        title.isNotBlank() && title.length <= 80 &&
        packages.size <= 64 && packages.all { it.length <= 200 && it.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) } &&
        (allApps || packages.isNotEmpty()) && durationSeconds in setOf(30, 60, 120, 300) &&
        days in 1..127 && startHour in 0..23 && endHour in 0..24
}

internal enum class SnippetBoundary { NONE, EMPTY, NEW_LINE }

internal class SmartSnippetSession {
    private data class State(val deadline: Long, var shown: Boolean = false, var consumed: Boolean = false)
    private var packageName: String? = null
    private var hiddenAt: Long? = null
    private val states = HashMap<Long, State>()

    fun enter(pkg: String, now: Long, rules: List<SnippetRule>) {
        val hidden = hiddenAt
        if (packageName != pkg || (hidden != null && now - hidden >= 300_000L)) {
            states.clear()
        } else if (hidden != null) {
            rules.filter { it.cadence == SnippetCadence.KEYBOARD }.forEach { states.remove(it.id) }
        }
        packageName = pkg
        hiddenAt = null
        states.keys.retainAll(rules.map { it.id }.toSet())
    }

    fun hide(now: Long) { if (hiddenAt == null) hiddenAt = now }
    fun reset() { packageName = null; hiddenAt = null; states.clear() }
    fun forget(id: Long) { states.remove(id) }
    fun consume(id: Long) { states[id]?.consumed = true }
    fun deadline(id: Long) = states[id]?.deadline ?: 0L
    fun markShown(id: Long) { states[id]?.shown = true }

    fun eligible(rule: SnippetRule, pkg: String, now: Long, today: Int,
                 dayOfWeek: Int, hour: Int, boundary: SnippetBoundary, currentlyVisible: Boolean): Boolean {
        if (pkg != packageName || hiddenAt != null || !rule.matches(pkg) ||
            !rule.scheduled(dayOfWeek, hour) ||
            (rule.cadence == SnippetCadence.DAILY && rule.usedDay == today)) return false
        // Never renew a visit on repeated callbacks, typing, field changes or deletion.
        val state = states.getOrPut(rule.id) { State(now + rule.durationSeconds * 1000L) }
        if (state.consumed || now >= state.deadline || boundary == SnippetBoundary.NONE) return false
        return when (boundary) {
            SnippetBoundary.EMPTY -> currentlyVisible || !state.shown || rule.emptyAgain
            SnippetBoundary.NEW_LINE -> rule.newLine
            SnippetBoundary.NONE -> false
        }
    }
}
