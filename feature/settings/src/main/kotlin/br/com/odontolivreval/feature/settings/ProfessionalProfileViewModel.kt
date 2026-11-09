package br.com.odontolivreval.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.ProfessionalProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class ProfileError { LOAD, SAVE, INCOMPLETE }

internal data class ProfileState(
    val profile: ProfessionalProfile = ProfessionalProfile(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: ProfileError? = null,
)

internal class ProfessionalProfileViewModel(private val store: ClinicStore) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileState())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (state.value.saving) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val profile = store.getProfessionalProfile()
                mutableState.update { it.copy(profile = profile, loading = false) }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(loading = false, error = ProfileError.LOAD) }
            }
        }
    }

    fun change(profile: ProfessionalProfile) {
        if (!state.value.saving) {
            mutableState.update { it.copy(profile = profile, saved = false, error = null) }
        }
    }

    fun save() {
        val current = state.value
        if (current.saving || current.loading) return
        if (current.profile.name.isBlank() || current.profile.cro.isBlank()) {
            mutableState.update { it.copy(error = ProfileError.INCOMPLETE, saved = false) }
            return
        }
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val saved = store.saveProfessionalProfile(current.profile)
                mutableState.update { it.copy(profile = saved, saving = false, saved = true) }
            } catch (_: ClinicStorageException) {
                mutableState.update { it.copy(saving = false, error = ProfileError.SAVE) }
            }
        }
    }

    fun clear() {
        mutableState.update {
            it.copy(
                profile = ProfessionalProfile(),
                saved = false,
                error = null,
            )
        }
    }

    companion object {
        fun factory(store: ClinicStore) = viewModelFactory {
            initializer { ProfessionalProfileViewModel(store) }
        }
    }
}
