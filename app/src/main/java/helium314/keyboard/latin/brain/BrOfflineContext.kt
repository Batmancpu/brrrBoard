// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain

import android.content.Context
import helium314.keyboard.latin.NgramContext
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo
import helium314.keyboard.latin.dictionary.Dictionary
import helium314.keyboard.latin.dictionary.EnglishApostropheLexicon
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Immutable, public, compact n-gram tables. Editor text is never kept by this object. */
internal object BrOfflineContext {
    internal const val VERSION = "brr-context-1"
    private const val ASSET = "contextlm/production.tsv"
    private const val MAX_ROWS = 20_000
    private const val MAX_BYTES = 768 * 1024
    private const val MAX_CANDIDATES = 8
    private const val PREDICTION_FLOOR = 110
    private const val SECOND_PREDICTION_FLOOR = 105
    private const val MANUAL_RANK_FLOOR = 126
    private const val DEADLINE_NS = 5_000_000L
    private val tokenPattern = Regex("[a-z]{1,24}(?:['-][a-z]{1,12}){0,2}")
    private val modelLoadStarted = AtomicBoolean()
    private val fallbacks = AtomicLong()
    private val timedOut = AtomicLong()
    private val accepted = AtomicLong()
    @Volatile private var loadState = "not loaded"
    @Volatile private var model: Model? = null

    internal data class Choice(val surface: String, val weight: Int)
    internal class Model(
        private val english: Map<String, List<Choice>>,
        private val mixed: Map<String, List<Choice>>,
    ) {
        fun lookup(context: List<String>, preferMixed: Boolean): List<Choice> {
            for (start in context.indices) {
                val key = context.subList(start, context.size).joinToString(" ")
                val choices = if (preferMixed) mixed[key] ?: english[key] else english[key]
                if (choices != null) return choices
            }
            return emptyList()
        }
    }

    fun warm(context: Context) {
        if (!modelLoadStarted.compareAndSet(false, true)) return
        val app = context.applicationContext
        loadState = "loading"
        Thread({
            EnglishApostropheLexicon.warmReview(app)
            try {
                val bytes = app.assets.open(ASSET).use { input ->
                    val data = ByteArray(MAX_BYTES + 1)
                    var size = 0
                    while (size < data.size) {
                        val count = input.read(data, size, data.size - size)
                        if (count < 0) break
                        require(count > 0)
                        size += count
                    }
                    require(size <= MAX_BYTES)
                    String(data, 0, size, Charsets.US_ASCII)
                }
                model = parse(bytes.lineSequence())
                loadState = "ready"
            } catch (_: IOException) {
                loadState = "asset unavailable"
            } catch (_: IllegalArgumentException) {
                loadState = "asset rejected"
            }
        }, "brr-offline-context").start()
    }

    internal fun parse(lines: Sequence<String>): Model {
        val rows = lines.iterator()
        require(rows.hasNext() && rows.next() == VERSION)
        val en = HashMap<String, List<Choice>>()
        val mix = HashMap<String, List<Choice>>()
        for (row in rows) {
            if (row.isEmpty()) continue
            val fields = row.split('\t')
            require(fields.size in 4..8 && fields.size % 2 == 0)
            val context = fields[1].split(' ')
            require(context.size in 1..3 && context.withIndex().all { (index, word) ->
                (index == 0 && word == "^") || tokenPattern.matches(word)
            })
            val map = when (fields[0]) {
                "en" -> en
                "mix" -> mix
                else -> throw IllegalArgumentException("Unknown locale channel")
            }
            val choices = ArrayList<Choice>(3)
            for (i in 2 until fields.size step 2) {
                val word = fields[i]
                val weight = fields[i + 1].toInt()
                require(tokenPattern.matches(word) && weight in 1..255 && choices.none { it.surface == word })
                choices.add(Choice(word, weight))
            }
            require(map.put(fields[1], choices) == null && en.size + mix.size <= MAX_ROWS)
        }
        require(en.isNotEmpty() && mix.isNotEmpty())
        return Model(en, mix)
    }

    private fun tokens(context: NgramContext): List<String> {
        val words = ArrayList<String>(3)
        for (i in 1..minOf(3, context.prevWordCount)) {
            if (context.isNthPrevWordBeginningOfSentence(i)) {
                words.add("^")
                break
            }
            val raw = context.getNthPrevWord(i) ?: break
            if (raw.length !in 1..48) return emptyList()
            val word = normalize(raw.toString())
            if (!tokenPattern.matches(word)) return emptyList()
            words.add(word)
        }
        words.reverse()
        return words
    }

    fun normalize(text: String): String = text.replace('’', '\'').lowercase(Locale.ROOT)

    private fun alternative(info: SuggestedWordInfo): Boolean =
        info.mWord.length in 1..48 && tokenPattern.matches(normalize(info.mWord)) &&
            !info.isPossiblyOffensive && !info.isExactMatch &&
            !info.isExactMatchWithIntentionalOmission &&
            info.mIndexOfTouchPointOfSecondWord == SuggestedWordInfo.NOT_AN_INDEX &&
            (info.kind == SuggestedWordInfo.KIND_COMPLETION ||
                info.kind == SuggestedWordInfo.KIND_CORRECTION ||
                info.kind == SuggestedWordInfo.KIND_PREDICTION) &&
            (info.mSourceDict.mDictType == Dictionary.TYPE_MAIN ||
                info.mSourceDict.mDictType == Dictionary.TYPE_BRR_PERSONAL)

    fun rank(
        native: ArrayList<SuggestedWordInfo>, context: NgramContext, locale: Locale,
        typed: String, prediction: Boolean, swipe: Boolean, pinnedCount: Int,
        allowNewAlias: Boolean = false,
        isCurrent: () -> Boolean, allowNewPrediction: (String) -> Boolean,
        activeModel: Model? = model,
    ): ArrayList<SuggestedWordInfo> {
        if (locale.language != "en" && (locale.language != "hi" || locale.script != "Latn")) return native
        val active = activeModel ?: run { fallbacks.incrementAndGet(); return native }
        val started = BrainPerformanceMetrics.begin()
        try {
            fun cancelled(): Boolean = !isCurrent() || System.nanoTime() - started >= DEADLINE_NS
            if (typed.length > 48 || cancelled()) return native
            val previous = tokens(context)
            val mixed = locale.language == "hi" && locale.script == "Latn" ||
                locale.language == "en" && previous.any { it in romanMarkers }
            val choices = active.lookup(previous, mixed)
            if (choices.isEmpty() || cancelled()) return native
            val result = ArrayList(native)
            if (prediction) {
                var insert = 0
                while (insert < result.size && !alternative(result[insert])) ++insert
                var proposed = 0
                for (choice in choices) {
                    if (cancelled() || proposed >= 2) break
                    val floor = if (proposed == 0) PREDICTION_FLOOR else SECOND_PREDICTION_FLOOR
                    if (choice.weight < floor) continue
                    val display = displayPrediction(choice.surface, locale, previous.last() == "^")
                    if (!allowNewPrediction(display)) continue
                    val key = normalize(display)
                    val oldIndex = result.indexOfFirst { normalize(it.mWord) == key }
                    if (oldIndex in 0 until insert) continue
                    if (oldIndex >= 0 && !alternative(result[oldIndex])) continue
                    val info = if (oldIndex >= 0) result.removeAt(oldIndex) else SuggestedWordInfo(
                        display, "", choice.weight, SuggestedWordInfo.KIND_PREDICTION, Dictionary.DICTIONARY_OFFLINE_CONTEXT,
                        SuggestedWordInfo.NOT_AN_INDEX, SuggestedWordInfo.NOT_A_CONFIDENCE,
                    )
                    result.add(insert++, info)
                    proposed++
                }
            } else if (!swipe && pinnedCount == 0 && allowNewAlias &&
                promoteAmbiguousAlias(result, choices, typed, allowNewPrediction)) {
                // The reviewed literal remains pinned outside this manual list.
            } else if (result.size > pinnedCount + 1) {
                // A single adjacent swap among close manual alternatives. No winner, touch index,
                // explicit user item, exact typed literal or automatic target can move.
                val prefix = normalize(typed)
                fun evidence(info: SuggestedWordInfo): Int =
                    choices.firstOrNull { it.surface == normalize(info.mWord) }?.weight ?: 0
                for (i in pinnedCount until minOf(result.lastIndex, MAX_CANDIDATES - 1)) {
                    if (cancelled()) break
                    val left = result[i]
                    val right = result[i + 1]
                    if (!alternative(left) || !alternative(right) || right.mScore <= 0 ||
                        right.mScore.toDouble() < left.mScore.toDouble() * 0.90) continue
                    val omission = "'" in right.mWord &&
                        normalize(right.mWord).replace("'", "") == prefix
                    if (!swipe && !omission &&
                        (prefix.length < 2 || !normalize(right.mWord).startsWith(prefix))) continue
                    if (evidence(right) < maxOf(MANUAL_RANK_FLOOR, evidence(left) + 36)) continue
                    result[i] = right
                    result[i + 1] = left
                    break
                }
            }
            if (cancelled() || result == native) return native
            accepted.incrementAndGet()
            return result
        } finally {
            if (System.nanoTime() - started >= DEADLINE_NS) timedOut.incrementAndGet()
            BrainPerformanceMetrics.end(BrainPerformanceMetrics.Stage.OFFLINE_CONTEXT, started)
        }
    }

    private fun promoteAmbiguousAlias(
        result: ArrayList<SuggestedWordInfo>, choices: List<Choice>, typed: String,
        allowed: (String) -> Boolean,
    ): Boolean {
        val target = EnglishApostropheLexicon.canonicalAlias(typed) ?: return false
        if (!EnglishApostropheLexicon.isAmbiguousOmission(typed, target) ||
            result.firstOrNull()?.mSourceDict?.isUserSpecific == true) return false
        val key = normalize(target)
        // Require strong evidence against changing the existing manual order.
        val aliasScore = choices.firstOrNull { it.surface == key }?.weight ?: return false
        val literalScore = choices.firstOrNull { it.surface == normalize(typed) }?.weight ?: 0
        if (aliasScore < maxOf(160, literalScore + 36) || !allowed(target)) return false
        val index = result.indexOfFirst { normalize(it.mWord) == key }
        if (index == 0) return false
        val info = if (index > 0) {
            if (result[index].mSourceDict.isUserSpecific) return false
            result.removeAt(index)
        } else SuggestedWordInfo(target, "", aliasScore, SuggestedWordInfo.KIND_OFFLINE_ALIAS,
            Dictionary.DICTIONARY_OFFLINE_CONTEXT, SuggestedWordInfo.NOT_AN_INDEX, SuggestedWordInfo.NOT_A_CONFIDENCE)
        result.add(0, info)
        return true
    }

    private fun displayPrediction(word: String, locale: Locale, start: Boolean): String {
        val regional = if (locale.country == "US") usSpellings[word] ?: word
            else if (locale.country == "GB") gbSpellings[word] ?: word else word
        return if (regional == "i" || regional.startsWith("i'") || start)
            regional.replaceFirstChar { it.uppercaseChar() } else regional
    }

    private val romanMarkers = setOf(
        "aaj", "abhi", "accha", "achha", "bahut", "bhai", "chahiye", "chalo", "haan", "hain",
        "kaise", "karna", "kyun", "milte", "mujhe", "nahi", "nahin", "shukriya", "theek", "yaar",
    )

    private val usSpellings = mapOf(
        "colour" to "color", "flavour" to "flavor", "favourite" to "favorite",
        "centre" to "center", "organise" to "organize", "recognise" to "recognize",
    )
    private val gbSpellings = usSpellings.entries.associate { (uk, us) -> us to uk }

    fun status(): String = "$VERSION · $loadState · " +
        "${accepted.get()} adjusted · ${fallbacks.get()} cold/unavailable · ${timedOut.get()} over budget"
}
