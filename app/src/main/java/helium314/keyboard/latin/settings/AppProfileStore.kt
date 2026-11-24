// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import android.content.Context
import android.os.UserManager
import android.util.AtomicFile
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import helium314.keyboard.compat.isDeviceLocked
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import helium314.keyboard.latin.utils.prefs
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/** Encrypted sparse preferences. Only a confirmed aggregate preference is saved; no event or typing history. */
object AppProfileStore {
    data class Profile(
        val autocorrect: Boolean? = null,
        val suggestions: Boolean? = null,
        val numberRow: Boolean? = null,
        val sound: Boolean? = null,
        val haptics: Boolean? = null,
        val privateMode: Boolean = false,
        val autoCap: Boolean? = null,
        val doubleSpacePeriod: Boolean? = null,
        val automaticAutocorrectOff: Boolean = false
    ) {
        fun autocorrect(global: Boolean) = autocorrect ?: (global && !automaticAutocorrectOff)
        fun suggestions(global: Boolean) = suggestions ?: global
        fun numberRow(global: Boolean) = numberRow ?: global
        fun sound(global: Boolean) = sound ?: global
        fun haptics(global: Boolean) = haptics ?: global
        fun privateMode(global: Boolean) = global || privateMode
        fun autoCap(global: Boolean) = autoCap ?: global
        fun doubleSpacePeriod(global: Boolean) = doubleSpacePeriod ?: global
    }
    private const val MAX_PROFILES = 32
    private const val MAX_BYTES = 16_384
    private const val ALIAS = "com.mangoloads.board.app_profiles_v2"
    private val aad = "brrBoard.app_profiles.v2".toByteArray(Charsets.UTF_8)
    private val worker = Executors.newSingleThreadExecutor()
    private val queued = AtomicBoolean(false)
    private val rejectionWindow = AppProfileRejectionWindow()
    private val generation = AtomicLong()
    private var cached: Map<String, Profile>? = null
    private var unreadable = false
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    @JvmStatic fun revision(): Long = generation.get()
    private fun available(context: Context): Boolean =
        !context.isDeviceProtectedStorage &&
            context.getSystemService(UserManager::class.java)?.isUserUnlocked == true && !isDeviceLocked(context)
    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "app_profiles_v2.enc"))
    private fun legacy(context: Context) = AtomicFile(File(context.noBackupFilesDir, "app_profiles_v1.json"))
    private fun exists(f: AtomicFile) = f.baseFile.exists() || File(f.baseFile.path + ".bak").exists()
    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(ALIAS, null)
        if (existing is SecretKey) return existing
        check(create && existing == null)
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(false).build())
        return gen.generateKey()
    }
    internal fun seal(key: SecretKey, plain: ByteArray): ByteArray {
        require(plain.size <= MAX_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        require(cipher.iv.size == 12)
        cipher.updateAAD(aad)
        return cipher.iv + cipher.doFinal(plain)
    }
    internal fun open(key: SecretKey, encrypted: ByteArray): ByteArray {
        require(encrypted.size in 28..MAX_BYTES + 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, encrypted.copyOfRange(0,12)))
        cipher.updateAAD(aad)
        return cipher.doFinal(encrypted,12,encrypted.size-12)
    }
    private fun readBounded(f: AtomicFile, limit: Int): ByteArray = f.openRead().use { input ->
        val bytes = ByteArray(limit + 1)
        var n = 0
        while (n < bytes.size) {
            val read = input.read(bytes, n, bytes.size - n)
            if (read <= 0) break
            n += read
        }
        require(n <= limit)
        bytes.copyOf(n).also { bytes.fill(0) }
    }
    private fun persist(context: Context, plain: ByteArray) {
        val encrypted = seal(key(true), plain)
        check(available(context))
        val f = file(context)
        val out = f.startWrite()
        try { out.write(encrypted); f.finishWrite(out) }
        catch (e: Exception) { f.failWrite(out); throw e }
    }
    private fun removeLegacy(context: Context) {
        val f = legacy(context)
        f.delete()
        check(!exists(f) && !File(f.baseFile.path + ".new").exists())
    }

    private fun load(context: Context): Map<String, Profile> {
        if (!available(context)) return emptyMap()
        cached?.let { return it }
        val result = LinkedHashMap<String, Profile>()
        var plain: ByteArray? = null
        try {
            val f = file(context)
            val old = legacy(context)
            val encrypted = exists(f)
            if (encrypted || exists(old)) {
                val bytes = if (encrypted) open(key(false), readBounded(f, MAX_BYTES + 28))
                    else readBounded(old, MAX_BYTES)
                plain = bytes
                val root = JSONObject(bytes.toString(Charsets.UTF_8))
                require(root.getInt("schema") == 1)
                val apps = root.getJSONObject("profiles")
                require(apps.length() <= MAX_PROFILES)
                for (pkg in apps.keys()) {
                    require(pkg.length <= 200 && packagePattern.matches(pkg))
                    val row = apps.getJSONObject(pkg)
                    fun optional(key: String): Boolean? = if (row.has(key)) row.getBoolean(key) else null
                    result[pkg] = Profile(optional("autocorrect"), optional("suggestions"),
                        optional("numberRow"), optional("sound"), optional("haptics"),
                        row.optBoolean("privateMode", false), optional("autoCap"), optional("doubleSpacePeriod"),
                        encrypted && row.optBoolean("automaticAutocorrectOff", false))
                }
                if (!encrypted) persist(context, bytes)
                if (exists(old)) removeLegacy(context)
            }
        } catch (_: Exception) {
            unreadable = true
            result.clear()
        } finally { plain?.fill(0) }
        return result.toMap().also { cached = it }
    }
    @JvmStatic @Synchronized
    fun get(context: Context, packageName: String?): Profile {
        if (packageName.isNullOrEmpty()) return Profile()
        val c = context.applicationContext ?: context
        if (!available(c)) return Profile()
        val profiles = load(c)
        return if (unreadable) Profile(privateMode = true) else profiles[packageName] ?: Profile()
    }
    @Synchronized fun packages(context: Context): Set<String> {
        val c = context.applicationContext ?: context
        return if (available(c)) load(c).keys.toSet() else emptySet()
    }
    @Synchronized fun save(context: Context, packageName: String, profile: Profile): Boolean {
        val c = context.applicationContext ?: context
        if (!available(c) || packageName.length > 200 || !packagePattern.matches(packageName)) return false
        val next = LinkedHashMap(load(c))
        if (unreadable) return false
        if (profile == Profile()) next.remove(packageName) else next[packageName] = profile
        if (next.size > MAX_PROFILES) return false
        val rows = JSONObject()
        for ((pkg, p) in next) {
            val row = JSONObject()
            p.autocorrect?.let { row.put("autocorrect", it) }
            p.suggestions?.let { row.put("suggestions", it) }
            p.numberRow?.let { row.put("numberRow", it) }
            p.sound?.let { row.put("sound", it) }
            p.haptics?.let { row.put("haptics", it) }
            if (p.privateMode) row.put("privateMode", true)
            p.autoCap?.let { row.put("autoCap", it) }
            p.doubleSpacePeriod?.let { row.put("doubleSpacePeriod", it) }
            if (p.automaticAutocorrectOff) row.put("automaticAutocorrectOff", true)
            rows.put(pkg, row)
        }
        val bytes = JSONObject().put("schema", 1).put("profiles", rows).toString().toByteArray(Charsets.UTF_8)
        return try {
            require(bytes.size <= MAX_BYTES)
            persist(c,bytes)
            cached = next.toMap(); generation.incrementAndGet()
            true
        } catch (_: Exception) { false }
        finally { bytes.fill(0) }
    }
    /** Learn only from repeated, host-confirmed autocorrection reversals.
     * No text is passed here. The temporary count is scoped to an editor epoch. */
    @JvmStatic @Synchronized fun noteRejection(context: Context, packageName: String, epoch: Long, now: Long, values: SettingsValues) {
        val c = context.applicationContext ?: context
        if (!values.mUsePersonalizedDicts || values.mIncognitoModeEnabled ||
            !values.mAutoCorrectEnabled || !available(c) || packageName.length > 200 ||
            !packagePattern.matches(packageName)) return
        if (!rejectionWindow.record(epoch, now) || !queued.compareAndSet(false,true)) return
        val expected = generation.get()
        worker.execute {
            try {
                synchronized(this) {
                    if (generation.get() != expected || !available(c) ||
                        Settings.getValues() !== values ||
                        !c.prefs().getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS,true) ||
                        c.prefs().getBoolean(Settings.PREF_ALWAYS_INCOGNITO_MODE,false)) return@synchronized
                    val profile = get(c,packageName)
                    if (!unreadable && !profile.privateMode && profile.autocorrect == null &&
                        !profile.automaticAutocorrectOff)
                        save(c,packageName,profile.copy(automaticAutocorrectOff = true))
                }
            } finally { queued.set(false) }
        }
    }
    @Synchronized fun storageAvailable(context: Context): Boolean {
        val c = context.applicationContext ?: context
        if (!available(c)) return false
        load(c)
        return !unreadable
    }
    /** A deliberate reset can recover an unreadable record; never export package identifiers. */
    @Synchronized fun reset(context: Context): Boolean {
        val c = context.applicationContext ?: context
        if (!available(c)) return false
        return try {
            generation.incrementAndGet()
            rejectionWindow.clear()
            // Remove legacy first, so an interrupted reset cannot resurrect plaintext.
            removeLegacy(c)
            val f = file(c); f.delete()
            if (exists(f) || File(f.baseFile.path + ".new").exists()) return false
            cached = emptyMap(); unreadable = false
            true
        } catch (_: Exception) { false }
    }
}
