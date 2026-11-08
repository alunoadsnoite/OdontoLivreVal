package br.com.odontolivreval

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import br.com.odontolivreval.core.demo.AppointmentSaveResult
import br.com.odontolivreval.core.demo.DemoClinicStore
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.VisitRecord
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoClinicStoreTest {
    @Test
    fun assignsUniqueRecordAndVisitIdsAndKeepsAllEvolutions() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(app.cacheDir.toPath(), "clinic-record-test-").toFile()
        val isolatedContext = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = directory
        }
        try {
            val store = DemoClinicStore(isolatedContext)
            val first = store.savePatient(null, "Paciente Um")
            val second = store.savePatient(null, "Paciente Dois")
            val firstRecord = store.saveRecordAndVisit(
                PatientRecord(id = 0, patientId = first.id, phone = "(11) 91234-5678"),
                VisitRecord(id = 0, patientId = first.id, visitDay = 20_000, visitMinute = 600, diagnosis = "A"),
            )
            val secondRecord = store.saveRecordAndVisit(
                PatientRecord(id = 0, patientId = second.id),
                VisitRecord(id = 0, patientId = second.id, visitDay = 20_001, visitMinute = 700, diagnosis = "B"),
            )
            assertTrue(firstRecord.record.id != secondRecord.record.id)
            assertTrue(firstRecord.visit.id != secondRecord.visit.id)

            val secondVisit = store.saveRecordAndVisit(
                store.getPatientRecord(first.id)!!,
                VisitRecord(id = 0, patientId = first.id, visitDay = 20_002, visitMinute = 800, diagnosis = "C"),
            )
            assertEquals(secondVisit.visit.id, store.listVisits(first.id).first().id)
            assertEquals(2, store.listVisits(first.id).size)
            assertEquals(1, store.listVisits(second.id).size)
            assertEquals("(11) 91234-5678", store.getPatientRecord(first.id)!!.phone)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun upgradesPatientDatabaseAndPersistsAppointmentChanges() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(app.cacheDir.toPath(), "clinic-test-").toFile()
        val isolatedContext = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = directory
        }
        val day = LocalDate.of(2026, 11, 9)
        val time = LocalTime.of(9, 30)
        try {
            SQLiteDatabase.openOrCreateDatabase(directory.resolve("demo-patients.db"), null).use { db ->
                db.execSQL("CREATE TABLE patients (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
                db.execSQL("INSERT INTO patients (id, name) VALUES (7, 'Paciente fictício')")
                db.execSQL(
                    "CREATE TABLE appointments (id INTEGER PRIMARY KEY, patient_id INTEGER, day INTEGER, minute INTEGER, cancelled INTEGER)",
                )
                db.execSQL("INSERT INTO appointments VALUES (4, 7, ${day.minusDays(1).toEpochDay()}, 480, 0)")
                db.version = 2
            }
            DemoClinicStore(isolatedContext).let { store ->
                assertEquals(7L, store.listPatients().single().id)
                assertEquals(LocalTime.of(8, 0), store.listAppointments(day.minusDays(1)).single().time)
                assertEquals(AppointmentSaveResult.SAVED, store.saveAppointment(null, 7, day, time))
                assertEquals(AppointmentSaveResult.TIME_OCCUPIED, store.saveAppointment(null, 7, day, time))
                val appointment = store.listAppointments(day).single()
                store.savePatient(7, "Paciente fictício revisado")
                assertEquals(AppointmentSaveResult.SAVED, store.saveAppointment(appointment.id, 7, day.plusDays(1), time))
                assertTrue(store.listAppointments(day).isEmpty())
            }
            assertTrue(!directory.resolve("demo-patients.db").exists())
            assertTrue(directory.resolve("demo-clinic.enc").isFile)
            DemoClinicStore(isolatedContext).let { store ->
                val appointment = store.listAppointments(day.plusDays(1)).single()
                assertEquals("Paciente fictício revisado", appointment.patientName)
                assertEquals(time, appointment.time)
                store.cancelAppointment(appointment.id)
                assertTrue(store.listAppointments(day.plusDays(1)).single().cancelled)
                assertEquals(AppointmentSaveResult.SAVED, store.saveAppointment(null, 7, day.plusDays(1), time))

                val backup = Uri.fromFile(directory.resolve("test-backup.odl"))
                val password = "senha-de-teste-longa".toCharArray()
                store.exportBackup(backup, password)
                store.savePatient(7, "Alteração depois do backup")
                val wrongPassword = runCatching { store.restoreBackup(backup, "senha-incorreta".toCharArray()) }
                assertTrue(wrongPassword.exceptionOrNull() is ClinicStorageException)
                assertEquals("Alteração depois do backup", store.listPatients().single().name)
                store.restoreBackup(backup, password)
                assertEquals("Paciente fictício revisado", store.listPatients().single().name)
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
