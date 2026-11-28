// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.snippets

import android.content.Context
import android.os.UserManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import helium314.keyboard.compat.isDeviceLocked
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

/** Rules contain only authenticated clipboard references and configuration, never clip bodies.
 *  Separate CE/no-backup AES-GCM store; existing ciphertext is never replaced on read failure.
 */
internal object SmartSnippetStore {
    private const val ALIAS = "com.mangoloads.board.smart_snippets_v1"
    private const val MAX_BYTES = 262144
    private const val MAX_RULES = 64
    private val aad = "brrBoard.smart_snippets.v1".toByteArray(Charsets.UTF_8)
    private var cache: List<SnippetRule>? = null
    private var failed = false

    private fun available(c: Context) = !c.isDeviceProtectedStorage &&
        c.getSystemService(UserManager::class.java)?.isUserUnlocked == true && !isDeviceLocked(c)
    private fun file(c: Context) = AtomicFile(File(c.noBackupFilesDir, "smart_snippets_v1.enc"))
    private fun exists(f: AtomicFile) = f.baseFile.exists() || File(f.baseFile.path + ".bak").exists()
    private fun key(create: Boolean): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey(ALIAS, null)
        if (existing is SecretKey) return existing
        check(create && existing == null)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256)
                .setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(false).build())
        }.generateKey()
    }
    fun fingerprint(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
    fun today(c: Calendar = Calendar.getInstance()): Int =
        c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)

    private fun encode(rules: List<SnippetRule>): ByteArray {
        val rows = JSONArray()
        for (r in rules) rows.put(JSONObject().apply {
            put("id",r.id); put("hash",r.fingerprint); put("title",r.title)
            put("apps",JSONArray(r.packages.toList())); put("all",r.allApps)
            put("cadence",r.cadence.name); put("seconds",r.durationSeconds)
            put("empty",r.emptyAgain); put("line",r.newLine); put("enabled",r.enabled)
            put("usedDay",r.usedDay); put("days",r.days); put("start",r.startHour); put("end",r.endHour)
        })
        return JSONObject().put("schema",1).put("rules",rows).toString().toByteArray(Charsets.UTF_8)
    }
    private fun decode(bytes: ByteArray): List<SnippetRule> {
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.getInt("schema") == 1)
        val rows = root.getJSONArray("rules")
        require(rows.length() <= MAX_RULES)
        val result = (0 until rows.length()).map { i ->
            val row = rows.getJSONObject(i)
            val apps = row.getJSONArray("apps")
            require(apps.length() <= 64)
            SnippetRule(row.getLong("id"),row.getString("hash"),row.getString("title"),
                (0 until apps.length()).map { apps.getString(it) }.toSet(), row.getBoolean("all"),
                SnippetCadence.valueOf(row.getString("cadence")),row.getInt("seconds"),
                row.getBoolean("empty"),row.getBoolean("line"),row.getBoolean("enabled"),
                row.getInt("usedDay"),row.optInt("days",127),row.optInt("start",0),row.optInt("end",24))
                .also { require(it.valid()) }
        }
        require(result.map { it.id }.distinct().size == result.size)
        return result
    }
    @Synchronized fun snapshot(c: Context): List<SnippetRule> {
        if (!available(c)) return emptyList()
        cache?.let { return it }
        if (failed) return emptyList()
        val f = file(c)
        if (!exists(f)) return emptyList<SnippetRule>().also { cache = it }
        var plain: ByteArray? = null
        try {
            val encrypted = f.openRead().use { stream ->
                val buffer = ByteArray(MAX_BYTES + 29)
                var n = 0
                while (n < buffer.size) {
                    val read = stream.read(buffer,n,buffer.size-n)
                    if (read <= 0) break
                    n += read
                }
                require(n in 28..MAX_BYTES + 28)
                buffer.copyOf(n)
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(false),GCMParameterSpec(128,encrypted.copyOfRange(0,12)))
            cipher.updateAAD(aad)
            val decoded = cipher.doFinal(encrypted,12,encrypted.size-12)
            plain = decoded
            return decode(decoded).also { cache = it }
        } catch (_: Exception) {
            failed = true
            return emptyList()
        } finally { plain?.fill(0) }
    }
    @Synchronized fun storageAvailable(c: Context): Boolean {
        if (!available(c)) return false
        snapshot(c)
        return !failed
    }
    @Synchronized fun save(c: Context, rule: SnippetRule): Boolean {
        if (!rule.valid()) return false
        val old = snapshot(c)
        return write(c, old.filterNot { it.id == rule.id } + rule)
    }
    @Synchronized fun remove(c: Context, id: Long): Boolean =
        write(c, snapshot(c).filterNot { it.id == id })
    @Synchronized fun clear(c: Context): Boolean = write(c, emptyList())
    @Synchronized fun markUsed(c: Context, rule: SnippetRule): Boolean {
        val live = snapshot(c).firstOrNull { it.id == rule.id && it.fingerprint == rule.fingerprint } ?: return false
        if (live.cadence != SnippetCadence.DAILY) return true
        return save(c, live.copy(usedDay = today()))
    }
    private fun write(c: Context, rules: List<SnippetRule>): Boolean {
        if (!available(c) || failed || rules.size > MAX_RULES) return false
        val f = file(c)
        var plain: ByteArray? = null
        return try {
            val bytes = encode(rules); plain = bytes
            require(bytes.size <= MAX_BYTES)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE,key(true))
            require(cipher.iv.size == 12)
            cipher.updateAAD(aad)
            val encrypted = cipher.iv + cipher.doFinal(bytes)
            val out = f.startWrite()
            try { out.write(encrypted); f.finishWrite(out) }
            catch (e: Exception) { f.failWrite(out); throw e }
            cache = rules.toList()
            true
        } catch (_: Exception) { false }
        finally { plain?.fill(0) }
    }
    @Synchronized fun discardCache() { cache = null; failed = false }
}
