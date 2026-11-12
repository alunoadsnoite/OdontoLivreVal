package br.com.odontolivreval

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.designsystem.OdontoTheme
import br.com.odontolivreval.core.demo.DocumentPhoto
import br.com.odontolivreval.core.demo.PrescriptionFile
import br.com.odontolivreval.core.demo.PrescriptionFormat
import br.com.odontolivreval.feature.patients.PatientDocumentPhotos
import br.com.odontolivreval.feature.documents.DocumentPdfRequest
import br.com.odontolivreval.feature.patients.PatientPrescriptions
import br.com.odontolivreval.feature.patients.PatientRadiographs
import br.com.odontolivreval.feature.settings.SettingsViewModel
import br.com.odontolivreval.core.demo.ClearingManager
import java.io.IOException
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private lateinit var lock: DeviceLock
    private lateinit var backupModel: BackupViewModel
    private lateinit var radiographModel: RadiographImportViewModel
    private lateinit var prescriptionModel: PrescriptionViewModel
    private lateinit var documentFileModel: DocumentFileExportViewModel
    private var pendingPrescriptionArchive = false

    private val createDocumentPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        documentFileModel.finish(applicationContext, uri)
    }
    private lateinit var documentPhotoModel: DocumentPhotoImportViewModel
    private var pendingPrescriptionPatient: Long? = null
    private var pendingPrescriptionCopy: Pair<Long, Long>? = null
    private var pendingDocumentPhotoPatient: Long? = null

    private val pickPrescription = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val patientId = pendingPrescriptionPatient
        pendingPrescriptionPatient = null
        if (uri != null && patientId != null) {
            prescriptionModel.transfer((application as OdontoApplication).demoClinic, patientId, uri, pendingPrescriptionArchive)
        }
    }

    private val savePrescriptionCopy = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val request = pendingPrescriptionCopy
        pendingPrescriptionCopy = null
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && uri != null && request != null) {
            prescriptionModel.transfer((application as OdontoApplication).demoClinic, request.first, uri, pendingPrescriptionArchive, request.second)
        }
    }

    private lateinit var pdfModel: OdontogramExportViewModel
    private var pendingPdf: OdontogramRequest? = null

    private val createOdontogramPdf = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val request = pendingPdf
        pendingPdf = null
        if (uri != null && request != null) {
            pdfModel.write(applicationContext, (application as OdontoApplication).demoClinic, request, uri)
        }
    }

    private var pendingRadiographPatient: Long? = null
    private val captureFile get() = cacheDir.resolve("radiograph-capture/pending.jpg")

    private val pickRadiograph = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        finishRadiograph(uri)
    }

    private val takeRadiograph = registerForActivityResult(object : ActivityResultContracts.TakePicture() {
        override fun createIntent(context: Context, input: Uri): Intent =
            super.createIntent(context, input).apply {
                clipData = ClipData.newRawUri("radiograph", input)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
    }) { saved ->
        val file = captureFile
        finishRadiograph(if (saved) Uri.fromFile(file) else null, file)
    }

    private val captureDocumentFile get() = cacheDir.resolve("document-capture/pending.jpg")

    private val pickDocumentPhoto = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        finishDocumentPhoto(uri)
    }

    private val takeDocumentPhoto = registerForActivityResult(object : ActivityResultContracts.TakePicture() {
        override fun createIntent(context: Context, input: Uri): Intent =
            super.createIntent(context, input).apply {
                clipData = ClipData.newRawUri("document_photo", input)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
    }) { saved ->
        val file = captureDocumentFile
        finishDocumentPhoto(if (saved) Uri.fromFile(file) else null, file)
    }

    private val exportBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        backupModel.select(BackupAction.EXPORT, it)
    }

    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        backupModel.select(BackupAction.RESTORE, it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lock = DeviceLock(this)
        val models = ViewModelProvider(this)
        backupModel = models[BackupViewModel::class.java]
        radiographModel = models[RadiographImportViewModel::class.java]
        pdfModel = models[OdontogramExportViewModel::class.java]
        prescriptionModel = models[PrescriptionViewModel::class.java]
        documentFileModel = models[DocumentFileExportViewModel::class.java]
        pendingPrescriptionArchive = savedInstanceState?.getBoolean("prescriptionArchive") == true
        documentPhotoModel = models[DocumentPhotoImportViewModel::class.java]
        pendingPrescriptionPatient = savedInstanceState?.getLong("prescriptionPatient")?.takeIf { it > 0 }
        savedInstanceState?.getLongArray("prescriptionCopy")?.let { pendingPrescriptionCopy = it[0] to it[1] }
        savedInstanceState?.let {
            val patient = it.getLong("pdfPatient")
            val visit = it.getLong("pdfVisit")
            if (patient > 0 && visit > 0) pendingPdf = OdontogramRequest(patient, visit)
        }
        pendingRadiographPatient = savedInstanceState?.getLong("radiographPatient")?.takeIf { it > 0 }
        if (pendingRadiographPatient == null && !radiographModel.state.value.busy) captureFile.delete()
        pendingDocumentPhotoPatient = savedInstanceState?.getLong("documentPhotoPatient")?.takeIf { it > 0 }
        if (pendingDocumentPhotoPatient == null && !documentPhotoModel.state.value.busy) captureDocumentFile.delete()
        applyCaptureProtection(lock.enabled)
        enableEdgeToEdge()
        val odontoApplication = application as OdontoApplication
        setContent {
            val themeModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(odontoApplication.themes))
            val state by themeModel.state.collectAsStateWithLifecycle()
            val backup by backupModel.state.collectAsStateWithLifecycle()
            val radiograph by radiographModel.state.collectAsStateWithLifecycle()
            val pdf by pdfModel.state.collectAsStateWithLifecycle()
            val prescription by prescriptionModel.state.collectAsStateWithLifecycle()
            val documentFile by documentFileModel.state.collectAsStateWithLifecycle()
            val documentPhoto by documentPhotoModel.state.collectAsStateWithLifecycle()
            val screens = rememberSaveableStateHolder()
            val protected = lock.enabled
            SideEffect { applyCaptureProtection(protected) }
            state.theme?.let { mode ->
                OdontoTheme(mode) {
                    if (lock.unlocked) {
                        screens.SaveableStateProvider("clinic") {
                            OdontoApp(
                                state,
                                themeModel::selectTheme,
                                odontoApplication.demoClinic,
                                backup.revision,
                                exportOdontogram = { patientId, visitId ->
                                    PdfExportControls(
                                        visitId = visitId,
                                        result = pdf?.takeIf { it.patientId == patientId }?.result,
                                        onExport = { launchOdontogramPdf(patientId, it) },
                                    )
                                },
                                onSaveDocument = ::launchDocumentPdf,
                                documentExportStatus = { patientId ->
                                    val result = documentFile?.takeIf { it.patientId == patientId }?.result
                                    result?.let {
                                        stringResource(when (it) {
                                            DocumentFileResult.SAVING -> R.string.document_pdf_saving
                                            DocumentFileResult.SAVED -> R.string.document_pdf_saved
                                            DocumentFileResult.FAILED -> R.string.document_pdf_failed
                                        })
                                    }
                                },
                                prescriptions = { patientId, archive ->
                                    PatientPrescriptions(
                                        store = odontoApplication.demoClinic,
                                        patientId = patientId,
                                        revision = prescription.revision,
                                        busy = prescription.busy,
                                        archivedDocuments = archive,
                                        transferFailed = prescription.patientId == patientId &&
                                            prescription.archivedDocument == archive && prescription.failed,
                                        copied = prescription.patientId == patientId &&
                                            prescription.archivedDocument == archive && prescription.copied,
                                        onImport = { launchPrescriptionImport(patientId, archive) },
                                        onCopy = ::launchPrescriptionCopy,
                                    )
                                },
                                radiographs = { patientId ->
                                    PatientRadiographs(
                                        store = odontoApplication.demoClinic,
                                        patientId = patientId,
                                        importRevision = radiograph.revision,
                                        importing = radiograph.busy,
                                        importFailed = radiograph.failedPatient == patientId,
                                        onPick = { launchRadiograph(patientId, fromCamera = false) },
                                        onCamera = { launchRadiograph(patientId, fromCamera = true) },
                                    )
                                },
                                documentPhotos = { patientId ->
                                    PatientDocumentPhotos(
                                        store = odontoApplication.demoClinic,
                                        patientId = patientId,
                                        onPick = { launchDocumentPhoto(patientId, fromCamera = false) },
                                        onCamera = { launchDocumentPhoto(patientId, fromCamera = true) },
                                    )
                                },
                            ) {
                                ClinicProtectionScreen(
                                    lock = lock,
                                    backup = backup,
                                    onExport = { exportBackup.launch("OdontoLivreVal-${LocalDate.now()}.odl") },
                                    onRestore = { importBackup.launch(arrayOf("*/*")) },
                                    onPassword = { backupModel.run(odontoApplication.demoClinic, it) },
                                    onDismiss = backupModel::dismiss,
                                )
                            }
                        }
                    } else {
                        LockedScreen(lock, onOpenSettings = { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) })
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingRadiographPatient?.let { outState.putLong("radiographPatient", it) }
        pendingPrescriptionPatient?.let { outState.putLong("prescriptionPatient", it) }
        outState.putBoolean("prescriptionArchive", pendingPrescriptionArchive)
        pendingPrescriptionCopy?.let { outState.putLongArray("prescriptionCopy", longArrayOf(it.first, it.second)) }
        pendingPdf?.let {
            outState.putLong("pdfPatient", it.patientId)
            outState.putLong("pdfVisit", it.visitId)
        }
        pendingDocumentPhotoPatient?.let { outState.putLong("documentPhotoPatient", it) }
        super.onSaveInstanceState(outState)
    }

    private fun launchDocumentPdf(request: DocumentPdfRequest) {
        if (!documentFileModel.prepare(request)) return
        try {
            createDocumentPdf.launch(request.filename)
        } catch (_: ActivityNotFoundException) {
            documentFileModel.pickerFailed()
        }
    }

    private fun launchPrescriptionImport(patientId: Long, archive: Boolean) {
        if (pendingPrescriptionPatient != null || pendingPrescriptionCopy != null || prescriptionModel.state.value.busy) return
        pendingPrescriptionPatient = patientId
        pendingPrescriptionArchive = archive
        try {
            val formats = if (archive) listOf(PrescriptionFormat.PDF) else PrescriptionFormat.entries
            pickPrescription.launch(formats.map { it.mimeType }.toTypedArray())
        } catch (_: ActivityNotFoundException) {
            pendingPrescriptionPatient = null
            prescriptionModel.fail(patientId, archive)
        }
    }

    private fun launchPrescriptionCopy(file: PrescriptionFile) {
        if (pendingPrescriptionPatient != null || pendingPrescriptionCopy != null || prescriptionModel.state.value.busy) return
        pendingPrescriptionCopy = file.patientId to file.id
        pendingPrescriptionArchive = file.archivedDocument
        try {
            savePrescriptionCopy.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = file.format.mimeType
                putExtra(Intent.EXTRA_TITLE, file.name)
            })
        } catch (_: ActivityNotFoundException) {
            pendingPrescriptionCopy = null
            prescriptionModel.fail(file.patientId, file.archivedDocument)
        }
    }

    private fun launchOdontogramPdf(patientId: Long, visitId: Long) {
        if (pendingPdf != null || pdfModel.state.value?.result == OdontogramResult.SAVING) return
        pendingPdf = OdontogramRequest(patientId, visitId)
        try {
            createOdontogramPdf.launch("Odontograma.pdf")
        } catch (_: ActivityNotFoundException) {
            pendingPdf = null
            pdfModel.fail(patientId)
        }
    }

    private fun launchRadiograph(patientId: Long, fromCamera: Boolean) {
        if (pendingRadiographPatient != null || radiographModel.state.value.busy) return
        pendingRadiographPatient = patientId
        try {
            if (fromCamera) {
                val file = captureFile
                file.parentFile?.mkdirs()
                file.outputStream().close()
                takeRadiograph.launch(FileProvider.getUriForFile(this, "$packageName.radiographs", file))
            } else {
                pickRadiograph.launch(arrayOf("image/jpeg", "image/png"))
            }
        } catch (_: ActivityNotFoundException) {
            abortRadiograph(patientId)
        } catch (_: IOException) {
            abortRadiograph(patientId)
        }
    }

    private fun abortRadiograph(patientId: Long) {
        pendingRadiographPatient = null
        captureFile.delete()
        radiographModel.failed(patientId)
    }

    private fun finishRadiograph(uri: Uri?, temporary: java.io.File? = null) {
        val patientId = pendingRadiographPatient
        pendingRadiographPatient = null
        if (uri == null || patientId == null) {
            temporary?.delete()
            return
        }
        radiographModel.import((application as OdontoApplication).demoClinic, patientId, uri, temporary)
    }

    private fun launchDocumentPhoto(patientId: Long, fromCamera: Boolean) {
        if (pendingDocumentPhotoPatient != null || documentPhotoModel.state.value.busy) return
        pendingDocumentPhotoPatient = patientId
        try {
            if (fromCamera) {
                val file = captureDocumentFile
                file.parentFile?.mkdirs()
                file.outputStream().close()
                takeDocumentPhoto.launch(FileProvider.getUriForFile(this, "$packageName.radiographs", file))
            } else {
                pickDocumentPhoto.launch(arrayOf("image/jpeg", "image/png"))
            }
        } catch (_: ActivityNotFoundException) {
            abortDocumentPhoto(patientId)
        } catch (_: IOException) {
            abortDocumentPhoto(patientId)
        }
    }

    private fun abortDocumentPhoto(patientId: Long) {
        pendingDocumentPhotoPatient = null
        captureDocumentFile.delete()
        documentPhotoModel.failed(patientId)
    }

    private fun finishDocumentPhoto(uri: Uri?, temporary: java.io.File? = null) {
        val patientId = pendingDocumentPhotoPatient
        pendingDocumentPhotoPatient = null
        if (uri == null || patientId == null) {
            temporary?.delete()
            return
        }
        documentPhotoModel.import((application as OdontoApplication).demoClinic, patientId, uri, temporary)
    }

    override fun onResume() {
        super.onResume()
        lock.refreshCredentialStatus()
        val app = application as OdontoApplication
        AppointmentReminders.sync(this, app.demoClinic)
    }

    override fun onStop() {
        lock.lock()
        cleanupPendingCaptureFiles()
        ClearingManager.clearAll()
        super.onStop()
    }

    override fun onDestroy() {
        lock.dispose()
        cleanupPendingCaptureFiles()
        ClearingManager.clearAll()
        super.onDestroy()
    }

    private fun cleanupPendingCaptureFiles() {
        if (pendingRadiographPatient == null && !radiographModel.state.value.busy) {
            captureFile.delete()
        }
        if (pendingDocumentPhotoPatient == null && !documentPhotoModel.state.value.busy) {
            captureDocumentFile.delete()
        }
    }

    private fun applyCaptureProtection(enabled: Boolean) {
        val flag = WindowManager.LayoutParams.FLAG_SECURE
        if (enabled) window.addFlags(flag) else window.clearFlags(flag)
    }
}

@Composable
private fun LockedScreen(lock: DeviceLock, onOpenSettings: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(R.string.security_locked), style = MaterialTheme.typography.headlineSmall)
            if (lock.secure) {
                Button(onClick = lock::unlock) { Text(stringResource(R.string.security_unlock)) }
            } else {
                Text(stringResource(R.string.security_setup))
                Button(onClick = onOpenSettings) { Text(stringResource(R.string.security_open_settings)) }
            }
        }
    }
}

@Composable
private fun PdfExportControls(visitId: Long?, result: OdontogramResult?, onExport: (Long) -> Unit) {
    OutlinedButton(
        onClick = { visitId?.let(onExport) },
        enabled = visitId != null && result != OdontogramResult.SAVING,
    ) { Text(stringResource(R.string.odontogram_pdf_export)) }
    Text(
        stringResource(if (visitId == null) R.string.odontogram_pdf_save_first else R.string.odontogram_pdf_privacy),
        style = MaterialTheme.typography.bodySmall,
    )
    if (result != null) {
        Text(stringResource(when (result) {
            OdontogramResult.SAVING -> R.string.odontogram_pdf_working
            OdontogramResult.EXPORTED -> R.string.odontogram_pdf_saved
            OdontogramResult.FAILED -> R.string.odontogram_pdf_failed
        }))
    }
}
