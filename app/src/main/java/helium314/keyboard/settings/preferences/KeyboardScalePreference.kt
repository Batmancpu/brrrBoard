// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import helium314.keyboard.latin.settings.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.FoldableUtils
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.WithSmallTitle
import helium314.keyboard.settings.dialogs.ThreeButtonAlertDialog
import helium314.keyboard.latin.utils.previewDark
import androidx.core.content.edit

// actual key for each setting is baseKey with one _true/_false appended per dimension (need to keep order!)
// should dimension checkboxes have any other effect than just showing / hiding sliders?
//  one could argue that e.g. when disabling the split checkbox, then split mode should not affect the setting
@Composable
fun KeyboardScalePreference(
    name: String,
    baseKey: String,
    dimensions: List<String>,
    defaults: Array<Float>,
    range:  ClosedFloatingPointRange<Float>,
    description: (Float) -> String,
    onDone: () -> Unit
) {
    if (defaults.size != 1.shl(dimensions.size))
        throw ArithmeticException("defaults size does not match with dimensions, expected ${1.shl(dimensions.size)}, got ${defaults.size}")
    var showDialog by remember { mutableStateOf(false) }
    Preference(
        name = name,
        onClick = { showDialog = true },
        // no description because it can easily take up too much space
    )
    if (showDialog)
        KeyboardScaleDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(name) },
            baseKey = baseKey,
            onDone = onDone,
            defaultValues = defaults,
            range = range,
            dimensions = dimensions,
            positionString = description
        )
}

// SliderDialog specialized for keyboard scale settings using multiple sliders with same range, each with a different setting and title
@Composable
private fun KeyboardScaleDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    baseKey: String,
    onDone: () -> Unit,
    defaultValues: Array<Float>,
    range: ClosedFloatingPointRange<Float>,
    dimensions: List<String>,
    modifier: Modifier = Modifier,
    positionString: (Float) -> String,
) {
    val (variants, keys) = createVariantsAndKeys(dimensions, baseKey)
    val foldedString = stringResource(R.string.folded) // we want to hide foldable settings for non-foldable phones
    val ctx = LocalContext.current
    var checked by remember { mutableStateOf(dimensions.map { FoldableUtils.isFoldable || !it.contains(foldedString) }) }
    val prefs = ctx.prefs()
    val original = remember(keys) { keys.mapIndexed { index, key -> prefs.getFloat(key, defaultValues[index]).let { if (it.isFinite()) it.coerceIn(range) else defaultValues[index].coerceIn(range) } } }
    var draft by remember(keys) { mutableStateOf(original) }

    ThreeButtonAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmed = {
            if (draft != original) {
                prefs.edit {
                    draft.forEachIndexed { index, value ->
                        if (value != original[index]) {
                            if (value == defaultValues[index]) remove(keys[index]) else putFloat(keys[index], value)
                        }
                    }
                }
                onDone()
            }
        },
        modifier = modifier,
        title = title,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyLarge
            ) {
                val state = rememberScrollState()
                Column(Modifier.verticalScroll(state)) {
                    if (dimensions.size > 1) {
                        dimensions.forEachIndexed { i, dimension ->
                            // hide "folded" box for non-foldables
                            if (FoldableUtils.isFoldable || !dimension.contains(foldedString))
                                DimensionCheckbox(checked[i], dimension) {
                                    checked = checked.mapIndexed { j, c -> if (i == j) it else c }
                                }
                        }
                    }
                    variants.forEachIndexed { i, variant ->
                        val sliderPosition = draft[i]
                        fun setPosition(value: Float) {
                            draft = draft.mapIndexed { index, old -> if (index == i) value.coerceIn(range) else old }
                        }
                        val forbiddenDimensions = dimensions.filterIndexed { index, _ -> !checked[index] }
                        val visible = variant.split(SPLIT).none { it in forbiddenDimensions }
                        // default animations make the dialog flash (see also DictionaryDialog)
                        AnimatedVisibility(visible, exit = fadeOut(), enter = fadeIn()) {
                            WithSmallTitle(variant.ifEmpty { stringResource(R.string.button_default) }) {
                                GeometryDragPreview(baseKey, sliderPosition, range, ::setPosition)
                                SettingsSlider(
                                    value = sliderPosition,
                                    onValueChange = ::setPosition,
                                    valueRange = range,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(positionString(sliderPosition))
                                    TextButton({ setPosition(defaultValues[i]) }) { Text(stringResource(R.string.button_default)) }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        },
    )
}

/** Schematic direct manipulation shares the exact precision slider draft.
 *  The existing IME tryout shows the real geometry after Apply; never writes during drag. */
@Composable
private fun GeometryDragPreview(baseKey: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val horizontal = baseKey == Settings.PREF_SIDE_PADDING_SCALE_PREFIX || baseKey == Settings.PREF_SPLIT_SPACER_SCALE_PREFIX
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    val accent = MaterialTheme.colorScheme.primary
    val panel = MaterialTheme.colorScheme.surfaceVariant
    val keyColor = MaterialTheme.colorScheme.surface
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Text(if (horizontal) "Drag the side handle or use the slider" else "Drag the top handle or use the slider",
        style = MaterialTheme.typography.labelSmall)
    Canvas(Modifier.fillMaxWidth().height(108.dp)
        .semantics { contentDescription = "Keyboard geometry preview; precise adjustment available below" }
        .pointerInput(baseKey, range) {
            var start = 0f
            var distance = 0f
            detectDragGestures(onDragStart = { start = current; distance = 0f }) { pointer, delta ->
                pointer.consume()
                distance += if (horizontal) delta.x else -delta.y
                val length = (if (horizontal) size.width else size.height).coerceAtLeast(1)
                change((start + distance / length * (range.endInclusive - range.start)).coerceIn(range))
            }
        }) {
        val outerPad = if (baseKey == Settings.PREF_SIDE_PADDING_SCALE_PREFIX) size.width * (.04f + .24f * fraction) else size.width * .04f
        val top = if (baseKey == Settings.PREF_KEYBOARD_HEIGHT_SCALE_PREFIX) size.height * (.45f - .4f * fraction) else size.height * .07f
        val bottom = size.height * (if (baseKey == Settings.PREF_BOTTOM_PADDING_SCALE_PREFIX) .03f + .35f * fraction else .03f)
        val h = (size.height - top - bottom).coerceAtLeast(1f)
        val w = (size.width - 2 * outerPad).coerceAtLeast(1f)
        drawRoundRect(panel, Offset(outerPad, top), Size(w, h), CornerRadius(10.dp.toPx()))
        val gap = 3.dp.toPx()
        val split = if (baseKey == Settings.PREF_SPLIT_SPACER_SCALE_PREFIX) w * (.025f + .18f * fraction) else 0f
        val bottomFraction = if (baseKey == Settings.PREF_BOTTOM_ROW_SCALE_PREFIX) .14f + .24f * fraction else .24f
        val rowH = h * (1f - bottomFraction) / 3f
        repeat(3) { row ->
            val count = 10 - row
            val cell = ((w - split) / count).coerceAtLeast(1f)
            repeat(count) { column ->
                val left = outerPad + column * cell + (if (column >= count / 2) split else 0f) + gap / 2
                drawRoundRect(keyColor, Offset(left, top + row * rowH + gap / 2),
                    Size((cell - gap).coerceAtLeast(1f), (rowH - gap).coerceAtLeast(1f)), CornerRadius(4.dp.toPx()))
            }
        }
        drawRoundRect(keyColor, Offset(outerPad + w * .2f, top + rowH * 3 + gap / 2),
            Size(w * .6f, (h * bottomFraction - gap).coerceAtLeast(1f)), CornerRadius(4.dp.toPx()))
        if (horizontal) drawRoundRect(accent, Offset(if (baseKey == Settings.PREF_SPLIT_SPACER_SCALE_PREFIX) outerPad + w / 2 + split / 2 else outerPad, top + h * .34f), Size(4.dp.toPx(), h * .32f), CornerRadius(2.dp.toPx()))
        else drawRoundRect(accent, Offset(outerPad + w * .42f, when (baseKey) {
            Settings.PREF_BOTTOM_ROW_SCALE_PREFIX -> top + rowH * 3
            Settings.PREF_BOTTOM_PADDING_SCALE_PREFIX -> top + h - 4.dp.toPx()
            else -> top
        }), Size(w * .16f, 4.dp.toPx()), CornerRadius(2.dp.toPx()))
    }
}

@Composable
private fun DimensionCheckbox(checked: Boolean, dimension: String, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) }
        )
        Text(dimension)
    }
}

private fun createVariantsAndKeys(dimensions: List<String>, baseKey: String): Pair<List<String>, List<String>> {
    val variants = mutableListOf("")
    val keys = mutableListOf(createPrefKeyForBooleanSettings(baseKey, 0, dimensions.size))
    var i = 1
    dimensions.forEach { dimension ->
        variants.toList().forEach { variant ->
            if (variant.isEmpty()) variants.add(dimension)
            else variants.add(variant + SPLIT + dimension)
            keys.add(createPrefKeyForBooleanSettings(baseKey, i, dimensions.size))
            i++
        }
    }
    return variants to keys
}

private const val SPLIT = " / "

@Preview
@Composable
private fun Preview() {
    Theme(previewDark) {
        KeyboardScaleDialog(
            onDismissRequest = { },
            onDone = { },
            positionString = { "${it.toInt()}%"},
            defaultValues = Array(8) { 100f - it % 2 * 50f },
            range = 0f..500f,
            title = { Text("bottom padding scale") },
            dimensions = listOf("landscape", "split", "folded"),
            baseKey = ""
        )
    }
}
