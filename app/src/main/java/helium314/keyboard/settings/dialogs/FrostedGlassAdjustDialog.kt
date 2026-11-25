// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.dialogs

import helium314.keyboard.settings.preferences.SettingsSwitch
import helium314.keyboard.settings.preferences.SettingsSlider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.latin.common.Constants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import helium314.keyboard.keyboard.FrostedLiveValues
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max



private data class FrostedProfileSnapshot(
    val blurRadius: Int,
    val keyTransparency: Int,
    val bgTransparency: Int,
    val colorBlend: Int,
    val saturation: Int,
    val specialVibrancy: Int,
    val alphabetVibrancy: Int,
    val dustAlpha: Float
)

private data class FrostedSettingsSnapshot(
    val light: FrostedProfileSnapshot,
    val dark: FrostedProfileSnapshot,
    val dustEnabled: Boolean
)

@Composable
fun FrostedGlassAdjustDialog(
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    startAtHighlight: Boolean = false
) {
    val context = LocalContext.current
    val prefs = context.prefs()
    val coroutineScope = rememberCoroutineScope()
    val contentScrollState = rememberScrollState()
    val density = LocalDensity.current

    // The previous reflection deep link opens the Surface tab directly.
    var editorSection by remember { mutableIntStateOf(0) }

    var activeProfile by remember { mutableStateOf(if (helium314.keyboard.latin.utils.ResourceUtils.isNight(context.resources)) "dark" else "light") }


    // 1. Snapshot the initial state when the dialog opens
    val initialSnapshot = remember {
        val legacyDustAlpha = prefs.getFloat(Settings.PREF_FROSTED_DUST_ALPHA, Defaults.PREF_FROSTED_DUST_ALPHA)
        FrostedSettingsSnapshot(
            light = FrostedProfileSnapshot(
                blurRadius = prefs.getInt(Settings.PREF_FROSTED_BLUR_RADIUS, Defaults.PREF_FROSTED_BLUR_RADIUS),
                keyTransparency = prefs.getInt(Settings.PREF_FROSTED_KEY_TRANSPARENCY, Defaults.PREF_FROSTED_KEY_TRANSPARENCY),
                bgTransparency = prefs.getInt(Settings.PREF_FROSTED_BG_TRANSPARENCY, Defaults.PREF_FROSTED_BG_TRANSPARENCY),
                colorBlend = prefs.getInt(Settings.PREF_FROSTED_COLOR_BLEND, Defaults.PREF_FROSTED_COLOR_BLEND),
                saturation = prefs.getInt(Settings.PREF_FROSTED_SATURATION, Defaults.PREF_FROSTED_SATURATION),
                specialVibrancy = prefs.getInt(Settings.PREF_FROSTED_SPECIAL_VIBRANCY, Defaults.PREF_FROSTED_SPECIAL_VIBRANCY),
                alphabetVibrancy = prefs.getInt(Settings.PREF_FROSTED_ALPHABET_VIBRANCY, Defaults.PREF_FROSTED_ALPHABET_VIBRANCY),
                dustAlpha = legacyDustAlpha.coerceIn(1f, 10f)
            ),
            dark = FrostedProfileSnapshot(
                blurRadius = prefs.getInt(Settings.PREF_FROSTED_BLUR_RADIUS_NIGHT, Defaults.PREF_FROSTED_BLUR_RADIUS_NIGHT),
                keyTransparency = prefs.getInt(Settings.PREF_FROSTED_KEY_TRANSPARENCY_NIGHT, Defaults.PREF_FROSTED_KEY_TRANSPARENCY_NIGHT),
                bgTransparency = prefs.getInt(Settings.PREF_FROSTED_BG_TRANSPARENCY_NIGHT, Defaults.PREF_FROSTED_BG_TRANSPARENCY_NIGHT),
                colorBlend = prefs.getInt(Settings.PREF_FROSTED_COLOR_BLEND_NIGHT, Defaults.PREF_FROSTED_COLOR_BLEND_NIGHT),
                saturation = prefs.getInt(Settings.PREF_FROSTED_SATURATION_NIGHT, Defaults.PREF_FROSTED_SATURATION_NIGHT),
                specialVibrancy = prefs.getInt(Settings.PREF_FROSTED_SPECIAL_VIBRANCY_NIGHT, Defaults.PREF_FROSTED_SPECIAL_VIBRANCY_NIGHT),
                alphabetVibrancy = prefs.getInt(Settings.PREF_FROSTED_ALPHABET_VIBRANCY_NIGHT, Defaults.PREF_FROSTED_ALPHABET_VIBRANCY_NIGHT),
                dustAlpha = prefs.getFloat(
                    Settings.PREF_FROSTED_DUST_ALPHA_NIGHT,
                    legacyDustAlpha
                ).coerceIn(1f, 10f)
            ),
            dustEnabled = prefs.getBoolean(Settings.PREF_FROSTED_DUST_ENABLED, Defaults.PREF_FROSTED_DUST_ENABLED)
        )
    }

    // 2. The temporary live state for the sliders
    var snapshot by remember { mutableStateOf(initialSnapshot) }
    var isSaved by remember { mutableStateOf(false) }
    var previewTouched by remember { mutableStateOf(false) }
    // Edge reflection uses a transient renderer override; Cancel/process death writes nothing.
    val initialHighlightStyle = remember {
        prefs.getInt(Settings.PREF_KEYBOARD_TOP_HIGHLIGHT_STYLE,
            Defaults.PREF_KEYBOARD_TOP_HIGHLIGHT_STYLE).coerceIn(0, 3)
    }
    var selectedHighlightStyle by remember { mutableIntStateOf(initialHighlightStyle) }

    fun previewHighlightStyle(style: Int) {
        selectedHighlightStyle = style.coerceIn(0, 3)
        if (selectedHighlightStyle == initialHighlightStyle && !previewTouched) return
        previewTouched = true
        KeyboardTheme.livePreviewHighlightStyle = selectedHighlightStyle
        LatinIME.getInstance()?.requestFrostedLivePreviewRefresh()
    }


    fun applyLivePreview(
        profile: FrostedProfileSnapshot,
        dustEnabled: Boolean = snapshot.dustEnabled
    ) {
        previewTouched = true
        KeyboardTheme.livePreviewValues = FrostedLiveValues(
            blurRadius = profile.blurRadius,
            keyTransparency = profile.keyTransparency,
            bgTransparency = profile.bgTransparency,
            colorBlend = profile.colorBlend,
            saturation = profile.saturation,
            specialVibrancy = profile.specialVibrancy,
            alphabetVibrancy = profile.alphabetVibrancy,
            dustEnabled = dustEnabled,
            dustAlpha = profile.dustAlpha.coerceIn(1f, 10f)
        )
        LatinIME.getInstance()?.requestFrostedLivePreviewRefresh()
    }

    fun writeSnapshotToPrefs(values: FrostedSettingsSnapshot, wallpaperUri: String?) {
        prefs.edit()
            .putInt(Settings.PREF_FROSTED_BLUR_RADIUS, values.light.blurRadius)
            .putInt(Settings.PREF_FROSTED_KEY_TRANSPARENCY, values.light.keyTransparency)
            .putInt(Settings.PREF_FROSTED_BG_TRANSPARENCY, values.light.bgTransparency)
            .putInt(Settings.PREF_FROSTED_COLOR_BLEND, values.light.colorBlend)
            .putInt(Settings.PREF_FROSTED_SATURATION, values.light.saturation)
            .putInt(Settings.PREF_FROSTED_SPECIAL_VIBRANCY, values.light.specialVibrancy)
            .putInt(Settings.PREF_FROSTED_ALPHABET_VIBRANCY, values.light.alphabetVibrancy)
            .putFloat(Settings.PREF_FROSTED_DUST_ALPHA, values.light.dustAlpha.coerceIn(1f, 10f))
            .putInt(Settings.PREF_FROSTED_BLUR_RADIUS_NIGHT, values.dark.blurRadius)
            .putInt(Settings.PREF_FROSTED_KEY_TRANSPARENCY_NIGHT, values.dark.keyTransparency)
            .putInt(Settings.PREF_FROSTED_BG_TRANSPARENCY_NIGHT, values.dark.bgTransparency)
            .putInt(Settings.PREF_FROSTED_COLOR_BLEND_NIGHT, values.dark.colorBlend)
            .putInt(Settings.PREF_FROSTED_SATURATION_NIGHT, values.dark.saturation)
            .putInt(Settings.PREF_FROSTED_SPECIAL_VIBRANCY_NIGHT, values.dark.specialVibrancy)
            .putInt(Settings.PREF_FROSTED_ALPHABET_VIBRANCY_NIGHT, values.dark.alphabetVibrancy)
            .putFloat(Settings.PREF_FROSTED_DUST_ALPHA_NIGHT, values.dark.dustAlpha.coerceIn(1f, 10f))
            .putBoolean(Settings.PREF_FROSTED_DUST_ENABLED, values.dustEnabled)
            .putInt(Settings.PREF_KEYBOARD_TOP_HIGHLIGHT_STYLE, selectedHighlightStyle)
            .also { editor -> if (wallpaperUri == null) editor.remove("pref_frosted_preview_wallpaper_uri") else editor.putString("pref_frosted_preview_wallpaper_uri", wallpaperUri) }
            .apply()
    }

    fun requestFinalFrostedSync() {
        LatinIME.getInstance()?.requestFrostedHardThemeReset()
            ?: prefs.edit().putLong(Settings.PREF_FROSTED_GLASS_TRIGGER, System.currentTimeMillis()).apply()
    }

    // Helper to update current profile in snapshot and redraw the keyboard from staged values.
    fun updateCurrentProfile(update: (FrostedProfileSnapshot) -> FrostedProfileSnapshot) {
        val oldProfile = if (activeProfile == "light") snapshot.light else snapshot.dark
        val newProfile = update(oldProfile)
        
        snapshot = if (activeProfile == "light") {
            snapshot.copy(light = newProfile)
        } else {
            snapshot.copy(dark = newProfile)
        }

        applyLivePreview(newProfile)
    }

    fun updateSparklesEnabled(enabled: Boolean) {
        val newSnapshot = snapshot.copy(dustEnabled = enabled)
        snapshot = newSnapshot
        val profile = if (activeProfile == "light") newSnapshot.light else newSnapshot.dark
        applyLivePreview(profile, enabled)
    }

    // Current profile view for easier slider binding
    val currentValues = if (activeProfile == "light") snapshot.light else snapshot.dark

    // 3. The Tab Switch Effect is now handled explicitly in the Tab onClick to prevent "redraw floods"
    // during the unmounting process.

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                KeyboardTheme.livePreviewHighlightStyle = null
                KeyboardTheme.themeOverride = null
                KeyboardTheme.livePreviewValues = null
                if (previewTouched) requestFinalFrostedSync()
            } else if (event == Lifecycle.Event.ON_RESUME && previewTouched && !isSaved) {
                KeyboardTheme.themeOverride = activeProfile
                KeyboardTheme.livePreviewHighlightStyle = selectedHighlightStyle
                applyLivePreview(if (activeProfile == "light") snapshot.light else snapshot.dark)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            helium314.keyboard.settings.SettingsActivity.isTopBarHidden = false
            // Nothing was persisted by a preview, including reflections.
            KeyboardTheme.livePreviewHighlightStyle = null

            // Clear spoofing state
            KeyboardTheme.themeOverride = null
            KeyboardTheme.livePreviewValues = null
            
            // Avoid a destructive keyboard-theme rebuild when the sheet was only opened.
            if (previewTouched || isSaved) requestFinalFrostedSync()
        }
    }

    val testFieldState = rememberTextFieldState("", TextRange(0))

    val initialImageUriString = remember { prefs.getString("pref_frosted_preview_wallpaper_uri", null) }
    var selectedImageUri by remember {
        mutableStateOf(
            initialImageUriString?.let {
                try { android.net.Uri.parse(it) } catch (e: Throwable) { null }
            }
        ) 
    }
    var wallpaperBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(wallpaperBitmap) {
        helium314.keyboard.settings.SettingsActivity.isTopBarHidden = (wallpaperBitmap != null)
    }

    LaunchedEffect(selectedImageUri) {
        val uri = selectedImageUri
        if (uri != null) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    // Two bounded passes prevent a 50 MP wallpaper from allocating
                    // hundreds of megabytes just to fill a small live-preview sheet.
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 1440) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
                } catch (e: Throwable) {
                    null
                }
            }
            if (bitmap != null) {
                wallpaperBitmap = bitmap
            } else {
                selectedImageUri = null
                wallpaperBitmap = null
            }
        } else {
            wallpaperBitmap = null
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    BackHandler {
        onDismissRequest()
    }

    val panelShape = MaterialTheme.shapes.extraLarge
    val surfaceColor = MaterialTheme.colorScheme.surface
    val panelTint = remember(surfaceColor) { surfaceColor.copy(alpha = 0.78f) }
    val dialogHazeStyle = remember(panelTint) {
        HazeStyle(blurRadius = 22.dp, tint = panelTint)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Transparent scrim background capturing clicks outside the panel (no background dimming)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() }
        )

        // Full screen background image behind dialog and keyboard
        if (wallpaperBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .haze(state = hazeState)
                        } else Modifier
                    )
            ) {
                Image(
                    bitmap = wallpaperBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Dialog content container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .systemBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
                    .padding(16.dp)
                    .clip(panelShape)
                    .then(
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            Modifier.hazeChild(
                                state = hazeState,
                                shape = panelShape,
                                style = dialogHazeStyle
                            )
                        } else {
                            Modifier.background(surfaceColor)
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(contentScrollState)
                            .padding(20.dp)
                    ) {
                        Text("Glass Studio", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("Fine-tune the keyboard against a real background. Changes stay in preview until you save.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))

                        // Theme Profile Selector
                        TabRow(
                            selectedTabIndex = if (activeProfile == "light") 0 else 1,
                            modifier = Modifier.padding(bottom = 16.dp),
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            Tab(
                                selected = activeProfile == "light",
                                onClick = { 
                                    if (activeProfile != "light") {
                                        activeProfile = "light"
                                        KeyboardTheme.themeOverride = "light"
                                        applyLivePreview(snapshot.light)
                                    }
                                },
                                text = { Text("Light") }
                            )
                            Tab(
                                selected = activeProfile == "dark",
                                onClick = { 
                                    if (activeProfile != "dark") {
                                        activeProfile = "dark"
                                        KeyboardTheme.themeOverride = "dark"
                                        applyLivePreview(snapshot.dark)
                                    }
                                },
                                text = { Text("Dark") }
                            )
                        }

                        val focusRequester = remember { FocusRequester() }
                        // Keep the actual keyboard onscreen while comparing reflections.
                        LaunchedEffect(focusRequester) {
                            kotlinx.coroutines.delay(180)
                            focusRequester.requestFocus()
                        }
                        OutlinedTextField(
                            state = testFieldState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .padding(bottom = 12.dp),
                            placeholder = { Text(stringResource(R.string.frosted_glass_test_input_title)) },
                            lineLimits = TextFieldLineLimits.SingleLine,
                            keyboardOptions = KeyboardOptions(platformImeOptions = PlatformImeOptions(
                                privateImeOptions = "${context.packageName}.${Constants.ImeOption.NO_PERSONALIZED_LEARNING}")),
                        )

                        TabRow(selectedTabIndex = editorSection, containerColor = Color.Transparent) {
                            listOf("Surface", "Keys", "Accent").forEachIndexed { index, name ->
                                Tab(selected = editorSection == index, onClick = { editorSection = index }, text = { Text(name) })
                            }
                        }
                        when (editorSection) {
                            0 -> {
                        Text("Light & reflection", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.keyboard_highlight_style_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.keyboard_highlight_style_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                            )
                            val highlightStyles = listOf(
                                Triple(0, stringResource(R.string.keyboard_highlight_classic),
                                    stringResource(R.string.keyboard_highlight_classic_detail)),
                                Triple(1, stringResource(R.string.keyboard_highlight_soft),
                                    stringResource(R.string.keyboard_highlight_soft_detail)),
                                Triple(2, stringResource(R.string.keyboard_highlight_studio),
                                    stringResource(R.string.keyboard_highlight_studio_detail)),
                                Triple(3, stringResource(R.string.keyboard_highlight_quiet),
                                    stringResource(R.string.keyboard_highlight_quiet_detail))
                            )
                            highlightStyles.chunked(2).forEach { rowStyles ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowStyles.forEach { style ->
                                        val chosen = selectedHighlightStyle == style.first
                                        OutlinedButton(
                                            onClick = {
                                                previewHighlightStyle(style.first)
                                                focusRequester.requestFocus()
                                            },
                                            modifier = Modifier.weight(1f).height(74.dp),
                                            shape = RoundedCornerShape(15.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                if (chosen) 2.dp else 1.dp,
                                                if (chosen) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (chosen)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            ),
                                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 7.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = style.second,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = style.third,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 1. Wallpaper Picker Row (styled like a slider row, placed first)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("wallpaper", fontFamily = helium314.keyboard.settings.materialSymbols,
                                        style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (selectedImageUri == null) "Test with your wallpaper" else "Change wallpaper",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (selectedImageUri != null && wallpaperBitmap != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Image(
                                        bitmap = wallpaperBitmap!!,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(MaterialTheme.shapes.small)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            selectedImageUri = null
                                            wallpaperBitmap = null
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text(
                                            text = "✕",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        Text("Clarity", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                        // Slider 1: Blur Radius
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${stringResource(R.string.pref_frosted_blur_radius_title)}: ${currentValues.blurRadius} px",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            SettingsSlider(
                                value = currentValues.blurRadius.toFloat(),
                                onValueChange = { newValue ->
                                    updateCurrentProfile { it.copy(blurRadius = newValue.toInt()) }
                                },
                                valueRange = 10f..150f
                            )
                        }

                        // Slider 3: Background Transparency
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${stringResource(R.string.pref_frosted_bg_transparency_title)}: ${(100 * currentValues.bgTransparency / 255)}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            SettingsSlider(
                                value = currentValues.bgTransparency.toFloat(),
                                onValueChange = { newValue ->
                                    updateCurrentProfile { it.copy(bgTransparency = newValue.toInt()) }
                                },
                                valueRange = 0f..255f
                            )
                        }

                        Text("Color & contrast", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                        // Slider 4: Wallpaper Color Blend
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${stringResource(R.string.pref_frosted_color_blend_title)}: ${currentValues.colorBlend}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            SettingsSlider(
                                value = currentValues.colorBlend.toFloat(),
                                onValueChange = { newValue ->
                                    updateCurrentProfile { it.copy(colorBlend = newValue.toInt()) }
                                },
                                valueRange = 0f..100f
                            )
                        }

                        // Slider 5: Saturation Boost
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${stringResource(R.string.pref_frosted_saturation_title)}: ${currentValues.saturation}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            SettingsSlider(
                                value = currentValues.saturation.toFloat(),
                                onValueChange = { newValue ->
                                    updateCurrentProfile { it.copy(saturation = newValue.toInt()) }
                                },
                                valueRange = 50f..250f
                            )
                        }

                        if (!Defaults.LIMIT_EXPENSIVE_RENDERING) {
                            Text("Surface detail", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.pref_frosted_dust_enabled_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                SettingsSwitch(
                                    checked = snapshot.dustEnabled,
                                    onCheckedChange = { checked ->
                                        updateSparklesEnabled(checked)
                                    }
                                )
                            }

                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "${stringResource(R.string.pref_frosted_dust_alpha_title)}: ${
                                        String.format(java.util.Locale.US, "%.1f", currentValues.dustAlpha)
                                    }",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                SettingsSlider(
                                    value = currentValues.dustAlpha,
                                    onValueChange = { newValue ->
                                        updateCurrentProfile { it.copy(dustAlpha = newValue.coerceIn(1f, 10f)) }
                                    },
                                    valueRange = 1f..10f,
                                    enabled = snapshot.dustEnabled
                                )
                            }
                        }
                            }
                            1 -> {
                        // Slider 2: Key Transparency
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${stringResource(R.string.pref_frosted_key_transparency_title)}: ${(100 * currentValues.keyTransparency / 255)}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            SettingsSlider(
                                value = currentValues.keyTransparency.toFloat(),
                                onValueChange = { newValue ->
                                    updateCurrentProfile { it.copy(keyTransparency = newValue.toInt()) }
                                },
                                valueRange = 0f..255f
                            )
                        }

                        // Slider 7: Alphabet Key Vibrance
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${stringResource(R.string.pref_frosted_alphabet_vibrancy_title)}: ${currentValues.alphabetVibrancy}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            SettingsSlider(
                                value = currentValues.alphabetVibrancy.toFloat(),
                                onValueChange = { newValue ->
                                    updateCurrentProfile { it.copy(alphabetVibrancy = newValue.toInt()) }
                                },
                                valueRange = 0f..500f
                            )
                        }


                            }
                            2 -> {
                        // Slider 6: Special Key Vibrance
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "${stringResource(R.string.pref_frosted_special_vibrancy_title)}: ${currentValues.specialVibrancy}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            SettingsSlider(
                                value = currentValues.specialVibrancy.toFloat(),
                                onValueChange = { newValue ->
                                    updateCurrentProfile { it.copy(specialVibrancy = newValue.toInt()) }
                                },
                                valueRange = 0f..500f
                            )
                        }


                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        thickness = 1.dp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val resetProfile = if (activeProfile == "light") {
                                        FrostedProfileSnapshot(
                                            blurRadius = Defaults.PREF_FROSTED_BLUR_RADIUS,
                                            keyTransparency = Defaults.PREF_FROSTED_KEY_TRANSPARENCY,
                                            bgTransparency = Defaults.PREF_FROSTED_BG_TRANSPARENCY,
                                            colorBlend = Defaults.PREF_FROSTED_COLOR_BLEND,
                                            saturation = Defaults.PREF_FROSTED_SATURATION,
                                            specialVibrancy = Defaults.PREF_FROSTED_SPECIAL_VIBRANCY,
                                            alphabetVibrancy = Defaults.PREF_FROSTED_ALPHABET_VIBRANCY,
                                            dustAlpha = Defaults.PREF_FROSTED_DUST_ALPHA
                                        )
                                    } else {
                                        FrostedProfileSnapshot(
                                            blurRadius = Defaults.PREF_FROSTED_BLUR_RADIUS_NIGHT,
                                            keyTransparency = Defaults.PREF_FROSTED_KEY_TRANSPARENCY_NIGHT,
                                            bgTransparency = Defaults.PREF_FROSTED_BG_TRANSPARENCY_NIGHT,
                                            colorBlend = Defaults.PREF_FROSTED_COLOR_BLEND_NIGHT,
                                            saturation = Defaults.PREF_FROSTED_SATURATION_NIGHT,
                                            specialVibrancy = Defaults.PREF_FROSTED_SPECIAL_VIBRANCY_NIGHT,
                                            alphabetVibrancy = Defaults.PREF_FROSTED_ALPHABET_VIBRANCY_NIGHT,
                                            dustAlpha = Defaults.PREF_FROSTED_DUST_ALPHA_NIGHT
                                        )
                                    }
                                    snapshot = if (activeProfile == "light") {
                                        snapshot.copy(
                                            light = resetProfile,
                                            dustEnabled = Defaults.PREF_FROSTED_DUST_ENABLED
                                        )
                                    } else {
                                        snapshot.copy(
                                            dark = resetProfile,
                                            dustEnabled = Defaults.PREF_FROSTED_DUST_ENABLED
                                        )
                                    }
                                    applyLivePreview(
                                        resetProfile,
                                        Defaults.PREF_FROSTED_DUST_ENABLED
                                    )
                                    previewHighlightStyle(Defaults.PREF_KEYBOARD_TOP_HIGHLIGHT_STYLE)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(percent = 50),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.button_reset),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            FilledTonalButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(percent = 50),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = stringResource(android.R.string.cancel),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    writeSnapshotToPrefs(snapshot, selectedImageUri?.toString())
                                    isSaved = true
                                    onDismissRequest()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(percent = 50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "Apply",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

