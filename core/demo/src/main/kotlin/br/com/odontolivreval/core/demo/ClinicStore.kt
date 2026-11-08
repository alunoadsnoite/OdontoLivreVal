package br.com.odontolivreval.core.demo

import android.graphics.Bitmap
import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Contrato de persistência da clínica de demonstração.
 *
 * A interface permite testar os ViewModels sem Android e deixa explícito o
 * conjunto de operações que a camada de dados precisa oferecer.
 */
interface ClinicStore {
    suspend fun listPatients(): List<DemoPatient>

    suspend fun savePatient(id: Long?, name: String): DemoPatient

    suspend fun listAppointments(day: LocalDate): List<DemoAppointment>

    suspend fun countActiveAppointments(month: YearMonth): Map<LocalDate, Int>

    suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
        durationMinutes: Int = minOf(defaultAppointmentMinutes, 1440 - time.hour * 60 - time.minute),
        returnOfId: Long? = null,
    ): AppointmentSaveResult

    suspend fun cancelAppointment(id: Long)

    /**
     * Consultas ativas a partir de [from], somando [days] dias, em ordem
     * cronológica. Usada pelo painel de próximas consultas do Início.
     */
    suspend fun listUpcomingAppointments(from: LocalDate, days: Int): List<DemoAppointment>

    suspend fun getProfessionalProfile(): ProfessionalProfile

    suspend fun saveProfessionalProfile(profile: ProfessionalProfile): ProfessionalProfile

    suspend fun getPatientRecord(patientId: Long): PatientRecord?

    suspend fun savePatientRecord(record: PatientRecord): PatientRecord

    /**
     * Grava a ficha clínica e a consulta em uma única transação, atribuindo
     * identificadores novos quando necessário.
     */
    suspend fun saveRecordAndVisit(record: PatientRecord, visit: VisitRecord): SavedRecord

    suspend fun deletePatient(id: Long)

    suspend fun listVisits(patientId: Long): List<VisitRecord>

    suspend fun saveVisit(visit: VisitRecord): VisitRecord

    suspend fun listRadiographs(patientId: Long): List<Radiograph>

    suspend fun previewRadiograph(patientId: Long, id: Long): Bitmap?

    suspend fun addRadiograph(patientId: Long, uri: Uri, fromCamera: Boolean)

    suspend fun removeRadiograph(patientId: Long, id: Long)

    suspend fun listPrescriptions(patientId: Long): List<PrescriptionFile>

    suspend fun addPrescription(patientId: Long, uri: Uri)

    suspend fun copyPrescription(patientId: Long, id: Long, uri: Uri)

    suspend fun removePrescription(patientId: Long, id: Long)

    suspend fun exportBackup(uri: Uri, password: CharArray)

    suspend fun restoreBackup(uri: Uri, password: CharArray)
}

data class SavedRecord(val record: PatientRecord, val visit: VisitRecord)
