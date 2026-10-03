package app.nougat.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * Provides the fixed tokens for the system appearance and the chosen accent, and gives Material's
 * own components (bars, menus, dialogs, switches) the same colours.
 */
@Composable
fun NougatTheme(accent: Accent, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkColors else LightColors
    val a = accent.spec(LocalContext.current).colors(dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = a.text, onPrimary = a.onAccent, primaryContainer = a.fill, onPrimaryContainer = a.onAccent,
        inversePrimary = a.onInverse, secondary = a.text, onSecondary = a.onAccent, tertiary = a.text,
        background = c.paper, onBackground = c.ink, surface = c.paper, onSurface = c.ink,
        surfaceVariant = c.fill, onSurfaceVariant = c.ink2, surfaceTint = c.paper,
        surfaceContainerLowest = c.surface, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surface, surfaceBright = c.surface, surfaceDim = c.background,
        inverseSurface = c.inverse, inverseOnSurface = c.onInverse, outline = c.ink3, outlineVariant = c.hairline,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalColors provides c, LocalAccent provides a, content = content)
    }
}
