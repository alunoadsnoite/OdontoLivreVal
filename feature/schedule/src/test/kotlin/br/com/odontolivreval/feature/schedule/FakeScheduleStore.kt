package br.com.odontolivreval.feature.schedule

import android.graphics.Bitmap
import android.net.Uri
import br.com.odontolivreval.core.demo.AppointmentSaveResult
import br.com.odontolivreval.core.demo.CertificateStatus
import br.com.odontolivreval.core.demo.CertificateType
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoAppointment
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.MedicalCertificate
import br.com.odontolivreval.core.demo.MedicalCertificateFile
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
    val medicalCertificates = mutableListOf<MedicalCertificateFile>()
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

    override suspend fun listPrescriptions(patientId: Long, archivedDocuments: Boolean): List<PrescriptionFile> = error("Not used")

    override suspend fun addPrescription(patientId: Long, uri: Uri, archivedDocument: Boolean) = error("Not used")

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

    override suspend fun uploadProfessionalLogo(uri: Uri) = error("Not used")

    override suspend fun previewProfessionalLogo(): Bitmap? = error("Not used")

    override suspend fun removeProfessionalLogo() = error("Not used")

    override suspend fun exportBackup(uri: Uri, password: CharArray) = error("Not used")

    override suspend fun restoreBackup(uri: Uri, password: CharArray) = error("Not used")

    // --- Atestados e declarações (fake para testes) ---

    override suspend fun listMedicalCertificates(patientId: Long): List<MedicalCertificateFile> =
        medicalCertificates.filter { it.patientId == patientId }

    override suspend fun getMedicalCertificate(patientId: Long, id: Long): MedicalCertificate? =
        medicalCertificates.find { it.patientId == patientId && it.id == id }
            ?.let { file ->
                MedicalCertificate(
                    id = file.id,
                    patientId = file.patientId,
                    type = file.type,
                    status = file.status,
                    title = file.title,
                    content = "",
                    serviceDate = file.serviceDate,
                    leaveDays = file.leaveDays,
                    version = file.version,
                    originalId = file.originalId,
                    placeOfIssue = "",
                    issueDate = file.serviceDate,
                )
            }

    override suspend fun saveMedicalCertificate(patientId: Long, certificate: MedicalCertificate): MedicalCertificate {
        val existing = medicalCertificates.find { it.patientId == patientId && it.id == certificate.id }
        val savedFile = if (existing != null) {
            val updated = medicalCertificates.map {
                if (it.patientId == patientId && it.id == certificate.id) {
                    it.copy(
                        type = certificate.type,
                        status = certificate.status,
                        title = certificate.title,
                        serviceDate = certificate.serviceDate,
                        leaveDays = certificate.leaveDays,
                        version = certificate.version,
                        originalId = certificate.originalId,
                    )
                } else it
            }
            medicalCertificates.clear()
            medicalCertificates.addAll(updated)
            existing
        } else {
            val newId = (medicalCertificates.maxOfOrNull { it.id } ?: 0) + 1
            val file = MedicalCertificateFile(
                id = newId,
                patientId = patientId,
                type = certificate.type,
                status = certificate.status,
                title = certificate.title,
                serviceDate = certificate.serviceDate,
                leaveDays = certificate.leaveDays,
                version = certificate.version,
                originalId = certificate.originalId,
            )
            medicalCertificates.add(file)
            file
        }
        return MedicalCertificate(
            id = savedFile.id,
            patientId = patientId,
            type = savedFile.type,
            status = savedFile.status,
            title = savedFile.title,
            content = certificate.content,
            serviceDate = savedFile.serviceDate,
            leaveDays = savedFile.leaveDays,
            version = savedFile.version,
            originalId = savedFile.originalId,
            placeOfIssue = certificate.placeOfIssue,
            issueDate = savedFile.serviceDate,
        )
    }

    override suspend fun cancelMedicalCertificate(patientId: Long, id: Long, reason: String) {
        val idx = medicalCertificates.indexOfFirst { it.patientId == patientId && it.id == id }
        if (idx >= 0) {
            medicalCertificates[idx] = medicalCertificates[idx].copy(status = CertificateStatus.CANCELLED)
        }
    }

    override suspend fun createMedicalCertificateVersion(patientId: Long, id: Long): MedicalCertificate {
        val existing = medicalCertificates.find { it.patientId == patientId && it.id == id }
            ?: throw IllegalStateException("Certificate not found")
        val newId = (medicalCertificates.maxOfOrNull { it.id } ?: 0) + 1
        val newFile = MedicalCertificateFile(
            id = newId,
            patientId = patientId,
            type = existing.type,
            status = CertificateStatus.DRAFT,
            title = existing.title,
            serviceDate = existing.serviceDate,
            leaveDays = existing.leaveDays,
            version = existing.version + 1,
            originalId = existing.originalId ?: id,
        )
        medicalCertificates.add(newFile)
        return MedicalCertificate(
            id = newId,
            patientId = patientId,
            type = newFile.type,
            status = newFile.status,
            title = newFile.title,
            content = "",
            serviceDate = newFile.serviceDate,
            leaveDays = newFile.leaveDays,
            version = newFile.version,
            originalId = newFile.originalId,
            placeOfIssue = "",
            issueDate = newFile.serviceDate,
        )
    }

    override suspend fun deleteMedicalCertificate(patientId: Long, id: Long) {
        medicalCertificates.removeAll { it.patientId == patientId && it.id == id }
    }
}
