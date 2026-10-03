package app.nougat.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/** Provides the fixed tokens for the system appearance and the chosen accent. */
@Composable
fun NougatTheme(accent: Accent, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalColors provides if (dark) DarkColors else LightColors,
        LocalAccent provides accent.spec(LocalContext.current).colors(dark),
        content = content,
    )
}
