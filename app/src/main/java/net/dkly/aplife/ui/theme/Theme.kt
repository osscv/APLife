package net.dkly.aplife.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = NeutralCard,
    primaryContainer = BrandIndigoSoft,
    onPrimaryContainer = BrandIndigoDark,
    secondary = BrandTeal,
    onSecondary = NeutralCard,
    secondaryContainer = BrandTealSoft,
    onSecondaryContainer = NeutralInk,
    tertiary = BrandAmber,
    onTertiary = NeutralInk,
    tertiaryContainer = BrandAmberSoft,
    onTertiaryContainer = NeutralInk,
    background = NeutralBg,
    onBackground = NeutralInk,
    surface = NeutralCard,
    onSurface = NeutralInk,
    surfaceVariant = NeutralBg,
    onSurfaceVariant = NeutralMuted,
    outline = NeutralLine,
    outlineVariant = NeutralLine,
    error = BrandRose,
    onError = NeutralCard,
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigo,
    onPrimary = NeutralCard,
    primaryContainer = BrandIndigoDark,
    onPrimaryContainer = BrandIndigoSoft,
    secondary = BrandTeal,
    onSecondary = NeutralCard,
    secondaryContainer = SurfaceDarkRaised,
    onSecondaryContainer = BrandTealSoft,
    tertiary = BrandAmber,
    onTertiary = NeutralInk,
    tertiaryContainer = SurfaceDarkRaised,
    onTertiaryContainer = BrandAmberSoft,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDarkRaised,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OnSurfaceVariantDark,
    outlineVariant = SurfaceDarkRaised,
    error = BrandRose,
    onError = NeutralCard,
)

@Composable
fun APLifeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
