// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import android.content.Context
import android.os.UserManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import helium314.keyboard.compat.isDeviceLocked
import helium314.keyboard.latin.inputlogic.DynamicTextTemplate
import helium314.keyboard.latin.inputlogic.DynamicTextTemplate.Template
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** User-created snippets in a separate authenticated, credential-storage no-backup file. */
object TextTemplateStore {
    data class Snapshot(val templates: List<Template>, val revision: Long)
    private const val ALIAS = "com.mangoloads.board.text_templates_v1"
    private const val MAX_BYTES = 32_768
    private val revision = AtomicLong()
    private val aad = "brrBoard.text_templates.v1".toByteArray(Charsets.UTF_8)
    fun revision(): Long = revision.get()
    private fun available(c: Context) = runCatching {
        !c.isDeviceProtectedStorage && c.getSystemService(UserManager::class.java)?.isUserUnlocked == true && !isDeviceLocked(c)
    }.getOrDefault(false)
    private fun file(c: Context) = AtomicFile(File(c.noBackupFilesDir, "text_templates_v1.enc"))
    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(ALIAS,null)
        if (existing is SecretKey) return existing
        check(create && existing == null)
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false).build())
        return gen.generateKey()
    }

    internal fun seal(key: SecretKey, bytes: ByteArray): ByteArray {
        require(bytes.size <= MAX_BYTES - 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key)
        require(cipher.iv.size == 12)
        cipher.updateAAD(aad)
        return cipher.iv + cipher.doFinal(bytes)
    }
    internal fun open(key: SecretKey, sealed: ByteArray): ByteArray {
        require(sealed.size in 28..MAX_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key,GCMParameterSpec(128,sealed.copyOfRange(0,12)))
        cipher.updateAAD(aad)
        return cipher.doFinal(sealed,12,sealed.size-12)
    }
    private fun decode(bytes: ByteArray): List<Template> {
        val array = JSONObject(bytes.toString(Charsets.UTF_8)).getJSONArray("templates")
        require(array.length() <= DynamicTextTemplate.MAX_TEMPLATES)
        val items = List(array.length()) { i ->
            val row = array.getJSONObject(i)
            Template(row.getString("name"), row.getString("body"))
        }
        require(DynamicTextTemplate.validList(items))
        return items
    }
    @Synchronized fun load(context: Context): Snapshot? {
        val c = context.applicationContext ?: context
        if (!available(c)) return null
        val f = try { file(c) } catch (_: Exception) { return null }
        if (!f.baseFile.exists() && !File(f.baseFile.path+".bak").exists())
            return Snapshot(emptyList(),revision.get())
        var plain: ByteArray? = null
        return try {
            val encrypted = f.openRead().use { input ->
                val buffer = ByteArray(MAX_BYTES+1)
                var total = 0
                while (total < buffer.size) {
                    val n = input.read(buffer,total,buffer.size-total)
                    if (n < 0) break
                    total += n
                }
                require(total <= MAX_BYTES)
                buffer.copyOf(total)
            }
            val decoded = open(key(false),encrypted)
            plain = decoded
            if (!available(c)) return null
            Snapshot(decode(decoded),revision.get())
        } catch (_: Exception) { null }
        finally { plain?.fill(0) }
    }
    @Synchronized fun save(context: Context, expected: Snapshot, items: List<Template>): Boolean {
        val c = context.applicationContext ?: context
        if (!available(c) || !DynamicTextTemplate.validList(items)) return false
        val live = load(c) ?: return false
        if (live != expected) return false
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("name",it.name).put("body",it.body)) }
        val plain = JSONObject().put("templates",array).toString().toByteArray(Charsets.UTF_8)
        return try {
            val f = file(c)
            val sealed = seal(key(true),plain)
            if (!available(c)) return false
            val stream = f.startWrite()
            try { stream.write(sealed); f.finishWrite(stream) }
            catch (e: Exception) { f.failWrite(stream); throw e }
            revision.incrementAndGet()
            true
        } catch (_: Exception) { false }
        finally { plain.fill(0) }
    }
}
