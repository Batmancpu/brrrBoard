// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import helium314.keyboard.settings.preferences.SettingsSwitch
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import helium314.keyboard.keyboard.EditingGestureConfig
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.utils.prefs

@Composable
internal fun EditingGestureControls() {
    val context = LocalContext.current
    val prefs = context.prefs()
    var config by remember { mutableStateOf(EditingGestureConfig.read(prefs)) }
    fun set(key: String,value: String) {
        prefs.edit().putString(key,value).apply()
        config=EditingGestureConfig.read(prefs)
    }
    Card {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Editing gesture key",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                SettingsSwitch(config.enabled,{ enabled ->
                    prefs.edit().putBoolean(EditingGestureConfig.ENABLED,enabled).apply()
                    config=EditingGestureConfig.read(prefs)
                    KeyboardSwitcher.getInstance().setThemeNeedsReload()
                })
            }
            Text("Optional Edit key in the ordinary alphabet row; unavailable in protected fields, URL/email, split and one-handed layouts. Existing keys and glide typing are unchanged by default.",
                style=MaterialTheme.typography.bodySmall)
            if(config.enabled) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(config.mode==EditingGestureConfig.Mode.LETTER,
                        {set(EditingGestureConfig.MODE,EditingGestureConfig.Mode.LETTER.name)}, {Text("Slide to letter")})
                    FilterChip(config.mode==EditingGestureConfig.Mode.FLICK,
                        {set(EditingGestureConfig.MODE,EditingGestureConfig.Mode.FLICK.name)}, {Text("Directional flick")})
                }
                Text("Tap Edit to open text tools. " + if(config.mode==EditingGestureConfig.Mode.LETTER)
                    "Slide from Edit to C/V/X/A/Z for Copy/Paste/Cut/Select all/Undo."
                    else "Quickly flick from Edit in one direction. Diagonal or long gestures are ignored.",
                    style=MaterialTheme.typography.bodySmall)
                if(config.mode==EditingGestureConfig.Mode.FLICK) {
                    for((label,key,current) in listOf(
                        Triple("Up",EditingGestureConfig.UP,config.up),
                        Triple("Down",EditingGestureConfig.DOWN,config.down),
                        Triple("Left",EditingGestureConfig.LEFT,config.left),
                        Triple("Right",EditingGestureConfig.RIGHT,config.right))) {
                        var expanded by remember(key){mutableStateOf(false)}
                        Box {
                            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text("$label · ${current.label}")}
                            DropdownMenu(expanded,{expanded=false}) {
                                for(action in EditingGestureConfig.Action.entries)
                                    DropdownMenuItem(text={Text(action.label)},onClick={expanded=false;set(key,action.name)})
                            }
                        }
                    }
                }
            }
        }
    }
}
