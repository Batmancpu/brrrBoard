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
    val shape = RoundedCornerShape(30.dp)
    val deepBlue = MaterialTheme.colorScheme.surface
    val sapphire = MaterialTheme.colorScheme.primaryContainer
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
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .animateContentSize(if (reduceMotion) androidx.compose.animation.core.snap() else spring(dampingRatio = .9f, stiffness = 520f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(deepBlue, sapphire.copy(alpha = .76f), deepBlue)))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .72f), gold.copy(alpha = .12f))), shape)
            .then(if (!expanded) Modifier.clickable(role = Role.Button) { expanded = true } else Modifier)
            .padding(horizontal = 17.dp, vertical = 16.dp),
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(tween(if (reduceMotion) 0 else 130)) togetherWith fadeOut(tween(if (reduceMotion) 0 else 75)) },
            label = "keyboard-tryout-morph",
        ) { showEditor ->
            if (!showEditor) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Your keyboard", color = foreground, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text("A space to make it yours.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    // Decorative keycap illustration, not a second input field or captured user text.
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(deepBlue.copy(alpha = .55f)).padding(9.dp)
                        .then(Modifier.clearAndSetSemantics { }),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM").forEach { letters ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = if (letters.length == 10) 0.dp else 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                                letters.forEach { letter ->
                                    Box(Modifier.weight(1f).height(20.dp).clip(RoundedCornerShape(5.dp))
                                        .background(deepBlue).border(.5.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(5.dp)),
                                        contentAlignment = Alignment.Center) {
                                        Text(letter.toString(), color = foreground, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("123", "space", "return").forEach { label ->
                                Box(Modifier.weight(if (label == "space") 2.8f else 1f).height(19.dp)
                                    .clip(RoundedCornerShape(5.dp)).background(if (label == "return") gold else deepBlue),
                                    contentAlignment = Alignment.Center) {
                                    Text(label, color = if (label == "return") MaterialTheme.colorScheme.onPrimary else foreground, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(deepBlue.copy(alpha = .78f))
                        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Try typing", color = gold, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("chevron_right", fontFamily = materialSymbols, color = gold, fontSize = 19.sp)
                    }
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
                        shape = RoundedCornerShape(13.dp),
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
                    IconButton(onClick = ::closePreview, modifier = Modifier.size(38.dp)) {
                        Text("close", fontFamily = materialSymbols,
                            color = foreground, fontSize = 21.sp)
                    }
                }
            }
        }
    }
}
