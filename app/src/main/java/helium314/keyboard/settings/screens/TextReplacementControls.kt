// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import helium314.keyboard.settings.preferences.SettingsSwitch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.inputlogic.LiteralTextReplacement
import helium314.keyboard.latin.inputlogic.LiteralTextReplacement.Rule
import helium314.keyboard.latin.settings.TextReplacementStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun TextReplacementControls() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<TextReplacementStore.Snapshot?>(null) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        snapshot = withContext(Dispatchers.IO) { TextReplacementStore.load(context) }
        error = snapshot == null; busy = false
    }
    fun save(next: List<Rule>) {
        val expected = snapshot ?: return
        if (busy) return
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val ok = TextReplacementStore.save(context,expected,next)
                ok to TextReplacementStore.load(context)
            }
            snapshot = result.second; error = !result.first; busy = false
        }
    }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Find & replace rules",style=MaterialTheme.typography.titleMedium)
            Text("Create literal rules here, then select text in any eligible app and tap Text tools → Find & replace. Rules are encrypted; no automatic replacement.",
                style=MaterialTheme.typography.bodySmall)
            val items = snapshot?.rules.orEmpty()
            items.forEachIndexed { index,item ->
                OutlinedButton(onClick={ editing=index },enabled=!busy,
                    modifier=Modifier.fillMaxWidth()) { Text(item.name) }
            }
            Button(onClick={ editing=-1 },enabled=!busy && snapshot!=null &&
                items.size < LiteralTextReplacement.MAX_RULES,modifier=Modifier.fillMaxWidth()) { Text("Add rule") }
            if(error) Text("Rule storage unavailable or changed. Existing data was not overwritten.",
                color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }
    }
    editing?.let { index ->
        val base = snapshot?.rules.orEmpty()
        val existing = base.getOrNull(index)
        var name by remember(index) { mutableStateOf(existing?.name.orEmpty()) }
        var find by remember(index) { mutableStateOf(existing?.find.orEmpty()) }
        var replacement by remember(index) { mutableStateOf(existing?.replacement.orEmpty()) }
        var matchCase by remember(index) { mutableStateOf(existing?.matchCase ?: true) }
        var wholeWord by remember(index) { mutableStateOf(existing?.wholeWord ?: false) }
        var all by remember(index) { mutableStateOf(existing?.all ?: true) }
        val rule = Rule(name.trim(),find,replacement,matchCase,wholeWord,all)
        val next = if(index<0) base+rule else base.toMutableList().apply { set(index,rule) }
        AlertDialog(onDismissRequest={ editing=null },title={ Text(if(index<0) "Add replacement rule" else "Edit rule") },
            text={
                Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name,{ if(it.length<=LiteralTextReplacement.MAX_NAME) name=it },singleLine=true,label={Text("Name")})
                    OutlinedTextField(find,{ if(it.length<=LiteralTextReplacement.MAX_FIND) find=it },label={Text("Find (literal)")})
                    OutlinedTextField(replacement,{ if(it.length<=LiteralTextReplacement.MAX_REPLACEMENT) replacement=it },
                        label={Text("Replace with (empty deletes matches)")})
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Match case",Modifier.weight(1f)); SettingsSwitch(matchCase,{matchCase=it})
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Whole words",Modifier.weight(1f)); SettingsSwitch(wholeWord,{wholeWord=it})
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Replace all",Modifier.weight(1f)); SettingsSwitch(all,{all=it})
                    }
                    Text("No regex. Do not store passwords or private codes.",style=MaterialTheme.typography.bodySmall)
                    if(existing!=null) TextButton(onClick={ editing=null; save(base.filterIndexed { i,_ -> i!=index }) }) {Text("Delete rule")}
                }
            },confirmButton={ Button(enabled=LiteralTextReplacement.validRules(next),onClick={ editing=null;save(next) }) {Text("Save")} },
            dismissButton={TextButton(onClick={editing=null}){Text("Cancel")}})
    }
}
