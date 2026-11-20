// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** A fixed, keyboard-independent palette for the settings and onboarding surfaces.
 *  Keyboard themes, user wallpapers and typing preferences are deliberately untouched.
 */
private val PearlPalette = lightColorScheme(
    primary = Color(0xFF2D60C8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF193A76),
    secondary = Color(0xFF536C90),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDF5),
    onSecondaryContainer = Color(0xFF314560),
    tertiary = Color(0xFF866638),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4E7D4),
    onTertiaryContainer = Color(0xFF5D462B),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF17263D),
    surface = Color.White,
    onSurface = Color(0xFF17263D),
    surfaceVariant = Color(0xFFE8EDF5),
    onSurfaceVariant = Color(0xFF52647F),
    surfaceTint = Color(0xFF2D60C8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FAFD),
    surfaceContainer = Color(0xFFF0F4FA),
    surfaceContainerHigh = Color(0xFFE8EDF5),
    surfaceContainerHighest = Color(0xFFE0E7F2),
    outline = Color(0xFF9EABC0),
    outlineVariant = Color(0xFFDCE3ED),
    inverseSurface = Color(0xFF24344E),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFA5C0F7),
)

private val MidnightPalette = darkColorScheme(
    primary = Color(0xFF89ACED),
    onPrimary = Color(0xFF112D5D),
    primaryContainer = Color(0xFF264778),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFAFC5E4),
    onSecondary = Color(0xFF233B56),
    secondaryContainer = Color(0xFF263D59),
    onSecondaryContainer = Color(0xFFE1EAFA),
    tertiary = Color(0xFFD6A366),
    onTertiary = Color(0xFF493016),
    tertiaryContainer = Color(0xFF584025),
    onTertiaryContainer = Color(0xFFF5E5CF),
    background = Color(0xFF0C1527),
    onBackground = Color(0xFFF5F7FC),
    surface = Color(0xFF162A45),
    onSurface = Color(0xFFF5F7FC),
    surfaceVariant = Color(0xFF273C58),
    onSurfaceVariant = Color(0xFFAFC5E4),
    surfaceTint = Color(0xFF89ACED),
    surfaceContainerLowest = Color(0xFF0C1527),
    surfaceContainerLow = Color(0xFF12213A),
    surfaceContainer = Color(0xFF162A45),
    surfaceContainerHigh = Color(0xFF203654),
    surfaceContainerHighest = Color(0xFF293E5B),
    outline = Color(0xFF7589A5),
    outlineVariant = Color(0xFF344861),
    inverseSurface = Color(0xFFE9EEF7),
    inverseOnSurface = Color(0xFF162A45),
    inversePrimary = Color(0xFF2D60C8),
)

@Composable
fun SettingsBrandTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) MidnightPalette else PearlPalette
    // An IME's custom keycap font must not unexpectedly become the settings UI font.
    val typography = MaterialTheme.typography.let { base ->
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = googleSansFlex),
            headlineLarge = base.headlineLarge.copy(fontFamily = googleSansFlex),
            headlineMedium = base.headlineMedium.copy(fontFamily = googleSansFlex),
            headlineSmall = base.headlineSmall.copy(fontFamily = googleSansFlex),
            titleLarge = base.titleLarge.copy(fontFamily = googleSansFlex),
            titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif),
            bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
            bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif),
            labelLarge = base.labelLarge.copy(fontFamily = FontFamily.SansSerif),
        )
    }
    MaterialTheme(colorScheme = palette, typography = typography, content = content)
}
