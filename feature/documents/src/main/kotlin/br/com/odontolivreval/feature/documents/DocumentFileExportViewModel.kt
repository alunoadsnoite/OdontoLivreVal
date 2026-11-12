package br.com.odontolivreval.feature.documents

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.odontolivreval.feature.documents.ClinicPdf
import br.com.odontolivreval.feature.documents.DocumentPdfRequest
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class DocumentFileResult { SAVING, SAVED, FAILED }
internal data class DocumentFileExport(val patientId: Long, val result: DocumentFileResult)

internal class DocumentFileExportViewModel : ViewModel() {
    private val mutableState = MutableStateFlow<DocumentFileExport?>(null)
    val state = mutableState.asStateFlow()
    private var pending: DocumentPdfRequest? = null

    fun prepare(request: DocumentPdfRequest): Boolean {
        if (pending != null || state.value?.result == DocumentFileResult.SAVING) return false
        pending = request
        mutableState.value = null
        return true
    }

    fun pickerFailed() {
        pending?.let { mutableState.value = DocumentFileExport(it.patient.id, DocumentFileResult.FAILED) }
        pending = null
    }

    fun finish(context: Context, uri: Uri?) {
        val request = pending
        pending = null
        if (request == null || uri == null) return
        val patientId = request.patient.id
        mutableState.value = DocumentFileExport(patientId, DocumentFileResult.SAVING)
        viewModelScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IOException("Cannot open PDF destination")
                    output.use { ClinicPdf.writeTo(request, it) }
                }
                DocumentFileResult.SAVED
            } catch (_: IOException) {
                DocumentFileResult.FAILED
            } catch (_: SecurityException) {
                DocumentFileResult.FAILED
            }
            mutableState.value = DocumentFileExport(patientId, result)
        }
    }

    /** Limpa dados de exportação de documento da memória quando o app vai para segundo plano. */
    fun clear() {
        pending = null
        mutableState.value = null
    }
}