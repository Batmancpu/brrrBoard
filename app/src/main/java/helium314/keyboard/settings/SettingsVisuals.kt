// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Shared settings-only spacing and surface geometry. The IME must not consume these tokens. */
internal object SettingsVisuals {
    val pageInset = 20.dp
    val rowInset = 14.dp
    val groupRadius = 12.dp
    val dialogRadius = 18.dp
    val fieldRadius = 10.dp
    val rowHeight = 52.dp
    val rowVertical = 9.dp
    val sectionGap = 16.dp
    val iconSize = 24.dp
    val divider = 1.dp
    val controlTarget = 48.dp
    val actionShape = RoundedCornerShape(percent = 50)
}
