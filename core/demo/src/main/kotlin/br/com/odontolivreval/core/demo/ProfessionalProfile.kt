package br.com.odontolivreval.core.demo

/**
 * Dados do profissional que assina os documentos da clínica.
 *
 * O perfil é gravado junto com o restante dos dados de demonstração para que
 * acompanhe o backup criptografado.
 */
data class ProfessionalProfile(
    val name: String = "",
    val cro: String = "",
    val croUF: String = "",
    val cpf: String = "",
    val specialty: String = "",
    val clinicName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val logoFileId: Long? = null,
) {
    val isEmpty: Boolean
        get() = name.isBlank() && cro.isBlank()

    companion object {
        const val MAX_NAME = 120
        const val MAX_CRO = 30
        const val MAX_CRO_UF = 2
        const val MAX_CPF = 14
        const val MAX_SPECIALTY = 80
        const val MAX_CLINIC = 120
        const val MAX_ADDRESS = 200
        const val MAX_PHONE = 40
        const val MAX_EMAIL = 120
    }
}
