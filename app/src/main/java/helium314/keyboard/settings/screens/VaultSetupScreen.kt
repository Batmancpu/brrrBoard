// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.latin.VaultFooterGestureView
import helium314.keyboard.latin.VaultGestureStore
import helium314.keyboard.latin.VaultGestureTrace
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchSettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** No saved-state password, raw traces, clipboard or exported entry point. */
@Composable
internal fun VaultSetupScreen(onClickBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.getActivity()
    val owner = LocalLifecycleOwner.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val samples = remember { mutableStateListOf<FloatArray>() }
    var authorized by remember { mutableStateOf(false) }
    var pendingAuth by remember { mutableStateOf(false) }
    var configured by remember { mutableStateOf(VaultGestureStore.configured(context)) }
    var password by remember { mutableStateOf("") }
    var instant by remember { mutableStateOf(false) }
    var enrolling by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Confirm your Android screen lock to continue.") }
    fun clearEnrollment() {
        samples.forEach { it.fill(0f) }; samples.clear()
        password = ""; enrolling = false
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        pendingAuth = false
        authorized = result.resultCode == Activity.RESULT_OK && VaultGestureStore.isDeviceUnlocked(context)
        if (authorized) {
            authorized = runCatching { VaultGestureStore.resetFailures(context) }.isSuccess
            status = if (!authorized) "Could not unlock vault." else if (configured) "Vault unlocked. Your saved password stays hidden." else "Enter a password and enroll your gesture."
        } else status = "Authentication cancelled or unavailable."
    }
    fun authenticate() {
        val kg = context.getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT < 30 || kg == null || !kg.isDeviceSecure) {
            status = "Android 11 or later with a secure screen lock is required."; return
        }
        @Suppress("DEPRECATION")
        val intent = kg.createConfirmDeviceCredentialIntent("Private password vault", "Confirm your device credential")
        if (intent == null) { status = "Device credential unavailable."; return }
        pendingAuth = true
        try { launcher.launch(intent) } catch (_: Exception) {
            pendingAuth = false; status = "Unable to start system authentication."
        }
    }
    DisposableEffect(activity, owner) {
        val window = activity?.window
        val wasSecure = window != null && window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                authorized = false
                clearEnrollment()
                confirmDelete = false
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            clearEnrollment()
            if (!wasSecure) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    LaunchedEffect(Unit) { authenticate() }
    SearchSettingsScreen(onClickBack = onClickBack, title = "Private password vault",
        settings = emptyList(), hideTopSearchBar = true) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(status, style = MaterialTheme.typography.bodyMedium)
            if (!authorized) {
                Button(onClick = { authenticate() }, enabled = !pendingAuth && !busy) { Text("Unlock with device credential") }
            } else {
                Text("Start in the center footer below the space bar, then draw across the keyboard without lifting your finger. The gesture works in masked password fields outside protected apps. Never in ordinary text or swipe typing. Use brrBoard as your active keyboard when entering your password here.", style = MaterialTheme.typography.bodyMedium)
                Text("Smart mode requires recent Android screen-lock authentication (five minutes). Instant mode is less secure: anyone who knows your gesture can use it on your unlocked device.", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Allow instant gesture on unlocked phone", Modifier.weight(1f))
                    Switch(checked = instant, onCheckedChange = { instant = it }, enabled = !enrolling && !busy)
                }
                if (!enrolling) {
                    OutlinedTextField(value = password, onValueChange = { if (it.length <= 512) password = it },
                        label = { Text(if (configured) "New password (replace)" else "Password to insert") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        enabled = !busy)
                    Button(onClick = {
                        focus.clearFocus(); keyboard?.hide()
                        samples.forEach { it.fill(0f) }; samples.clear()
                        enrolling = true; status = "Start below the space bar and draw the same continuous pattern three times."
                    }, enabled = password.length in 4..512 && !busy) { Text("Enroll keyboard pattern") }
                    if (configured) OutlinedButton(onClick = { confirmDelete = true }, enabled = !busy) { Text("Delete saved password and pattern") }
                } else {
                    Text("Sample ${samples.size}/3", style = MaterialTheme.typography.titleMedium)
                    Text("Practice keyboard · start in the outlined footer, continue over any keys, lift once to finish.", style = MaterialTheme.typography.bodySmall)
                    val practiceInk = MaterialTheme.colorScheme.onSurface.toArgb()
                    val practiceAccent = MaterialTheme.colorScheme.primary.toArgb()
                    AndroidView(factory = { VaultFooterGestureView(it).apply { setCaptureEnabled(true) } },
                            modifier = Modifier.fillMaxWidth().height(240.dp).border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            update = { view ->
                                view.practiceInk = practiceInk
                                view.practiceAccent = practiceAccent
                                view.setCaptureEnabled(!busy)
                                view.onRejected = { if (!busy && authorized) status = "Start inside the highlighted footer; draw one continuous shape with a clear turn." }
                                view.onTrace = { sample ->
                                    if (!busy && authorized && samples.size < 3) {
                                        if (samples.any { !VaultGestureTrace.matches(it, sample) }) {
                                            status = "That stroke differs from the saved samples. Try again; accepted samples are kept."
                                        } else {
                                        samples.add(sample.copyOf())
                                        status = "Accepted ${samples.size}/3. Repeat at a comfortable speed."
                                        if (samples.size == 3) {
                                            val template = VaultGestureTrace.average(samples)
                                            if (template == null) {
                                                samples.forEach { it.fill(0f) }; samples.clear()
                                                status = "Patterns were inconsistent. Draw the same shape three times."
                                            } else {
                                                busy = true
                                                val value = password
                                                val mode = instant
                                                scope.launch {
                                                    val ok = try {
                                                        withContext(Dispatchers.IO) { VaultGestureStore.save(context, template, value, mode) }
                                                        true
                                                    } catch (_: Exception) { false }
                                                    finally { template.fill(0f); clearEnrollment(); busy = false }
                                                    configured = VaultGestureStore.configured(context)
                                                    status = if (ok) "Saved encrypted. Draw the pattern in the keyboard footer of a password field." else "Could not save. Reauthenticate and try again."
                                                }
                                            }
                                        }
                                        }
                                    }
                                }
                            })
                    TextButton(onClick = { samples.forEach { it.fill(0f) }; samples.clear(); status = "Samples reset. Draw your new pattern." }, enabled = !busy) { Text("Reset samples") }
                    OutlinedButton(onClick = { clearEnrollment(); status = "Enrollment cancelled." }, enabled = !busy) { Text("Cancel enrollment") }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("Delete saved credential?") }, text = { Text("This removes the encrypted mapping and its vault keys.") },
        confirmButton = { TextButton(onClick = {
            confirmDelete = false; busy = true
            scope.launch {
                val ok = runCatching { withContext(Dispatchers.IO) { VaultGestureStore.delete(context) } }.isSuccess
                busy = false; configured = VaultGestureStore.configured(context)
                status = if (ok) "Saved credential deleted." else "Could not complete deletion."
            }
        }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
}
