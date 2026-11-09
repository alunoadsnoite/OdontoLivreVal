package br.com.odontolivreval.feature.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.CertificateStatus
import br.com.odontolivreval.core.demo.CertificateType
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.MedicalCertificate
import br.com.odontolivreval.core.demo.MedicalCertificateFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class CertificateError {
    LOAD, SAVE, CANCEL, VERSION, INCOMPLETE, NOT_FOUND,
}

internal data class CertificateFormState(
    val type: CertificateType = CertificateType.MEDICAL_CERTIFICATE,
    val title: String = "",
    val content: String = "",
    val serviceDate: String = "",
    val leaveDays: String = "0",
    val entryTime: String = "",
    val exitTime: String = "",
    val companionName: String = "",
    val cid: String = "",
    val cidAuthorized: Boolean = false,
    val placeOfIssue: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: CertificateError? = null,
)

internal data class CertificateListState(
    val certificates: List<MedicalCertificateFile> = emptyList(),
    val loading: Boolean = true,
    val error: CertificateError? = null,
)

internal class CertificateViewModel(private val store: ClinicStore, private val patientId: Long) : ViewModel() {

    // ---------- Form state ----------

    private val formMutable = MutableStateFlow(CertificateFormState())
    val formState = formMutable.asStateFlow()

    fun setType(type: CertificateType) {
        formMutable.update { it.copy(type = type, error = null) }
    }

    fun updateTitle(value: String) {
        formMutable.update { it.copy(title = value, error = null) }
    }

    fun updateContent(value: String) {
        formMutable.update { it.copy(content = value, error = null) }
    }

    fun updateServiceDate(value: String) {
        formMutable.update { it.copy(serviceDate = value, error = null) }
    }

    fun updateLeaveDays(value: String) {
        formMutable.update { it.copy(leaveDays = value, error = null) }
    }

    fun updateEntryTime(value: String) {
        formMutable.update { it.copy(entryTime = value, error = null) }
    }

    fun updateExitTime(value: String) {
        formMutable.update { it.copy(exitTime = value, error = null) }
    }

    fun updateCompanionName(value: String) {
        formMutable.update { it.copy(companionName = value, error = null) }
    }

    fun updateCid(value: String) {
        formMutable.update { it.copy(cid = value, error = null) }
    }

    fun toggleCidAuthorized() {
        formMutable.update { it.copy(cidAuthorized = !it.cidAuthorized, error = null) }
    }

    fun updatePlaceOfIssue(value: String) {
        formMutable.update { it.copy(placeOfIssue = value, error = null) }
    }

    fun loadForm(certificate: MedicalCertificate) {
        formMutable.update {
            it.copy(
                type = certificate.type,
                title = certificate.title,
                content = certificate.content,
                serviceDate = certificate.serviceDate.toString(),
                leaveDays = certificate.leaveDays.toString(),
                entryTime = certificate.entryTime.orEmpty(),
                exitTime = certificate.exitTime.orEmpty(),
                companionName = certificate.companionName.orEmpty(),
                cid = certificate.cid.orEmpty(),
                cidAuthorized = certificate.cidAuthorized,
                placeOfIssue = certificate.placeOfIssue,
                saving = false,
                saved = false,
                error = null,
            )
        }
    }

    fun clearForm() {
        formMutable.update { CertificateFormState() }
    }

    fun validateForm(): Boolean {
        val current = formState.value
        val error = when {
            current.title.isBlank() -> CertificateError.INCOMPLETE
            current.content.isBlank() -> CertificateError.INCOMPLETE
            current.serviceDate.isBlank() -> CertificateError.INCOMPLETE
            // CID requer autorização explícita
            current.cid.isNotBlank() && !current.cidAuthorized -> CertificateError.INCOMPLETE
            // Declaração de comparecimento não deve ter CID
            current.type == CertificateType.ATTENDANCE_DECLARATION && current.cid.isNotBlank() -> CertificateError.INCOMPLETE
            else -> null
        }
        if (error != null) {
            formMutable.update { it.copy(error = error) }
            return false
        }
        formMutable.update { it.copy(error = null) }
        return true
    }

    fun saveCertificate() {
        if (formState.value.saving) return
        if (!validateForm()) return

        val current = formState.value
        formMutable.update { it.copy(saving = true, error = null, saved = false) }

        viewModelScope.launch {
            try {
                val leaveDays = current.leaveDays.toIntOrNull() ?: 0
                val certificate = MedicalCertificate(
                    id = 0,
                    patientId = patientId,
                    type = current.type,
                    status = CertificateStatus.DRAFT,
                    title = current.title,
                    content = current.content,
                    serviceDate = java.time.LocalDate.parse(current.serviceDate),
                    leaveDays = leaveDays,
                    entryTime = current.entryTime.takeIf { it.isNotBlank() },
                    exitTime = current.exitTime.takeIf { it.isNotBlank() },
                    companionName = current.companionName.takeIf { it.isNotBlank() },
                    cid = current.cid.takeIf { it.isNotBlank() },
                    cidAuthorized = current.cidAuthorized,
                    placeOfIssue = current.placeOfIssue,
                )
                val saved = store.saveMedicalCertificate(patientId, certificate)
                formMutable.update {
                    it.copy(
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = CertificateError.SAVE) }
            } catch (_: IllegalArgumentException) {
                formMutable.update { it.copy(saving = false, error = CertificateError.INCOMPLETE) }
            }
        }
    }

    fun finalizeCertificate() {
        if (!validateForm()) return
        saveCertificate()
        // O status FINALIZED é definido pelo saveMedicalCertificate quando o rascunho é salvo como finalizado.
        // Para simplificar, o usuário salva como rascunho e depois finaliza na tela de lista.
    }

    // ---------- List state ----------

    private val listMutable = MutableStateFlow(CertificateListState())
    val listState = listMutable.asStateFlow()

    fun loadList() {
        listMutable.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val certs = store.listMedicalCertificates(patientId)
                listMutable.update { it.copy(certificates = certs, loading = false) }
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(loading = false, error = CertificateError.LOAD) }
            }
        }
    }

    fun cancelCertificate(id: Long, reason: String) {
        if (reason.isBlank()) {
            listMutable.update { it.copy(error = CertificateError.INCOMPLETE) }
            return
        }
        viewModelScope.launch {
            try {
                store.cancelMedicalCertificate(patientId, id, reason)
                loadList()
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = CertificateError.CANCEL) }
            }
        }
    }

    fun createVersion(id: Long) {
        viewModelScope.launch {
            try {
                val newCert = store.createMedicalCertificateVersion(patientId, id)
                listMutable.update { it.copy(error = null) }
                formMutable.update {
                    it.copy(
                        type = newCert.type,
                        title = newCert.title,
                        content = newCert.content,
                        serviceDate = newCert.serviceDate.toString(),
                        leaveDays = newCert.leaveDays.toString(),
                        entryTime = newCert.entryTime.orEmpty(),
                        exitTime = newCert.exitTime.orEmpty(),
                        companionName = newCert.companionName.orEmpty(),
                        cid = newCert.cid.orEmpty(),
                        cidAuthorized = newCert.cidAuthorized,
                        placeOfIssue = newCert.placeOfIssue,
                        saving = false,
                        saved = false,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = CertificateError.VERSION) }
            }
        }
    }

    fun deleteCertificate(id: Long) {
        viewModelScope.launch {
            try {
                store.deleteMedicalCertificate(patientId, id)
                loadList()
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = CertificateError.CANCEL) }
            }
        }
    }

    companion object {
        fun factory(store: ClinicStore, patientId: Long) = viewModelFactory {
            initializer { CertificateViewModel(store, patientId) }
        }
    }
}
