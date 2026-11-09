package br.com.odontolivreval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class HomeState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val upcoming: List<DemoAppointment> = emptyList(),
)

internal class HomeViewModel(private val store: ClinicStore) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, failed = false) }
        viewModelScope.launch {
            try {
                val upcoming = store.listUpcomingAppointments(LocalDate.now(), UPCOMING_DAYS)
                    .take(UPCOMING_LIMIT)
                mutableState.update { it.copy(loading = false, upcoming = upcoming) }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    /** Limpa dados de próximas consultas da memória quando o app vai para segundo plano. */
    fun clear() {
        mutableState.update { it.copy(upcoming = emptyList()) }
    }

    companion object {
        private const val UPCOMING_DAYS = 30
        private const val UPCOMING_LIMIT = 6

        fun factory(store: ClinicStore) = viewModelFactory {
            initializer { HomeViewModel(store) }
        }
    }
}
