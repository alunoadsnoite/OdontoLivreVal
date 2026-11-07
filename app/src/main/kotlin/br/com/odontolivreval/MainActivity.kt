package br.com.odontolivreval

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.designsystem.OdontoTheme
import br.com.odontolivreval.feature.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory((application as OdontoApplication).themes))
            val state by vm.state.collectAsStateWithLifecycle()
            state.theme?.let { mode ->
                OdontoTheme(mode) { OdontoApp(state, vm::selectTheme) }
            }
        }
    }
}
