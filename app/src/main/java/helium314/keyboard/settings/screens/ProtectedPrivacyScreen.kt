// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.settings.SearchSettingsScreen
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** One bounded, encrypted private note. Plaintext is never written to preferences, logs or backup. */
private object PrivateVaultStore {
    private const val KEY_ALIAS = "brr.private.vault.biometric.v1"
    private const val MAGIC = 0x42525631 // BRV1
    private const val MAX_PLAINTEXT_BYTES = 8 * 1024
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    data class DecryptOperation(val cipher: Cipher, val iv: ByteArray)

    fun hasNote(context: Context): Boolean = file(context).baseFile.exists()

    fun newEncryptOperation(context: Context): Cipher {
        requireStrongBiometricPlatform()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher
    }

    fun newDecryptOperation(context: Context): DecryptOperation {
        requireStrongBiometricPlatform()
        val bytes = file(context).openRead().use { it.readBytes() }
        require(bytes.size > 4 + IV_BYTES && readInt(bytes, 0) == MAGIC) { "Private note is missing or invalid" }
        val iv = bytes.copyOfRange(4, 4 + IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        return DecryptOperation(cipher, iv)
    }

    fun save(context: Context, cipher: Cipher, note: String) {
        val plain = note.toByteArray(Charsets.UTF_8)
        require(plain.size <= MAX_PLAINTEXT_BYTES) { "Private note is too large" }
        try {
            cipher.updateAAD(aad())
            val encrypted = cipher.doFinal(plain)
            val encoded = byteArrayOf(
                (MAGIC ushr 24).toByte(), (MAGIC ushr 16).toByte(),
                (MAGIC ushr 8).toByte(), MAGIC.toByte()
            ) + cipher.iv + encrypted
            val atomic = file(context)
            val stream = atomic.startWrite()
            try {
                stream.write(encoded)
                atomic.finishWrite(stream)
            } catch (t: Throwable) {
                atomic.failWrite(stream)
                throw t
            }
            encoded.fill(0)
            encrypted.fill(0)
        } finally {
            plain.fill(0)
        }
    }

    fun read(context: Context, cipher: Cipher): String {
        val bytes = file(context).openRead().use { it.readBytes() }
        require(bytes.size > 4 + IV_BYTES && readInt(bytes, 0) == MAGIC) { "Private note is missing or invalid" }
        val encrypted = bytes.copyOfRange(4 + IV_BYTES, bytes.size)
        return try {
            cipher.updateAAD(aad())
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } finally {
            encrypted.fill(0)
            bytes.fill(0)
        }
    }

    fun delete(context: Context) {
        file(context).delete()
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun file(context: Context) = AtomicFile(File(context.noBackupFilesDir, "private_note.v1"))
    private fun aad() = "brrBoard/private-note/v1".toByteArray(Charsets.UTF_8)
    private fun requireStrongBiometricPlatform() {
        check(Build.VERSION.SDK_INT >= 30) { "Strong biometric-only storage requires Android 11 or newer" }
    }
    private fun readInt(bytes: ByteArray, offset: Int) =
        ((bytes[offset].toInt() and 255) shl 24) or ((bytes[offset + 1].toInt() and 255) shl 16) or
            ((bytes[offset + 2].toInt() and 255) shl 8) or (bytes[offset + 3].toInt() and 255)
}

@Composable
fun ProtectedPrivacyScreen(
    onClickBack: () -> Unit,
    onOpenBrainDetails: () -> Unit = {},
    showDashboardAfterAuth: Boolean = false,
    onOpenCorrection: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var authenticated by remember { mutableStateOf(false) }
    var promptPending by remember { mutableStateOf(false) }
    var hasNote by remember { mutableStateOf(PrivateVaultStore.hasNote(context)) }
    var note by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Confirm a strong biometric to open this protected area.") }

    fun prompt(cipher: Cipher? = null, onSuccess: (Cipher?) -> Unit) {
        if (Build.VERSION.SDK_INT < 30 || activity == null) {
            status = "Biometric-only protection requires Android 11 or newer. This area remains locked."
            return
        }
        if (promptPending) return
        promptPending = true
        try {
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle("Unlock protected data")
                .setSubtitle("Use a strong biometric. Device PIN and password are not accepted.")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            val executor = activity.mainExecutor
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptPending = false
                    authenticated = true
                    status = "Protected area unlocked for this foreground session."
                    onSuccess(result.cryptoObject?.cipher)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptPending = false
                    authenticated = false
                    note = ""
                    status = "Still locked: $errString"
                }

                override fun onAuthenticationFailed() {
                    status = "Biometric not recognized. Protected data remains locked."
                }
            }
            val signal = CancellationSignal()
            if (cipher == null) prompt.authenticate(signal, executor, callback)
            else prompt.authenticate(BiometricPrompt.CryptoObject(cipher), signal, executor, callback)
        } catch (error: Exception) {
            promptPending = false
            authenticated = false
            status = "Biometric protection is unavailable. Protected data remains locked."
        }
    }

    LaunchedEffect(activity) { prompt() {} }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                authenticated = false
                note = ""
                status = "Locked when the app left the foreground."
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            authenticated = false
            note = ""
        }
    }

    if (showDashboardAfterAuth && authenticated) {
        BrainDashboardScreen(
            onClickBack = onClickBack,
            onOpenCorrection = onOpenCorrection,
            showDetailedStats = true,
        )
    } else SearchSettingsScreen(onClickBack = onClickBack, title = "Protected privacy", settings = emptyList()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Biometric privacy", style = MaterialTheme.typography.titleLarge)
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    if (!authenticated) {
                        Button(onClick = { prompt() {} }, enabled = !promptPending) {
                            Text(if (promptPending) "Waiting for fingerprint…" else "Unlock with fingerprint")
                        }
                    }
                }
            }
            if (authenticated) {
                if (!showDashboardAfterAuth) {
                    OutlinedButton(onClick = onOpenBrainDetails, enabled = !promptPending) {
                        Text("Open detailed BR Brain")
                    }
                }
                Text("Private note vault", style = MaterialTheme.typography.titleLarge)
                Text("One small note, encrypted locally. It is excluded from Android backup and is never copied automatically.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!hasNote) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.toByteArray(Charsets.UTF_8).size <= 8 * 1024) note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Private note") },
                        minLines = 3,
                        maxLines = 6,
                    )
                    Button(onClick = {
                        try { prompt(PrivateVaultStore.newEncryptOperation(context)) { cipher ->
                            try {
                                requireNotNull(cipher)
                                PrivateVaultStore.save(context, cipher, note)
                                hasNote = true
                                status = "Private note saved with biometric-bound encryption."
                            } catch (_: Exception) { status = "Could not save the note. It was not stored in plaintext." }
                        } } catch (_: Exception) { status = "Could not prepare encrypted storage. No plaintext copy was saved." }
                    }, enabled = note.isNotBlank() && !promptPending) { Text("Save securely") }
                } else {
                    OutlinedButton(onClick = {
                        try {
                            val operation = PrivateVaultStore.newDecryptOperation(context)
                            prompt(operation.cipher) { cipher ->
                                try {
                                    note = PrivateVaultStore.read(context, requireNotNull(cipher))
                                    status = "Note decrypted only after biometric authorization."
                                } catch (_: Exception) { note = ""; status = "The note could not be decrypted; no fallback was used." }
                            }
                        } catch (_: Exception) { status = "The note is unavailable or its biometric key was invalidated." }
                    }, enabled = !promptPending) { Text("Open note with fingerprint") }
                    if (note.isNotEmpty()) {
                        OutlinedTextField(value = note, onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Decrypted private note") },
                            minLines = 3, maxLines = 6)
                        Button(onClick = {
                            try { prompt(PrivateVaultStore.newEncryptOperation(context)) { cipher ->
                                try {
                                    PrivateVaultStore.save(context, requireNotNull(cipher), note)
                                    status = "Private note updated."
                                } catch (_: Exception) { status = "Could not update the note; encrypted data was retained." }
                            } } catch (_: Exception) { status = "Could not prepare encrypted storage." }
                        }, enabled = !promptPending) { Text("Save changes with fingerprint") }
                        OutlinedButton(onClick = {
                            activity?.let { host ->
                                AlertDialog.Builder(host)
                                    .setTitle("Delete private note?")
                                    .setMessage("This permanently removes the encrypted note.")
                                    .setNegativeButton("Cancel", null)
                                    .setPositiveButton("Delete") { _, _ ->
                                        PrivateVaultStore.delete(context)
                                        note = ""
                                        hasNote = false
                                        status = "Private note deleted."
                                    }.show()
                            }
                        }) { Text("Delete note") }
                    }
                }
            }
        }
    }
}
