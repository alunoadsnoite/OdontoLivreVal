package br.com.odontolivreval.feature.patients

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class RadiographImportState(
    val revision: Int = 0,
    val busy: Boolean = false,
    val failedPatient: Long? = null,
)

internal class RadiographImportViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(RadiographImportState())
    val state = mutableState.asStateFlow()

    fun failed(patientId: Long) {
        mutableState.value = mutableState.value.copy(failedPatient = patientId)
    }

    fun import(store: ClinicStore, patientId: Long, uri: Uri, temporary: File? = null) {
        mutableState.value = mutableState.value.copy(busy = true, failedPatient = null)
        viewModelScope.launch {
            try {
                store.addRadiograph(patientId, uri, fromCamera = temporary != null)
                mutableState.value = mutableState.value.copy(revision = mutableState.value.revision + 1)
            } catch (_: ClinicStorageException) {
                failed(patientId)
            } finally {
                temporary?.delete()
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }

    /** Limpa dados de importação de radiografia da memória quando o app vai para segundo plano. */
    fun clear() {
        mutableState.value = mutableState.value.copy(
            busy = false,
            failedPatient = null,
        )
    }
}

class RadiographFileProvider : FileProvider()