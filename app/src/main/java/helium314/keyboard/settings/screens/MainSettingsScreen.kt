// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package helium314.keyboard.settings.screens
import androidx.compose.foundation.layout.heightIn
import helium314.keyboard.settings.LocalSearchState
import helium314.keyboard.settings.StaticSearchField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onLongClick as semanticsOnLongClick
import androidx.compose.ui.semantics.onClick as semanticsOnClick
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.compositeOver
import dev.chrisbanes.haze.haze
import helium314.keyboard.settings.LocalHazeState
import helium314.keyboard.settings.LocalSearchInnerPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.latin.voice.VoiceModelInstaller
import helium314.keyboard.latin.voice.VoiceModelChoice
import kotlinx.coroutines.delay
import helium314.keyboard.latin.utils.JniUtils
import helium314.keyboard.latin.utils.NextScreenIcon
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.SafeCrashReport
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.utils.previewDark

// The bundled font is a fixed instance of exactly FILL=0, GRAD=0, opsz=24, wght=400.
// Keeping it static preserves the settings icon outlines while avoiding ~13 MB of
// variation deltas in every APK.
val materialSymbols = FontFamily(Font(R.font.material_symbols_rounded))

@Composable
fun MainSettingsScreen(
    onClickAbout: () -> Unit,
    onClickPreferences: () -> Unit,
    onClickToolbar: () -> Unit,
    onClickGestureTyping: () -> Unit,
    onClickBrainDashboard: () -> Unit,
    onClickTextCorrection: () -> Unit,
    onClickAdvanced: () -> Unit,
    onClickAppearance: () -> Unit,
    onClickLanguage: () -> Unit,
    onClickLayouts: () -> Unit,
    onClickDictionaries: () -> Unit,
    onClickWelcomeWizard: () -> Unit,
    onClickKeys: () -> Unit,
    onClickSound: () -> Unit,
    onClickClipboard: () -> Unit,
    onClickPrivacy: () -> Unit,
    onClickVault: () -> Unit = {},
    onRecoverVault: () -> Unit = {},
    onClickBackup: () -> Unit,
    onClickVoice: () -> Unit,
    onClickBack: () -> Unit,
    onClickTypingHub: () -> Unit = onClickTextCorrection,
    onClickAdvancedHub: () -> Unit = onClickAdvanced,
    onClickKeyboardHub: () -> Unit = onClickAppearance,
    onClickInteractionsHub: () -> Unit = onClickSound,
    onClickToolsHub: () -> Unit = onClickToolbar,
) {
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = "brrBoard",
        settings = emptyList(),
        showBackButton = false,
        onTitleHold = onClickVault,
        onTitleRecovery = onRecoverVault,
    ) {
        val ctx = LocalContext.current
        LaunchedEffect(ctx) {
            AudioAndHapticFeedbackManager.getInstance().refreshForSettings(ctx)
        }
        KeyboardTryoutCard()
        val b = (ctx.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
        var offlineVoiceReady by remember { mutableStateOf(VoiceModelChoice.active(ctx) != null) }
        var offlineInstallState by remember { mutableStateOf(VoiceModelInstaller.installState) }
        var offlineInstallPercent by remember { mutableStateOf(VoiceModelInstaller.installPercent) }
        LaunchedEffect(ctx) {
            // The one-time installer verifies and extracts the model on an I/O worker. Refresh
            // this tiny status while Settings is visible without blocking composition.
            repeat(40) {
                if (offlineVoiceReady) return@LaunchedEffect
                delay(1_500)
                offlineVoiceReady = VoiceModelChoice.active(ctx) != null
                offlineInstallState = VoiceModelInstaller.installState
                offlineInstallPercent = VoiceModelInstaller.installPercent
            }
        }

        // Android's actual keyboard enable/default state is the only setup criterion.
        // Optional swipe typing or dictionary preferences are not unfinished setup.
        val imm = remember(ctx) { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
        fun ready() = UncachedInputMethodManagerUtils.isThisImeEnabled(ctx, imm) &&
            UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm)
        var isImeReady by remember { mutableStateOf(ready()) }
        val owner = LocalLifecycleOwner.current
        androidx.compose.runtime.DisposableEffect(owner, ctx) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) isImeReady = ready()
            }
            owner.lifecycle.addObserver(observer)
            onDispose { owner.lifecycle.removeObserver(observer) }
        }
        if (!isImeReady) {
            Md3ePreferenceGroup("Finish setup") {
                Md3ePreference(
                    icon = "keyboard",
                    title = "Enable brrBoard",
                    description = "Choose brrBoard as your default keyboard to finish setup",
                    onClick = onClickWelcomeWizard,
                    isFirst = true,
                    isLast = true
                )
            }
        }

        Md3ePreferenceGroup("Your keyboard") {
            Md3ePreference("keyboard", "Keyboard", "Glass, size, keys, languages and fonts", onClickKeyboardHub, isFirst = true)
            Md3ePreference("psychology", "Typing & intelligence", "Correction, predictions, swipe and BR Brain", onClickTypingHub)
            Md3ePreference("graphic_eq", "Interactions", "Gestures, haptics, sound and motion", onClickInteractionsHub)
            Md3ePreference("mic", "Voice",
                if (offlineVoiceReady) "On-device dictation ready" else when (offlineInstallState) {
                    VoiceModelInstaller.InstallState.INSTALLING -> "Preparing model · $offlineInstallPercent%"
                    VoiceModelInstaller.InstallState.FAILED -> "Model unavailable"
                    else -> "Models and microphone"
                }, onClickVoice, isLast = true)
        }
        Md3ePreferenceGroup("Tools & management") {
            Md3ePreference("content_paste", "Tools", "Toolbar, clipboard, emoji and text tools", onClickToolsHub, isFirst = true)
            Md3ePreference("shield", "Privacy & app profiles", "Personal learning, secure apps and permissions", onClickPrivacy)
            Md3ePreference(icon = R.drawable.mangoloads_brand_small, title = "Manage brrBoard",
                description = "Updates, backup, resources and troubleshooting", onClick = onClickAdvancedHub, isLast = true, brandIcon = true)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun Md3ePreferenceGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp, bottom = 7.dp, top = 2.dp)
        )
        Column(
            modifier = Modifier.clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            content()
        }
    }
}

@Composable
fun Md3ePreference(
    icon: Any,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    brandIcon: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    holdDurationMillis: Long = 2500,
) {
    val hostView = LocalView.current
    val longClick = onLongClick
    val interactionModifier = if (longClick == null) {
        Modifier.clickable { onClick() }
    } else {
        val holdAction = requireNotNull(longClick)
        Modifier.semantics {
            semanticsOnClick { onClick(); true }
            semanticsOnLongClick(label = "Open options") {
                holdAction()
                true
            }
        }.pointerInput(holdAction, holdDurationMillis) {
            while (true) {
                awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
                val releasedNormally = withTimeoutOrNull(holdDurationMillis) {
                    awaitPointerEventScope { waitForUpOrCancellation() != null }
                }
                if (releasedNormally == null) {
                    hostView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    // Finish the pointer gesture before navigation replaces this node.
                    // Navigating while the pointer coroutine is active can tear down the
                    // route during dispatch on some OEM Compose builds.
                    val releasedAfterHold = awaitPointerEventScope {
                        waitForUpOrCancellation() != null
                    }
                    if (releasedAfterHold) holdAction()
                } else if (releasedNormally) {
                    onClick()
                }
            }
        }
    }
    val iconColor = when (icon) {
        "psychology" -> Color(0xFF6255C7)
        "palette" -> Color(0xFF8B55BD)
        "mic" -> Color(0xFFC83D52)
        "content_paste" -> Color(0xFFA96818)
        "shield" -> Color(0xFF23815D)
        "graphic_eq" -> Color(0xFFC14476)
        "tune" -> Color(0xFF676B78)
        else -> Color(0xFF2865C7)
    }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().then(interactionModifier)
                .heightIn(min = 54.dp).padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val tile = RoundedCornerShape(9.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp).then(if (brandIcon) Modifier else Modifier
                    .clip(tile)
                    .background(Brush.verticalGradient(listOf(iconColor.copy(alpha = .86f), iconColor)))
                    .border(.5.dp, Color.White.copy(alpha = .22f), tile))
            ) {
                if (brandIcon && icon is Int) {
                    Image(painterResource(id = icon), null, Modifier.size(32.dp))
                } else if (icon is String) {
                    Text(icon, fontFamily = materialSymbols, fontSize = 20.sp, color = Color.White,
                        modifier = Modifier.clearAndSetSemantics { })
                } else if (icon is Int) {
                    Icon(painterResource(id = icon), null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurface)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("chevron_right", fontFamily = materialSymbols, fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f),
                modifier = Modifier.clearAndSetSemantics { })
        }
        if (!isLast) HorizontalDivider(Modifier.padding(start = 59.dp),
            thickness = .5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun QuickSetupCard(
    onClickGestureTyping: () -> Unit,
    onClickDictionaries: () -> Unit,
    onDismiss: () -> Unit,
    isGestureComplete: Boolean,
    isDictionaryComplete: Boolean,
    allStepsComplete: Boolean,
) {
    val ctx = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val accentColor = if (allStepsComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val baseCardColor = MaterialTheme.colorScheme.surfaceVariant
    val opaqueContainerColor = if (allStepsComplete) Color(0xFF4CAF50).copy(alpha = 0.15f).compositeOver(baseCardColor) else baseCardColor

    val offsetX = remember { Animatable(0f) }
    var cardWidth by remember { mutableStateOf(0f) }
    val dismissThreshold = cardWidth * 0.4f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .onSizeChanged { cardWidth = it.width.toFloat() }
    ) {
        val absoluteOffset = abs(offsetX.value)
        val isSwiping = absoluteOffset > 0f

        if (isSwiping) {
            val isSwipingLeft = offsetX.value < 0
            val alignment = if (isSwipingLeft) Alignment.CenterEnd else Alignment.CenterStart
            val revealWidth = with(LocalDensity.current) { absoluteOffset.toDp() }
            // To ensure no gap between the cards, we can stretch the reveal card slightly wider
            val extraOverlap = 20.dp
            
            // Dynamic corner radius: starts as pill (height/2), then blends to match card corners (12dp) on the inner edge
            // Material Card default corner radius is typically 12dp.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(alignment)
                        .width(revealWidth + extraOverlap)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(
                            topStart = if (isSwipingLeft) 50.dp else 12.dp,
                            bottomStart = if (isSwipingLeft) 50.dp else 12.dp,
                            topEnd = if (!isSwipingLeft) 50.dp else 12.dp,
                            bottomEnd = if (!isSwipingLeft) 50.dp else 12.dp
                        ))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = alignment
                ) {
                    val iconPadding = 24.dp
                    Box(modifier = Modifier.padding(horizontal = iconPadding)) {
                        Text(
                            text = "check",
                            fontFamily = materialSymbols,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.scale(minOf(1f, absoluteOffset / 150f))
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = opaqueContainerColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(CardDefaults.shape)
                .clickable { isExpanded = !isExpanded }
                .pointerInput(allStepsComplete) {
                    if (!allStepsComplete) return@pointerInput
                    val velocityTracker = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().x
                            coroutineScope.launch {
                                if (abs(offsetX.value) > dismissThreshold || abs(velocity) > 800f) {
                                    val target = if (offsetX.value > 0) cardWidth else -cardWidth
                                    offsetX.animateTo(target, spring(stiffness = 200f))
                                    onDismiss()
                                } else {
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "check_circle",
                        fontFamily = materialSymbols,
                        fontSize = 24.sp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (allStepsComplete) "Setup is complete!" else stringResource(R.string.quick_setup_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "expand_more",
                        fontFamily = materialSymbols,
                        fontSize = 24.sp,
                        color = accentColor,
                        modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (allStepsComplete) "All essential settings have been successfully configured." else stringResource(R.string.quick_setup_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))

                        QuickSetupStep(
                            icon = "gesture",
                            title = stringResource(R.string.quick_setup_gesture_title),
                            description = stringResource(R.string.quick_setup_gesture_desc),
                            onClick = onClickGestureTyping,
                            isComplete = isGestureComplete,
                            isFirst = true
                        )
                        QuickSetupStep(
                            icon = "dictionary",
                            title = stringResource(R.string.quick_setup_dict_title),
                            description = stringResource(R.string.quick_setup_dict_desc),
                            onClick = onClickDictionaries,
                            isComplete = isDictionaryComplete,
                            isLast = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSetupStep(
    icon: Any,
    title: String,
    description: String,
    onClick: () -> Unit,
    isComplete: Boolean = false,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 16.dp else 4.dp,
        topEnd = if (isFirst) 16.dp else 4.dp,
        bottomStart = if (isLast) 16.dp else 4.dp,
        bottomEnd = if (isLast) 16.dp else 4.dp,
    )
    val isDark = isSystemInDarkTheme()
    val bgColor = Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isComplete) Color(0xFF4CAF50).copy(alpha = 0.15f) else bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon is String) {
            Text(
                text = icon,
                fontFamily = materialSymbols,
                fontSize = 20.sp,
                color = if (isComplete) Color(0xFF388E3C) else MaterialTheme.colorScheme.secondary
            )
        } else if (icon is Int) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isComplete) Color(0xFF388E3C) else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isComplete) Color(0xFF388E3C) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isComplete) Color(0xFF388E3C).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isComplete) {
            Text(
                text = "check_circle",
                fontFamily = materialSymbols,
                fontSize = 20.sp,
                color = Color(0xFF388E3C)
            )
        } else {
            Text(
                text = "chevron_right",
                fontFamily = materialSymbols,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
@Preview
@Composable
private fun PreviewScreen() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            MainSettingsScreen(
                onClickAbout = {}, onClickPreferences = {},
                onClickToolbar = {}, onClickGestureTyping = {}, onClickBrainDashboard = {},
                onClickAdvanced = {}, onClickAppearance = {}, onClickLanguage = {},
                onClickLayouts = {}, onClickDictionaries = {}, onClickWelcomeWizard = {},
                onClickKeys = {}, onClickSound = {}, onClickClipboard = {},
                onClickPrivacy = {}, onClickBackup = {}, onClickVoice = {},
                onClickTextCorrection = {}, onClickBack = {}
            )
        }
    }
}
