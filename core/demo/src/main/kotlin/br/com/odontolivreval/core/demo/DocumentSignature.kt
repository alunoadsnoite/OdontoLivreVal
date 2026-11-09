package br.com.odontolivreval.core.demo

import java.math.BigDecimal
import java.time.LocalDate
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Modalidade de assinatura de um documento.
 *
 * O aplicativo diferencia claramente as modalidades para não confundir
 * assinatura manuscrita digitalizada com assinatura digital criptográfica.
 */
enum class SignatureModality {
    /** Assinatura manuscrita em documento impresso (PDF com campos para assinatura manual posterior). */
    HANDWRITTEN_ON_PAPER,
    /** Assinatura manuscrita capturada na tela (desenho via toque/caneta).
     * Não é assinatura digital ICP-Brasil nem prova automática de identidade. */
    HANDWRITTEN_DIGITAL,
    /** Assinatura digital criptográfica (requer chave, certificado e validação apropriados).
     * Preparado para integração futura com provedores compatíveis. */
    CRYPTOGRAPHIC,
}

/**
 * Estado de uma assinatura em um documento.
 */
enum class SignatureStatus {
    /** Assinatura ainda não realizada. */
    PENDING,
    /** Assinatura realizada (qualquer modalidade). */
    SIGNED,
    /** Assinatura revogada ou cancelada. */
    REVOKED,
}

/**
 * Dados de identificação do signatário.
 *
 * Os dados são informados pelo usuário no aplicativo e não são
 * automaticamente verificados contra qualquer base externa.
 */
data class SignerIdentification(
    val role: String,              // "dentist", "patient", "legal_representative"
    val name: String,
    val idDocument: String? = null,
    val registrationNumber: String? = null, // CRO para dentista, CPF para paciente, etc.
)

/**
 * Comprovante de uma assinatura em um documento.
 *
 * Registra separadamente: quem assinou, quando, de que forma e o estado.
 */
data class SignatureProof(
    val modality: SignatureModality,
    val status: SignatureStatus,
    val signedAt: Long = System.currentTimeMillis(),
    val identification: SignerIdentification? = null,
    val hasVisualRepresentation: Boolean = false, // imagem da assinatura manuscrita capturada
    val cryptographicValid: Boolean = false,      // true apenas se assinatura criptográfica efetivamente verificada
    val notes: String? = null,
)

/**
 * Dados de uma assinatura em um documento (sem anexo de arquivo sensível).
 */
data class DocumentSignature(
    val id: Long,
    val patientId: Long,
    val documentType: String,          // "prescription", "certificate", "budget", "consent", "treatment_plan"
    val documentId: Long,              // ID do documento assinado
    val documentVersion: Int,          // versão exata do documento no momento da assinatura
    val documentHash: String? = null,  // SHA-256 do conteúdo do documento
    val proof: SignatureProof,
)

/**
 * Metadados de uma assinatura armazenada (sem o conteúdo da assinatura manuscrita).
 */
data class DocumentSignatureFile(
    val id: Long,
    val patientId: Long,
    val documentType: String,
    val documentId: Long,
    val documentVersion: Int,
    val modality: SignatureModality,
    val status: SignatureStatus,
    val signedAt: Long,
    val identificationRole: String? = null,
    val identificationName: String? = null,
)

internal data class StoredDocumentSignature(
    val file: DocumentSignatureFile,
    val content: AttachmentContent,
)

/**
 * Hash SHA-256 de um documento finalizado, para verificação de integridade.
 */
data class DocumentHash(
    val id: Long,
    val patientId: Long,
    val documentType: String,
    val documentId: Long,
    val documentVersion: Int,
    val hashAlgorithm: String = "SHA-256",
    val hashValue: String,
    val calculatedAt: Long = System.currentTimeMillis(),
    val integrityState: String = "OK", // "OK", "ALTERED", "UNKNOWN"
)

/**
 * Metadados de um hash de integridade armazenado.
 */
data class DocumentHashFile(
    val id: Long,
    val patientId: Long,
    val documentType: String,
    val documentId: Long,
    val documentVersion: Int,
    val hashAlgorithm: String,
    val hashValue: String,
    val calculatedAt: Long,
    val integrityState: String,
)

internal data class StoredDocumentHash(
    val file: DocumentHashFile,
    val content: AttachmentContent,
)

/**
 * Calcula o hash SHA-256 de um array de bytes.
 * Retorna null se o algoritmo não estiver disponível (impraticável em Android moderno).
 */
fun computeSha256(data: ByteArray): String? {
    return try {
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
    } catch (_: NoSuchAlgorithmException) {
        null
    }
}

/** Serializa um DocumentSignature para bytes JSON. */
internal fun DocumentSignature.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("documentType", documentType)
    obj.put("documentId", documentId)
    obj.put("documentVersion", documentVersion)
    obj.put("documentHash", documentHash ?: JSONObject.NULL)
    obj.put("modality", proof.modality.name)
    obj.put("status", proof.status.name)
    obj.put("signedAt", proof.signedAt)
    obj.put("hasVisualRepresentation", proof.hasVisualRepresentation)
    obj.put("cryptographicValid", proof.cryptographicValid)
    proof.identification?.let { id ->
        obj.put("identificationRole", id.role)
        obj.put("identificationName", id.name ?: JSONObject.NULL)
        obj.put("identificationIdDocument", id.idDocument ?: JSONObject.NULL)
        obj.put("identificationRegistrationNumber", id.registrationNumber ?: JSONObject.NULL)
    }
    proof.notes?.let { obj.put("notes", it) }
    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}

/** Serializa um DocumentHash para bytes JSON. */
internal fun DocumentHash.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("documentType", documentType)
    obj.put("documentId", documentId)
    obj.put("documentVersion", documentVersion)
    obj.put("hashAlgorithm", hashAlgorithm)
    obj.put("hashValue", hashValue)
    obj.put("calculatedAt", calculatedAt)
    obj.put("integrityState", integrityState)
    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}
