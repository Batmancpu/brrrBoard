// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import helium314.keyboard.keyboard.AdaptiveTouchGeometry
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
                Text("Keyboard evidence overview (QWERTY reference, not physical key geometry)",style=MaterialTheme.typography.bodySmall)
                val allPeak=(0 until 26).maxOf { active.count(it) }.coerceAtLeast(1)
                for (keys in listOf("qwertyuiop","asdfghjkl","zxcvbnm")) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                        for (key in keys) {
                            val i=key-'a'
                            Box(Modifier.weight(1f).height(34.dp)
                                .background(lerp(palette.surfaceVariant,palette.primary,active.count(i).toFloat()/allPeak*.70f),RoundedCornerShape(6.dp))
                                .clickable { letter=i },contentAlignment=Alignment.Center) {
                                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                    Text(key.uppercase(),fontWeight=if(i==letter) FontWeight.Bold else FontWeight.Normal)
                                    Text(active.count(i).toString(),style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Text("Each key shows retained samples; tap it for the detailed heatmap below.",style=MaterialTheme.typography.bodySmall)
                val map=remember(active.layout,active.model) { AdaptiveTouchGeometry.sampleDecisionMap(active.layout,active.model) }
                if(map!=null) {
                    Text("Sampled recognition boundaries",fontWeight=FontWeight.SemiBold)
                    Text("Original detector",style=MaterialTheme.typography.bodySmall)
                    RecognitionMap(map,letter,false)
                    Text("Adaptive detector",style=MaterialTheme.typography.bodySmall)
                    RecognitionMap(map,letter,true)
                    Text("${map.changes} of ${map.columns*map.rows} sampled positions change letter. Highlight: selected key; accent: changed decision. This is an isolated-tap simulation, not recorded touch locations or swipe/hysteresis behaviour.",style=MaterialTheme.typography.bodySmall)
                } else Text("Open the matching alphabet layout and type a few letters to make its sampled recognition map available. Saved evidence alone cannot reconstruct physical boundaries.",style=MaterialTheme.typography.bodySmall)
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
                val literalKeys=(0 until 26).filter { active.mismatchMask(letter) and (1 shl it) != 0 }.joinToString(", ") { ('A'+it).toString() }
                Text(if(literalKeys.isEmpty()) "No retained literal-key identities." else "Recent mismatching literal keys: $literalKeys",style=MaterialTheme.typography.bodySmall)
                Text("Identities are unordered and expire at decay; older mismatches may have no identity.",style=MaterialTheme.typography.bodySmall)
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

@Composable private fun RecognitionMap(map: AdaptiveTouchGeometry.DecisionMap,letter: Int,adapted: Boolean) {
    val palette=MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxWidth().aspectRatio(map.aspect.coerceIn(1f,8f))) {
        val dx=size.width/map.columns
        val dy=size.height/map.rows
        for(row in 0 until map.rows) for(col in 0 until map.columns) {
            val i=row*map.columns+col
            val raw=map.originalAt(i)
            val key=if(adapted) map.adaptedAt(i) else raw
            val color=when {
                key<0 -> palette.surface
                adapted && key!=raw -> palette.tertiary
                key==letter -> palette.primary
                else -> lerp(palette.surfaceVariant,palette.onSurfaceVariant,(key%4)*.12f)
            }
            drawRect(color,Offset(col*dx,row*dy),Size(dx+.3f,dy+.3f))
        }
    }
}
