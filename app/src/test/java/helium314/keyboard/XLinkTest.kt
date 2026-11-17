package helium314.keyboard

import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.App
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.common.Links
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.latin.utils.getKnownDictionariesForLocale
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class XLinkTest { // Without the X, SubtypeTests fail with ClassCastException. WTF?
    @Test fun knownDictionaries() {
        if (BuildConfig.BUILD_TYPE == "runTests") return // don't spam requests to Codeberg on every PR update
        val context = ApplicationProvider.getApplicationContext<App>()
        val urls = mutableSetOf<String>()
        context.assets.open("dictionaries_in_dict_repo.csv").reader().readLines().forEach { line ->
            getKnownDictionariesForLocale(line.split(",")[1].constructLocale(), context).forEach {
                urls.add(it.second)
            }
        }
        // can't check everything at once, this will trigger some rate limit
        val typeToCheck = listOf("/dictionaries_experimental/", "/emoji_cldr_signal_dictionaries/", "/dictionaries/").random()
        urls.forEach {
            if (it.contains(typeToCheck))
            checkLink(it)
        }
    }

    @Test fun readmeLinks() {
        val file = File("../README.md")
        val linkRegex = "(?:https?:\\/\\/.)?(?:www\\.)?[-a-zA-Z0-9@%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b(?:[-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)".toRegex()
        val links = linkRegex.findAll(file.readText())
        links.forEach {
            if (it.value.contains("heli", true))
                checkLink(it.value.trim('.'))
        }
    }

    @Test fun layoutsLinks() {
        val file = File("../layouts.md")
        val linkRegex = "(?:https?:\\/\\/.)?(?:www\\.)?[-a-zA-Z0-9@%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b(?:[-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)".toRegex()
        val links = linkRegex.findAll(file.readText())
        links.forEach {
            if (it.value.contains("heli", true))
                checkLink(it.value)
        }
    }

    @Test fun layoutsLinksInternal() {
        val file = File("../layouts.md")
        val internalLinkRegex = "app/src/\\b(?:[-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)".toRegex()
        val links = internalLinkRegex.findAll(file.readText())
        links.forEach {
            val path = it.value.substringBefore('#').substringBefore('?')
            if (BuildConfig.BUILD_TYPE == "runTests") {
                // Validate repository-local documentation without depending on
                // the public GitHub HTTP HEAD endpoint during release signing.
                assertTrue(File("../$path").exists(), "Missing internal layout link: $path")
            } else {
                checkLink(it.value.replace("app/src", Links.GITHUB + "/blob/main/app/src"))
            }
        }
    }

    @Test fun otherLinks() {
        listOf(Links.LICENSE, Links.LAYOUT_WIKI_URL, Links.WIKI_URL, Links.CUSTOM_LAYOUTS, Links.CUSTOM_COLORS).forEach {
            checkLink(it)
        }
    }

    private fun checkLink(link: String) {
        if (BuildConfig.BUILD_TYPE == "runTests") {
            // External availability is not deterministic in CI: GitHub/Codeberg
            // can rate-limit HEAD or fail transiently. Still validate each URL's
            // structure, leaving live reachability tests for other build types.
            val url = URL(link)
            assertTrue(url.protocol == "http" || url.protocol == "https",
                "Unsupported link protocol: $link")
            assertTrue(url.host.isNotBlank(), "Missing link host: $link")
            return
        }
        if (link.contains("wiki/"))
            return checkWikiLink(link)
        val url = URL(link)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        if (connection.responseCode != 200)
            println("error checking $link")
        assertEquals(200, connection.responseCode)
    }

    private fun checkWikiLink(link: String) {
        val url = URL(link)
        val connection = url.openConnection() as HttpURLConnection
        if (connection.responseCode != 200)
            println("error checking $link")
        assertEquals(200, connection.responseCode)
        val text = connection.getInputStream().reader().readText()
        if ("Create new page" in text)
            println("error checking wiki $link")
        assert("Create new page" !in text)
    }
}
