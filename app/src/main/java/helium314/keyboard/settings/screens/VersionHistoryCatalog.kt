// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class VersionHistoryChange(val title: String, val detail: String)

data class VersionHistoryEntry(
    val version: String,
    val releaseTimeUtc: String,
    val summary: String,
    val commit: String,
    val changes: List<VersionHistoryChange>,
    val isPublicationTime: Boolean,
)

/** Built at release time from signed GitHub releases. Never requires app network access. */
object VersionHistoryCatalog {
    fun load(context: Context): List<VersionHistoryEntry> = runCatching {
        context.assets.open("version_history.json").bufferedReader(Charsets.UTF_8).use {
            parse(it.readText())
        }
    }.getOrDefault(emptyList())

    fun parse(json: String): List<VersionHistoryEntry> {
        val root = JSONObject(json)
        if (root.optInt("schema") != 1) return emptyList()
        val records = root.optJSONArray("releases") ?: return emptyList()
        val results = ArrayList<VersionHistoryEntry>(records.length().coerceAtMost(100))
        val seen = HashSet<String>()
        for (i in 0 until records.length().coerceAtMost(300)) {
            val release = records.optJSONObject(i) ?: continue
            val version = release.optString("version").trim()
            val time = release.optString("releaseTimeUtc").trim()
            val summary = release.optString("summary").trim()
            if (!version.matches(Regex("1\\.0\\.[0-9]+")) || !validTime(time) ||
                summary.isBlank() || !seen.add(version)) continue
            val details = release.optJSONArray("changes") ?: continue
            val changes = buildList {
                for (j in 0 until details.length().coerceAtMost(30)) {
                    val change = details.optJSONObject(j) ?: continue
                    val title = change.optString("title").trim()
                    val detail = change.optString("detail").trim()
                    if (title.isNotBlank() && detail.isNotBlank())
                        add(VersionHistoryChange(title.take(100), detail.take(800)))
                }
            }
            if (changes.isEmpty()) continue
            results.add(VersionHistoryEntry(version, time, summary.take(500),
                release.optString("commit").take(40), changes,
                release.optString("timeSource") == "github_published"))
        }
        return results.sortedByDescending { it.version.substringAfterLast('.').toIntOrNull() ?: -1 }
    }

    fun localTime(entry: VersionHistoryEntry, locale: Locale = Locale.getDefault(),
                  zone: TimeZone = TimeZone.getDefault()): String {
        val instant = utcParser().parse(entry.releaseTimeUtc) ?: return entry.releaseTimeUtc
        return SimpleDateFormat("d MMM yyyy · h:mm a z", locale).apply { timeZone = zone }
            .format(instant)
    }

    private fun validTime(value: String): Boolean = try {
        value.length == 20 && utcParser().parse(value) != null
    } catch (_: Exception) { false }

    private fun utcParser() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
}
