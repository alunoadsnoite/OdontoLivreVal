package br.com.odontolivreval

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

internal data class DocumentPhotoImportState(
    val revision: Int = 0,
    val busy: Boolean = false,
    val failedPatient: Long? = null,
)

internal class DocumentPhotoImportViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(DocumentPhotoImportState())
    val state = mutableState.asStateFlow()

    fun failed(patientId: Long) {
        mutableState.value = mutableState.value.copy(failedPatient = patientId)
    }

    fun import(store: ClinicStore, patientId: Long, uri: Uri, temporary: File? = null) {
        mutableState.value = mutableState.value.copy(busy = true, failedPatient = null)
        viewModelScope.launch {
            try {
                store.addDocumentPhoto(patientId, uri)
                mutableState.value = mutableState.value.copy(revision = mutableState.value.revision + 1)
            } catch (_: ClinicStorageException) {
                failed(patientId)
            } finally {
                temporary?.delete()
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }
}
