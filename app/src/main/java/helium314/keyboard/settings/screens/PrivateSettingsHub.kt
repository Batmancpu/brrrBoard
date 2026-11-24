// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import helium314.keyboard.settings.SearchSettingsScreen

/** The NavHost owns authorization, not this UI. No biometric prompt on entry. */
@Composable
internal fun PrivateSettingsHub(
    onClickBack: () -> Unit,
    onOpenBrainDetails: () -> Unit,
    onManageCode: () -> Unit,
    onLock: () -> Unit,
) {
    SearchSettingsScreen(onClickBack = onClickBack, title = "Private settings",
        settings = emptyList(), hideTopSearchBar = true) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Unlocked for this foreground session.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onOpenBrainDetails, modifier = Modifier.fillMaxWidth()) {
                Text("BR Brain details")
            }
            OutlinedButton(onClick = onManageCode, modifier = Modifier.fillMaxWidth()) {
                Text("Manage WhatsApp secret code")
            }
            TextButton(onClick = onLock) { Text("Lock now") }
        }
    }
}
