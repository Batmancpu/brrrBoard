// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.InputType
import android.util.AtomicFile
import helium314.keyboard.latin.utils.prefs
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Mac
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Independent credential store; the legacy biometric note is never migrated or weakened. */
object VaultGestureStore {
    const val PREF_ENABLED = "brr_vault_gesture_enabled_v1"
    const val PREF_FAILURES = "brr_vault_failed_matches_v1"
    private const val FILE = "brr_vault_mapping_v1"
    private const val ALIAS = "brr.vault.credential.v1."
    private const val AUTH_SECONDS = 300
    private const val MAX_FAILURES = 5
    private const val MAX_BYTES = 4096
    private const val SETTINGS_FILE = "brr_vault_settings_verifier_v1"
    private const val SETTINGS_ALIAS = "brr.vault.settings.hmac.v1"
    private const val SETTINGS_FAILURES = "brr_vault_settings_failures_v1"
    private val worker by lazy { Executors.newSingleThreadExecutor() }
    private val pending = AtomicBoolean()
    private val main by lazy { Handler(Looper.getMainLooper()) }

    @JvmStatic fun isMaskedPassword(inputType: Int): Boolean {
        val cls = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return cls == InputType.TYPE_CLASS_TEXT &&
            (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
    }

    @JvmStatic fun isDeviceUnlocked(context: Context): Boolean {
        val kg = context.getSystemService(KeyguardManager::class.java) ?: return false
        return Build.VERSION.SDK_INT >= 30 && kg.isDeviceSecure && !kg.isDeviceLocked
    }

    fun configured(context: Context): Boolean = file(context).baseFile.exists()
    fun resetFailures(context: Context) { check(context.prefs().edit().putInt(PREF_FAILURES, 0).putInt(SETTINGS_FAILURES, 0).commit()) }

    @Synchronized fun save(context: Context, samples: List<FloatArray>, password: String, instant: Boolean) {
        require(isDeviceUnlocked(context))
        val average = requireNotNull(VaultGestureTrace.average(samples))
        average.fill(0f)
        require(password.length in 4..512)
        val bytes = password.toByteArray(Charsets.UTF_8)
        var plain = ByteArray(0)
        var encrypted = ByteArray(0)
        var encoded = ByteArray(0)
        try {
            require(bytes.size <= 2048)
            val mode = if (instant) 2 else 1
            // Negative size marks a new three-template payload. The encrypted file,
            // AES key aliases and associated data are unchanged for safe in-place upgrades.
            plain = ByteBuffer.allocate(4 + VaultGestureTrace.ENROLLMENT_SAMPLES * VaultGestureTrace.POINT_COUNT * 8 + bytes.size).apply {
                putInt(-bytes.size); samples.forEach { sample -> sample.forEach { putFloat(it) } }; put(bytes)
            }.array()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key(mode, true))
            cipher.updateAAD(aad(mode))
            encrypted = cipher.doFinal(plain)
            val iv = cipher.iv
            require(iv.size == 12)
            encoded = ByteBuffer.allocate(1 + 12 + encrypted.size).apply {
                put(mode.toByte()); put(iv); put(encrypted)
            }.array()
            val atomic = file(context)
            val output = atomic.startWrite()
            try { output.write(encoded); atomic.finishWrite(output) }
            catch (e: Exception) { atomic.failWrite(output); throw e }
            writeSettingsVerifier(context, password)
            check(context.prefs().edit().putInt(PREF_FAILURES, 0).putInt(SETTINGS_FAILURES, 0).putBoolean(PREF_ENABLED, true)
                .putBoolean(WhatsAppVaultPolicy.PREF_CONFIRMED, true).commit())
        } finally {
            bytes.fill(0); plain.fill(0); encrypted.fill(0); encoded.fill(0)
        }
    }

    @Synchronized fun delete(context: Context) {
        check(context.prefs().edit().putBoolean(PREF_ENABLED, false).putInt(PREF_FAILURES, 0).putInt(SETTINGS_FAILURES, 0)
            .putBoolean(WhatsAppVaultPolicy.PREF_CONFIRMED, false).commit())
        file(context).delete()
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.deleteEntry(ALIAS + 1); ks.deleteEntry(ALIAS + 2)
        settingsFile(context).delete()
        ks.deleteEntry(SETTINGS_ALIAS)
    }

    @JvmStatic fun matchAsync(context: Context, trace: FloatArray, callback: Consumer<CharArray?>) {
        if (!pending.compareAndSet(false, true)) return
        val app = context.applicationContext
        val copy = trace.copyOf()
        worker.execute {
            val secret = try { match(app, copy) } catch (_: Exception) { null }
            finally { copy.fill(0f) }
            main.post {
                try { callback.accept(secret) }
                finally { secret?.fill('\u0000'); pending.set(false) }
            }
        }
    }

    @Synchronized private fun match(context: Context, trace: FloatArray): CharArray? {
        if (!isDeviceUnlocked(context) || !context.prefs().getBoolean(PREF_ENABLED, false) ||
            !context.prefs().getBoolean(WhatsAppVaultPolicy.PREF_CONFIRMED, false)) return null
        val failures = context.prefs().getInt(PREF_FAILURES, 0)
        if (failures >= MAX_FAILURES) return null
        val secret = readMapping(context, trace) ?: return null
        // Existing scoped installations acquire the verifier after a successful
        // gesture, without changing their encrypted credential or auth mode.
        runCatching { if (readSettingsVerifier(context) == null) writeSettingsVerifier(context, String(secret)) }
        return secret
    }

    // Called only inside the synchronized store. Null trace is restricted to
    // local settings verification/migration, never returned to an external caller.
    private fun readMapping(context: Context, trace: FloatArray?): CharArray? {
        val failures = context.prefs().getInt(PREF_FAILURES, 0)
        val atomic = file(context)
        val encoded = atomic.openRead().use { input ->
            val buffer = ByteArray(MAX_BYTES + 1)
            try {
                var size = 0
                while (size < buffer.size) {
                    val n = input.read(buffer, size, buffer.size - size)
                    if (n <= 0) break
                    size += n
                }
                require(size in 30..MAX_BYTES)
                buffer.copyOf(size)
            } finally { buffer.fill(0) }
        }
        var plain = ByteArray(0)
        val templates = List(VaultGestureTrace.ENROLLMENT_SAMPLES) { FloatArray(VaultGestureTrace.POINT_COUNT * 2) }
        try {
            val mode = encoded[0].toInt()
            require(mode == 1 || mode == 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(mode, false), GCMParameterSpec(128, encoded, 1, 12))
            cipher.updateAAD(aad(mode))
            plain = cipher.doFinal(encoded, 13, encoded.size - 13)
            val input = ByteBuffer.wrap(plain)
            val encodedSize = input.int
            // Only explicitly re-enrolled WhatsApp mappings are eligible. A legacy
            // generic password blob is never silently repurposed for normal fields.
            require(encodedSize in -2048..-1)
            val size = -encodedSize
            require(input.remaining() == templates.size * templates[0].size * 4 + size)
            templates.forEach { template -> for (i in template.indices) template[i] = input.float }
            if (trace != null && !VaultGestureTrace.matchesEnrollment(templates, trace)) {
                context.prefs().edit().putInt(PREF_FAILURES, failures + 1).commit()
                return null
            }
            val chars = Charsets.UTF_8.newDecoder().decode(input)
            try {
                if (trace != null && !context.prefs().edit().putInt(PREF_FAILURES, 0).commit()) return null
                return CharArray(chars.remaining()).also { chars.get(it) }
            } finally { if (chars.hasArray()) chars.array().fill('\u0000') }
        } finally { encoded.fill(0); plain.fill(0); templates.forEach { it.fill(0f) } }
    }

    /** No plaintext getter. Separate keyed verifier permits a code check without
     * changing the smart-mode AES authentication requirement or exposing the code. */
    @Synchronized fun verifySettingsCode(context: Context, candidate: String): Boolean {
        if (candidate.length !in 4..512 || !isDeviceUnlocked(context) ||
            !context.prefs().getBoolean(PREF_ENABLED, false) ||
            !context.prefs().getBoolean(WhatsAppVaultPolicy.PREF_CONFIRMED, false)) return false
        return runCatching {
            val prefs = context.prefs()
            val failures = prefs.getInt(SETTINGS_FAILURES, 0)
            if (failures >= MAX_FAILURES) return false
            val verifier = readSettingsVerifier(context)
            val matched: Boolean
            if (verifier != null) {
                if (candidate.length != verifier.length) return false
                val tag = settingsTag(candidate, verifier.digest)
                matched = try { MessageDigest.isEqual(tag, verifier.tag) } finally { tag.fill(0) }
            } else {
                // Legacy smart-mode mappings still require recent device auth.
                val stored = readMapping(context, null) ?: return false
                try {
                    if (candidate.length != stored.size) return false
                    val actual = String(stored).toByteArray(Charsets.UTF_8)
                    val entered = candidate.toByteArray(Charsets.UTF_8)
                    matched = try { MessageDigest.isEqual(actual, entered) }
                        finally { actual.fill(0); entered.fill(0) }
                    if (matched) writeSettingsVerifier(context, candidate)
                } finally { stored.fill('\u0000') }
            }
            if (!prefs.edit().putInt(SETTINGS_FAILURES, if (matched) 0 else failures + 1).commit()) return false
            matched
        }.getOrDefault(false)
    }

    /** Called after explicit Android device authentication to migrate an existing mapping. */
    @Synchronized fun prepareSettingsVerifier(context: Context) {
        if (!isDeviceUnlocked(context) || !context.prefs().getBoolean(PREF_ENABLED, false) ||
            !context.prefs().getBoolean(WhatsAppVaultPolicy.PREF_CONFIRMED, false)) return
        if (readSettingsVerifier(context) != null) return
        val stored = readMapping(context, null) ?: return
        try { writeSettingsVerifier(context, String(stored)) } finally { stored.fill('\u0000') }
    }

    private data class SettingsVerifier(val length: Int, val digest: ByteArray, val tag: ByteArray)
    private fun settingsFile(context: Context) = AtomicFile(File(context.noBackupFilesDir, SETTINGS_FILE))

    private fun mappingDigest(context: Context): ByteArray {
        val bytes = file(context).openRead().use { it.readBytes() }
        return try {
            require(bytes.size in 30..MAX_BYTES)
            MessageDigest.getInstance("SHA-256").digest(bytes)
        } finally { bytes.fill(0) }
    }

    private fun readSettingsVerifier(context: Context): SettingsVerifier? {
        if (!settingsFile(context).baseFile.exists()) return null
        val record = settingsFile(context).openRead().use { it.readBytes() }
        try {
            require(record.size == 68)
            val input = ByteBuffer.wrap(record)
            val length = input.int
            require(length in 4..512)
            val digest = ByteArray(32).also { input.get(it) }
            val tag = ByteArray(32).also { input.get(it) }
            if (!MessageDigest.isEqual(digest, mappingDigest(context))) return null
            return SettingsVerifier(length, digest, tag)
        } finally { record.fill(0) }
    }

    private fun writeSettingsVerifier(context: Context, password: String) {
        val digest = mappingDigest(context)
        val tag = settingsTag(password, digest, create = true)
        val record = ByteBuffer.allocate(68).putInt(password.length).put(digest).put(tag).array()
        try {
            val atomic = settingsFile(context)
            val output = atomic.startWrite()
            try { output.write(record); atomic.finishWrite(output) }
            catch (e: Exception) { atomic.failWrite(output); throw e }
        } finally { digest.fill(0); tag.fill(0); record.fill(0) }
    }

    private fun settingsTag(candidate: String, digest: ByteArray, create: Boolean = false): ByteArray {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val secretKey = (ks.getKey(SETTINGS_ALIAS, null) as? SecretKey) ?: run {
            check(create)
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(SETTINGS_ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUnlockedDeviceRequired(true).build())
            }.generateKey()
        }
        val bytes = candidate.toByteArray(Charsets.UTF_8)
        return try {
            Mac.getInstance("HmacSHA256").apply { init(secretKey); update(digest) }.doFinal(bytes)
        } finally { bytes.fill(0) }
    }

    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, FILE))
    private fun aad(mode: Int) = "brrBoard/vault/v1/$mode".toByteArray(Charsets.UTF_8)
    private fun key(mode: Int, create: Boolean): SecretKey {
        check(Build.VERSION.SDK_INT >= 30)
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = ALIAS + mode
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create)
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUnlockedDeviceRequired(true)
        if (mode == 1) spec.setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(AUTH_SECONDS, KeyProperties.AUTH_DEVICE_CREDENTIAL)
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(spec.build())
        return generator.generateKey()
    }
}
