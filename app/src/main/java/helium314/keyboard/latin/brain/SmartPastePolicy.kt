// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain

import android.text.InputType
import android.view.inputmethod.EditorInfo
import helium314.keyboard.latin.ClipboardEntityExtractor
import helium314.keyboard.latin.CleanLinkPolicy
import java.net.URI
import java.util.Locale

/** Deterministic local policy. No IO, editor queries, clipboard writes or content learning. */
object SmartPastePolicy {
    enum class Kind { TEXT, LINK, EMAIL, PHONE, ADDRESS, DATE, TRACKING, IDENTIFIER, OTP, RICH }
    enum class Destination { GENERAL, URL, EMAIL, PHONE, SEARCH, ADDRESS, DATE, TRACKING, IDENTIFIER, OTP, UNSUPPORTED }
    data class Candidate(val kind: Kind, val start: Int, val end: Int, val specific: Boolean = false)
    data class Decision(val candidate: Candidate, val destination: Destination, val lifetime: Long,
        val score: Int, val prominent: Boolean)
    private val otpField = Regex("(?i)(?:^|[^a-z0-9])(?:otp|sms.?otp.?code|2fa.?app.?otp.?code|one[ _-]?time[ _-]?(?:code|password|passcode)|verification[ _-]?code|authentication[ _-]?code|2fa[ _-]?code|security[ _-]?code)(?:$|[^a-z0-9])")
    private val street = Regex("(?i)\\b[0-9]{1,6}\\s+[\\p{L}0-9 .'-]{2,60}\\b(?:street|st\\.?|road|rd\\.?|avenue|ave\\.?|lane|ln\\.?|drive|sector|main road)\\b")
    private val date = Regex("(?<![0-9])(?:19|20)[0-9]{2}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12][0-9]|3[01])(?![0-9])")
    private val tracking = Regex("(?i)(?<![a-z0-9])(?:1Z[A-Z0-9]{16}|[A-Z]{2}[0-9]{9}[A-Z]{2})(?![a-z0-9])")
    private val identifier = Regex("(?i)\\b(?:order|reference|ticket|booking|invoice)\\s*(?:id|number|no\\.?)?\\s*[:#]\\s*([a-z0-9][a-z0-9-]{4,39})\\b")
    private val link = Regex("(?i)https?://[^\\s<>\"']{1,4090}")
    private val phone = Regex("(?<![\\p{L}0-9])(?:\\+\\d{1,3}[ -]?)?(?:\\(?\\d{2,5}\\)?[ .-]?){2,5}\\d{2,5}(?![\\p{L}0-9])")
    private val intentPatterns = mapOf(
        Kind.LINK to Regex("(?i)(?:here(?: is|'s) (?:the |a )?link|link (?:is|yeh hai|bhej)|(?:yeh|ye) link|url)\\s*[:=-]?\\s*$"),
        Kind.EMAIL to Regex("(?i)(?:my email(?: address)? is|email(?: id)?[: ]|mera email(?: hai)?)\\s*[:=-]?\\s*$"),
        Kind.PHONE to Regex("(?i)(?:call me at|my (?:phone|mobile|number)(?: number)? is|mera number(?: hai)?)\\s*[:=-]?\\s*$"),
        Kind.ADDRESS to Regex("(?i)(?:my address is|address|mera address(?: hai)?)\\s*[:=-]?\\s*$"),
        Kind.TRACKING to Regex("(?i)(?:tracking(?: number| id)?|tracking code)\\s*[:=-]?\\s*$"),
        Kind.DATE to Regex("(?i)(?:date is|date)\\s*[:=-]?\\s*$"),
        Kind.IDENTIFIER to Regex("(?i)(?:order|reference|ticket|booking|invoice) (?:id|number)\\s*[:=-]?\\s*$")
    )

    @JvmStatic fun isOtpEditor(editor: EditorInfo?): Boolean = destination(editor) == Destination.OTP

    @JvmStatic fun destination(editor: EditorInfo?): Destination {
        if (editor == null) return Destination.UNSUPPORTED
        val type = editor.inputType
        val clazz = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        val hints = listOfNotNull(editor.hintText?.toString(), editor.fieldName)
            .joinToString(" ").take(160).lowercase(Locale.ROOT)
        if (clazz != InputType.TYPE_CLASS_TEXT && clazz != InputType.TYPE_CLASS_NUMBER &&
            clazz != InputType.TYPE_CLASS_PHONE && clazz != InputType.TYPE_CLASS_DATETIME) return Destination.UNSUPPORTED
        if ((clazz == InputType.TYPE_CLASS_TEXT && variation in setOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)) ||
            (clazz == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) ||
            editor.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0) return Destination.UNSUPPORTED
        // Never guess an OTP field solely from a numeric keyboard or a generic "code" label.
        if ((clazz == InputType.TYPE_CLASS_TEXT || clazz == InputType.TYPE_CLASS_NUMBER) &&
            otpField.containsMatchIn(hints) && !Regex("(?i)credit|card|cvv|cvc|pin|postal|zip").containsMatchIn(hints))
            return Destination.OTP
        if (clazz == InputType.TYPE_CLASS_PHONE) return Destination.PHONE
        if (clazz == InputType.TYPE_CLASS_DATETIME) return Destination.DATE
        if (clazz == InputType.TYPE_CLASS_NUMBER) return Destination.UNSUPPORTED
        if (variation == InputType.TYPE_TEXT_VARIATION_URI) return Destination.URL
        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) return Destination.EMAIL
        if (variation == InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS) return Destination.ADDRESS
        if (Regex("(?i)\\btracking (?:number|id|code)\\b").containsMatchIn(hints)) return Destination.TRACKING
        if (Regex("(?i)\\b(?:order|reference|booking|ticket|invoice) (?:id|number)\\b").containsMatchIn(hints)) return Destination.IDENTIFIER
        if (editor.imeOptions and EditorInfo.IME_MASK_ACTION == EditorInfo.IME_ACTION_SEARCH) return Destination.SEARCH
        return Destination.GENERAL
    }

    /** Only one explicit labelled code or an all-digit 4–8 digit copy. No unrelated digit extraction. */
    @JvmStatic fun copiedOtp(text: String): Candidate? {
        val range = ClipboardEntityExtractor.verificationCodeRange(text) ?: return null
        return Candidate(Kind.OTP, range.first, range.last + 1, true)
    }

    private fun exactUrl(value: String): Boolean = try {
        val uri = URI(value)
        uri.host != null && uri.rawUserInfo == null &&
            (uri.scheme.equals("http", true) || uri.scheme.equals("https", true))
    } catch (_: Exception) { false }

    /** All offsets refer to the exact unmodified copy. Candidates are bounded; no network. */
    @JvmStatic fun classify(text: String): List<Candidate> {
        if (text.isBlank() || text.length > ClipboardEntityExtractor.MAX_CHARS ||
            ClipboardEntityExtractor.excludeFromAutomaticHistory(text)) return emptyList()
        val out = ArrayList<Candidate>()
        fun add(kind: Kind, start: Int, end: Int) {
            if (out.size < ClipboardEntityExtractor.MAX_ENTITIES && start >= 0 && end > start &&
                out.none { start < it.end && end > it.start }) out.add(Candidate(kind, start, end, true))
        }
        for (match in link.findAll(text)) {
            val raw = match.value
            val value = if (match.range.first == 0 && match.range.last == text.lastIndex) raw
                else raw.trimEnd('.', ',', ';', '!', ')', ']', '}')
            if (exactUrl(value)) add(Kind.LINK, match.range.first, match.range.first + value.length)
        }
        for (entity in ClipboardEntityExtractor.extract(text)) {
            val kind = when (entity.kind) {
                ClipboardEntityExtractor.Kind.EMAIL -> Kind.EMAIL
                ClipboardEntityExtractor.Kind.PHONE -> Kind.PHONE
                else -> continue
            }
            add(kind, entity.start, entity.end)
        }
        for (match in date.findAll(text)) {
            val bits = match.value.split('-')
            val y = bits[0].toInt(); val mo = bits[1].toInt(); val d = bits[2].toInt()
            val max = when (mo) { 2 -> if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
                4, 6, 9, 11 -> 30; else -> 31 }
            if (d <= max) add(Kind.DATE, match.range.first, match.range.last + 1)
        }
        for (match in tracking.findAll(text)) add(Kind.TRACKING, match.range.first, match.range.last + 1)
        for (match in identifier.findAll(text)) match.groups[1]?.let { add(Kind.IDENTIFIER, it.range.first, it.range.last + 1) }
        for (match in street.findAll(text)) add(Kind.ADDRESS, match.range.first, match.range.last + 1)
        // Keep the original as a distinct action; a general message is never silently sliced.
        val whole = out.firstOrNull { it.start == 0 && it.end == text.length }?.kind ?: Kind.TEXT
        return listOf(Candidate(whole, 0, text.length)) + out
    }

    fun intent(kind: Kind, cachedSuffix: String): Boolean =
        intentPatterns[kind]?.containsMatchIn(cachedSuffix.takeLast(96)) == true

    private fun required(destination: Destination): Kind? = when (destination) {
        Destination.URL -> Kind.LINK; Destination.EMAIL -> Kind.EMAIL; Destination.PHONE -> Kind.PHONE
        Destination.ADDRESS -> Kind.ADDRESS; Destination.DATE -> Kind.DATE
        Destination.TRACKING -> Kind.TRACKING; Destination.IDENTIFIER -> Kind.IDENTIFIER
        Destination.OTP -> Kind.OTP; else -> null
    }

    /** In a structured field, only a single unambiguous compatible entity may be offered. */
    fun decide(candidates: List<Candidate>, dest: Destination, age: Long, suffix: String,
               activeTyping: Boolean, learned: Int, chromeEligible: Boolean): Decision? {
        if (age < 0 || dest == Destination.UNSUPPORTED || candidates.isEmpty()) return null
        val expected = required(dest)
        val candidate = if (expected == null) candidates.first()
            else candidates.filter { it.kind == expected }.distinctBy { it.start to it.end }.singleOrNull()
                ?: return null
        val strong = expected != null
        val lifetime = when {
            dest == Destination.OTP -> 60_000L
            strong -> 180_000L
            else -> 60_000L
        }
        if (age >= lifetime) return null
        val fresh = when { age < 15_000 -> 25; age < 60_000 -> 15; else -> 5 }
        val wanted = intent(candidate.kind, suffix)
        val compatibility = if (strong) 35 else 30
        val intentScore = when { wanted -> 25; !activeTyping && age < 15_000 -> 25; !activeTyping -> 15; else -> 0 }
        val score = (compatibility + fresh + intentScore + 10 + learned.coerceIn(-5,5)).coerceIn(0,100)
        if (score < 50 && !chromeEligible) return null
        return Decision(candidate, dest, lifetime, score, chromeEligible || dest == Destination.OTP || score >= 80)
    }
}

/** Bounded category counters only. No clipboard bytes, domains, app identifiers or timestamps. */
object SmartPasteLearning {
    private const val WIDTH = 2
    private val counts = IntArray(SmartPastePolicy.Kind.values().size * SmartPastePolicy.Destination.values().size * WIDTH)
    private fun index(kind: SmartPastePolicy.Kind, dest: SmartPastePolicy.Destination) =
        (kind.ordinal * SmartPastePolicy.Destination.values().size + dest.ordinal) * WIDTH
    @Synchronized fun bias(kind: SmartPastePolicy.Kind, dest: SmartPastePolicy.Destination): Int {
        if (kind == SmartPastePolicy.Kind.OTP || dest == SmartPastePolicy.Destination.OTP) return 0
        val i = index(kind,dest)
        val a = counts[i]; val d = counts[i+1]
        return if (a+d < 4) 0 else ((a-d)*5/(a+d+4)).coerceIn(-5,5)
    }
    @Synchronized fun record(kind: SmartPastePolicy.Kind, dest: SmartPastePolicy.Destination, accepted: Boolean) {
        if (kind == SmartPastePolicy.Kind.OTP || dest == SmartPastePolicy.Destination.OTP) return
        val i = index(kind,dest)
        if (counts[i]+counts[i+1] >= 32) { counts[i] /= 2; counts[i+1] /= 2 }
        counts[i+if (accepted) 0 else 1]++
    }
    @JvmStatic @Synchronized fun reset() { counts.fill(0) }
}
