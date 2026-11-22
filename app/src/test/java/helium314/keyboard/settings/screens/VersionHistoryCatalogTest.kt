// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VersionHistoryCatalogTest {
    @Test fun newestNumericVersionIsDefaultEvenWhenManifestOrderIsWrong() {
        val fixture = """
            {"schema":1,"releases":[
              {"version":"1.0.99","releaseTimeUtc":"2026-11-20T09:00:00Z",
               "timeSource":"github_published","summary":"Older update",
               "changes":[{"title":"Previous behavior","detail":"A verified older feature."}]},
              {"version":"1.0.187","releaseTimeUtc":"2026-11-20T10:00:00Z",
               "timeSource":"release_cut","summary":"Newer update",
               "changes":[{"title":"Release history","detail":"An offline version selector."}]},
              {"version":"1.0.187","releaseTimeUtc":"2026-11-20T11:00:00Z",
               "summary":"Duplicate record","changes":[{"title":"Duplicate","detail":"Should be ignored."}]},
              {"version":"evil","releaseTimeUtc":"2026-11-20T10:00:00Z",
               "summary":"Bad record","changes":[{"title":"Invalid","detail":"Should be ignored."}]}
            ]}
        """.trimIndent()
        val entries = VersionHistoryCatalog.parse(fixture)
        assertEquals(listOf("1.0.187", "1.0.99"), entries.map { it.version })
        assertEquals("Release history", entries[0].changes[0].title)
    }

    @Test fun releaseUtcTimeIsPresentedInTheOwnersLocalTimeZone() {
        val entry = VersionHistoryEntry("1.0.186", "2026-11-20T09:52:58Z",
            "Signed build", "a".repeat(40), listOf(VersionHistoryChange("Safe", "Detail")), true)
        val shown = VersionHistoryCatalog.localTime(entry, Locale.US, TimeZone.getTimeZone("Asia/Kolkata"))
        assertTrue(shown.contains("20 Nov 2026"))
        assertTrue(shown.contains("3:22 PM"))
    }

    @Test fun malformedEntriesCannotReplaceTheLatestValidHistory() {
        assertTrue(VersionHistoryCatalog.parse("""{"schema":2,"releases":[]}""").isEmpty())
        assertTrue(VersionHistoryCatalog.parse("""{"schema":1,"releases":[
          {"version":"1.0.188","releaseTimeUtc":"tomorrow", "summary":"Fake", "changes":[]}
        ]}""").isEmpty())
    }

    @Test fun historyShowsOnlyTheLatestTenSignedUpdates() {
        val records = (180..194).reversed().joinToString(",") { version ->
            """{"version":"1.0.$version","releaseTimeUtc":"2026-11-20T09:00:00Z","summary":"Update $version","changes":[{"title":"Change","detail":"Verified release note."}]}"""
        }
        val entries = VersionHistoryCatalog.parse("""{"schema":1,"releases":[$records]}""")
        assertEquals(10, entries.size)
        assertEquals("1.0.194", entries.first().version)
        assertEquals("1.0.185", entries.last().version)
    }
}
