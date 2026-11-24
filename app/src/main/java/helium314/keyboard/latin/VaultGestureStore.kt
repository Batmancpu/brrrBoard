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
    fun resetFailures(context: Context) { check(context.prefs().edit().putInt(PREF_FAILURES, 0).commit()) }

    @Synchronized fun save(context: Context, template: FloatArray, password: String, instant: Boolean) {
        require(isDeviceUnlocked(context))
        require(template.size == VaultGestureTrace.POINT_COUNT * 2 && template.all { it.isFinite() })
        require(password.length in 4..512)
        val bytes = password.toByteArray(Charsets.UTF_8)
        var plain = ByteArray(0)
        var encrypted = ByteArray(0)
        var encoded = ByteArray(0)
        try {
            require(bytes.size <= 2048)
            val mode = if (instant) 2 else 1
            plain = ByteBuffer.allocate(4 + template.size * 4 + bytes.size).apply {
                putInt(bytes.size); template.forEach { putFloat(it) }; put(bytes)
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
            check(context.prefs().edit().putInt(PREF_FAILURES, 0).putBoolean(PREF_ENABLED, true).commit())
        } finally {
            bytes.fill(0); plain.fill(0); encrypted.fill(0); encoded.fill(0)
        }
    }

    @Synchronized fun delete(context: Context) {
        check(context.prefs().edit().putBoolean(PREF_ENABLED, false).putInt(PREF_FAILURES, 0).commit())
        file(context).delete()
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.deleteEntry(ALIAS + 1); ks.deleteEntry(ALIAS + 2)
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
        if (!isDeviceUnlocked(context) || !context.prefs().getBoolean(PREF_ENABLED, false)) return null
        val failures = context.prefs().getInt(PREF_FAILURES, 0)
        if (failures >= MAX_FAILURES) return null
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
        val template = FloatArray(VaultGestureTrace.POINT_COUNT * 2)
        try {
            val mode = encoded[0].toInt()
            require(mode == 1 || mode == 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(mode, false), GCMParameterSpec(128, encoded, 1, 12))
            cipher.updateAAD(aad(mode))
            plain = cipher.doFinal(encoded, 13, encoded.size - 13)
            val input = ByteBuffer.wrap(plain)
            val size = input.int
            require(size in 1..2048 && input.remaining() == template.size * 4 + size)
            for (i in template.indices) template[i] = input.float
            if (!VaultGestureTrace.matches(template, trace)) {
                context.prefs().edit().putInt(PREF_FAILURES, failures + 1).commit()
                return null
            }
            val chars = Charsets.UTF_8.newDecoder().decode(input)
            try {
                if (!context.prefs().edit().putInt(PREF_FAILURES, 0).commit()) return null
                return CharArray(chars.remaining()).also { chars.get(it) }
            } finally { if (chars.hasArray()) chars.array().fill('\u0000') }
        } finally { encoded.fill(0); plain.fill(0); template.fill(0f) }
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
