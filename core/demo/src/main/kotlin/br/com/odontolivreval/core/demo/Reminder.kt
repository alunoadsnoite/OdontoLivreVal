package br.com.odontolivreval.core.demo

import java.time.LocalDate
import java.time.LocalTime
import java.nio.charset.StandardCharsets
import org.json.JSONObject

/**
 * Status de uma consulta odontológica.
 *
 * O ciclo de vida de uma consulta segue a ordem:
 * SCHEDULED -> CONFIRMED -> IN_PROGRESS -> COMPLETED
 *
 * Uma consulta também pode ser CANCELLED ou MISSED a qualquer momento.
 */
enum class AppointmentStatus {
    SCHEDULED,      // Agendada
    CONFIRMED,      // Confirmada
    IN_PROGRESS,    // Em atendimento
    COMPLETED,      // Concluída
    CANCELLED,      // Cancelada
    MISSED,         // Não compareceu
}

/**
 * Tipo de lembrete de consulta.
 *
 * Indica quando o lembrete deve ser disparado antes da consulta.
 */
enum class ReminderType {
    DAY_24H,    // 24 horas antes
    HOUR_2,     // 2 horas antes
    MINUTE_30,  // 30 minutos antes
    CUSTOM,     // Horário personalizado
}

/**
 * Dados de um lembrete de consulta.
 *
 * Cada consulta pode ter múltiplos lembretes associados, cada um com seu próprio tipo e status.
 */
data class Reminder(
    val id: Long,
    val appointmentId: Long,
    val type: ReminderType,
    val customMinutes: Int? = null,    // Apenas usado quando type == CUSTOM
    val enabled: Boolean = true,        // Se o lembrete está ativo
    val sent: Boolean = false,          // Se o lembrete já foi enviado
    val sentAt: Long? = null,           // Timestamp quando foi enviado
)

/**
 * Metadados de um lembrete armazenado (sem o tipo personalizado se não for CUSTOM).
 */
data class ReminderFile(
    val id: Long,
    val patientId: Long,
    val appointmentId: Long,
    val type: ReminderType,
    val customMinutes: Int?,
    val enabled: Boolean,
    val sent: Boolean,
    val sentAt: Long?,
)

internal data class StoredReminder(
    val file: ReminderFile,
    val content: AttachmentContent,
)

/**
 * Dados de configuração de lembretes para um paciente.
 *
 * Permite configurar quais lembretes estão ativos e com qual antecedência.
 */
data class ReminderSettings(
    val patientId: Long,
    val reminders24h: Boolean = true,
    val reminders2h: Boolean = true,
    val reminders30m: Boolean = true,
    val customMinutes: Int? = null,
    val customEnabled: Boolean = false,
)

/**
 * Metadados de configuração de lembretes armazenados.
 */
data class ReminderSettingsFile(
    val patientId: Long,
    val reminders24h: Boolean,
    val reminders2h: Boolean,
    val reminders30m: Boolean,
    val customMinutes: Int?,
    val customEnabled: Boolean,
)

internal data class StoredReminderSettings(
    val file: ReminderSettingsFile,
    val content: AttachmentContent,
)

/**
 * Serializa um Reminder para bytes JSON.
 */
internal fun Reminder.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("type", type.name)
    obj.put("customMinutes", customMinutes ?: JSONObject.NULL)
    obj.put("enabled", enabled)
    obj.put("sent", sent)
    obj.put("sentAt", sentAt ?: JSONObject.NULL)
    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}

/**
 * Serializa um ReminderFile para bytes JSON.
 */
internal fun ReminderFile.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("appointmentId", appointmentId)
    obj.put("type", type.name)
    obj.put("customMinutes", customMinutes ?: JSONObject.NULL)
    obj.put("enabled", enabled)
    obj.put("sent", sent)
    obj.put("sentAt", sentAt ?: JSONObject.NULL)
    obj.put("patientId", patientId)
    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}

/**
 * Serializa um ReminderSettings para bytes JSON.
 */
internal fun ReminderSettings.toJsonBytes(): ByteArray {
    val obj = JSONObject()
    obj.put("reminders24h", reminders24h)
    obj.put("reminders2h", reminders2h)
    obj.put("reminders30m", reminders30m)
    obj.put("customMinutes", customMinutes ?: JSONObject.NULL)
    obj.put("customEnabled", customEnabled)
    return obj.toString().toByteArray(StandardCharsets.UTF_8)
}

internal fun ReminderSettingsFile.toJsonBytes(): ByteArray = ReminderSettings(
    patientId = patientId,
    reminders24h = reminders24h,
    reminders2h = reminders2h,
    reminders30m = reminders30m,
    customMinutes = customMinutes,
    customEnabled = customEnabled,
).toJsonBytes()
