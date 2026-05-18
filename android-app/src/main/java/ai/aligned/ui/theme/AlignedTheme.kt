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
        bg = Tokens.Palette.darkBg, surface = Tokens.Palette.darkSurface,
        elev1 = Tokens.Palette.darkElev1, elev2 = Tokens.Palette.darkElev2,
        text = Tokens.Palette.darkText, textSecondary = Tokens.Palette.darkTextSecondary,
        textTertiary = Tokens.Palette.darkTextTertiary,
        separator = Tokens.Palette.darkSeparator,
        accent = Tokens.Palette.accent, destructive = Tokens.Palette.destructive
    ) else AlignedColors(
        bg = Tokens.Palette.lightBg, surface = Tokens.Palette.lightSurface,
        elev1 = Tokens.Palette.lightElev1, elev2 = Tokens.Palette.lightElev2,
        text = Tokens.Palette.lightText, textSecondary = Tokens.Palette.lightTextSecondary,
        textTertiary = Tokens.Palette.lightTextTertiary,
        separator = Tokens.Palette.lightSeparator,
        accent = Tokens.Palette.accent, destructive = Tokens.Palette.destructive
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
