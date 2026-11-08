package br.com.odontolivreval

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class PrescriptionState(
    val patientId: Long? = null,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val revision: Int = 0,
    val copied: Boolean = false,
)

internal class PrescriptionViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(PrescriptionState())
    val state = mutableState.asStateFlow()

    fun fail(patientId: Long) {
        mutableState.value = state.value.copy(patientId = patientId, busy = false, failed = true, copied = false)
    }

    fun transfer(store: ClinicStore, patientId: Long, uri: Uri, fileId: Long? = null) {
        if (state.value.busy) return
        mutableState.value = state.value.copy(patientId = patientId, busy = true, failed = false, copied = false)
        viewModelScope.launch {
            try {
                if (fileId == null) store.addPrescription(patientId, uri)
                else store.copyPrescription(patientId, fileId, uri)
                mutableState.value = state.value.copy(
                    busy = false,
                    revision = state.value.revision + 1,
                    copied = fileId != null,
                )
            } catch (_: ClinicStorageException) {
                fail(patientId)
            }
        }
    }
}
