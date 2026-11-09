package br.com.odontolivreval.core.demo

import java.math.BigDecimal
import java.time.LocalDate
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

/**
 * Status de execução de um procedimento odontológico.
 *
 * O status reflete o andamento clínico do procedimento, e não o financeiro.
 * Um procedimento pago não está automaticamente "Concluído" clinicamente.
 */
enum class ProcedureStatus {
    PLANNED,      // Planejado
    APPROVED,     // Aprovado
    IN_PROGRESS,  // Em andamento
    COMPLETED,    // Concluído
    CANCELLED,    // Cancelado
}

/**
 * Status de um orçamento odontológico.
 */
enum class BudgetStatus {
    DRAFT,       // Rascunho - pode ser editado livremente
    PRESENTED,   // Apresentado ao paciente - imutável
    APPROVED,    // Aprovado pelo paciente
    REJECTED,    // Recusado pelo paciente
    EXPIRED,     // Expirado (passou do prazo de validade)
    CANCELLED,   // Cancelado
}

/**
 * Um procedimento odontológico dentro de um plano de tratamento.
 *
 * Cada procedimento é informado pelo profissional. O app não sugere diagnósticos
 * nem tratamentos automaticamente.
 */
data class TreatmentProcedure(
    val id: Long,
    val planId: Long,
    val description: String,
    val tooth: String? = null,          // numeração FDI (ex: "11", "51")
    val surface: String? = null,        // superfície (ex: "M", "D", "V", "L", "I", "O")
    val quantity: Int = 1,
    val unitValue: BigDecimal = BigDecimal.ZERO,
    val totalValue: BigDecimal = BigDecimal.ZERO,
    val treatmentStage: Int = 1,        // etapa/sequência do tratamento
    val status: ProcedureStatus = ProcedureStatus.PLANNED,
    val observations: String = "",
)

/**
 * Metadados de um procedimento armazenado (sem conteúdo sensível extra).
 */
data class TreatmentProcedureFile(
    val id: Long,
    val planId: Long,
    val description: String,
    val tooth: String? = null,
    val surface: String? = null,
    val quantity: Int = 1,
    val unitValue: BigDecimal = BigDecimal.ZERO,
    val totalValue: BigDecimal = BigDecimal.ZERO,
    val treatmentStage: Int = 1,
    val status: ProcedureStatus = ProcedureStatus.PLANNED,
)

internal data class StoredTreatmentProcedure(
    val file: TreatmentProcedureFile,
    val content: AttachmentContent,
)

/**
 * Um plano de tratamento para um paciente.
 *
 * Um paciente pode ter vários planos. Cada plano contém procedimentos
 * individuais vinculados a dentes e faces quando aplicável.
 */
data class TreatmentPlan(
    val id: Long,
    val patientId: Long,
    val title: String,
    val description: String,
    val clinicalObjectives: String = "",
    val estimatedSessions: Int = 1,
    val startDate: LocalDate? = null,
    val observations: String = "",
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Metadados de um plano de tratamento armazenado.
 */
data class TreatmentPlanFile(
    val id: Long,
    val patientId: Long,
    val title: String,
    val description: String,
    val estimatedSessions: Int = 1,
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

internal data class StoredTreatmentPlan(
    val file: TreatmentPlanFile,
    val content: AttachmentContent,
)

/**
 * Um item de orçamento (procedimento + valores para o orçamento).
 */
data class BudgetItem(
    val procedureId: Long,
    val description: String,
    val tooth: String? = null,
    val quantity: Int = 1,
    val unitValue: BigDecimal = BigDecimal.ZERO,
    val totalValue: BigDecimal = BigDecimal.ZERO,
)

/**
 * Um orçamento odontológico gerado a partir de um plano de tratamento.
 *
 * Valores monetários usam BigDecimal para evitar problemas de ponto flutuante.
 * Após a emissão (status diferente de DRAFT), o orçamento se torna imutável.
 */
data class TreatmentBudget(
    val id: Long,
    val patientId: Long,
    val planId: Long,
    val budgetNumber: String,
    val status: BudgetStatus = BudgetStatus.DRAFT,
    val items: List<BudgetItem> = emptyList(),
    val discount: BigDecimal = BigDecimal.ZERO,
    val paymentConditions: String = "",
    val validityDays: Int = 30,
    val observations: String = "",
    val issueDate: LocalDate = LocalDate.now(),
    val approvedAt: LocalDate? = null,
    val approvalObservations: String? = null,
    val revision: Int = 1,
    val originalId: Long? = null,
)

/**
 * Metadados de um orçamento armazenado.
 */
data class TreatmentBudgetFile(
    val id: Long,
    val patientId: Long,
    val planId: Long,
    val budgetNumber: String,
    val status: BudgetStatus,
    val revision: Int,
    val originalId: Long?,
    val issueDate: LocalDate,
    val validityDays: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

internal data class StoredTreatmentBudget(
    val file: TreatmentBudgetFile,
    val content: AttachmentContent,
)

/** Serializa um TreatmentPlan para bytes JSON. */
internal fun TreatmentPlan.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("description", description)
    obj.put("clinicalObjectives", clinicalObjectives)
    obj.put("estimatedSessions", estimatedSessions)
    obj.put("startDate", startDate?.toEpochDay() ?: JSONObject.NULL)
    obj.put("observations", observations)
    obj.put("status", status)
    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}

/** Serializa um TreatmentProcedure para bytes JSON. */
internal fun TreatmentProcedure.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("description", description)
    obj.put("tooth", tooth ?: JSONObject.NULL)
    obj.put("surface", surface ?: JSONObject.NULL)
    obj.put("quantity", quantity)
    obj.put("unitValue", unitValue.toPlainString())
    obj.put("totalValue", totalValue.toPlainString())
    obj.put("treatmentStage", treatmentStage)
    obj.put("status", status.name)
    obj.put("observations", observations)
    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}

/** Serializa um TreatmentBudget para bytes JSON. */
internal fun TreatmentBudget.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("items", JSONArray().apply {
        items.forEach { item ->
            put(JSONArray().put(item.procedureId).put(item.description)
                .put(item.tooth ?: JSONObject.NULL)
                .put(item.quantity).put(item.unitValue.toPlainString())
                .put(item.totalValue.toPlainString()))
        }
    })
    obj.put("discount", discount.toPlainString())
    obj.put("paymentConditions", paymentConditions)
    obj.put("validityDays", validityDays)
    obj.put("observations", observations)
    obj.put("approvedAt", approvedAt?.toEpochDay() ?: JSONObject.NULL)
    obj.put("approvalObservations", approvalObservations ?: JSONObject.NULL)
    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}
