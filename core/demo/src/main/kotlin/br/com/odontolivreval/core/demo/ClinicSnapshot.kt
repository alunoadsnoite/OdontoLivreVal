package br.com.odontolivreval.core.demo

import java.io.IOException
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

internal data class AppointmentRecord(
    val id: Long,
    val patientId: Long,
    val day: Long,
    val minute: Int,
    val cancelled: Boolean,
)

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
) {
    fun encode(): ByteArray = JSONObject()
        .put("version", currentVersion)
        .put("patients", JSONArray().apply {
            patients.forEach { put(JSONArray().put(it.id).put(it.name)) }
        })
        .put("appointments", JSONArray().apply {
            appointments.forEach {
                put(JSONArray().put(it.id).put(it.patientId).put(it.day).put(it.minute).put(it.cancelled))
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
        .toString()
        .toByteArray(Charsets.UTF_8)

    fun validate() {
        val patientIds = patients.map { it.id }.toSet()
        if (patientIds.size != patients.size ||
            patients.any { it.id <= 0 || it.name.isBlank() || it.name.length > 100 } ||
            appointments.map { it.id }.toSet().size != appointments.size ||
            appointments.any {
                it.id <= 0 || it.patientId !in patientIds || it.day !in minDay..maxDay ||
                    it.minute !in 0..1439
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
                    AppointmentRecord(it.integer(0), it.integer(1), it.integer(2), minute.toInt(), cancelled)
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
                            map[pair.getInt(0).toString()] = pair.getString(1)
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

            return ClinicSnapshot(patients, appointments, patientRecords, visits).apply { validate() }
        }

        private val minDay = LocalDate.MIN.toEpochDay()
        private val maxDay = LocalDate.MAX.toEpochDay()
        private const val currentVersion = 2

        private fun JSONArray.integer(index: Int): Long = when (val value = get(index)) {
            is Int -> value.toLong()
            is Long -> value
            else -> throw IOException("Expected integer")
        }

        private fun <T> JSONArray.rows(parse: (JSONArray) -> T): List<T> =
            List(length()) { index -> parse(getJSONArray(index)) }
    }
}
