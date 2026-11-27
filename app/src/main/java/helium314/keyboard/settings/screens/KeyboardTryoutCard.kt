// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.settings.SettingsVisuals
import helium314.keyboard.latin.settings.getTransitionAnimationScale
import kotlinx.coroutines.isActive

/** A private, in-place IME tryout directly below Settings search. */
@Composable
internal fun KeyboardTryoutCard() {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var previewText by remember { mutableStateOf("") }
    val reduceMotion = getTransitionAnimationScale(context) <= 0f
    val shape = RoundedCornerShape(SettingsVisuals.groupRadius)
    val deepBlue = MaterialTheme.colorScheme.surface
    val gold = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onSurface

    fun closePreview() {
        expanded = false
        previewText = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }

    BackHandler(enabled = expanded, onBack = ::closePreview)
    LaunchedEffect(expanded, reduceMotion) {
        if (expanded) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsVisuals.pageInset, vertical = 6.dp)
            .animateContentSize(if (reduceMotion) androidx.compose.animation.core.snap() else spring(dampingRatio = .9f, stiffness = 520f))
            .clip(shape)
            .background(deepBlue)
            .then(if (!expanded) Modifier.clickable(role = Role.Button) { expanded = true } else Modifier)
            .padding(horizontal = SettingsVisuals.rowInset, vertical = SettingsVisuals.rowVertical),
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(tween(if (reduceMotion) 0 else 130)) togetherWith fadeOut(tween(if (reduceMotion) 0 else 75)) },
            label = "keyboard-tryout-morph",
        ) { showEditor ->
            if (!showEditor) {
                Row(Modifier.fillMaxWidth().heightIn(min = SettingsVisuals.rowHeight - SettingsVisuals.rowVertical * 2),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("keyboard", fontFamily = materialSymbols, fontSize = 23.sp, color = gold,
                        modifier = Modifier.clearAndSetSemantics { })
                    Column(Modifier.weight(1f)) {
                        Text("Try your keyboard", style = MaterialTheme.typography.bodyLarge, color = foreground)
                        Text("Preview typing and suggestions", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("chevron_right", fontFamily = materialSymbols, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clearAndSetSemantics { })
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedTextField(
                        value = previewText,
                        onValueChange = { previewText = it },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        placeholder = { Text("Type to preview suggestions", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        singleLine = true,
                        shape = RoundedCornerShape(SettingsVisuals.fieldRadius),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                            platformImeOptions = PlatformImeOptions(
                                privateImeOptions = listOf(
                                    Constants.ImeOption.NO_PERSONALIZED_LEARNING,
                                    Constants.ImeOption.NO_MICROPHONE,
                                ).joinToString(",") { "${context.packageName}.$it" }
                            ),
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = foreground,
                            unfocusedTextColor = foreground,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedIndicatorColor = gold,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = gold,
                        ),
                    )
                    IconButton(onClick = ::closePreview, modifier = Modifier.size(SettingsVisuals.controlTarget)) {
                        Text("close", fontFamily = materialSymbols,
                            color = foreground, fontSize = 21.sp)
                    }
                }
            }
        }
    }
}
