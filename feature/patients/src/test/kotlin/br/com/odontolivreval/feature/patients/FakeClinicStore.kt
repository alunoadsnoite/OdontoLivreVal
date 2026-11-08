package br.com.odontolivreval.feature.patients

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
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

internal class FakeClinicStore : ClinicStore {
    val patients = mutableListOf<DemoPatient>()
    val records = mutableListOf<PatientRecord>()
    val visits = mutableListOf<VisitRecord>()
    var saves = 0
        private set
    private var nextRecordId = 1L

    override suspend fun listPatients(): List<DemoPatient> = patients.toList()

    override suspend fun savePatient(id: Long?, name: String): DemoPatient {
        val saved = DemoPatient(id ?: ((patients.maxOfOrNull { it.id } ?: 0) + 1), name)
        patients.removeAll { it.id == saved.id }
        patients.add(saved)
        return saved
    }

    override suspend fun getPatientRecord(patientId: Long): PatientRecord? =
        records.find { it.patientId == patientId }

    override suspend fun savePatientRecord(record: PatientRecord): PatientRecord {
        val saved = record.copy(id = record.id.takeIf { it > 0 } ?: nextRecordId++)
        records.removeAll { it.patientId == saved.patientId }
        records.add(saved)
        saves++
        return saved
    }

    override suspend fun saveRecordAndVisit(record: PatientRecord, visit: VisitRecord): SavedRecord {
        val savedRecord = savePatientRecord(record)
        val savedVisit = saveVisit(visit)
        return SavedRecord(savedRecord, savedVisit)
    }

    override suspend fun listVisits(patientId: Long): List<VisitRecord> = visits
        .filter { it.patientId == patientId }
        .sortedWith(compareBy<VisitRecord>({ -it.visitDay }, { -it.visitMinute }, { -it.id }))

    override suspend fun saveVisit(visit: VisitRecord): VisitRecord {
        val id = if (visit.id > 0) visit.id else ((visits.maxOfOrNull { it.id } ?: 0) + 1)
        val saved = visit.copy(id = id)
        visits.removeAll { it.id == id }
        visits.add(saved)
        saves++
        return saved
    }

    override suspend fun deletePatient(id: Long) {
        patients.removeAll { it.id == id }
        records.removeAll { it.patientId == id }
        visits.removeAll { it.patientId == id }
    }

    override suspend fun listAppointments(day: LocalDate): List<DemoAppointment> = emptyList()

    override suspend fun countActiveAppointments(month: YearMonth): Map<LocalDate, Int> = error("Not used")

    override suspend fun saveAppointment(
        id: Long?,
        patientId: Long,
        day: LocalDate,
        time: LocalTime,
        durationMinutes: Int,
        returnOfId: Long?,
    ): AppointmentSaveResult = error("Not used in patients tests")

    override suspend fun cancelAppointment(id: Long) = error("Not used in patients tests")

    override suspend fun listUpcomingAppointments(from: LocalDate, days: Int): List<DemoAppointment> = emptyList()

    override suspend fun getProfessionalProfile(): ProfessionalProfile = ProfessionalProfile()

    override suspend fun saveProfessionalProfile(profile: ProfessionalProfile): ProfessionalProfile = profile

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

    override suspend fun exportBackup(uri: Uri, password: CharArray) = error("Not used in patients tests")

    override suspend fun restoreBackup(uri: Uri, password: CharArray) = error("Not used in patients tests")
}
