package com.nruge.iceinfo.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DBLightColorScheme = lightColorScheme(

    primary = DBRot,
    onPrimary = DBWeiss,
    primaryContainer = DBRotHell,
    onPrimaryContainer = DBDunkelgrau,

    secondary = DBMittelgrau,
    onSecondary = DBWeiss,
    secondaryContainer = DBGrauContainer,
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
    primaryContainer = DBDarkPrimaryContainer,
    onPrimaryContainer = DBDarkOnPrimaryContainer,

    secondary = DBDarkSecondary,
    onSecondary = DBDunkelblau,
    secondaryContainer = DBDarkSecondaryContainer,
    onSecondaryContainer = DBDarkOnSecondaryContainer,

    tertiary = DBDarkTertiary,
    onTertiary = DBDunkelblau,
    tertiaryContainer = DBDarkTertiaryContainer,
    onTertiaryContainer = DBDarkOnTertiaryContainer,

    error = DBDarkError,
    onError = DBDunkelblau,
    errorContainer = DBDarkErrorContainer,
    onErrorContainer = DBDarkOnErrorContainer,

    background = DBDarkBackground,
    onBackground = DBDarkOnBackground,

    surface = DBDarkSurface,
    onSurface = DBDarkOnSurface,
    surfaceVariant = DBDarkSurfaceVariant,
    onSurfaceVariant = DBDarkOnSurfaceVariant,

    outline = DBDarkOutline,
    outlineVariant = DBDarkOutlineVariant
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