// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import helium314.keyboard.settings.preferences.SettingsSwitch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.AppsManager
import helium314.keyboard.latin.settings.AppProfileStore

@Composable
internal fun AppProfileControls() {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    val count = remember(revision) { AppProfileStore.packages(context).size }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Per-app profiles", style = MaterialTheme.typography.titleMedium)
            Text("$count configured. Encrypted locally. After six verified autocorrection reversals within one 30-second editor window, BR Brain can disable automatic correction for that app at the next editor session. Explicit overrides win. No typed words or event history are stored.",
                style = MaterialTheme.typography.bodySmall)
            Button(onClick = { query = ""; picking = true }, modifier = Modifier.fillMaxWidth()) { Text("Choose an app") }
            if (count > 0 || error || !remember(revision) { AppProfileStore.storageAvailable(context) }) OutlinedButton(onClick = { reset = true }) { Text("Reset all profiles") }
            if (error || !remember(revision) { AppProfileStore.storageAvailable(context) }) Text("Profile storage unavailable. Reset to recover if necessary.", color = MaterialTheme.colorScheme.error)
        }
    }
    if (reset) AlertDialog(onDismissRequest = { reset = false },
        title = { Text("Reset all profiles?") },
        text = { Text("All apps will use global settings again.") },
        confirmButton = { Button(onClick = {
            error = !AppProfileStore.reset(context); revision++; reset = false
        }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { reset = false }) { Text("Cancel") } })
    if (picking) {
        val installed = remember { AppsManager(context).getPackagesAndNames().toMap() }
        val options = remember(installed, query, revision) {
            (installed.keys + AppProfileStore.packages(context)).distinct()
                .filter { (installed[it] ?: it).contains(query, true) || it.contains(query,true) }
                .sortedBy { installed[it] ?: it }
        }
        AlertDialog(onDismissRequest = { picking = false }, title = { Text("Choose an app") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(query,{ query = it },singleLine = true,label = { Text("Search apps") })
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(options, key = { it }) { pkg ->
                            Column(Modifier.fillMaxWidth().clickable {
                                selected = pkg; picking = false
                            }.padding(vertical = 10.dp)) {
                                Text(installed[pkg] ?: pkg)
                                Text(pkg, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { picking = false }) { Text("Close") } })
    }
    selected?.let { pkg ->
        var profile by remember(pkg) { mutableStateOf(AppProfileStore.get(context,pkg)) }
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("App profile") },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(pkg, style = MaterialTheme.typography.bodySmall)
                    ProfileChoice("Autocorrect",profile.autocorrect) { profile = profile.copy(autocorrect = it) }
                    if (profile.automaticAutocorrectOff) {
                        Text("Learned: autocorrection off when inheriting", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { profile = profile.copy(automaticAutocorrectOff = false) }) { Text("Forget learned preference") }
                    }
                    ProfileChoice("Suggestions",profile.suggestions) { profile = profile.copy(suggestions = it) }
                    ProfileChoice("Auto-capitalization",profile.autoCap) { profile = profile.copy(autoCap = it) }
                    ProfileChoice("Double-space period",profile.doubleSpacePeriod) { profile = profile.copy(doubleSpacePeriod = it) }
                    ProfileChoice("Number row",profile.numberRow) { profile = profile.copy(numberRow = it) }
                    ProfileChoice("Sound",profile.sound) { profile = profile.copy(sound = it) }
                    ProfileChoice("Haptics",profile.haptics) { profile = profile.copy(haptics = it) }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Always private",modifier = Modifier.weight(1f))
                        SettingsSwitch(profile.privateMode,{ profile = profile.copy(privateMode=it) })
                    }
                    Text("Inherit uses global settings. Always private only tightens privacy.",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { profile = AppProfileStore.Profile() }) { Text("Clear overrides") }
                }
            }, confirmButton = { Button(onClick = {
                error = !AppProfileStore.save(context,pkg,profile)
                revision++; selected = null
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Cancel") } })
    }
}

@Composable private fun ProfileChoice(label: String, value: Boolean?, change: (Boolean?) -> Unit) {
    Column {
        Text(label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "Inherit", true to "On", false to "Off").forEach { (choice, name) ->
                FilterChip(selected = value == choice, onClick = { change(choice) }, label = { Text(name) })
            }
        }
    }
}
