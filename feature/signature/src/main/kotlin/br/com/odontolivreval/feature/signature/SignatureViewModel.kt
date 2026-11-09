package br.com.odontolivreval.feature.signature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.odontolivreval.core.demo.ConsentStatus
import br.com.odontolivreval.core.demo.ConsentType
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DocumentHash
import br.com.odontolivreval.core.demo.DocumentHashFile
import br.com.odontolivreval.core.demo.DocumentSignature
import br.com.odontolivreval.core.demo.DocumentSignatureFile
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.InformedConsent
import br.com.odontolivreval.core.demo.SignatureModality
import br.com.odontolivreval.core.demo.SignatureProof
import br.com.odontolivreval.core.demo.SignatureStatus
import br.com.odontolivreval.core.demo.SignerIdentification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime

data class SignatureListState(
    val signatures: List<DocumentSignatureFile> = emptyList(),
    val hashes: List<DocumentHashFile> = emptyList(),
    val loading: Boolean = true,
    val error: SignatureError? = null,
)

data class SignatureFormState(
    val documentType: String = "",
    val documentId: Long = 0,
    val documentVersion: Int = 1,
    val modality: SignatureModality = SignatureModality.HANDWRITTEN_DIGITAL,
    val signerRole: String = "dentist",
    val signerName: String = "",
    val signerIdDocument: String = "",
    val signerRegistrationNumber: String = "",
    val hasVisualRepresentation: Boolean = false,
    val cryptographicValid: Boolean = false,
    val notes: String = "",
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: SignatureError? = null,
)

data class HashVerificationState(
    val documentType: String = "",
    val documentId: Long = 0,
    val documentVersion: Int = 1,
    val currentHash: String = "",
    val storedHash: String? = null,
    val integrityMatches: Boolean = false,
    val loading: Boolean = false,
    val error: SignatureError? = null,
)

enum class SignatureError { LOAD, SAVE, DELETE, HASH_ERROR, NOT_FOUND, INCOMPLETE }

class SignatureViewModel(private val store: ClinicStore, private val patientId: Long) : ViewModel() {

    // ---------- Lista de assinaturas e hashes ----------

    private val listMutable = MutableStateFlow(SignatureListState())
    val listState = listMutable.asStateFlow()

    fun loadSignaturesAndHashes() {
        listMutable.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val sigs = store.listDocumentSignatures(patientId)
                val hashes = store.listDocumentHashes(patientId)
                listMutable.update { it.copy(signatures = sigs, hashes = hashes, loading = false) }
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(loading = false, error = SignatureError.LOAD) }
            }
        }
    }

    fun deleteSignature(id: Long) {
        viewModelScope.launch {
            try {
                store.deleteDocumentSignature(patientId, id)
                loadSignaturesAndHashes()
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = SignatureError.DELETE) }
            }
        }
    }

    fun deleteHash(id: Long) {
        viewModelScope.launch {
            try {
                store.deleteDocumentHash(patientId, id)
                loadSignaturesAndHashes()
            } catch (_: ClinicStorageException) {
                listMutable.update { it.copy(error = SignatureError.DELETE) }
            }
        }
    }

    // ---------- Formulário de assinatura ----------

    private val formMutable = MutableStateFlow(SignatureFormState())
    val formState = formMutable.asStateFlow()

    fun initForm(documentType: String, documentId: Long, documentVersion: Int) {
        formMutable.update {
            it.copy(
                documentType = documentType,
                documentId = documentId,
                documentVersion = documentVersion,
                modality = SignatureModality.HANDWRITTEN_DIGITAL,
                signerRole = "dentist",
                signerName = "",
                signerIdDocument = "",
                signerRegistrationNumber = "",
                hasVisualRepresentation = false,
                cryptographicValid = false,
                notes = "",
                loading = false,
                saving = false,
                saved = false,
                error = null,
            )
        }
    }

    fun updateModality(modality: SignatureModality) {
        formMutable.update { it.copy(modality = modality, error = null) }
    }

    fun updateSignerRole(role: String) {
        formMutable.update { it.copy(signerRole = role, error = null) }
    }

    fun updateSignerName(name: String) {
        formMutable.update { it.copy(signerName = name, error = null) }
    }

    fun updateSignerIdDocument(id: String) {
        formMutable.update { it.copy(signerIdDocument = id, error = null) }
    }

    fun updateSignerRegistrationNumber(reg: String) {
        formMutable.update { it.copy(signerRegistrationNumber = reg, error = null) }
    }

    fun updateHasVisualRepresentation(has: Boolean) {
        formMutable.update { it.copy(hasVisualRepresentation = has, error = null) }
    }

    fun updateCryptographicValid(valid: Boolean) {
        formMutable.update { it.copy(cryptographicValid = valid, error = null) }
    }

    fun updateNotes(notes: String) {
        formMutable.update { it.copy(notes = notes, error = null) }
    }

    fun validateForm(): Boolean {
        val current = formState.value
        val error = when {
            current.documentType.isBlank() -> SignatureError.INCOMPLETE
            current.documentId <= 0 -> SignatureError.INCOMPLETE
            current.documentVersion <= 0 -> SignatureError.INCOMPLETE
            current.signerName.isBlank() -> SignatureError.INCOMPLETE
            current.signerRole.isBlank() -> SignatureError.INCOMPLETE
            else -> null
        }
        if (error != null) {
            formMutable.update { it.copy(error = error) }
            return false
        }
        formMutable.update { it.copy(error = null) }
        return true
    }

    fun saveSignature() {
        if (formState.value.saving) return
        if (!validateForm()) return

        val current = formState.value
        formMutable.update { it.copy(saving = true, error = null, saved = false) }

        viewModelScope.launch {
            try {
                val proof = SignatureProof(
                    modality = current.modality,
                    status = SignatureStatus.SIGNED,
                    signedAt = System.currentTimeMillis(),
                    identification = SignerIdentification(
                        role = current.signerRole,
                        name = current.signerName,
                        idDocument = current.signerIdDocument.takeIf { it.isNotBlank() },
                        registrationNumber = current.signerRegistrationNumber.takeIf { it.isNotBlank() },
                    ),
                    hasVisualRepresentation = current.hasVisualRepresentation,
                    cryptographicValid = current.cryptographicValid,
                    notes = current.notes.takeIf { it.isNotBlank() },
                )
                val signature = DocumentSignature(
                    id = 0,
                    patientId = patientId,
                    documentType = current.documentType,
                    documentId = current.documentId,
                    documentVersion = current.documentVersion,
                    documentHash = null,
                    proof = proof,
                )
                val saved = store.saveDocumentSignature(patientId, signature)
                formMutable.update {
                    it.copy(
                        saving = false,
                        saved = true,
                        error = null,
                    )
                }
                loadSignaturesAndHashes()
            } catch (_: ClinicStorageException) {
                formMutable.update { it.copy(saving = false, error = SignatureError.SAVE) }
            }
        }
    }

    fun clearForm() {
        formMutable.update { SignatureFormState() }
    }

    // ---------- Verificação de integridade ----------

    private val hashMutable = MutableStateFlow(HashVerificationState())
    val hashState = hashMutable.asStateFlow()

    fun verifyHash(documentType: String, documentId: Long, documentVersion: Int, currentHash: String) {
        hashMutable.update {
            it.copy(
                documentType = documentType,
                documentId = documentId,
                documentVersion = documentVersion,
                currentHash = currentHash,
                loading = true,
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                val stored = store.listDocumentHashes(patientId)
                    .find { it.documentType == documentType && it.documentId == documentId && it.documentVersion == documentVersion }
                val storedHash = stored?.hashValue
                val integrityMatches = storedHash?.let { it == currentHash } ?: false
                hashMutable.update {
                    it.copy(
                        storedHash = storedHash,
                        integrityMatches = integrityMatches,
                        loading = false,
                        error = null,
                    )
                }
            } catch (_: ClinicStorageException) {
                hashMutable.update { it.copy(loading = false, error = SignatureError.HASH_ERROR) }
            }
        }
    }

    fun saveHash(hash: DocumentHash) {
        if (hashMutable.value.loading) return
        hashMutable.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                val saved = store.saveDocumentHash(patientId, hash)
                hashMutable.update {
                    it.copy(
                        loading = false,
                        storedHash = saved.hashValue,
                        integrityMatches = saved.hashValue == hashMutable.value.currentHash,
                        error = null,
                    )
                }
                loadSignaturesAndHashes()
            } catch (_: ClinicStorageException) {
                hashMutable.update { it.copy(loading = false, error = SignatureError.SAVE) }
            }
        }
    }

    companion object {
        fun factory(store: ClinicStore, patientId: Long) = viewModelFactory {
            initializer { SignatureViewModel(store, patientId) }
        }
    }
}
