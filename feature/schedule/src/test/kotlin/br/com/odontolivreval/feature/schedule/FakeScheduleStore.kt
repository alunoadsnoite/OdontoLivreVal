package br.com.odontolivreval.feature.schedule

import android.net.Uri
import br.com.odontolivreval.core.demo.AppointmentSaveResult
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.SavedRecord
import br.com.odontolivreval.core.demo.VisitRecord
import java.time.LocalDate
import java.time.LocalTime

internal class FakeScheduleStore : ClinicStore {
    val patients = mutableListOf(DemoPatient(1, "Ana"))
    val appointments = mutableMapOf<LocalDate, MutableList<DemoAppointment>>()
    private var nextId = 1L

    override suspend fun listPatients(): List<DemoPatient> = patients.toList()

    override suspend fun listAppointments(day: LocalDate): List<DemoAppointment> =
        appointments[day].orEmpty().sortedBy { it.time }

    override suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
    ): AppointmentSaveResult {
        val list = appointments.getOrPut(day) { mutableListOf() }
        val minute = time.hour * 60 + time.minute
        if (list.any { !it.cancelled && it.time.hour * 60 + it.time.minute == minute && it.id != id }) {
            return AppointmentSaveResult.TIME_OCCUPIED
        }
        val appointment = DemoAppointment(
            id = id ?: nextId++,
            patientId = patientId,
            patientName = patients.first { it.id == patientId }.name,
            time = time,
            cancelled = false,
        )
        list.removeAll { it.id == appointment.id }
        list.add(appointment)
        return AppointmentSaveResult.SAVED
    }

    override suspend fun cancelAppointment(id: Long) {
        appointments.values.forEach { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index >= 0) list[index] = list[index].copy(cancelled = true)
        }
    }

    override suspend fun savePatient(id: Long?, name: String): DemoPatient = error("Not used")

    override suspend fun getPatientRecord(patientId: Long): PatientRecord? = error("Not used")

    override suspend fun savePatientRecord(record: PatientRecord): PatientRecord = error("Not used")

    override suspend fun saveRecordAndVisit(record: PatientRecord, visit: VisitRecord): SavedRecord =
        error("Not used")

    override suspend fun deletePatient(id: Long) = error("Not used")

    override suspend fun listVisits(patientId: Long): List<VisitRecord> = error("Not used")

    override suspend fun saveVisit(visit: VisitRecord): VisitRecord = error("Not used")

    override suspend fun exportBackup(uri: Uri, password: CharArray) = error("Not used")

    override suspend fun restoreBackup(uri: Uri, password: CharArray) = error("Not used")
}
