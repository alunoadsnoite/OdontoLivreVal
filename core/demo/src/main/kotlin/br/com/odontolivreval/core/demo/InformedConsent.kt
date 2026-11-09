package br.com.odontolivreval.core.demo

import java.math.BigDecimal
import java.time.LocalDate
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tipo de documento de consentimento.
 */
enum class ConsentType {
    /** Tratamento odontológico geral. */
    GENERAL_TREATMENT,
    /** Extração dentária. */
    TOOTH_EXTRACTION,
    /** Tratamento endodôntico. */
    ENDODONTIC_TREATMENT,
    /** Tratamento periodontal. */
    PERIODONTAL_TREATMENT,
    /** Procedimentos restauradores. */
    RESTORATIVE_PROCEDURE,
    /** Tratamento protético. */
    PROSTHETIC_TREATMENT,
    /** Implantes dentários. */
    DENTAL_IMPLANT,
    /** Tratamento ortodôntico. */
    ORTHODONTIC_TREATMENT,
    /** Procedimentos estéticos odontológicos. */
    COSMETIC_PROCEDURE,
}

/**
 * Status de consentimento do paciente.
 *
 * O status reflete a decisão do paciente e a rastreabilidade do termo.
 * Um simples toque do dentista no app não constitui prova de consentimento do paciente.
 */
enum class ConsentStatus {
    /** Rascunho em edição pelo profissional. */
    DRAFT,
    /** Termo apresentado ao paciente. */
    PRESENTED,
    /** Aguardando manifestação do paciente. */
    PENDING,
    /** Aceito pelo paciente, com comprovação registrada. */
    ACCEPTED,
    /** Recusado pelo paciente, com comprovação registrada. */
    REJECTED,
    /** Revogado, quando aplicável. */
    REVOKED,
    /** Substituído por nova versão. */
    SUPERSEDED,
}

/**
 * Dados do responsável legal, quando necessário.
 */
data class LegalRepresentative(
    val name: String = "",
    val relationship: String = "",
    val identification: String = "",
)

/**
 * Comprovação de consentimento do paciente.
 *
 * Registra separadamente a decisão, a data e o método de comprovação.
 */
data class ConsentProof(
    val patientDecision: String? = null,
    val recordedAt: LocalDate? = null,
    val proofMethod: String? = null,
    val hasSignedDocument: Boolean = false,
)

/**
 * Dados do termo de consentimento (sem anexo de arquivo).
 */
data class InformedConsent(
    val id: Long,
    val patientId: Long,
    val type: ConsentType,
    val status: ConsentStatus,
    val title: String,
    val content: String,
    val procedure: String = "",
    val objectives: String = "",
    val explanation: String = "",
    val benefits: String = "",
    val risks: String = "",
    val alternatives: String = "",
    val consequencesOfNotPerforming: String = "",
    val careInstructions: String = "",
    val limitations: String = "",
    val patientQuestions: String = "",
    val clarificationStatement: String = "",
    val consentProof: ConsentProof = ConsentProof(),
    val legalRepresentative: LegalRepresentative? = null,
    val placeOfIssue: String = "",
    val issueDate: LocalDate = LocalDate.now(),
    val professionalId: Long? = null,
    val planId: Long? = null,
    val procedureId: Long? = null,
    val version: Int = 1,
    val originalId: Long? = null,
    val revokedReason: String? = null,
    val revokedAt: LocalDate? = null,
)

/**
 * Metadados do termo armazenado (sem o conteúdo textual sensível).
 */
data class InformedConsentFile(
    val id: Long,
    val patientId: Long,
    val type: ConsentType,
    val status: ConsentStatus,
    val title: String,
    val procedure: String,
    val version: Int,
    val originalId: Long?,
    val planId: Long?,
    val procedureId: Long?,
    val issueDate: LocalDate,
    val createdAt: Long = System.currentTimeMillis(),
    val revokedReason: String? = null,
    val revokedAt: LocalDate? = null,
)

internal data class StoredInformedConsent(
    val file: InformedConsentFile,
    val content: AttachmentContent,
)

/** Serializa um InformedConsent para bytes JSON. */
internal fun InformedConsent.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("type", type.name)
    obj.put("status", status.name)
    obj.put("title", title)
    obj.put("content", content)
    obj.put("procedure", procedure)
    obj.put("objectives", objectives)
    obj.put("explanation", explanation)
    obj.put("benefits", benefits)
    obj.put("risks", risks)
    obj.put("alternatives", alternatives)
    obj.put("consequencesOfNotPerforming", consequencesOfNotPerforming)
    obj.put("careInstructions", careInstructions)
    obj.put("limitations", limitations)
    obj.put("patientQuestions", patientQuestions)
    obj.put("clarificationStatement", clarificationStatement)
    obj.put("placeOfIssue", placeOfIssue)
    obj.put("issueDate", issueDate.toEpochDay())
    obj.put("professionalId", professionalId ?: JSONObject.NULL)
    obj.put("planId", planId ?: JSONObject.NULL)
    obj.put("procedureId", procedureId ?: JSONObject.NULL)
    obj.put("version", version)
    obj.put("originalId", originalId ?: JSONObject.NULL)
    obj.put("revokedReason", revokedReason ?: JSONObject.NULL)
    obj.put("revokedAt", revokedAt?.toEpochDay() ?: JSONObject.NULL)

    val proofObj = JSONObject()
    proofObj.put("patientDecision", consentProof.patientDecision ?: JSONObject.NULL)
    proofObj.put("recordedAt", consentProof.recordedAt?.toEpochDay() ?: JSONObject.NULL)
    proofObj.put("proofMethod", consentProof.proofMethod ?: JSONObject.NULL)
    proofObj.put("hasSignedDocument", consentProof.hasSignedDocument)
    obj.put("consentProof", proofObj)

    val repObj = JSONObject()
    legalRepresentative?.let { rep ->
        repObj.put("name", rep.name)
        repObj.put("relationship", rep.relationship)
        repObj.put("identification", rep.identification)
        obj.put("legalRepresentative", repObj)
    }

    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}
