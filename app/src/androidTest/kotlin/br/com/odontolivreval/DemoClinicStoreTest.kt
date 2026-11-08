package br.com.odontolivreval

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import br.com.odontolivreval.core.demo.AppointmentSaveResult
import br.com.odontolivreval.core.demo.DemoClinicStore
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.PatientRecord
import br.com.odontolivreval.core.demo.VisitRecord
import br.com.odontolivreval.core.demo.PrescriptionFormat
import br.com.odontolivreval.feature.patients.OdontogramPdf
import java.io.File
import java.nio.file.Files
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
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
                val nextDay = day.plusDays(1)
                assertEquals(AppointmentSaveResult.SAVED, store.saveAppointment(null, 7, nextDay, time, 45))
                val source = store.listAppointments(nextDay).single { !it.cancelled }
                assertEquals(AppointmentSaveResult.TIME_OCCUPIED, store.saveAppointment(null, 7, nextDay, time.plusMinutes(44)))
                assertEquals(
                    AppointmentSaveResult.RETURN_BEFORE_SOURCE,
                    store.saveAppointment(null, 7, nextDay, time.minusHours(1), returnOfId = source.id),
                )
                assertEquals(
                    AppointmentSaveResult.SAVED,
                    store.saveAppointment(null, 7, nextDay, time.plusMinutes(45), 20, source.id),
                )
                assertEquals(
                    AppointmentSaveResult.RETURN_BEFORE_SOURCE,
                    store.saveAppointment(source.id, 7, nextDay, time, 60),
                )

                val backup = Uri.fromFile(directory.resolve("test-backup.odl"))
                val password = "senha-de-teste-longa".toCharArray()
                store.exportBackup(backup, password)
                store.savePatient(7, "Alteração depois do backup")
                val wrongPassword = runCatching { store.restoreBackup(backup, "senha-incorreta".toCharArray()) }
                assertTrue(wrongPassword.exceptionOrNull() is ClinicStorageException)
                assertEquals("Alteração depois do backup", store.listPatients().single().name)
                store.restoreBackup(backup, password)
                assertEquals("Paciente fictício revisado", store.listPatients().single().name)
                assertEquals(20, store.listAppointments(nextDay).single { it.returnOfId == source.id }.durationMinutes)
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun exportsOdontogramAndRestoresPatientAttachments() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(app.cacheDir.toPath(), "clinic-attachments-").toFile()
        val isolatedContext = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = directory
        }
        try {
            val store = DemoClinicStore(isolatedContext)
            val patient = store.savePatient(null, "Paciente fictício")
            val image = directory.resolve("exemplo.png")
            val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
            image.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            store.addRadiograph(patient.id, Uri.fromFile(image), fromCamera = false)
            assertEquals(16, store.previewRadiograph(patient.id, store.listRadiographs(patient.id).single().id)!!.width)

            val marks = mapOf("11" to "E", "11:M" to "C", "12" to "R", "55:O" to "R")
            val visit = store.saveVisit(VisitRecord(0, patient.id, 20_000, 600, odontogram = marks))
            assertEquals(marks, DemoClinicStore(isolatedContext).listVisits(patient.id).single().odontogram)
            val pdf = directory.resolve("odontograma.pdf")
            pdf.outputStream().use { OdontogramPdf.write(app, patient.name, visit, it) }
            PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                assertEquals(2, renderer.pageCount)
                renderer.openPage(0).use { page -> assertEquals(595, page.width) }
            }
            store.addPrescription(patient.id, Uri.fromFile(pdf))
            val recipe = store.listPrescriptions(patient.id).single()
            assertEquals(PrescriptionFormat.PDF, recipe.format)

            val backupFile = directory.resolve("anexos.odl")
            val backup = Uri.fromFile(backupFile)
            val password = "senha-de-teste-longa".toCharArray()
            store.exportBackup(backup, password)
            store.removePrescription(patient.id, recipe.id)
            store.removeRadiograph(patient.id, store.listRadiographs(patient.id).single().id)
            val truncated = directory.resolve("incompleto.odl")
            truncated.writeBytes(backupFile.readBytes().dropLast(1).toByteArray())
            val failed = runCatching { store.restoreBackup(Uri.fromFile(truncated), password) }
            assertTrue(failed.exceptionOrNull() is ClinicStorageException)
            assertTrue(store.listRadiographs(patient.id).isEmpty())
            assertTrue(store.listPrescriptions(patient.id).isEmpty())
            store.restoreBackup(backup, password)
            assertEquals(marks, store.listVisits(patient.id).single().odontogram)
            assertEquals(1, store.listRadiographs(patient.id).size)
            val copy = directory.resolve("copia.pdf")
            store.copyPrescription(patient.id, recipe.id, Uri.fromFile(copy))
            assertTrue(copy.readBytes().contentEquals(pdf.readBytes()))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun migratesAnEmbeddedPrescriptionFromThePreviousOnDeviceFormat() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(app.cacheDir.toPath(), "legacy-attachments-").toFile()
        val isolatedContext = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = directory
        }
        try {
            DemoClinicStore(isolatedContext).savePatient(null, "Paciente fictício")
            val bytes = "%PDF-1.4".encodeToByteArray()
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val legacy = (
                """{"version":4,"patients":[[1,"Paciente fictício"]],"appointments":[],""" +
                    """"radiographs":[],"prescriptions":[[1,1,"Receita.pdf","PDF","$encoded"]]}"""
                ).encodeToByteArray()
            val key = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .getKey("odontolivreval.demo.snapshot.v1", null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val header = "ODL-LOC1".encodeToByteArray()
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(header)
            directory.resolve("demo-clinic.enc").writeBytes(header + cipher.iv + cipher.doFinal(legacy))

            val store = DemoClinicStore(isolatedContext)
            val attachment = store.listPrescriptions(1).single()
            assertEquals("Receita.pdf", attachment.name)
            assertEquals(1, directory.resolve("clinic-attachments").listFiles()?.size)
            val output = directory.resolve("copia.pdf")
            store.copyPrescription(1, attachment.id, Uri.fromFile(output))
            assertTrue(bytes.contentEquals(output.readBytes()))
        } finally {
            directory.deleteRecursively()
        }
    }
}
