package ai.aligned.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AlignedColors(
    val bg: Color, val surface: Color, val elev1: Color, val elev2: Color,
    val text: Color, val textSecondary: Color, val textTertiary: Color,
    val separator: Color, val accent: Color, val destructive: Color
)

val LocalAlignedColors = staticCompositionLocalOf<AlignedColors> {
    error("AlignedColors not provided")
}

@Composable
fun AlignedTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val c = if (dark) AlignedColors(
        bg = Tokens.Color.darkBg, surface = Tokens.Color.darkSurface,
        elev1 = Tokens.Color.darkElev1, elev2 = Tokens.Color.darkElev2,
        text = Tokens.Color.darkText, textSecondary = Tokens.Color.darkTextSecondary,
        textTertiary = Tokens.Color.darkTextTertiary,
        separator = Tokens.Color.darkSeparator,
        accent = Tokens.Color.accent, destructive = Tokens.Color.destructive
    ) else AlignedColors(
        bg = Tokens.Color.lightBg, surface = Tokens.Color.lightSurface,
        elev1 = Tokens.Color.lightElev1, elev2 = Tokens.Color.lightElev2,
        text = Tokens.Color.lightText, textSecondary = Tokens.Color.lightTextSecondary,
        textTertiary = Tokens.Color.lightTextTertiary,
        separator = Tokens.Color.lightSeparator,
        accent = Tokens.Color.accent, destructive = Tokens.Color.destructive
    )

    val m3 = if (dark)
        darkColorScheme(background = c.bg, surface = c.surface, onBackground = c.text, onSurface = c.text, primary = c.accent)
    else
        lightColorScheme(background = c.bg, surface = c.surface, onBackground = c.text, onSurface = c.text, primary = c.accent)

    CompositionLocalProvider(LocalAlignedColors provides c) {
        MaterialTheme(colorScheme = m3, content = content)
    }
}

object AlignedTokens {
    val colors @Composable get() = LocalAlignedColors.current
}
