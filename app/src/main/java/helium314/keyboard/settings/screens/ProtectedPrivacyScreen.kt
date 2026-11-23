// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.latin.PrivateGestureSession
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.NoBackupStorageMigration
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

    fun hasNote(context: Context): Boolean {
        val migrated = runCatching {
            NoBackupStorageMigration.migrateEncryptedRecord(context, "private_note.v1")
        }.getOrDefault(false)
        if (!migrated) {
            // If storage cannot be inspected, fail closed as though a note exists. This
            // prevents the first render from crashing or offering to overwrite a vault.
            return runCatching {
                val legacy = File(context.createDeviceProtectedStorageContext().noBackupFilesDir, "private_note.v1")
                legacy.exists() || file(context).baseFile.exists()
            }.getOrDefault(true)
        }
        return runCatching { file(context).baseFile.exists() }.getOrDefault(true)
    }

    fun newEncryptOperation(context: Context): Cipher {
        requireStrongBiometricPlatform()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher
    }

    fun newDecryptOperation(context: Context): DecryptOperation {
        requireStrongBiometricPlatform()
        check(NoBackupStorageMigration.migrateEncryptedRecord(context, "private_note.v1")) {
            "Private note storage migration did not complete"
        }
        val bytes = file(context).openRead().use { it.readBytes() }
        require(bytes.size > 4 + IV_BYTES && readInt(bytes, 0) == MAGIC) { "Private note is missing or invalid" }
        val iv = bytes.copyOfRange(4, 4 + IV_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        return DecryptOperation(cipher, iv)
    }

    fun save(context: Context, cipher: Cipher, note: String) {
        check(NoBackupStorageMigration.migrateEncryptedRecord(context, "private_note.v1")) {
            "Private note storage migration did not complete"
        }
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
        check(NoBackupStorageMigration.migrateEncryptedRecord(context, "private_note.v1")) {
            "Private note storage migration did not complete"
        }
        val bytes = file(context).openRead().use { it.readBytes() }
        require(bytes.size > 4 + IV_BYTES && readInt(bytes, 0) == MAGIC) { "Private note is missing or invalid" }
        val encrypted = bytes.copyOfRange(4 + IV_BYTES, bytes.size)
        return try {
            cipher.updateAAD(aad())
            val plaintext = cipher.doFinal(encrypted)
            try {
                String(plaintext, Charsets.UTF_8)
            } finally {
                plaintext.fill(0)
            }
        } finally {
            encrypted.fill(0)
            bytes.fill(0)
        }
    }

    fun delete(context: Context) {
        file(context).delete()
        File(context.createDeviceProtectedStorageContext().noBackupFilesDir, "private_note.v1").delete()
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.deleteEntry(KEY_ALIAS)
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}

@Composable
fun ProtectedPrivacyScreen(
    onClickBack: () -> Unit,
    onOpenBrainDetails: () -> Unit = {},
    showDashboardAfterAuth: Boolean = false,
    onOpenCorrection: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val gestureConfigured = remember(context) {
        context.prefs().getBoolean(PrivateGestureSession.PREF_ENABLED, false)
            && PrivateGestureVault.isConfigured(context)
    }
    var authenticated by remember { mutableStateOf(false) }
    var promptPending by remember { mutableStateOf(false) }
    var legacyNote by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Set up a custom footer gesture in Privacy & protected apps, then use it here.") }
    val gestureEvent by PrivateGestureSession.eventFlow.collectAsState(initial = null)

    DisposableEffect(activity) {
        val window = activity?.window
        val wasSecure = window != null && (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (window != null && !wasSecure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun promptLegacyNote() {
        if (Build.VERSION.SDK_INT < 30 || activity == null || promptPending) {
            status = "The old note remains encrypted and untouched; strong biometric access is paused here."
            return
        }
        val availability = try {
            activity.getSystemService(BiometricManager::class.java)
                ?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                ?: BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
        } catch (_: SecurityException) { BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE }
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            status = "The legacy note is still encrypted. The biometric sensor is unavailable; no data was changed."
            return
        }
        try {
            val operation = PrivateVaultStore.newDecryptOperation(context)
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle("Open previously saved note")
                .setSubtitle("Strong biometric only; the note stays encrypted if unavailable.")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            promptPending = true
            prompt.authenticate(CancellationSignal(), activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        promptPending = false
                        try {
                            legacyNote = PrivateVaultStore.read(context, requireNotNull(result.cryptoObject?.cipher))
                            status = "Legacy note decrypted for this foreground session."
                        } catch (_: Exception) { status = "Could not decrypt the legacy note; the encrypted file was preserved." }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        promptPending = false
                        status = "Legacy note remains locked; no content was changed."
                    }
                    override fun onAuthenticationFailed() {
                        status = "Biometric not recognized. Legacy note remains locked."
                    }
                })
        } catch (_: Exception) {
            promptPending = false
            status = "Could not open the biometric prompt. The existing encrypted note was preserved."
        }
    }

    LaunchedEffect(gestureEvent) {
        when (gestureEvent) {
            PrivateGestureSession.Event.Unlocked -> {
                authenticated = true
                status = "Protected details unlocked for this foreground session."
                PrivateGestureSession.cancelCapture()
                keyboard?.hide()
            }
            PrivateGestureSession.Event.Mismatch -> status = "Gesture did not match. Protected details remain locked."
            is PrivateGestureSession.Event.SampleCaptured -> Unit
            PrivateGestureSession.Event.SecretInserted, null -> Unit
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                authenticated = false
                legacyNote = null
                PrivateGestureSession.lock()
                status = "Locked when the app left the foreground."
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            PrivateGestureSession.lock()
            authenticated = false
            legacyNote = null
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Custom gesture access", style = MaterialTheme.typography.titleLarge)
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                    if (!authenticated) {
                        if (!gestureConfigured) {
                            Text("Set up a private gesture and secret in Settings → Privacy & protected apps first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            var unlockProbe by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = unlockProbe,
                                onValueChange = { unlockProbe = "" },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                label = { Text("Gesture entry (input is immediately discarded)") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                    autoCorrectEnabled = false,
                                ),
                                singleLine = true,
                            )
                            Button(onClick = {
                                PrivateGestureSession.beginUnlock()
                                focusRequester.requestFocus()
                                keyboard?.show()
                                status = "Draw your saved gesture in the center strip below the space bar."
                            }, enabled = !promptPending) {
                                Text("Unlock with keyboard gesture")
                            }
                            Text("The gesture is limited to the reserved footer strip. This is a convenience unlock; it is less resistant to observation than biometrics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Private secret", style = MaterialTheme.typography.titleLarge)
                Text("The enrolled secret is encrypted on this device and inserted only after the reserved footer gesture matches. It is excluded in Incognito, password, secure-app, no-learning, dictation and accessibility touch-exploration states.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (PrivateVaultStore.hasNote(context)) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Previously saved biometric note", style = MaterialTheme.typography.titleMedium)
                            Text("This older note stays encrypted and was not discarded. It still uses its original biometric-bound key.",
                                style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = ::promptLegacyNote, enabled = !promptPending) {
                                Text(if (promptPending) "Waiting for fingerprint…" else "Try opening old note")
                            }
                        }
                    }
                }
                if (legacyNote != null) {
                    OutlinedTextField(value = legacyNote.orEmpty(), onValueChange = {},
                        modifier = Modifier.fillMaxWidth(), readOnly = true,
                        label = { Text("Legacy note — read only") }, minLines = 3, maxLines = 6)
                }
                Text("Unlocking lasts only while this screen stays in the foreground; returning to another app locks it again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
