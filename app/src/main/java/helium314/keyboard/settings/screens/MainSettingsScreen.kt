// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package helium314.keyboard.settings.screens
import helium314.keyboard.settings.LocalSearchState
import helium314.keyboard.settings.StaticSearchField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
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
import helium314.keyboard.latin.utils.SubtypeLocaleUtils.displayName
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.NextScreenIcon
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.SettingsResetManager
import helium314.keyboard.settings.dialogs.SlideActionRail
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
    onClickTextCorrection: () -> Unit,
    onClickPreferences: () -> Unit,
    onClickToolbar: () -> Unit,
    onClickGestureTyping: () -> Unit,
    onClickBrainDashboard: () -> Unit,
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
    onClickBackup: () -> Unit,
    onClickVoice: () -> Unit,
    onClickDeviceCheckup: () -> Unit,
    onClickBack: () -> Unit,
) {
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.ime_settings),
        settings = emptyList(),
        showBackButton = false,
    ) {
        val ctx = LocalContext.current
        LaunchedEffect(ctx) {
            AudioAndHapticFeedbackManager.getInstance().refreshForSettings(ctx)
        }
        var showAdvancedControls by rememberSaveable { mutableStateOf(false) }
        var resetUndoMs by remember { mutableStateOf(SettingsResetManager.remainingMs(ctx)) }
        LaunchedEffect(ctx, resetUndoMs > 0L) {
            while (resetUndoMs > 0L) {
                delay(60_000L)
                resetUndoMs = SettingsResetManager.remainingMs(ctx)
            }
        }
        val b = (ctx.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
        var offlineVoiceReady by remember { mutableStateOf(VoiceModelChoice.active(ctx) != null) }
        var offlineInstallState by remember { mutableStateOf(VoiceModelInstaller.installState) }
        var offlineInstallPercent by remember { mutableStateOf(VoiceModelInstaller.installPercent) }
        val voiceInstallScope = rememberCoroutineScope()
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

        val enabledSubtypes = remember(b?.value) { SubtypeSettings.getEnabledSubtypes(true) }
        val enabledSubtypeNames = remember(enabledSubtypes) {
            enabledSubtypes.joinToString(", ") { it.displayName() }
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
        Md3ePreferenceGroup("Device check") {
            Md3ePreference(
                icon = "fact_check",
                title = "Device checkup",
                description = "Run the guided keyboard checks and copy one report",
                onClick = onClickDeviceCheckup,
                isFirst = true,
                isLast = true,
            )
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

        // Intelligence is a primary feature, not a submenu hidden under Dictionary.
        Md3ePreferenceGroup("BR Brain") {
            Md3ePreference(
                icon = "psychology",
                title = "BR Brain",
                description = "Live prediction speed, offline learning and Smart Emoji",
                onClick = onClickBrainDashboard,
                isFirst = true,
                isLast = true,
            )
        }

        Md3ePreferenceGroup("Your keyboard") {
            Md3ePreference(
                icon = "palette",
                title = "Appearance & glass",
                description = "Themes, frosted glass and visual details",
                onClick = onClickAppearance,
                isFirst = true
            )
            Md3ePreference(
                icon = "language",
                title = stringResource(R.string.language_and_layouts_title),
                description = enabledSubtypeNames,
                onClick = onClickLanguage,
                isLast = true
            )
        }

        Md3ePreferenceGroup("Typing") {
            Md3ePreference(
                icon = "spellcheck",
                title = "Correction & suggestions",
                description = "Autocorrect, predictions and smart spacing",
                onClick = onClickTextCorrection,
                isFirst = true,
                isLast = true
            )
        }

        Md3ePreferenceGroup("Everyday tools") {
            Md3ePreference(
                icon = "graphic_eq",
                title = "Sound & haptics",
                description = "Keypress feedback, vibration and volume",
                onClick = onClickSound,
                isFirst = true
            )
            Md3ePreference(
                icon = "content_paste",
                title = "Clipboard",
                description = "History, screenshots and retention",
                onClick = onClickClipboard
            )
            Md3ePreference(
                icon = "mic",
                title = "Offline voice",
                description = if (offlineVoiceReady)
                    "Ready for on-device English dictation"
                else when (offlineInstallState) {
                    VoiceModelInstaller.InstallState.INSTALLING ->
                        "Preparing on-device model · $offlineInstallPercent%"
                    VoiceModelInstaller.InstallState.FAILED ->
                        "Voice model unavailable · Check status"
                    else -> "Check local model availability and permissions"
                },
                onClick = onClickVoice,
                isLast = true
            )
        }

        Md3ePreferenceGroup("Privacy") {
            Md3ePreference(
                icon = "shield",
                title = "Privacy & protected apps",
                description = "Incognito behavior and app-specific protection",
                onClick = onClickPrivacy,
                isFirst = true,
                isLast = true
            )
        }

        Md3ePreferenceGroup("More controls") {
            Md3ePreference(
                icon = if (showAdvancedControls) "expand_less" else "tune",
                title = if (showAdvancedControls) "Hide advanced controls" else "Show advanced controls",
                description = "Toolbar, key layout, gestures, dictionaries, backup and detailed options",
                onClick = { showAdvancedControls = !showAdvancedControls },
                isFirst = true,
                isLast = true
            )
        }

        AnimatedVisibility(
            visible = showAdvancedControls,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Md3ePreferenceGroup("Advanced controls") {
                Md3ePreference(
                    icon = "keyboard",
                    title = "Keys & layout",
                    description = "Number row, key hints, spacing and shortcuts",
                    onClick = onClickKeys,
                    isFirst = true,
                )
                Md3ePreference(
                    icon = R.drawable.ic_board_toolbar,
                    title = "Toolbar",
                    description = "Buttons, shortcuts and suggestion bar",
                    onClick = onClickToolbar,
                )
                Md3ePreference(
                    icon = "gesture",
                    title = "Swipe typing",
                    description = if (JniUtils.sHaveGestureLib) "Gesture input, trails and swipe behavior" else stringResource(R.string.gesture_not_loaded_summary),
                    onClick = onClickGestureTyping,
                )
                Md3ePreference(
                    icon = "dictionary",
                    title = "Dictionary & learning",
                    description = "Personal words, dictionaries and brrBrain insights",
                    onClick = onClickDictionaries,
                )
                Md3ePreference(
                    icon = "tune",
                    title = "Detailed preferences",
                    description = "Fine-tune key behavior and less common options",
                    onClick = onClickPreferences,
                )
                Md3ePreference(
                    icon = "settings",
                    title = "Compatibility & advanced",
                    description = "Detailed behavior, diagnostics and compatibility",
                    onClick = onClickAdvanced,
                )
                Md3ePreference(
                    icon = "backup",
                    title = "Backup & restore",
                    description = "Safeguard your settings and dictionaries",
                    onClick = onClickBackup,
                )
                Md3ePreference(
                    icon = R.drawable.mangoloads_brand_small,
                    title = "About brrBoard",
                    description = "Version, privacy, licenses and setup help",
                    onClick = onClickAbout,
                    brandIcon = true,
                    isLast = true,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reset settings", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text("Restore keyboard preferences to their defaults. Your dictionaries, clipboard history, recordings and installed voice models stay on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                SlideActionRail(
                    label = "Slide to reset settings",
                    onConfirmed = {
                        if (SettingsResetManager.reset(ctx)) {
                            resetUndoMs = SettingsResetManager.remainingMs(ctx)
                            AudioAndHapticFeedbackManager.getInstance().refreshForSettings(ctx)
                            runCatching { KeyboardSwitcher.getInstance().reloadKeyboard() }
                            AudioAndHapticFeedbackManager.getInstance().performImportantActionConfirmation()
                        }
                    },
                    onDragStateChanged = { dragging ->
                        val feedback = AudioAndHapticFeedbackManager.getInstance()
                        if (dragging) feedback.startImportantActionHapticLoop()
                        else feedback.stopImportantActionHapticLoop()
                    },
                )
                if (resetUndoMs > 0L) {
                    val minutes = (resetUndoMs / 60_000L).coerceAtLeast(1L)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Settings reset · undo available for ${minutes}m",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            if (SettingsResetManager.undo(ctx)) {
                                resetUndoMs = 0L
                                AudioAndHapticFeedbackManager.getInstance().refreshForSettings(ctx)
                                runCatching { KeyboardSwitcher.getInstance().reloadKeyboard() }
                            }
                        }) { Text("Undo changes") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun Md3ePreferenceGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp, top = 4.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
) {
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 19.dp else 5.dp,
        topEnd = if (isFirst) 19.dp else 5.dp,
        bottomStart = if (isLast) 19.dp else 5.dp,
        bottomEnd = if (isLast) 19.dp else 5.dp,
    )
    
    val cardColor = MaterialTheme.colorScheme.surface
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
        ) {
            if (brandIcon && icon is Int) {
                // Never apply Material Icon tint to the full-color MangoLoads mark.
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                )
            } else if (icon is String) {
                Text(
                    text = icon,
                    fontFamily = materialSymbols,
                    fontSize = 25.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            } else if (icon is Int) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(25.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
                onClickAbout = {}, onClickTextCorrection = {}, onClickPreferences = {},
                onClickToolbar = {}, onClickGestureTyping = {}, onClickBrainDashboard = {},
                onClickAdvanced = {}, onClickAppearance = {}, onClickLanguage = {},
                onClickLayouts = {}, onClickDictionaries = {}, onClickWelcomeWizard = {},
                onClickKeys = {}, onClickSound = {}, onClickClipboard = {},
                onClickPrivacy = {}, onClickBackup = {}, onClickVoice = {},
                onClickDeviceCheckup = {}, onClickBack = {}
            )
        }
    }
}
