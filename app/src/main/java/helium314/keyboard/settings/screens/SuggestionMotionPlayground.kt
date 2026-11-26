// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.suggestions.QuickTypeMotion
import helium314.keyboard.latin.suggestions.SuggestionConfidencePillDrawable
import helium314.keyboard.latin.suggestions.SuggestionConfidencePillGeometry
import helium314.keyboard.latin.suggestions.SuggestionMotionPreset
import helium314.keyboard.latin.utils.prefs
import kotlinx.coroutines.delay

internal fun suggestionMotionChoices(context: Context): List<Pair<String, String>> = listOf(
    context.getString(R.string.suggestion_motion_scale) to SuggestionMotionPreset.SCALE.id,
    context.getString(R.string.suggestion_motion_spring) to SuggestionMotionPreset.SPRING.id,
    context.getString(R.string.suggestion_motion_ripple) to SuggestionMotionPreset.RIPPLE.id,
    context.getString(R.string.suggestion_motion_classic) to SuggestionMotionPreset.CLASSIC.id,
)

@Composable
internal fun SuggestionMotionPlayground() {
    val context = LocalContext.current
    val prefs = context.prefs()
    var selected by remember { mutableStateOf(SuggestionMotionPreset.selected(context)) }
    var frame by remember { mutableIntStateOf(0) }
    var replay by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Settings.PREF_SUGGESTION_MOTION_STYLE) {
                selected = SuggestionMotionPreset.selected(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    LaunchedEffect(replay, selected) {
        if (replay > 0) {
            frame = 0
            delay(450)
            for (i in 1 until SuggestionMotionSampleView.frameCount) {
                frame = i
                delay(520)
            }
        }
    }
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val divider = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val capsule = MaterialTheme.colorScheme.secondaryContainer.toArgb()
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.suggestion_playground_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.suggestion_playground_summary), style = MaterialTheme.typography.bodyMedium)
            suggestionMotionChoices(context).forEach { (label, id) ->
                FilterChip(selected = selected.id == id, onClick = {
                    val preset = SuggestionMotionPreset.fromId(id)
                    replay = 0
                    frame = 0
                    prefs.edit().putString(Settings.PREF_SUGGESTION_MOTION_STYLE, preset.id).apply()
                    selected = preset
                }, label = { Text(label) })
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                factory = { SuggestionMotionSampleView(it) },
                update = { it.showFrame(frame, selected, foreground, divider, capsule) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { replay++ }) { Text(stringResource(R.string.suggestion_playground_replay)) }
                OutlinedButton(onClick = { replay = 0; frame = (frame + 1) % SuggestionMotionSampleView.frameCount }) {
                    Text(stringResource(R.string.suggestion_playground_next))
                }
            }
            if (!KeyboardMotionSpec.isIos27(context) || !KeyboardMotionSpec.optionalMotion(context)) {
                Text(stringResource(R.string.suggestion_playground_disabled), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Deterministic examples drawn by the actual IME renderer, without using user data or the decoder. */
private class SuggestionMotionSampleView(context: Context) : LinearLayout(context) {
    private data class Sample(val words: List<String>, val confidence: Boolean = false)
    private val words = List(3) {
        TextView(context).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            textSize = 17f
            maxLines = 1
            isSingleLine = true
        }
    }
    private val dividers = List(2) { View(context) }
    private var selected = SuggestionMotionPreset.SCALE
    private var frame = -1
    private var confident = false
    private var previousColors = emptyList<Int>()
    private val renderer = QuickTypeMotion(this, dividers) { selected }
    private val density = resources.displayMetrics.density
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        for (i in 0..2) {
            if (i > 0) addView(dividers[i - 1], LayoutParams(
                density.toInt().coerceAtLeast(1), (23 * density).toInt()))
            addView(words[i], LayoutParams(0, LayoutParams.MATCH_PARENT, if (i == 1) .40f else .30f))
        }
    }
    fun showFrame(index: Int, preset: SuggestionMotionPreset, foreground: Int, divider: Int, capsule: Int) {
        val colors = listOf(foreground, divider, capsule)
        if (index == frame && preset == selected && colors == previousColors) return
        val previous = if (frame >= 0 && preset == selected && colors == previousColors && isLaidOut)
            renderer.capture(words, confident) else null
        if (previous == null) renderer.cancel(words)
        selected = preset
        previousColors = colors
        frame = index
        val sample = samples[index.coerceIn(samples.indices)]
        confident = sample.confidence
        val inset = 5f * density
        words.forEachIndexed { i, view ->
            view.text = sample.words[i]
            view.setTextColor(foreground)
            view.background = ColorDrawable(Color.TRANSPARENT)
        }
        dividers.forEach {
            it.setBackgroundColor(divider)
            it.visibility = if (confident) View.INVISIBLE else View.VISIBLE
        }
        if (confident) {
            val width = SuggestionConfidencePillGeometry.fixedWidth(words[1].width.toFloat(), density)
            words[1].background = SuggestionConfidencePillDrawable(capsule, width, width, 1f, 1f, inset)
        }
        if (previous != null) renderer.morph(previous, words, confident, capsule, inset, false)
    }
    override fun onDetachedFromWindow() {
        renderer.cancel(words)
        super.onDetachedFromWindow()
    }
    companion object {
        private val samples = listOf(
            Sample(listOf("the", "instead", "inside")),
            Sample(listOf("install", "instead", "instant")),
            Sample(listOf("installed", "install", "instantly"), true),
            Sample(listOf("installing", "installation", "installer"), true),
            Sample(listOf("a", "the", "it")),
            Sample(listOf("now", "today", "tomorrow")),
        )
        val frameCount: Int get() = samples.size
    }
}
