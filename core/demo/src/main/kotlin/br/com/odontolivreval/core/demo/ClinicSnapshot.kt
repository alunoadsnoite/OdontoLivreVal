package br.com.odontolivreval.core.demo

import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

internal data class AppointmentRecord(
    val id: Long,
    val patientId: Long,
    val day: Long,
    val minute: Int,
    val cancelled: Boolean,
    val durationMinutes: Int = minOf(defaultAppointmentMinutes, 1440 - minute),
    val returnOfId: Long? = null,
) {
    val endMinute: Int get() = minute + durationMinutes

    fun overlaps(start: Int, end: Int): Boolean = minute < end && start < endMinute
}

data class PatientRecord(
    val id: Long,
    val patientId: Long,
    val birthDate: String = "",
    val phone: String = "",
    val complaint: String = "",
    val history: String = "",
    val observations: String = "",
)

data class VisitRecord(
    val id: Long,
    val patientId: Long,
    val visitDay: Long,
    val visitMinute: Int,
    val complaint: String = "",
    val anamnesis: String = "",
    val diagnosis: String = "",
    val conduct: String = "",
    val observations: String = "",
    val odontogram: Map<String, String> = emptyMap(),
)

internal data class ClinicSnapshot(
    val patients: List<DemoPatient> = emptyList(),
    val appointments: List<AppointmentRecord> = emptyList(),
    val patientRecords: List<PatientRecord> = emptyList(),
    val visits: List<VisitRecord> = emptyList(),
    val radiographs: List<StoredRadiograph> = emptyList(),
    val prescriptions: List<StoredPrescription> = emptyList(),
    val documentPhotos: List<StoredDocumentPhoto> = emptyList(),
    val digitalSignatures: List<StoredDigitalSignature> = emptyList(),
    val medicalCertificates: List<StoredMedicalCertificate> = emptyList(),
    val treatmentPlans: List<StoredTreatmentPlan> = emptyList(),
    val treatmentProcedures: List<StoredTreatmentProcedure> = emptyList(),
    val treatmentBudgets: List<StoredTreatmentBudget> = emptyList(),
    val informedConsents: List<StoredInformedConsent> = emptyList(),
    val professional: ProfessionalProfile = ProfessionalProfile(),
) {
    fun encode(): ByteArray = JSONObject()
        .put("version", currentVersion)
        .put("professional", JSONObject()
            .put("name", professional.name)
            .put("cro", professional.cro)
            .put("croUF", professional.croUF)
            .put("cpf", professional.cpf ?: JSONObject.NULL)
            .put("specialty", professional.specialty)
            .put("clinicName", professional.clinicName)
            .put("address", professional.address)
            .put("phone", professional.phone)
            .put("email", professional.email ?: JSONObject.NULL)
            .put("logoFileId", professional.logoFileId ?: JSONObject.NULL))
        .put("patients", JSONArray().apply {
            patients.forEach { put(JSONArray().put(it.id).put(it.name)) }
        })
        .put("appointments", JSONArray().apply {
            appointments.forEach {
                put(JSONArray().put(it.id).put(it.patientId).put(it.day).put(it.minute)
                    .put(it.cancelled).put(it.durationMinutes).put(it.returnOfId ?: JSONObject.NULL))
            }
        })
        .put("patientRecords", JSONArray().apply {
            patientRecords.forEach {
                put(JSONArray().put(it.id).put(it.patientId).put(it.birthDate).put(it.phone)
                    .put(it.complaint).put(it.history).put(it.observations))
            }
        })
        .put("visits", JSONArray().apply {
            visits.forEach {
                put(JSONArray().put(it.id).put(it.patientId).put(it.visitDay).put(it.visitMinute)
                    .put(it.complaint).put(it.anamnesis).put(it.diagnosis).put(it.conduct).put(it.observations)
                    .put(JSONArray(it.odontogram.entries.toList().sortedBy { it.key }.map { pair -> JSONArray().put(pair.key).put(pair.value) })))
            }
        })
        .put("radiographs", JSONArray().apply {
            radiographs.forEach {
                put(JSONArray().put(it.file.id).put(it.file.patientId).putReference(it.content))
            }
        })
        .put("prescriptions", JSONArray().apply {
            prescriptions.forEach {
                put(JSONArray().put(it.file.id).put(it.file.patientId).put(it.file.name)
                    .put(it.file.format.name).putReference(it.content).put(it.file.archivedDocument))
            }
        })
        .put("documentPhotos", JSONArray().apply {
            documentPhotos.forEach {
                put(JSONArray().put(it.file.id).put(it.file.patientId).putReference(it.content))
            }
        })
        .put("digitalSignatures", JSONArray().apply {
            digitalSignatures.forEach {
                put(JSONArray().put(it.file.id).put(it.file.patientId ?: JSONObject.NULL)
                    .put(it.file.documentType ?: JSONObject.NULL)
                    .put(it.file.createdAt)
                    .putReference(it.content))
            }
        })
        .put("medicalCertificates", JSONArray().apply {
            medicalCertificates.forEach {
                put(JSONArray().put(it.file.id).put(it.file.patientId)
                    .put(it.file.type.name).put(it.file.status.name)
                    .put(it.file.title).put(it.file.serviceDate.toEpochDay())
                    .put(it.file.leaveDays).put(it.file.version)
                    .put(it.file.originalId ?: JSONObject.NULL)
                    .put(it.file.createdAt)
                    .put(it.file.cancelledReason ?: JSONObject.NULL)
                    .put(it.file.cancelledAt ?: JSONObject.NULL)
                    .putReference(it.content))
            }
        })
        .put("treatmentPlans", JSONArray().apply {
            treatmentPlans.forEach {
                put(JSONArray().put(it.file.id).put(it.file.patientId)
                    .put(it.file.title).put(it.file.description)
                    .put(it.file.estimatedSessions).put(it.file.status)
                    .put(it.file.createdAt).put(it.file.updatedAt)
                    .putReference(it.content))
            }
        })
        .put("treatmentProcedures", JSONArray().apply {
            treatmentProcedures.forEach {
                put(JSONArray().put(it.file.id).put(it.file.planId)
                    .put(it.file.description).put(it.file.tooth ?: JSONObject.NULL)
                    .put(it.file.surface ?: JSONObject.NULL)
                    .put(it.file.quantity).put(it.file.unitValue.toPlainString())
                    .put(it.file.totalValue.toPlainString())
                    .put(it.file.treatmentStage).put(it.file.status.name)
                    .putReference(it.content))
            }
        })
        .put("treatmentBudgets", JSONArray().apply {
            treatmentBudgets.forEach {
                put(JSONArray().put(it.file.id).put(it.file.patientId).put(it.file.planId)
                    .put(it.file.budgetNumber).put(it.file.status.name)
                    .put(it.file.revision).put(it.file.originalId ?: JSONObject.NULL)
                    .put(it.file.issueDate.toEpochDay()).put(it.file.validityDays)
                    .put(it.file.createdAt).put(it.file.updatedAt)
                    .putReference(it.content))
            }
        })
        .put("informedConsents", JSONArray().apply {
            informedConsents.forEach {
                put(JSONArray().put(it.file.id).put(it.file.patientId)
                    .put(it.file.type.name).put(it.file.status.name)
                    .put(it.file.title).put(it.file.procedure)
                    .put(it.file.version).put(it.file.originalId ?: JSONObject.NULL)
                    .put(it.file.planId ?: JSONObject.NULL)
                    .put(it.file.procedureId ?: JSONObject.NULL)
                    .put(it.file.createdAt)
                    .put(it.file.revokedReason ?: JSONObject.NULL)
                    .put(it.file.revokedAt ?: JSONObject.NULL)
                    .putReference(it.content))
            }
        })
        .toString()
        .toByteArray(Charsets.UTF_8)

    fun attachments(): List<AttachmentContent> =
        radiographs.map { it.content } + prescriptions.map { it.content } +
            documentPhotos.map { it.content } + digitalSignatures.map { it.content } +
            medicalCertificates.map { it.content } +
            treatmentPlans.map { it.content } +
            treatmentProcedures.map { it.content } +
            treatmentBudgets.map { it.content } +
            informedConsents.map { it.content }

    fun mapAttachments(transform: (AttachmentContent) -> AttachmentContent): ClinicSnapshot = copy(
        radiographs = radiographs.map { it.copy(content = transform(it.content)) },
        prescriptions = prescriptions.map { it.copy(content = transform(it.content)) },
        documentPhotos = documentPhotos.map { it.copy(content = transform(it.content)) },
        digitalSignatures = digitalSignatures.map { it.copy(content = transform(it.content)) },
        medicalCertificates = medicalCertificates.map { it.copy(content = transform(it.content)) },
        treatmentPlans = treatmentPlans.map { it.copy(content = transform(it.content)) },
        treatmentProcedures = treatmentProcedures.map { it.copy(content = transform(it.content)) },
        treatmentBudgets = treatmentBudgets.map { it.copy(content = transform(it.content)) },
        informedConsents = informedConsents.map { it.copy(content = transform(it.content)) },
    )

    private fun JSONArray.putReference(content: AttachmentContent): JSONArray {
        val reference = content as? AttachmentRef ?: throw IOException("Embedded attachment must be migrated")
        return put(reference.token).put(reference.size)
    }

    fun validate() {
        val patientIds = patients.map { it.id }.toSet()
        val appointmentOwners = appointments.associate { it.id to it.patientId }
        val contents = attachments()
        val references = contents.filterIsInstance<AttachmentRef>()
        if (patientIds.size != patients.size ||
            patients.any { it.id <= 0 || it.name.isBlank() || it.name.length > 100 } ||
            appointments.map { it.id }.toSet().size != appointments.size ||
            appointments.any {
                it.id <= 0 || it.patientId !in patientIds || it.day !in minDay..maxDay ||
                    it.minute !in 0..1439 || it.durationMinutes !in 1..maxAppointmentMinutes ||
                    it.endMinute > 1440 || it.returnOfId?.let { parent ->
                        parent >= it.id || appointmentOwners[parent] != it.patientId
                    } == true
            } ||
            appointments.filterNot { it.cancelled }.distinctBy { it.day to it.minute }.size !=
            appointments.count { !it.cancelled } ||
            patientRecords.map { it.id }.toSet().size != patientRecords.size ||
            patientRecords.any {
                it.id <= 0 || it.patientId !in patientIds
            } ||
            patientRecords.map { it.patientId }.toSet().size != patientRecords.size ||
            visits.map { it.id }.toSet().size != visits.size ||
            visits.any {
                it.id <= 0 || it.patientId !in patientIds || it.visitDay !in minDay..maxDay ||
                    it.visitMinute !in 0..1439
            }
            || radiographs.map { it.file.id }.toSet().size != radiographs.size
            || radiographs.any { it.file.id <= 0 || it.file.patientId !in patientIds }
            || prescriptions.map { it.file.id }.toSet().size != prescriptions.size
            || prescriptions.any {
                it.file.id <= 0 || it.file.patientId !in patientIds ||
                    it.file.name.length !in 1..120 || (it.file.archivedDocument && it.file.format != PrescriptionFormat.PDF)
            }
            || documentPhotos.map { it.file.id }.toSet().size != documentPhotos.size
            || documentPhotos.any { it.file.id <= 0 || it.file.patientId !in patientIds }
            || digitalSignatures.map { it.file.id }.toSet().size != digitalSignatures.size
            || digitalSignatures.any {
                it.file.id <= 0 ||
                    (it.file.patientId != null && it.file.patientId !in patientIds)
            }
            || medicalCertificates.map { it.file.id }.toSet().size != medicalCertificates.size
            || medicalCertificates.any { it.file.id <= 0 || it.file.patientId !in patientIds }
            || medicalCertificates.any { it.file.title.length !in 1..120 }
            || medicalCertificates.any { !it.content.valid }
            || treatmentPlans.map { it.file.id }.toSet().size != treatmentPlans.size
            || treatmentPlans.any { it.file.id <= 0 || it.file.patientId !in patientIds }
            || treatmentPlans.any { it.file.title.length !in 1..120 }
            || treatmentPlans.any { !it.content.valid }
            || treatmentProcedures.map { it.file.id }.toSet().size != treatmentProcedures.size
            || treatmentProcedures.any { it.file.id <= 0 || it.file.planId <= 0 }
            || treatmentProcedures.any { it.file.description.isBlank() }
            || treatmentProcedures.any { it.file.quantity < 1 }
            || treatmentProcedures.any { !it.content.valid }
            || treatmentBudgets.map { it.file.id }.toSet().size != treatmentBudgets.size
            || treatmentBudgets.any { it.file.id <= 0 || it.file.patientId !in patientIds }
            || treatmentBudgets.any { !it.content.valid }
            || informedConsents.map { it.file.id }.toSet().size != informedConsents.size
            || informedConsents.any { it.file.id <= 0 || it.file.patientId !in patientIds }
            || informedConsents.any { it.file.title.length !in 1..120 }
            || informedConsents.any { !it.content.valid }
            || contents.any { !it.valid }
            || references.map { it.token }.toSet().size != references.size
            || references.sumOf { it.size.toLong() } > maxAttachmentTotal
            || professional.name.length > ProfessionalProfile.MAX_NAME
            || professional.cro.length > ProfessionalProfile.MAX_CRO
            || professional.croUF.length > ProfessionalProfile.MAX_CRO_UF
            || professional.cpf.length > ProfessionalProfile.MAX_CPF
            || professional.specialty.length > ProfessionalProfile.MAX_SPECIALTY
            || professional.clinicName.length > ProfessionalProfile.MAX_CLINIC
            || professional.address.length > ProfessionalProfile.MAX_ADDRESS
            || professional.phone.length > ProfessionalProfile.MAX_PHONE
            || professional.email.length > ProfessionalProfile.MAX_EMAIL
        ) throw IOException("Invalid clinic data")
    }

    companion object {
        fun decode(bytes: ByteArray): ClinicSnapshot {
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            val version = root.optInt("version", 1)
            if (version !in 1..currentVersion) throw IOException("Unsupported clinic data version")

            val patients = root.getJSONArray("patients").rows {
                DemoPatient(it.integer(0), it.get(1) as? String ?: throw IOException("Invalid patient name"))
            }

            val appointments = if (version >= 2 || root.has("appointments")) {
                root.getJSONArray("appointments").rows {
                    val minute = it.integer(3)
                    if (minute !in 0L..1439L) throw IOException("Invalid appointment time")
                    val cancelled = it.get(4) as? Boolean ?: throw IOException("Invalid appointment status")
                    val base = AppointmentRecord(it.integer(0), it.integer(1), it.integer(2), minute.toInt(), cancelled)
                    if (version < 6) base else {
                        val duration = it.integer(5)
                        if (duration !in 1L..maxAppointmentMinutes.toLong()) throw IOException("Invalid appointment duration")
                        base.copy(
                            durationMinutes = duration.toInt(),
                            returnOfId = if (it.isNull(6)) null else it.integer(6),
                        )
                    }
                }
            } else {
                emptyList()
            }

            val patientRecords = if (version >= 2 && root.has("patientRecords")) {
                root.getJSONArray("patientRecords").rows {
                    PatientRecord(
                        it.integer(0), it.integer(1),
                        it.get(2) as? String ?: "",
                        it.get(3) as? String ?: "",
                        it.get(4) as? String ?: "",
                        it.get(5) as? String ?: "",
                        it.get(6) as? String ?: "",
                    )
                }
            } else {
                emptyList()
            }

            val visits = if (version >= 2 && root.has("visits")) {
                root.getJSONArray("visits").rows {
                    val odontogram = if (version >= 2 && it.length() > 9 && it.get(9) is JSONArray) {
                        val arr = it.getJSONArray(9)
                        val map = mutableMapOf<String, String>()
                        for (i in 0 until arr.length()) {
                            val pair = arr.getJSONArray(i)
                            val key = if (version >= 4) pair.getString(0) else pair.getInt(0).toString()
                            map[key] = pair.getString(1)
                        }
                        map.toMap()
                    } else {
                        emptyMap()
                    }
                    VisitRecord(
                        it.integer(0), it.integer(1), it.integer(2), it.integer(3).toInt(),
                        it.get(4) as? String ?: "",
                        it.get(5) as? String ?: "",
                        it.get(6) as? String ?: "",
                        it.get(7) as? String ?: "",
                        it.get(8) as? String ?: "",
                        odontogram,
                    )
                }
            } else {
                emptyList()
            }

            val radiographs = if (version >= 3) {
                root.getJSONArray("radiographs").rows {
                    StoredRadiograph(Radiograph(it.integer(0), it.integer(1)), it.attachment(version, 2))
                }
            } else emptyList()

            val prescriptions = if (version >= 3 && root.has("prescriptions")) {
                root.getJSONArray("prescriptions").rows {
                    val format = PrescriptionFormat.entries.firstOrNull { format -> format.name == it.getString(3) }
                        ?: throw IOException("Invalid prescription format")
                    val archived = version >= 10 && it.getBoolean(6)
                    StoredPrescription(
                        PrescriptionFile(it.integer(0), it.integer(1), it.getString(2), format, archived),
                        it.attachment(version, 4),
                    )
                }
            } else emptyList()

            val documentPhotos = if (version >= 8 && root.has("documentPhotos")) {
                root.getJSONArray("documentPhotos").rows {
                    StoredDocumentPhoto(DocumentPhoto(it.integer(0), it.integer(1)), it.attachment(version, 2))
                }
            } else emptyList()

            val digitalSignatures = if (version >= 9 && root.has("digitalSignatures")) {
                root.getJSONArray("digitalSignatures").rows {
                    val patientId = if (it.isNull(2)) null else it.integer(2)
                    val documentType = if (it.isNull(3)) null else it.getString(3)
                    val createdAt = it.optLong(4, System.currentTimeMillis())
                    StoredDigitalSignature(
                        DigitalSignature(it.integer(0), patientId, documentType, createdAt),
                        it.attachment(version, 5),
                    )
                }
            } else emptyList()

            val medicalCertificates = if (version >= 12 && root.has("medicalCertificates")) {
                root.getJSONArray("medicalCertificates").rows {
                    val type = CertificateType.valueOf(it.getString(2))
                    val status = CertificateStatus.valueOf(it.getString(3))
                    val serviceDate = LocalDate.ofEpochDay(it.integer(4))
                    val cancelledReason = if (it.isNull(10)) null else it.getString(10)
                    val cancelledAt = if (it.isNull(11)) null else LocalDate.ofEpochDay(it.integer(11))
                    StoredMedicalCertificate(
                        MedicalCertificateFile(
                            id = it.integer(0),
                            patientId = it.integer(1),
                            type = type,
                            status = status,
                            title = it.getString(4),
                            serviceDate = serviceDate,
                            leaveDays = it.integer(5).toInt(),
                            version = it.integer(6).toInt(),
                            originalId = if (it.isNull(7)) null else it.integer(7),
                            createdAt = it.optLong(8, System.currentTimeMillis()),
                            cancelledReason = cancelledReason,
                            cancelledAt = cancelledAt,
                        ),
                        it.attachment(version, 9),
                    )
                }
            } else emptyList()

            val treatmentPlans = if (version >= 13 && root.has("treatmentPlans")) {
                root.getJSONArray("treatmentPlans").rows {
                    StoredTreatmentPlan(
                        TreatmentPlanFile(
                            id = it.integer(0),
                            patientId = it.integer(1),
                            title = it.getString(2),
                            description = it.getString(3),
                            estimatedSessions = it.integer(4).toInt(),
                            status = it.getString(5),
                            createdAt = it.optLong(6, System.currentTimeMillis()),
                            updatedAt = it.optLong(7, System.currentTimeMillis()),
                        ),
                        it.attachment(version, 8),
                    )
                }
            } else emptyList()

            val treatmentProcedures = if (version >= 13 && root.has("treatmentProcedures")) {
                root.getJSONArray("treatmentProcedures").rows {
                    StoredTreatmentProcedure(
                        TreatmentProcedureFile(
                            id = it.integer(0),
                            planId = it.integer(1),
                            description = it.getString(2),
                            tooth = if (it.isNull(3)) null else it.getString(3),
                            surface = if (it.isNull(4)) null else it.getString(4),
                            quantity = it.integer(5).toInt(),
                            unitValue = BigDecimal(it.getString(6)),
                            totalValue = BigDecimal(it.getString(7)),
                            treatmentStage = it.integer(8).toInt(),
                            status = ProcedureStatus.valueOf(it.getString(9)),
                        ),
                        it.attachment(version, 10),
                    )
                }
            } else emptyList()

            val treatmentBudgets = if (version >= 13 && root.has("treatmentBudgets")) {
                root.getJSONArray("treatmentBudgets").rows {
                    StoredTreatmentBudget(
                        TreatmentBudgetFile(
                            id = it.integer(0),
                            patientId = it.integer(1),
                            planId = it.integer(2),
                            budgetNumber = it.getString(3),
                            status = BudgetStatus.valueOf(it.getString(4)),
                            revision = it.integer(5).toInt(),
                            originalId = if (it.isNull(6)) null else it.integer(6),
                            issueDate = LocalDate.ofEpochDay(it.integer(7)),
                            validityDays = it.integer(8).toInt(),
                            createdAt = it.optLong(9, System.currentTimeMillis()),
                            updatedAt = it.optLong(10, System.currentTimeMillis()),
                        ),
                        it.attachment(version, 11),
                    )
                }
            } else emptyList()

            val informedConsents = if (version >= 14 && root.has("informedConsents")) {
                root.getJSONArray("informedConsents").rows {
                    StoredInformedConsent(
                        InformedConsentFile(
                            id = it.integer(0),
                            patientId = it.integer(1),
                            type = ConsentType.valueOf(it.getString(2)),
                            status = ConsentStatus.valueOf(it.getString(3)),
                            title = it.getString(4),
                            procedure = it.getString(5),
                            version = it.integer(6).toInt(),
                            originalId = if (it.isNull(7)) null else it.integer(7),
                            planId = if (it.isNull(8)) null else it.integer(8),
                            procedureId = if (it.isNull(9)) null else it.integer(9),
                            issueDate = if (it.isNull(10)) LocalDate.now() else LocalDate.ofEpochDay(it.integer(10)),
                            createdAt = it.optLong(11, System.currentTimeMillis()),
                            revokedReason = if (it.isNull(12)) null else it.getString(12),
                            revokedAt = if (it.isNull(13)) null else LocalDate.ofEpochDay(it.integer(13)),
                        ),
                        it.attachment(version, 14),
                    )
                }
            } else emptyList()

            val professional = if (version >= 7 && root.has("professional")) {
                val json = root.getJSONObject("professional")
                ProfessionalProfile(
                    name = json.optString("name", ""),
                    cro = json.optString("cro", ""),
                    croUF = json.optString("croUF", ""),
                    cpf = json.optString("cpf")?.takeIf { it.isNotBlank() } ?: "",
                    specialty = json.optString("specialty", ""),
                    clinicName = json.optString("clinicName", ""),
                    address = json.optString("address", ""),
                    phone = json.optString("phone", ""),
                    email = json.optString("email")?.takeIf { it.isNotBlank() } ?: "",
                    logoFileId = if (json.isNull("logoFileId")) null else json.optLong("logoFileId", 0L),
                )
            } else {
                ProfessionalProfile()
            }

            val schedule = if (version < 6) appointments.withLegacyDurations() else appointments
            return ClinicSnapshot(
                patients, schedule, patientRecords, visits, radiographs, prescriptions,
                documentPhotos, digitalSignatures, medicalCertificates,
                treatmentPlans, treatmentProcedures, treatmentBudgets, informedConsents,
                professional,
            ).apply { validate() }
        }

        private val minDay = LocalDate.MIN.toEpochDay()
        private val maxDay = LocalDate.MAX.toEpochDay()
        private const val currentVersion = 14

        private fun JSONArray.attachment(version: Int, index: Int): AttachmentContent {
            val value = getString(index)
            if (version < 5) return EmbeddedAttachment(value)
            val size = integer(index + 1)
            if (size !in 1L..maxAttachmentBytes.toLong()) throw IOException("Invalid attachment size")
            return AttachmentRef(value, size.toInt())
        }

        private fun JSONArray.integer(index: Int): Long = when (val value = get(index)) {
            is Int -> value.toLong()
            is Long -> value
            else -> throw IOException("Expected integer")
        }

        private fun <T> JSONArray.rows(parse: (JSONArray) -> T): List<T> =
            List(length()) { index -> parse(getJSONArray(index)) }
    }
}
