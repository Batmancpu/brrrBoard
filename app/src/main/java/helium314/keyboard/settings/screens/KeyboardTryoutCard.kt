// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

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
    val sweep = remember { Animatable(.5f) }
    val shape = RoundedCornerShape(17.dp)
    val deepBlue = Color(0xFF173B78)
    val sapphire = Color(0xFF2D63B5)
    val gold = Color(0xFFDCC58C)

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
        } else if (!reduceMotion) {
            while (isActive) {
                sweep.snapTo(-.45f)
                sweep.animateTo(1.45f, animationSpec = tween(2_400))
            }
        } else {
            sweep.snapTo(.5f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = .9f, stiffness = 520f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(deepBlue, sapphire)))
            .border(1.dp, gold.copy(alpha = .64f), shape)
            .then(if (!expanded) Modifier.clickable(role = Role.Button) { expanded = true } else Modifier)
            .drawWithContent {
                drawContent()
                if (!reduceMotion && !expanded) {
                    val x = size.width * sweep.value
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFD9EBFF).copy(alpha = .26f),
                                gold.copy(alpha = .17f),
                                Color.Transparent,
                            ),
                            start = Offset(x - size.width * .18f, 0f),
                            end = Offset(x + size.width * .18f, size.height),
                        ),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                }
            }
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(tween(130)) togetherWith fadeOut(tween(75)) },
            label = "keyboard-tryout-morph",
        ) { showEditor ->
            if (!showEditor) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("keyboard", fontFamily = materialSymbols,
                        color = Color(0xFFE5D09D), fontSize = 23.sp)
                    Text("Type", color = Color.White, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("arrow_forward", fontFamily = materialSymbols,
                        color = Color.White.copy(alpha = .82f), fontSize = 20.sp)
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
                        placeholder = { Text("Type to preview suggestions", color = Color.White.copy(alpha = .72f)) },
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = .72f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = .72f),
                            focusedIndicatorColor = Color(0xFFE5D09D),
                            unfocusedIndicatorColor = Color.White.copy(alpha = .28f),
                            cursorColor = Color(0xFFE5D09D),
                        ),
                    )
                    IconButton(onClick = ::closePreview, modifier = Modifier.size(38.dp)) {
                        Text("close", fontFamily = materialSymbols,
                            color = Color.White, fontSize = 21.sp)
                    }
                }
            }
        }
    }
}
