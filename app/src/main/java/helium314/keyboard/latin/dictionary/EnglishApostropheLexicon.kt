// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.dictionary

import android.content.Context
import com.android.inputmethod.latin.BinaryDictionary
import helium314.keyboard.latin.common.LocaleUtils
import helium314.keyboard.latin.utils.DictionaryInfoUtils
import helium314.keyboard.latin.utils.Log
import java.io.File
import java.util.Locale

/** An additive, explicitly reviewed English repair. Never rewrites a main dictionary. */
internal object EnglishApostropheLexicon {
    private const val ASSET = "brrPersonal_english_contractions.tsv"
    private const val MAX_ENTRIES = 96
    private val wordPattern = Regex("[A-Za-z]+(?:'[a-z]+){1,2}")
    data class Entry(val word: String, val frequency: Int)
    data class Audit(val missing: List<Entry>, val malformedAliases: Set<String>)

    // Do not remove legitimate words, names, regional spellings, abbreviations or I/ID.
    private val removableAliases = setOf(
        "dont", "doesnt", "didnt", "couldnt", "wouldnt", "shouldnt", "isnt", "arent",
        "wasnt", "werent", "havent", "hasnt", "hadnt", "youre", "theyre", "youve",
        "weve", "theyve", "couldve", "wouldve", "shouldve", "mightve", "mustve",
        "youll", "theyll", "neednt", "mustnt", "mightnt", "oughtnt", "shant"
    )

    private val ambiguousAliases = setOf(
        "cant", "wont", "were", "well", "ill", "its", "lets", "hell", "shell",
        "shed", "wed", "id", "im", "ive", "hes", "shes", "whos", "thats", "whats",
        "hows", "heres", "theres", "wheres", "whens", "maam", "yall", "oclock",
        "theyd", "youd", "hed", "itd"
    )

    /** Only guard a new supplement-sourced candidate; native main decisions stay unchanged. */
    @JvmStatic
    fun isAmbiguousOmission(typed: String, candidate: String?): Boolean {
        val lower = typed.lowercase(Locale.ROOT)
        if (lower !in ambiguousAliases || candidate == null) return false
        val canonical = candidate.replace('\u2019', '\'')
        return canonical.contains('\'') &&
            canonical.replace("'", "").lowercase(Locale.ROOT) == lower
    }

    fun parse(lines: Sequence<String>): List<Entry> {
        val entries = ArrayList<Entry>()
        val seen = HashSet<String>()
        for (line in lines) {
            if (line.isBlank() || line.startsWith("#")) continue
            val fields = line.split('\t')
            require(fields.size == 2)
            val word = fields[0]
            val frequency = fields[1].toInt()
            require(word.length <= 24 && wordPattern.matches(word))
            require(word == word.lowercase(Locale.ROOT) || word.startsWith("I'"))
            require(frequency in 32..96 && seen.add(word.lowercase(Locale.ROOT)))
            entries.add(Entry(word, frequency))
            require(entries.size <= MAX_ENTRIES)
        }
        require(entries.isNotEmpty())
        return entries
    }

    /** Pure gate, shared by native audit and tests. Preserve every existing native word. */
    fun plan(entries: List<Entry>, contains: (String) -> Boolean): Audit {
        val missing = ArrayList<Entry>()
        val aliases = HashSet<String>()
        for (entry in entries) {
            val word = entry.word
            val lower = word.lowercase(Locale.ROOT)
            if (!contains(word) && !contains(lower) &&
                !contains(word.replace('\'', '\u2019')) &&
                !contains(lower.replace('\'', '\u2019'))) {
                missing.add(entry)
            }
            val omitted = lower.replace("'", "")
            if (omitted in removableAliases && !contains(omitted)) aliases.add(omitted)
        }
        return Audit(missing, aliases)
    }

    /**
     * Called once on the existing static dictionary cache-build worker, never per keystroke.
     * Copy only a public bundled dictionary to a private temporary file, open read-only with
     * the exact native decoder, then close/delete it. If audit fails, keep the original seed.
     * Imported main dictionaries are not opened, changed, or copied by this helper.
     */
    @JvmStatic
    fun audit(context: Context, locale: Locale): Audit {
        val empty = Audit(emptyList(), emptySet())
        if (locale.language != "en") return empty
        return try {
            val entries = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use {
                parse(it.lineSequence())
            }
            val names = DictionaryInfoUtils.getAssetsDictionaryList(context).orEmpty()
                .filter { it.startsWith("main_en") && it.endsWith(".dict") }
            val name = LocaleUtils.getBestMatch(locale, names) {
                DictionaryInfoUtils.extractLocaleFromAssetsDictionaryFile(it)
            } ?: return empty
            val temporary = File.createTempFile("brr-apostrophe-audit-", ".dict", context.cacheDir)
            try {
                context.assets.open("dicts/$name").use { input ->
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                val native = BinaryDictionary(temporary.absolutePath, 0L, temporary.length(),
                    false, locale, Dictionary.TYPE_MAIN, false)
                try {
                    if (!native.isValidDictionary) return empty
                    val result = plan(entries) { native.isInDictionary(it) }
                    Log.i("EnglishApostropheLexicon", "Reviewed ${entries.size} bundled English entries; " +
                        "missing ${result.missing.size}; safe alias exclusions ${result.malformedAliases.size}")
                    result
                } finally {
                    native.close()
                }
            } finally {
                temporary.delete()
            }
        } catch (_: Exception) {
            Log.w("EnglishApostropheLexicon", "Bundled apostrophe audit unavailable; preserving original seed")
            empty
        }
    }
}
