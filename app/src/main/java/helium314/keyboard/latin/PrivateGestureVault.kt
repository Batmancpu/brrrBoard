// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypted, no-backup mapping of a private gesture to one user secret. */
internal object PrivateGestureVault {
    private const val KEY_ALIAS = "brr.private.gesture.mapping.v1"
    private const val FILE_NAME = "private_gesture_mapping.v1"
    private const val MAGIC = 0x42504731 // BPG1
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val MAX_CIPHERTEXT_BYTES = 8 * 1024
    private const val MAX_SECRET_BYTES = 2048

    fun isConfigured(context: Context): Boolean = runCatching {
        val mapping = read(context) ?: return false
        mapping.first.fill(0)
        mapping.second.fill(0)
        true
    }.getOrDefault(false)

    fun save(context: Context, template: FloatArray, secret: String) {
        require(template.size == PrivateGestureTrace.POINT_COUNT * 2)
        require(secret.length in MIN_SECRET_CHARS..MAX_SECRET_CHARS)
        val secretBytes = secret.toByteArray(Charsets.UTF_8)
        require(secretBytes.size <= MAX_SECRET_BYTES)
        val plainBuffer = ByteArrayOutputStream()
        DataOutputStream(plainBuffer).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(template.size)
            template.forEach(output::writeFloat)
            output.writeInt(secretBytes.size)
            output.write(secretBytes)
        }
        val plain = plainBuffer.toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(aad())
        val encrypted = cipher.doFinal(plain)
        val iv = cipher.iv
        val encoded = ByteArrayOutputStream()
        DataOutputStream(encoded).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(iv.size)
            output.write(iv)
            output.writeInt(encrypted.size)
            output.write(encrypted)
        }
        val encodedBytes = encoded.toByteArray()
        val atomic = AtomicFile(file(context))
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(encodedBytes)
            stream.fd.sync()
            atomic.finishWrite(stream)
            stream = null
        } finally {
            if (stream != null) atomic.failWrite(stream)
            secretBytes.fill(0)
            plain.fill(0)
            encrypted.fill(0)
            iv.fill(0)
            encodedBytes.fill(0)
        }
    }

    fun matches(context: Context, candidate: FloatArray): Boolean = runCatching {
        val mapping = read(context) ?: return false
        try { PrivateGestureTrace.matches(mapping.first, candidate) }
        finally { mapping.first.fill(0); mapping.second.fill(0) }
    }.getOrDefault(false)

    fun readSecret(context: Context): CharArray? = runCatching {
        val mapping = read(context) ?: return null
        try {
            val decoded = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(mapping.second))
            CharArray(decoded.remaining()).also(decoded::get)
        }
        finally { mapping.first.fill(0); mapping.second.fill(0) }
    }.getOrNull()

    fun clear(context: Context) {
        AtomicFile(file(context)).delete()
        runCatching {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            store.deleteEntry(KEY_ALIAS)
        }
    }

    private fun read(context: Context): Pair<FloatArray, ByteArray>? {
        val atomic = AtomicFile(file(context))
        val encoded = runCatching { atomic.openRead().use { readBounded(it, MAX_CIPHERTEXT_BYTES) } }.getOrNull()
            ?: return null
        var iv = ByteArray(0)
        var encrypted = ByteArray(0)
        var plain = ByteArray(0)
        try {
            DataInputStream(ByteArrayInputStream(encoded)).use { input ->
                require(input.readInt() == MAGIC)
                val ivSize = input.readInt()
                require(ivSize == IV_BYTES)
                iv = ByteArray(ivSize).also(input::readFully)
                val encryptedSize = input.readInt()
                require(encryptedSize in 1..MAX_CIPHERTEXT_BYTES && encryptedSize <= input.available())
                encrypted = ByteArray(encryptedSize).also(input::readFully)
                require(input.available() == 0)
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(aad())
            plain = cipher.doFinal(encrypted)
            DataInputStream(ByteArrayInputStream(plain)).use { input ->
                require(input.readInt() == MAGIC)
                val count = input.readInt()
                require(count == PrivateGestureTrace.POINT_COUNT * 2)
                val template = FloatArray(count) { input.readFloat() }
                val secretSize = input.readInt()
                require(secretSize in 1..MAX_SECRET_BYTES && secretSize == input.available())
                val secret = ByteArray(secretSize).also(input::readFully)
                return template to secret
            }
        } finally {
            encoded.fill(0)
            iv.fill(0)
            encrypted.fill(0)
            plain.fill(0)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build())
        return generator.generateKey()
    }

    private fun readBounded(input: FileInputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (output.size() < limit) {
            val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (count <= 0) break
            output.write(buffer, 0, count)
        }
        buffer.fill(0)
        return output.toByteArray()
    }

    private fun file(context: Context) = File(context.noBackupFilesDir, FILE_NAME)
    private fun aad() = "brrBoard/private-gesture/v1".toByteArray(Charsets.UTF_8)
    const val MIN_SECRET_CHARS = 4
    const val MAX_SECRET_CHARS = 512
}
