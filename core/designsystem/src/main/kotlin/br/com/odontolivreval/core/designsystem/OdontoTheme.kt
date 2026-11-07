package br.com.odontolivreval.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import br.com.odontolivreval.core.model.ThemeMode

@Composable
fun OdontoTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
}
