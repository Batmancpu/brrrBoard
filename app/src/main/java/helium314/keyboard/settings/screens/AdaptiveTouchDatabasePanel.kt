// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.brain.AdaptiveTouchStore
import java.util.Locale

/** Only composed inside the secret-authorized detailed BR Brain destination. */
@Composable
internal fun AdaptiveTouchDatabasePanel(refresh: Int) {
    val context=LocalContext.current
    val store=remember(context) { AdaptiveTouchStore.get(context) }
    val layouts=remember(refresh) { store.inspect() }
    val storage=remember(refresh) { store.inspectionStatus() }
    var selectedLayout by remember { mutableStateOf<Long?>(null) }
    var letter by remember { mutableIntStateOf(4) }
    val active=layouts.firstOrNull { it.layout==selectedLayout } ?: layouts.lastOrNull()
    val palette=MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Adaptive Touch database",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
            Text("Storage: $storage",style=MaterialTheme.typography.bodySmall)
            Text("Actual confirmed tap evidence, grouped by intended letter. No typed words, raw tap paths or editor identity are saved. New evidence appears after manually choosing a matching suggestion.",style=MaterialTheme.typography.bodySmall)
            if(active==null) {
                Text("No saved layout evidence yet. Confirm a few typed-word suggestions to begin.")
            } else {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    val index=layouts.indexOf(active)
                    OutlinedButton(onClick={selectedLayout=layouts[(index+layouts.size-1)%layouts.size].layout},enabled=layouts.size>1) { Text("Previous") }
                    Text("Layout ${index+1}/${layouts.size}",Modifier.weight(1f).padding(horizontal=8.dp))
                    OutlinedButton(onClick={selectedLayout=layouts[(index+1)%layouts.size].layout},enabled=layouts.size>1) { Text("Next") }
                }
                Text("Letter selector (not the physical keyboard layout)",style=MaterialTheme.typography.bodySmall)
                for (keys in listOf("qwertyuiop","asdfghjkl","zxcvbnm")) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                        for (key in keys) {
                            val i=key-'a'
                            Box(Modifier.weight(1f).height(34.dp)
                                .background(if(i==letter) palette.primaryContainer else palette.surfaceVariant,RoundedCornerShape(6.dp))
                                .clickable { letter=i },contentAlignment=Alignment.Center) { Text(key.uppercase()) }
                        }
                    }
                }
                val total=active.count(letter)
                val peak=(0 until 25).maxOf { active.countAt(letter,it) }.coerceAtLeast(1)
                Text("Intended letter ${('A'+letter)} · ${total} retained samples",fontWeight=FontWeight.SemiBold)
                Text("Top = above key center; right = right of center. The middle cell is the key center.",style=MaterialTheme.typography.bodySmall)
                for(y in 0 until 5) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                        for(x in 0 until 5) {
                            val n=active.countAt(letter,y*5+x)
                            val intensity=n.toFloat()/peak
                            Box(Modifier.weight(1f).height(34.dp)
                                .background(lerp(palette.surfaceVariant,palette.primary,intensity),RoundedCornerShape(5.dp)),contentAlignment=Alignment.Center) {
                                Text(n.toString(),color=if(intensity>.55f) palette.onPrimary else palette.onSurface)
                            }
                        }
                    }
                }
                Text("Literal key differed from confirmed intended letter: ${active.mismatch(letter)} / $total")
                val region=active.region(letter)
                if(region>=0) {
                    Text(if(active.model.ready(region))
                        String.format(Locale.ROOT,"Learned region offset: X %+.1f%% · Y %+.1f%% of key size",active.model.offsetX(region)*100,active.model.offsetY(region)*100)
                        else "Learning region needs more evidence before changing tap decisions.")
                }
                Text("The heatmap is a bounded, decaying sample window, not a lifetime count. The 6-region model controls uncertain boundaries only; key shapes remain unchanged.",style=MaterialTheme.typography.bodySmall)
                Text("Regional learning",fontWeight=FontWeight.SemiBold)
                for(g in 0 until 6) {
                    Text(String.format(Locale.ROOT,"%s %s · %d evidence · X %+.1f%% · Y %+.1f%%",
                        listOf("Upper","Middle","Lower")[g/2],if(g%2==0)"left" else "right",
                        active.model.evidence(g),active.model.offsetX(g)*100,active.model.offsetY(g)*100),
                        style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
