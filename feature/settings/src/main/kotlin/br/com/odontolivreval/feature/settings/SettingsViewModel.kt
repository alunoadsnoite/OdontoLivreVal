package br.com.odontolivreval.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.odontolivreval.core.model.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

data class SettingsState(val theme: ThemeMode? = null, val writeFailed: Boolean = false)
class SettingsViewModel(private val repository: ThemeRepository) : ViewModel() {
    private val failed = MutableStateFlow(false)
    val state = combine(repository.theme, failed) { mode, error -> SettingsState(mode, error) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())
    fun selectTheme(mode: ThemeMode) {
        viewModelScope.launch {
            try { repository.setTheme(mode); failed.value = false }
            catch (_: IOException) { failed.value = true }
        }
    }

    /** Limpa estado de erro de gravação de tema quando o app vai para segundo plano. */
    fun clear() {
        failed.value = false
    }
    companion object {
        fun factory(repository: ThemeRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(repository) as T
            }
        }
    }
}
