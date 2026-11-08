package br.com.odontolivreval.feature.schedule

import android.graphics.Bitmap
import android.net.Uri
import br.com.odontolivreval.core.demo.AppointmentSaveResult
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.Radiograph
import br.com.odontolivreval.core.demo.PrescriptionFile
import br.com.odontolivreval.core.demo.ProfessionalProfile
import br.com.odontolivreval.core.demo.DocumentPhoto
import br.com.odontolivreval.core.demo.DigitalSignature
import br.com.odontolivreval.core.demo.SavedRecord
import br.com.odontolivreval.core.demo.VisitRecord
import kotlin.comparisons.compareBy
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

internal class FakeScheduleStore : ClinicStore {
    val patients = mutableListOf(DemoPatient(1, "Ana"))
    val appointments = mutableMapOf<LocalDate, MutableList<DemoAppointment>>()
    private var nextId = 1L

    override suspend fun listPatients(): List<DemoPatient> = patients.toList()

    override suspend fun listAppointments(day: LocalDate): List<DemoAppointment> =
        appointments[day].orEmpty().sortedBy { it.time }

    override suspend fun countActiveAppointments(month: YearMonth): Map<LocalDate, Int> =
        appointments.filterKeys { YearMonth.from(it) == month }
            .mapValues { (_, visits) -> visits.count { !it.cancelled } }
            .filterValues { it > 0 }

    override suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
        durationMinutes: Int,
        returnOfId: Long?,
    ): AppointmentSaveResult {
        val list = appointments.getOrPut(day) { mutableListOf() }
        val minute = time.hour * 60 + time.minute
        if (list.any {
                val start = it.time.hour * 60 + it.time.minute
                !it.cancelled && it.id != id && start < minute + durationMinutes && minute < start + it.durationMinutes
            }
        ) {
            return AppointmentSaveResult.TIME_OCCUPIED
        }
        val appointment = DemoAppointment(
            id = id ?: nextId++,
            patientId = patientId,
            patientName = patients.first { it.id == patientId }.name,
            day = day,
            time = time,
            cancelled = false,
            durationMinutes = durationMinutes,
            returnOfId = returnOfId,
        )
        appointments.values.forEach { dayAppointments -> dayAppointments.removeAll { it.id == appointment.id } }
        list.add(appointment)
        return AppointmentSaveResult.SAVED
    }

    override suspend fun cancelAppointment(id: Long) {
        appointments.values.forEach { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index >= 0) list[index] = list[index].copy(cancelled = true)
        }
    }

    override suspend fun listUpcomingAppointments(from: LocalDate, days: Int): List<DemoAppointment> =
        appointments.filterKeys { it >= from && it < from.plusDays(days.toLong()) }
            .values.flatten()
            .filterNot { it.cancelled }
            .sortedWith(compareBy<DemoAppointment> { it.day }.thenBy { it.time })

    override suspend fun getProfessionalProfile(): ProfessionalProfile = ProfessionalProfile()

    override suspend fun saveProfessionalProfile(profile: ProfessionalProfile): ProfessionalProfile = profile

    override suspend fun savePatient(id: Long?, name: String): DemoPatient = error("Not used")

    override suspend fun getPatientRecord(patientId: Long): PatientRecord? = error("Not used")

    override suspend fun savePatientRecord(record: PatientRecord): PatientRecord = error("Not used")

    override suspend fun saveRecordAndVisit(record: PatientRecord, visit: VisitRecord): SavedRecord =
        error("Not used")

    override suspend fun deletePatient(id: Long) = error("Not used")

    override suspend fun listVisits(patientId: Long): List<VisitRecord> = error("Not used")

    override suspend fun saveVisit(visit: VisitRecord): VisitRecord = error("Not used")

    override suspend fun listRadiographs(patientId: Long): List<Radiograph> = error("Not used")

    override suspend fun previewRadiograph(patientId: Long, id: Long): Bitmap? = error("Not used")

    override suspend fun addRadiograph(patientId: Long, uri: Uri, fromCamera: Boolean) = error("Not used")

    override suspend fun removeRadiograph(patientId: Long, id: Long) = error("Not used")

    override suspend fun listPrescriptions(patientId: Long): List<PrescriptionFile> = error("Not used")

    override suspend fun addPrescription(patientId: Long, uri: Uri) = error("Not used")

    override suspend fun copyPrescription(patientId: Long, id: Long, uri: Uri) = error("Not used")

    override suspend fun removePrescription(patientId: Long, id: Long) = error("Not used")

    override suspend fun listDocumentPhotos(patientId: Long): List<DocumentPhoto> = error("Not used")

    override suspend fun previewDocumentPhoto(patientId: Long, id: Long): Bitmap? = error("Not used")

    override suspend fun addDocumentPhoto(patientId: Long, uri: Uri) = error("Not used")

    override suspend fun removeDocumentPhoto(patientId: Long, id: Long) = error("Not used")

    override suspend fun listDigitalSignatures(patientId: Long?): List<DigitalSignature> = emptyList()

    override suspend fun previewDigitalSignature(patientId: Long?, id: Long): Bitmap? = error("Not used")

    override suspend fun saveDigitalSignature(patientId: Long?, documentType: String?, bitmap: Bitmap): DigitalSignature = error("Not used")

    override suspend fun removeDigitalSignature(patientId: Long?, id: Long) = error("Not used")

    override suspend fun exportBackup(uri: Uri, password: CharArray) = error("Not used")

    override suspend fun restoreBackup(uri: Uri, password: CharArray) = error("Not used")
}
