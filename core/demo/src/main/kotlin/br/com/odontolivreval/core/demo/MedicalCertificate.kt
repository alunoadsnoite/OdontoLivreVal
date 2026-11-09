package br.com.odontolivreval.core.demo

import java.time.LocalDate

/**
 * Tipo de documento de atestado ou declaração.
 */
enum class CertificateType {
    /** Atestado odontológico: comprova atendimento e, quando clinicamente indicado, período de afastamento. */
    MEDICAL_CERTIFICATE,
    /** Declaração de comparecimento: comprova que o paciente compareceu à consulta. */
    ATTENDANCE_DECLARATION,
    /** Declaração de acompanhamento: comprova acompanhamento de paciente (criança, idoso, pessoa com deficiência). */
    COMPANION_DECLARATION,
}

/**
 * Status do documento no ciclo de vida.
 */
enum class CertificateStatus {
    /** Rascunho em edição, ainda não emitido. */
    DRAFT,
    /** Documento finalizado e emitido. Não pode ser sobrescrito. */
    FINALIZED,
    /** Documento cancelado, com motivo registrado. */
    CANCELLED,
}

/**
 * Dados do atestado ou declaração (sem anexo de arquivo).
 */
data class MedicalCertificate(
    val id: Long,
    val patientId: Long,
    val type: CertificateType,
    val status: CertificateStatus,
    val title: String,
    /** Conteúdo textual editável pelo profissional. */
    val content: String,
    /** Data do atendimento ou avaliação. */
    val serviceDate: LocalDate,
    /** Período de afastamento/repouso em dias (apenas atestado, quando clinicamente indicado). */
    val leaveDays: Int = 0,
    /** Horário de entrada (apenas declaração de comparecimento). */
    val entryTime: String? = null,
    /** Horário de saída (apenas declaração de comparecimento). */
    val exitTime: String? = null,
    /** Identificação do acompanhante (apenas declaração de acompanhamento). */
    val companionName: String? = null,
    /** CID apenas quando juridicamente apropriado e autorizado pelo paciente. */
    val cid: String? = null,
    /** Indica se o paciente autorizou a inclusão do CID. */
    val cidAuthorized: Boolean = false,
    /** Local de emissão. */
    val placeOfIssue: String = "",
    /** Data de emissão. */
    val issueDate: LocalDate = LocalDate.now(),
    /** Versão do documento (1 = original, >1 = correção de versão anterior). */
    val version: Int = 1,
    /** ID do documento original do qual esta versão deriva. */
    val originalId: Long? = null,
    /** Motivo do cancelamento (apenas quando status = CANCELLED). */
    val cancelledReason: String? = null,
    /** Data do cancelamento. */
    val cancelledAt: LocalDate? = null,
)

/**
 * Metadados do arquivo de certificado armazenado (sem o conteúdo textual sensível).
 */
data class MedicalCertificateFile(
    val id: Long,
    val patientId: Long,
    val type: CertificateType,
    val status: CertificateStatus,
    val title: String,
    val serviceDate: LocalDate,
    val leaveDays: Int,
    val version: Int,
    val originalId: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    /** Motivo do cancelamento (apenas quando status = CANCELLED). */
    val cancelledReason: String? = null,
    /** Data do cancelamento. */
    val cancelledAt: LocalDate? = null,
)

internal data class StoredMedicalCertificate(
    val file: MedicalCertificateFile,
    val content: AttachmentContent,
)
