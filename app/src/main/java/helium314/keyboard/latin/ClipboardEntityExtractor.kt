// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Bounded local parser. Returns offsets, not copied entity strings. Never opens URLs. */
object ClipboardEntityExtractor {
    const val MAX_CHARS = 4096
    const val MAX_ENTITIES = 8
    enum class Kind(val label: String) { LINK("Link"), EMAIL("Email"), PHONE("Phone"), CODE("Verification code") }
    data class Entity(val kind: Kind, val start: Int, val end: Int)
    private val link = Regex("(?i)https?://[^\\s<>\"']{1,512}")
    private val email = Regex("(?i)(?<![\\w.+-])[a-z0-9._%+-]{1,64}@[a-z0-9-]{1,63}(?:\\.[a-z0-9-]{1,63}){1,5}(?![\\w.-])")
    private val phone = Regex("(?<![\\w])(?:\\+\\d{1,3}[ -]?)?(?:\\(?\\d{2,5}\\)?[ .-]?){2,5}\\d{2,5}(?![\\w])")
    private val code = Regex("(?i)\\b(?:otp|verification code|one[ -]time(?: password| code)?|security code|passcode)\\b(?:[ \\t:=#-]{0,8}is)?[ \\t:=#-]{0,16}(\\d{4,8})(?!\\d)")
    private val reverseCode = Regex("(?i)(?<!\\d)(\\d{4,8})[ \\t]{1,8}(?:is[ \\t]{1,8})?(?:your[ \\t]{1,8})?(?:otp|verification code|security code|one[ -]time code)\\b")
    private val secret = Regex("(?i)\\b(?:password|passwd|passphrase|recovery (?:code|key|phrase)|private key|api[ _-]?key|access token|secret key|cvv|cvc|card pin)\\b[ \\t:=#-]{1,16}\\S{3,}")
    private val bareCode = Regex("\\s*\\d{4,8}\\s*")
    private val card = Regex("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)")
    private val pem = Regex("(?i)-----BEGIN [A-Z0-9 ]{0,48}PRIVATE KEY-----")
    private val bearer = Regex("(?i)\\bbearer[ \\t]+[a-z0-9._~+/=-]{8,}")
    private val credentialParameter = Regex("(?i)[?&#](?:access[_-]?token|refresh[_-]?token|id[_-]?token|token|api[_-]?key|password|passwd|secret|signature|auth|code|key)=")
    private fun hasSecret(text: CharSequence): Boolean {
        if (secret.containsMatchIn(text) || pem.containsMatchIn(text) || bearer.containsMatchIn(text)) return true
        return link.findAll(text).any { match ->
            val uri = try { java.net.URI(match.value) } catch (_: Exception) { null }
            uri?.rawUserInfo != null || credentialParameter.containsMatchIn(match.value) ||
                (uri != null && credentialParameter.containsMatchIn("?" + (uri.query ?: "") + "#" + (uri.fragment ?: "")))
        }
    }
    private fun luhn(digits: String): Boolean {
        if (digits.length !in 13..19) return false
        var sum = 0
        var twice = false
        for (ch in digits.reversed()) {
            val n = ch - '0'
            val v = if (twice) n * 2 else n
            sum += if (v > 9) v - 9 else v
            twice = !twice
        }
        return sum % 10 == 0
    }
    /** Conservative auto-history gate, not a claim to identify every secret. */
    fun excludeFromAutomaticHistory(text: CharSequence): Boolean {
        if (text.length > MAX_CHARS) return true
        return code.containsMatchIn(text) || reverseCode.containsMatchIn(text) || bareCode.matches(text) || hasSecret(text) ||
            card.findAll(text).any { luhn(it.value.filter { c -> c in '0'..'9' }) }
    }
    fun extract(text: CharSequence): List<Entity> {
        if (text.isEmpty() || text.length > MAX_CHARS || hasSecret(text)) return emptyList()
        val out = ArrayList<Entity>(MAX_ENTITIES)
        fun add(kind: Kind, start: Int, end: Int) {
            if (out.size < MAX_ENTITIES && start < end && out.none { start < it.end && end > it.start })
                out.add(Entity(kind, start, end))
        }
        for (m in code.findAll(text)) m.groups[1]?.let { add(Kind.CODE, it.range.first, it.range.last + 1) }
        for (m in reverseCode.findAll(text)) m.groups[1]?.let { add(Kind.CODE, it.range.first, it.range.last + 1) }
        for (m in link.findAll(text)) {
            val value = m.value.trimEnd('.', ',', ';', '!', '?', ')', ']', '}')
            val uri = try { java.net.URI(value) } catch (_: Exception) { null }
            if (uri?.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null)
                add(Kind.LINK, m.range.first, m.range.first + value.length)
        }
        for (m in email.findAll(text)) add(Kind.EMAIL, m.range.first, m.range.last + 1)
        for (m in phone.findAll(text)) {
            val digits = m.value.filter { it in '0'..'9' }
            if (digits.length in 7..15 && !luhn(digits)) add(Kind.PHONE, m.range.first, m.range.last + 1)
        }
        return out.sortedBy { it.start }
    }
}
