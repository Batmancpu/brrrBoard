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
import helium314.keyboard.latin.inputlogic.DynamicTextTemplate
import helium314.keyboard.latin.inputlogic.DynamicTextTemplate.Template
import helium314.keyboard.latin.settings.TextTemplateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun TextTemplateControls() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<TextTemplateStore.Snapshot?>(null) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        snapshot = withContext(Dispatchers.IO) { TextTemplateStore.load(context) }
        error = snapshot == null
        busy = false
    }
    fun save(next: List<Template>) {
        val expected = snapshot ?: return
        if (busy) return
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val ok = TextTemplateStore.save(context,expected,next)
                ok to TextTemplateStore.load(context)
            }
            snapshot = result.second
            error = !result.first
            busy = false
        }
    }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Text templates", style = MaterialTheme.typography.titleMedium)
            Text("Save your own non-sensitive snippets. Tap Text tools with the cursor between words to insert one. Encrypted locally; no automatic expansion.",
                style = MaterialTheme.typography.bodySmall)
            val items = snapshot?.templates.orEmpty()
            items.forEachIndexed { index,item ->
                OutlinedButton(onClick = { editing = index }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth()) { Text(item.name) }
            }
            Button(onClick = { editing = -1 }, enabled = !busy && snapshot != null &&
                items.size < DynamicTextTemplate.MAX_TEMPLATES, modifier = Modifier.fillMaxWidth()) {
                Text("Add template")
            }
            if (error) Text("Template storage unavailable or changed. Saved data was not overwritten.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
    editing?.let { index ->
        val base = snapshot?.templates.orEmpty()
        val existing = base.getOrNull(index)
        var name by remember(index) { mutableStateOf(existing?.name.orEmpty()) }
        var body by remember(index) { mutableStateOf(existing?.body.orEmpty()) }
        val item = Template(name.trim(),body)
        val next = if (index < 0) base + item else base.toMutableList().apply { set(index,item) }
        AlertDialog(onDismissRequest = { editing = null },
            title = { Text(if (index < 0) "Add template" else "Edit template") },
            text = {
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name,{ if(it.length<=DynamicTextTemplate.MAX_NAME) name=it },
                        singleLine = true,label={ Text("Name") })
                    OutlinedTextField(body,{ if(it.length<=DynamicTextTemplate.MAX_BODY) body=it },
                        minLines=3,label={ Text("Text") })
                    Text("Placeholders: {date}, {time}, {cursor}. Do not save credentials or private codes.",
                        style=MaterialTheme.typography.bodySmall)
                    if (existing != null) TextButton(onClick = {
                        editing = null
                        save(base.filterIndexed { i,_ -> i != index })
                    }) { Text("Delete template") }
                }
            }, confirmButton = {
                Button(enabled = DynamicTextTemplate.validList(next), onClick = {
                    editing = null; save(next)
                }) { Text("Save") }
            }, dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } })
    }
}
