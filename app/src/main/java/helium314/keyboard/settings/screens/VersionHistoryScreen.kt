// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import helium314.keyboard.settings.SettingsVisuals
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.settings.SearchSettingsScreen

@Composable
fun VersionHistoryScreen(onClickBack: () -> Unit) {
    val context = LocalContext.current
    val releases = remember(context) { VersionHistoryCatalog.load(context) }
    var selectedVersion by rememberSaveable { mutableStateOf(releases.firstOrNull()?.version.orEmpty()) }
    var menuOpen by remember { mutableStateOf(false) }
    val selected = releases.firstOrNull { it.version == selectedVersion } ?: releases.firstOrNull()

    SearchSettingsScreen(onClickBack = onClickBack,
        title = stringResource(R.string.version_history),
        settings = emptyList(), hideTopSearchBar = true) {
        Column(modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (selected == null) {
                Text(stringResource(R.string.version_history_empty),
                    style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            Text(stringResource(R.string.version_history_select),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { menuOpen = true },
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("v${selected.version}")
                        Text("▾")
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false },
                    modifier = Modifier.fillMaxWidth(0.9f)) {
                    releases.forEach { release ->
                        DropdownMenuItem(
                            text = { Text("v${release.version}") },
                            onClick = {
                                selectedVersion = release.version
                                menuOpen = false
                            },
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsVisuals.groupRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("v${selected.version}", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(selected.summary, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(VersionHistoryCatalog.localTime(selected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(if (selected.isPublicationTime)
                            R.string.version_history_published_at
                        else R.string.version_history_prepared_at),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Text(stringResource(R.string.version_history_changes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            selected.changes.forEach { change ->
                Card(modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(SettingsVisuals.groupRadius),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(change.title, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(change.detail, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
