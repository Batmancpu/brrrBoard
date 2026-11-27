// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import helium314.keyboard.settings.materialSymbols
import helium314.keyboard.settings.SettingsVisuals
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.settings.IconOrImage
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.previewDark

// partially taken from StreetComplete / SCEE

val LocalGroupedSettingsRows = staticCompositionLocalOf { false }

@Composable
fun PreferenceCategory(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column {
        Text(
            text = title,
            modifier = modifier.padding(top = SettingsVisuals.sectionGap, start = SettingsVisuals.pageInset + 2.dp, end = SettingsVisuals.pageInset, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun Preference(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    value: @Composable (RowScope.() -> Unit)? = null,
) {
    PreferenceRow(name, onClick, modifier, description, icon, value = value)
}

/** Preserve the public preference signature; toggle rows use one accessible switch target. */
@Composable
internal fun PreferenceRow(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    toggleState: Boolean? = null,
    interactionSource: MutableInteractionSource? = null,
    value: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (LocalGroupedSettingsRows.current) Modifier else Modifier
                .padding(horizontal = SettingsVisuals.pageInset, vertical = 2.dp)
                .clip(RoundedCornerShape(SettingsVisuals.groupRadius))
                .background(MaterialTheme.colorScheme.surface))
            .then(if (toggleState == null) Modifier.clickable { onClick() }
                else Modifier.toggleable(value = toggleState, role = Role.Switch, interactionSource = interactionSource,
                    indication = LocalIndication.current, onValueChange = { onClick() }))
            .heightIn(min = SettingsVisuals.rowHeight)
            .padding(vertical = SettingsVisuals.rowVertical, horizontal = SettingsVisuals.rowInset),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null)
            IconOrImage(icon, null, 24)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal))
            if (description != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodySmall,
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        if (value != null) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    textAlign = TextAlign.End,
                    hyphens = Hyphens.Auto
                ),
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.End
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) { value() }
            }
        } else {
            Text("chevron_right", fontFamily = materialSymbols, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f),
                modifier = Modifier.clearAndSetSemantics { })
        }
    }
}

@Preview
@Composable
private fun PreferencePreview() {
    Theme(previewDark) {
        Surface {
            Column {
                PreferenceCategory("Preference Category")
                Preference(
                    name = "Preference",
                    onClick = {},
                )
                Preference(
                    name = "Preference with icon",
                    onClick = {},
                    icon = R.drawable.ic_settings_about
                )
                SliderPreference(
                    name = "SliderPreference",
                    key = "",
                    default = 1,
                    description = { it.toString() },
                    range = -5f..5f
                )
                Preference(
                    name = "Preference with icon and description",
                    description = "some text",
                    onClick = {},
                    icon = R.drawable.ic_settings_about
                )
                Preference(
                    name = "Preference with switch",
                    onClick = {}
                ) {
                    SettingsSwitch(checked = true, onCheckedChange = {})
                }
                SwitchPreference(
                    name = "SwitchPreference",
                    key = "none",
                    default = true
                )
                Preference(
                    name = "Preference",
                    onClick = {},
                    description = "A long description which may actually be several lines long, so it should wrap."
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_left_rounded), null)
                }
                Preference(
                    name = "Long preference name that wraps",
                    onClick = {},
                ) {
                    Text("Long preference value")
                }
                Preference(
                    name = "Long preference name 2",
                    onClick = {},
                    description = "hello I am description"
                ) {
                    Text("Long preference value")
                }
            }
        }
    }
}
