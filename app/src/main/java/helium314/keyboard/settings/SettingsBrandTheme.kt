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
    secondaryContainer = Color(0xFFECEEF2),
    onSecondaryContainer = Color(0xFF314560),
    tertiary = Color(0xFF866638),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4E7D4),
    onTertiaryContainer = Color(0xFF5D462B),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF20242B),
    surface = Color.White,
    onSurface = Color(0xFF20242B),
    surfaceVariant = Color(0xFFECEEF2),
    onSurfaceVariant = Color(0xFF626B77),
    surfaceTint = Color(0xFF2D60C8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainer = Color(0xFFF0F2F5),
    surfaceContainerHigh = Color(0xFFECEEF2),
    surfaceContainerHighest = Color(0xFFE2E6EC),
    outline = Color(0xFF8D8E99),
    outlineVariant = Color(0xFFE0E3E8),
    inverseSurface = Color(0xFF24344E),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFA5C0F7),
)

private val MidnightPalette = darkColorScheme(
    primary = Color(0xFF89ACED),
    onPrimary = Color(0xFF112D5D),
    primaryContainer = Color(0xFF264778),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFFB9C1CD),
    onSecondary = Color(0xFF233B56),
    secondaryContainer = Color(0xFF263D59),
    onSecondaryContainer = Color(0xFFE1EAFA),
    tertiary = Color(0xFFD6A366),
    onTertiary = Color(0xFF493016),
    tertiaryContainer = Color(0xFF584025),
    onTertiaryContainer = Color(0xFFF5E5CF),
    background = Color(0xFF13171C),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF20252C),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF2C333D),
    onSurfaceVariant = Color(0xFFB9C1CD),
    surfaceTint = Color(0xFF89ACED),
    surfaceContainerLowest = Color(0xFF13171C),
    surfaceContainerLow = Color(0xFF1A1F26),
    surfaceContainer = Color(0xFF20252C),
    surfaceContainerHigh = Color(0xFF293039),
    surfaceContainerHighest = Color(0xFF363E49),
    outline = Color(0xFF8E8F9B),
    outlineVariant = Color(0xFF39414C),
    inverseSurface = Color(0xFFE9EEF7),
    inverseOnSurface = Color(0xFF20252C),
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
            titleLarge = base.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
            bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Normal),
            bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
            bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp),
            labelLarge = base.labelLarge.copy(fontFamily = FontFamily.SansSerif),
        )
    }
    MaterialTheme(colorScheme = palette, typography = typography, shapes = Shapes(
        extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(SettingsVisuals.groupRadius), large = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(SettingsVisuals.dialogRadius),
    ), content = content)
}
