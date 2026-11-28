// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.AppsManager
import helium314.keyboard.latin.snippets.SnippetCadence
import helium314.keyboard.latin.snippets.SnippetRule
import helium314.keyboard.latin.snippets.SmartSnippetStore
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.SettingsSwitch

@Composable
fun SmartSnippetSettingsScreen(onClickBack: () -> Unit) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf<SnippetRule?>(null) }
    var error by remember { mutableStateOf(false) }
    val rules = remember(revision) { SmartSnippetStore.snapshot(context) }
    val apps = remember { AppsManager(context).getPackagesAndNames().toMap() }
    SearchSettingsScreen(onClickBack,"Smart snippets",emptyList(),content = {
        Text("Create a snippet by long-pressing a text card in the keyboard Clipboard, then select Smart suggestion.",
            modifier = Modifier.padding(16.dp),style = MaterialTheme.typography.bodyMedium)
        if (!remember(revision) { SmartSnippetStore.storageAvailable(context) } || error)
            Text("Smart snippet storage unavailable. Unlock the device and try again.",
                modifier = Modifier.padding(16.dp),color = MaterialTheme.colorScheme.error)
        if (rules.isEmpty()) Text("No configured snippets",modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        rules.forEach { rule ->
            val targets = if (rule.allApps) "All apps" else rule.packages.map { apps[it] ?: it }.joinToString(", ")
            val cadence = when(rule.cadence) {
                SnippetCadence.SESSION -> "Per visit"
                SnippetCadence.DAILY -> "Daily until pasted"
                SnippetCadence.KEYBOARD -> "Per keyboard opening"
            }
            Preference(rule.title,onClick = { edit = rule },
                description = targets + " · " + cadence + " · " + rule.durationSeconds + " s") {
                SettingsSwitch(rule.enabled, {
                    error = !SmartSnippetStore.save(context,rule.copy(enabled=it)); revision++
                })
            }
        }
        Text("Only a private title is displayed. Clipboard contents remain in their existing storage; rules are encrypted separately and excluded from backup.",
            style = MaterialTheme.typography.bodySmall,modifier = Modifier.padding(16.dp))
    })
    edit?.let { original ->
        var draft by remember(original) { mutableStateOf(original) }
        var remove by remember(original) { mutableStateOf(false) }
        var appPicker by remember(original) { mutableStateOf(false) }
        if (appPicker) {
            var query by remember(original) { mutableStateOf("") }
            var selected by remember(original) { mutableStateOf(draft.packages) }
            var all by remember(original) { mutableStateOf(draft.allApps) }
            val packages = (apps.keys + selected).distinct().filter { (apps[it] ?: it).contains(query,true) || it.contains(query,true) }
                .sortedBy { apps[it] ?: it }
            AlertDialog(onDismissRequest={appPicker=false}, title={Text("Choose applications")},text={
                Column {
                    OutlinedTextField(query,{query=it},singleLine=true,label={Text("Search apps")})
                    Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(all,{all=it}); Text("All apps")
                    }
                    Column(Modifier.heightIn(max=280.dp).verticalScroll(rememberScrollState())) {
                        packages.forEach { pkg ->
                            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(pkg in selected,{on -> selected=if(on) selected+pkg else selected-pkg})
                                Text(apps[pkg] ?: pkg)
                            }
                        }
                    }
                }
            },confirmButton={TextButton(enabled=(all || selected.isNotEmpty()) && selected.size<=64,
                onClick={draft=draft.copy(allApps=all,packages=selected);appPicker=false}){Text("Done")}},
                dismissButton={TextButton(onClick={appPicker=false}){Text("Cancel")}})
        } else if (remove) {
            AlertDialog(onDismissRequest={remove=false},title={Text("Remove smart suggestion?")},
                text={Text("The pinned clipboard text will remain available.")},
                confirmButton={TextButton(onClick={error=!SmartSnippetStore.remove(context,original.id);revision++;edit=null}){Text("Remove")}},
                dismissButton={TextButton(onClick={remove=false}){Text("Cancel")}})
        } else AlertDialog(onDismissRequest={edit=null},title={Text("Edit smart snippet")},text={
            Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.title,{draft=draft.copy(title=it.take(80))},singleLine=true,label={Text("Private title")})
                TextButton(onClick={appPicker=true}){Text(if(draft.allApps) "All apps" else "${draft.packages.size} selected apps")}
                Text("Frequency",style=MaterialTheme.typography.labelLarge)
                SnippetCadence.values().forEach { cadence ->
                    Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(draft.cadence==cadence,{draft=draft.copy(cadence=cadence)})
                        Text(when(cadence) { SnippetCadence.SESSION -> "Per app visit"; SnippetCadence.DAILY -> "Daily until pasted"; SnippetCadence.KEYBOARD -> "Every keyboard opening" })
                    }
                }
                Text("Suggestion window")
                Row { listOf(30,60,120,300).forEach { seconds ->
                    FilterChip(draft.durationSeconds==seconds,{draft=draft.copy(durationSeconds=seconds)},label={Text(if(seconds==30) "30s" else "${seconds/60}m")})
                } }
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(draft.emptyAgain,{draft=draft.copy(emptyAgain=it)});Text("Reappear in empty field")
                }
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(draft.newLine,{draft=draft.copy(newLine=it)});Text("Reappear at new empty line")
                }
                Text("Edit days and hours from the keyboard Clipboard menu.")
                TextButton(onClick={remove=true}){Text("Remove smart suggestion")}
            }
        },confirmButton={TextButton(enabled=draft.valid(),onClick={error=!SmartSnippetStore.save(context,draft);revision++;edit=null}){Text("Save")}},
            dismissButton={TextButton(onClick={edit=null}){Text("Cancel")}})
    }
}
