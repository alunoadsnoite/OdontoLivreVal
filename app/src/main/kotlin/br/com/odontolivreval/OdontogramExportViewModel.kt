package br.com.odontolivreval

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.feature.patients.OdontogramPdf
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class OdontogramRequest(val patientId: Long, val visitId: Long)
internal enum class OdontogramResult { SAVING, EXPORTED, FAILED }
internal data class OdontogramExport(val patientId: Long, val result: OdontogramResult)

internal class OdontogramExportViewModel : ViewModel() {
    private val mutableState = MutableStateFlow<OdontogramExport?>(null)
    val state = mutableState.asStateFlow()

    fun fail(patientId: Long) {
        mutableState.value = OdontogramExport(patientId, OdontogramResult.FAILED)
    }

    fun write(context: Context, store: ClinicStore, request: OdontogramRequest, uri: Uri) {
        mutableState.value = OdontogramExport(request.patientId, OdontogramResult.SAVING)
        viewModelScope.launch {
            try {
                val patient = store.listPatients().firstOrNull { it.id == request.patientId }
                    ?: throw IOException("Patient missing")
                val visit = store.listVisits(request.patientId).firstOrNull { it.id == request.visitId }
                    ?: throw IOException("Visit missing")
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IOException("Cannot open PDF destination")
                    stream.use { OdontogramPdf.write(context, patient.name, visit, it) }
                }
                mutableState.value = OdontogramExport(request.patientId, OdontogramResult.EXPORTED)
            } catch (_: ClinicStorageException) {
                fail(request.patientId)
            } catch (_: IOException) {
                fail(request.patientId)
            } catch (_: SecurityException) {
                fail(request.patientId)
            }
        }
    }
}
