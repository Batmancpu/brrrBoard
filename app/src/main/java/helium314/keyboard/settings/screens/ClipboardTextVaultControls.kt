// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.database.ClipboardDao
import helium314.keyboard.latin.database.ClipboardTextVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ClipboardTextVaultControls() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<ClipboardTextVault.Status?>(null) }
    var busy by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        status = withContext(Dispatchers.IO) {
            runCatching { ClipboardDao.getInstance(context)?.textVaultStatus() }.getOrNull()
        }
        busy = false
    }
    fun activate() {
        if (busy) return
        busy = true; failed = false
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val dao = ClipboardDao.getInstance(context)
                val updated = runCatching { dao?.enableTextVault() }
                val actual = runCatching { dao?.textVaultStatus() }.getOrNull()
                (updated.getOrNull()==null) to actual
            }
            failed=result.first; status=result.second; busy=false
        }
    }
    Card {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Clipboard text vault",style=MaterialTheme.typography.titleMedium)
            Text("Optional device-bound AES-GCM protection for saved clipboard text and image references. Image files themselves are not yet encrypted. Does not protect the system clipboard, RAM or earlier backups.",
                style=MaterialTheme.typography.bodySmall)
            Text(when(status) {
                ClipboardTextVault.Status.ENCRYPTED -> "Enabled · database cleanup completed"
                ClipboardTextVault.Status.CLEANUP_PENDING -> "Enabled · SQLite cleanup pending"
                ClipboardTextVault.Status.OFF -> "Not enabled"
                null -> "Storage unavailable"
            })
            if (status != ClipboardTextVault.Status.ENCRYPTED)
                Button(enabled=!busy && status!=null,onClick={confirm=true},modifier=Modifier.fillMaxWidth()) {
                    Text(if(status==ClipboardTextVault.Status.CLEANUP_PENDING) "Retry database cleanup" else "Encrypt stored clipboard text")
                }
            if(failed) Text("Could not finish. Existing clipboard records have not been discarded.",
                color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},
        title={Text("Protect saved clipboard text?")},
        text={Text("This one-way upgrade preserves existing rows and pins. Keep your device key; old backups and images are not protected by this migration. A separately opted-in portable text backup is readable plaintext.")},
        confirmButton={Button(onClick={confirm=false;activate()}){Text("Proceed")}},
        dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
}
