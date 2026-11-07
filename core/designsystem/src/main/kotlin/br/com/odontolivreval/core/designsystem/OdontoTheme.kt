package br.com.odontolivreval.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import br.com.odontolivreval.core.model.ThemeMode

private val DentalPurple = Color(0xFF62368C)

private val LightColors = lightColorScheme(
    primary = DentalPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDDCFF),
    onPrimaryContainer = Color(0xFF32124F),
    secondary = Color(0xFF624D75),
    secondaryContainer = Color(0xFFF1E6FA),
    onSecondaryContainer = Color(0xFF301D41),
    background = Color.White,
    onBackground = Color(0xFF241B2D),
    surface = Color.White,
    onSurface = Color(0xFF241B2D),
    surfaceVariant = Color(0xFFF0E8F3),
    onSurfaceVariant = Color(0xFF53465D),
    outline = Color(0xFF766681),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFDDB9FF),
    onPrimary = Color(0xFF391A55),
    primaryContainer = Color(0xFF4E2B70),
    onPrimaryContainer = Color(0xFFF0DBFF),
    secondary = Color(0xFFD4C0E6),
    secondaryContainer = Color(0xFF483554),
    onSecondaryContainer = Color(0xFFF0E2FB),
    background = Color(0xFF18121D),
    onBackground = Color(0xFFF3EAF5),
    surface = Color(0xFF18121D),
    onSurface = Color(0xFFF3EAF5),
    surfaceVariant = Color(0xFF403548),
    onSurfaceVariant = Color(0xFFD8CCDE),
    outline = Color(0xFFAA9AB4),
)

@Composable
fun OdontoTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
