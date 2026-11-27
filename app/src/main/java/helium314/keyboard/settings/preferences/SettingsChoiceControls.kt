// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.settings.SettingsVisuals
import helium314.keyboard.settings.materialSymbols

/** The parent selectable row owns semantics, so a checkmark is decoration only. */
@Composable
internal fun SettingsChoiceMark(selected: Boolean) {
    Box(Modifier.size(24.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (selected) Text("check", fontFamily = materialSymbols, fontSize = 23.sp,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun SettingsSegments(labels: List<String>, selected: Int, onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(SettingsVisuals.fieldRadius)
    Row(modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(3.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            Box(Modifier.weight(1f).heightIn(min = SettingsVisuals.controlTarget)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                .selectable(selected = active, role = Role.Tab, onClick = { onSelected(index) })
                .padding(horizontal = 3.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (active)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
