// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.PrivateGestureSession
import helium314.keyboard.latin.PrivateGestureTrace
import helium314.keyboard.latin.PrivateGestureVault
import helium314.keyboard.latin.utils.prefs
import kotlinx.coroutines.flow.collect

@Composable
internal fun PrivateGestureSetupPanel() {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val samples = remember { mutableStateListOf<FloatArray>() }
    var secret by remember { mutableStateOf("") }
    var enabled by remember {
        mutableStateOf(context.prefs().getBoolean(PrivateGestureSession.PREF_ENABLED, false))
    }
    var configured by remember { mutableStateOf(PrivateGestureVault.isConfigured(context)) }
    var enrolling by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Off by default. Enroll three matching traces to save a custom gesture and secret.") }

    LaunchedEffect(context) {
        PrivateGestureSession.eventFlow.collect { event ->
            when (event) {
                is PrivateGestureSession.Event.SampleCaptured -> {
                    if (!enrolling) {
                        event.normalizedTrace.fill(0f)
                        return@collect
                    }
                    samples.add(event.normalizedTrace.copyOf())
                    event.normalizedTrace.fill(0f)
                    if (samples.size < PrivateGestureTrace.ENROLLMENT_SAMPLES) {
                        status = "Good trace ${samples.size}/${PrivateGestureTrace.ENROLLMENT_SAMPLES}. Draw the same shape again in the center footer strip."
                        PrivateGestureSession.beginEnrollment()
                        focusRequester.requestFocus()
                        keyboard?.show()
                    } else {
                        val template = PrivateGestureTrace.average(samples)
                        try {
                            requireNotNull(template)
                            PrivateGestureVault.save(context, template, secret)
                            template.fill(0f)
                            samples.forEach { it.fill(0f) }
                            samples.clear()
                            secret = ""
                            enrolling = false
                            configured = true
                            enabled = true
                            context.prefs().edit().putBoolean(PrivateGestureSession.PREF_ENABLED, true).apply()
                            PrivateGestureSession.cancelCapture()
                            keyboard?.hide()
                            status = "Saved encrypted on this device. Use this gesture in the footer strip to unlock protected details or insert your secret."
                        } catch (_: Exception) {
                            template?.fill(0f)
                            samples.forEach { it.fill(0f) }
                            samples.clear()
                            enrolling = false
                            PrivateGestureSession.cancelCapture()
                            status = "Could not safely save the gesture. Nothing was enabled; try again."
                        }
                    }
                }
                PrivateGestureSession.Event.Mismatch -> status = "That trace did not match the saved gesture. Try again."
                PrivateGestureSession.Event.Unlocked,
                PrivateGestureSession.Event.SecretInserted -> Unit
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Custom keyboard gesture", style = MaterialTheme.typography.titleLarge)
            Text("The fingerprint unlock path is paused. Set a gesture in the empty footer strip below the space bar, between Emoji and mic. The same saved gesture unlocks protected brrBoard details here and types your saved secret into eligible text fields.",
                style = MaterialTheme.typography.bodyMedium)
            Text("This is a convenience unlock and can be observed or copied by someone using your unlocked phone. It is less secure than biometric authentication. The gesture and secret are stored together only as AES-GCM ciphertext in app-private no-backup storage; neither is added to learning or sent off-device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = secret,
                onValueChange = { value -> if (value.length <= PrivateGestureVault.MAX_SECRET_CHARS) secret = value },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = { Text("Secret to type") },
                supportingText = { Text("Use 4–512 characters. The app does not show the saved secret again.") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                singleLine = true,
            )
            Button(
                onClick = {
                    samples.forEach { it.fill(0f) }
                    samples.clear()
                    enrolling = true
                    status = "Draw your chosen shape in the center footer strip three times. Keep each trace inside the strip."
                    PrivateGestureSession.beginEnrollment()
                    focusRequester.requestFocus()
                    keyboard?.show()
                },
                enabled = secret.length in PrivateGestureVault.MIN_SECRET_CHARS..PrivateGestureVault.MAX_SECRET_CHARS,
            ) { Text(if (configured) "Replace gesture and secret" else "Enroll gesture and secret") }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Allow gesture unlock and insertion", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = enabled,
                    enabled = configured,
                    onCheckedChange = { checked ->
                        enabled = checked
                        context.prefs().edit().putBoolean(PrivateGestureSession.PREF_ENABLED, checked).apply()
                        PrivateGestureSession.cancelCapture()
                        status = if (checked) "Custom gesture enabled." else "Custom gesture disabled. Encrypted mapping remains stored until you delete it."
                    },
                )
            }
            if (configured) {
                OutlinedButton(onClick = { showDeleteConfirm = true }) { Text("Delete saved gesture and secret") }
            }
            if (enrolling) {
                OutlinedButton(onClick = {
                    enrolling = false
                    samples.forEach { it.fill(0f) }
                    samples.clear()
                    secret = ""
                    PrivateGestureSession.cancelCapture()
                    keyboard?.hide()
                    status = "Enrollment cancelled. The existing saved mapping, if any, was kept."
                }) { Text("Cancel enrollment") }
            }
            Text(status, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete custom gesture?") },
            text = { Text("This permanently removes the encrypted gesture template and saved secret from this app.") },
            confirmButton = {
                TextButton(onClick = {
                    PrivateGestureVault.clear(context)
                    context.prefs().edit().putBoolean(PrivateGestureSession.PREF_ENABLED, false).apply()
                    PrivateGestureSession.lock()
                    configured = false
                    enabled = false
                    showDeleteConfirm = false
                    status = "Saved gesture and secret deleted."
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
}
