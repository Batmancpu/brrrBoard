// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
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
    secondaryContainer = Color(0xFFE8E8EF),
    onSecondaryContainer = Color(0xFF314560),
    tertiary = Color(0xFF866638),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4E7D4),
    onTertiaryContainer = Color(0xFF5D462B),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF202126),
    surface = Color.White,
    onSurface = Color(0xFF202126),
    surfaceVariant = Color(0xFFE8E8EF),
    onSurfaceVariant = Color(0xFF62636D),
    surfaceTint = Color(0xFF2D60C8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainer = Color(0xFFEFEFF4),
    surfaceContainerHigh = Color(0xFFE8E8EF),
    surfaceContainerHighest = Color(0xFFE0E0E7),
    outline = Color(0xFF8D8E99),
    outlineVariant = Color(0xFFDDDDE4),
    inverseSurface = Color(0xFF24344E),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFA5C0F7),
)

private val MidnightPalette = darkColorScheme(
    primary = Color(0xFF89ACED),
    onPrimary = Color(0xFF112D5D),
    primaryContainer = Color(0xFF264778),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFC3C4CF),
    onSecondary = Color(0xFF233B56),
    secondaryContainer = Color(0xFF263D59),
    onSecondaryContainer = Color(0xFFE1EAFA),
    tertiary = Color(0xFFD6A366),
    onTertiary = Color(0xFF493016),
    tertiaryContainer = Color(0xFF584025),
    onTertiaryContainer = Color(0xFFF5E5CF),
    background = Color(0xFF111114),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF242428),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF303037),
    onSurfaceVariant = Color(0xFFC3C4CF),
    surfaceTint = Color(0xFF89ACED),
    surfaceContainerLowest = Color(0xFF111114),
    surfaceContainerLow = Color(0xFF1C1C20),
    surfaceContainer = Color(0xFF242428),
    surfaceContainerHigh = Color(0xFF2B2B30),
    surfaceContainerHighest = Color(0xFF37373E),
    outline = Color(0xFF8E8F9B),
    outlineVariant = Color(0xFF3D3D45),
    inverseSurface = Color(0xFFE9EEF7),
    inverseOnSurface = Color(0xFF242428),
    inversePrimary = Color(0xFF2D60C8),
)

@Composable
fun SettingsBrandTheme(content: @Composable () -> Unit) {
    val palette = if (isSystemInDarkTheme()) MidnightPalette else PearlPalette
    // An IME's custom keycap font must not unexpectedly become the settings UI font.
    val typography = MaterialTheme.typography.let { base ->
        base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = FontFamily.SansSerif),
            headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.SansSerif),
            headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.SansSerif),
            headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.SansSerif),
            titleLarge = base.titleLarge.copy(fontFamily = FontFamily.SansSerif),
            titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
            bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-.25).sp, fontWeight = FontWeight.Normal),
            bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp),
            labelLarge = base.labelLarge.copy(fontFamily = FontFamily.SansSerif),
        )
    }
    MaterialTheme(colorScheme = palette, typography = typography, shapes = Shapes(
        extraSmall = RoundedCornerShape(9.dp), small = RoundedCornerShape(13.dp),
        medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(30.dp),
    ), content = content)
}
