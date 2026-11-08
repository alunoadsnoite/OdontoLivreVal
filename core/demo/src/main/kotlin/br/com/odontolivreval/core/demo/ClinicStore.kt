package br.com.odontolivreval.core.demo

import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime

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

    suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
    ): AppointmentSaveResult

    suspend fun cancelAppointment(id: Long)

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

    suspend fun exportBackup(uri: Uri, password: CharArray)

    suspend fun restoreBackup(uri: Uri, password: CharArray)
}

data class SavedRecord(val record: PatientRecord, val visit: VisitRecord)
