package com.nruge.iceinfo.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DBLightColorScheme = lightColorScheme(

    primary = DBRot,
    onPrimary = DBWeiss,
    primaryContainer = Color(0xFFFFDDE0),
    onPrimaryContainer = DBDunkelgrau,

    secondary = DBMittelgrau,
    onSecondary = DBWeiss,
    secondaryContainer = Color(0xFFFFDDE0),
    onSecondaryContainer = DBDunkelgrau,

    tertiary = DBBlau,
    onTertiary = DBWeiss,
    tertiaryContainer = DBBlauHell,
    onTertiaryContainer = DBDunkelgrau,

    error = DBRot,
    onError = DBWeiss,
    errorContainer = DBRotHell,
    onErrorContainer = DBRotDark,

    background = DBHellgrau,
    onBackground = DBDunkelgrau,

    surface = DBWeiss,
    onSurface = DBDunkelgrau,
    surfaceVariant = DBHellgrau,
    onSurfaceVariant = DBMittelgrau,

    outline = DBMittelgrau,
    outlineVariant = Color(0xFFCDD3D8)
)

private val DBDarkColorScheme = darkColorScheme(
    primary = DBRot,
    onPrimary = DBWeiss,
    primaryContainer = Color(0xFF3A1A1D),
    onPrimaryContainer = Color(0xFFFFB3B8),

    secondary = Color(0xFFADB5BD),
    onSecondary = DBDunkelblau,
    secondaryContainer = Color(0xFF2C3140),
    onSecondaryContainer = Color(0xFFCDD3D8),

    tertiary = Color(0xFF63B3E6),
    onTertiary = DBDunkelblau,
    tertiaryContainer = Color(0xFF1A3A52),
    onTertiaryContainer = Color(0xFFB3D9F2),

    error = Color(0xFFFF6B6B),
    onError = DBDunkelblau,
    errorContainer = Color(0xFF3A1A1D),
    onErrorContainer = Color(0xFFFFB3B8),

    background = DBDunkelblau,
    onBackground = Color(0xFFE8EBEE),

    surface = Color(0xFF1E2433),
    onSurface = Color(0xFFE8EBEE),
    surfaceVariant = Color(0xFF2C3140),
    onSurfaceVariant = Color(0xFFADB5BD),

    outline = Color(0xFF646973),
    outlineVariant = Color(0xFF3A3F4B)
)

@Composable
fun ICEInfoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DBDarkColorScheme else DBLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}