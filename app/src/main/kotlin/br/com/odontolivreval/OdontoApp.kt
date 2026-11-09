package br.com.odontolivreval

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import br.com.odontolivreval.core.demo.ClinicStore
import br.com.odontolivreval.core.demo.DemoPatient
import br.com.odontolivreval.core.demo.SignatureModality
import br.com.odontolivreval.core.model.ThemeMode
import br.com.odontolivreval.feature.documents.CertificateFormScreen
import br.com.odontolivreval.feature.documents.CertificateListScreen
import br.com.odontolivreval.feature.documents.CertificatePreviewScreen
import br.com.odontolivreval.feature.documents.DocumentPdfRequest
import br.com.odontolivreval.feature.documents.DocumentsScreen
import br.com.odontolivreval.feature.patients.PatientRecordScreen
import br.com.odontolivreval.feature.patients.PatientsScreen
import br.com.odontolivreval.feature.schedule.ScheduleScreen
import br.com.odontolivreval.feature.settings.*
import br.com.odontolivreval.feature.treatment.TreatmentBudgetFormScreen
import br.com.odontolivreval.feature.treatment.TreatmentBudgetListScreen
import br.com.odontolivreval.feature.treatment.TreatmentBudgetPreviewScreen
import br.com.odontolivreval.feature.treatment.TreatmentPlanFormScreen
import br.com.odontolivreval.feature.treatment.TreatmentPlanListScreen
import br.com.odontolivreval.feature.consent.ConsentFormScreen
import br.com.odontolivreval.feature.consent.ConsentListScreen
import br.com.odontolivreval.feature.consent.ConsentPreviewScreen
import br.com.odontolivreval.feature.signature.SignatureCaptureScreen
import br.com.odontolivreval.feature.signature.SignatureImportScreen
import br.com.odontolivreval.feature.signature.SignatureOptionsScreen
import br.com.odontolivreval.feature.signature.SignaturePreviewScreen
import br.com.odontolivreval.feature.signature.SignatureViewModel

private enum class Destination(@param:StringRes val label: Int) {
    HOME(R.string.home), PATIENTS(R.string.patients), SCHEDULE(R.string.schedule),
    SETTINGS(R.string.settings)
}

private val PatientSaver = listSaver<DemoPatient?, Any>(
    save = { patient -> if (patient == null) emptyList() else listOf(patient.id, patient.name) },
    restore = { saved ->
        if (saved.isEmpty()) null else DemoPatient(saved[0] as Long, saved[1] as String)
    },
)

@Composable
fun OdontoApp(
    state: SettingsState,
    onTheme: (ThemeMode) -> Unit,
    demoClinic: ClinicStore,
    backupRevision: Int,
    exportOdontogram: @Composable (Long, Long?) -> Unit,
    radiographs: @Composable (Long) -> Unit,
    prescriptions: @Composable (Long, Boolean) -> Unit,
    onSaveDocument: (DocumentPdfRequest) -> Unit,
    documentExportStatus: @Composable (Long) -> String?,
    documentPhotos: @Composable (Long) -> Unit,
    protection: @Composable () -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Destination.HOME.name
    var selectedPatient by rememberSaveable(stateSaver = PatientSaver) {
        mutableStateOf<DemoPatient?>(null)
    }
    var requestedSignatureModality by rememberSaveable {
        mutableStateOf(SignatureModality.HANDWRITTEN_DIGITAL.name)
    }
    fun navigate(destination: Destination) {
        selectedPatient = null
        nav.navigate(destination.name) {
            popUpTo(Destination.HOME.name) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun openPatient(patient: DemoPatient) {
        selectedPatient = patient
        nav.navigate("patient_record") { launchSingleTop = true }
    }
    LaunchedEffect(backupRevision) {
        if (backupRevision > 0) {
            nav.clearBackStack(Destination.PATIENTS.name)
            nav.clearBackStack(Destination.SCHEDULE.name)
        }
    }
    // A seleção só é limpa por ações explícitas de saída; navegar entre telas
    // do prontuário não pode descartar o paciente durante uma transição.
    Scaffold { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 600.dp
            Column(Modifier.fillMaxSize()) {
                if (route !in listOf("patient_record", "documents") && (route != "profile" || selectedPatient == null)) {
                    // FlowRow wraps labels at large font scales and on narrow screens.
                    FlowRow(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Destination.entries.forEach { destination ->
                            FilterChip(selected = route == destination.name, onClick = { navigate(destination) },
                                label = { Text(stringResource(destination.label)) })
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    NavHost(
                        navController = nav,
                        startDestination = Destination.HOME.name,
                        modifier = Modifier.widthIn(max = if (wide) 840.dp else 600.dp).fillMaxWidth(),
                    ) {
                        composable(Destination.HOME.name) {
                            ScreenContent {
                                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                                Text(stringResource(R.string.welcome), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(R.string.intro))
                                UpcomingAppointmentsPanel(demoClinic, ::openPatient)
                                Destination.entries.filter { it != Destination.HOME }.forEach { destination ->
                                    OutlinedButton(onClick = { navigate(destination) }) { Text(stringResource(destination.label)) }
                                }
                            }
                        }
                        composable(Destination.PATIENTS.name) {
                            PatientsScreen(demoClinic, ::openPatient)
                        }
                        composable(Destination.SCHEDULE.name) {
                            ScheduleScreen(
                                store = demoClinic,
                                onPatientClick = ::openPatient,
                                onPatients = { navigate(Destination.PATIENTS) },
                            )
                        }
                        composable(Destination.SETTINGS.name) {
                            ScreenContent {
                                OutlinedButton(onClick = { nav.navigate("profile") { launchSingleTop = true } }) {
                                    Text(stringResource(R.string.profile_title))
                                }
                                SettingsScreen(
                                    demoClinic,
                                    state,
                                    onTheme,
                                    onAbout = { nav.navigate("about") { launchSingleTop = true } },
                                )
                                OutlinedButton(onClick = { nav.navigate("protection") { launchSingleTop = true } }) {
                                    Text(stringResource(R.string.security_title))
                                }
                            }
                        }
                        composable("profile") {
                            ScreenContent {
                                ProfessionalProfileScreen(
                                    store = demoClinic,
                                    onClose = { nav.popBackStack() },
                                )
                            }
                        }
                        composable("documents") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    DocumentsScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        onClose = { nav.popBackStack() },
                                        onOpenProfile = { nav.navigate("profile") { launchSingleTop = true } },
                                        onSaveDocument = onSaveDocument,
                                        exportStatus = documentExportStatus(patient.id),
                                        archive = { prescriptions(patient.id, true) },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("certificates_list") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    CertificateListScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        onClose = { nav.popBackStack() },
                                        onCreateCertificate = {
                                            nav.navigate("certificates_form") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onViewCertificate = { certId ->
                                            nav.navigate("certificates_form?certificateId=$certId") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onCancelCertificate = { _, _ -> },
                                        onCreateVersion = { certId ->
                                            nav.navigate("certificates_form?certificateId=$certId") {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("certificates_form") {
                            ScreenContent {
                                val patient = selectedPatient
                                val certificateId = nav.currentBackStackEntry?.arguments?.getLong("certificateId") ?: -1L
                                if (patient != null) {
                                    CertificateFormScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        certificate = null,
                                        onClose = { nav.popBackStack() },
                                        onSave = { nav.popBackStack() },
                                        onFinalize = { nav.popBackStack() },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("certificates_preview") {
                            ScreenContent {
                                val patient = selectedPatient
                                val certificateId = nav.currentBackStackEntry?.arguments?.getLong("certificateId") ?: -1L
                                if (patient != null && certificateId > 0) {
                                    CertificatePreviewScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        certificateId = certificateId,
                                        onClose = { nav.popBackStack() },
                                        onShare = { /* handled internally */ },
                                        onCancel = { /* handled internally */ },
                                        onNewVersion = {
                                            nav.navigate("certificates_form?certificateId=$certificateId") {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("treatment_plan_list") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    TreatmentPlanListScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        onClose = { nav.popBackStack() },
                                        onCreatePlan = {
                                            nav.navigate("treatment_plan_form") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onOpenPlan = { planId ->
                                            nav.navigate("treatment_plan_form?planId=$planId") {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("treatment_plan_form") {
                            ScreenContent {
                                val patient = selectedPatient
                                val planId = nav.currentBackStackEntry?.arguments?.getLong("planId")
                                if (patient != null) {
                                    TreatmentPlanFormScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        planId = planId,
                                        onClose = { nav.popBackStack() },
                                        onSaveComplete = {
                                            nav.popBackStack()
                                            nav.navigate("treatment_budget_list") {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("treatment_budget_list") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    TreatmentBudgetListScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        onClose = { nav.popBackStack() },
                                        onCreateBudget = { planId ->
                                            nav.navigate("treatment_budget_form?planId=$planId") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onOpenBudget = { budgetId ->
                                            nav.navigate("treatment_budget_form?budgetId=$budgetId") {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("treatment_budget_form") {
                            ScreenContent {
                                val patient = selectedPatient
                                val planId = nav.currentBackStackEntry?.arguments?.getLong("planId") ?: -1L
                                val budgetId = nav.currentBackStackEntry?.arguments?.getLong("budgetId")
                                if (patient != null) {
                                    TreatmentBudgetFormScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        planId = planId,
                                        budgetId = budgetId,
                                        onClose = { nav.popBackStack() },
                                        onSaveComplete = { nav.popBackStack() },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("treatment_budget_preview") {
                            ScreenContent {
                                val patient = selectedPatient
                                val budgetId = nav.currentBackStackEntry?.arguments?.getLong("budgetId") ?: -1L
                                val planId = nav.currentBackStackEntry?.arguments?.getLong("planId") ?: -1L
                                if (patient != null && budgetId > 0) {
                                    TreatmentBudgetPreviewScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        budgetId = budgetId,
                                        planId = planId,
                                        onClose = { nav.popBackStack() },
                                        onShare = { /* handled internally */ },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("consent_list") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    ConsentListScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        onClose = { nav.popBackStack() },
                                        onCreateConsent = {
                                            nav.navigate("consent_form") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onOpenConsent = { consentId ->
                                            nav.navigate("consent_form?consentId=$consentId") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onRevokeConsent = { _, _ -> },
                                        onCreateRevision = { consentId ->
                                            nav.navigate("consent_form?consentId=$consentId") {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("consent_form") {
                            ScreenContent {
                                val patient = selectedPatient
                                val consentId = nav.currentBackStackEntry?.arguments?.getLong("consentId")
                                if (patient != null) {
                                    ConsentFormScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        consentId = consentId,
                                        onClose = { nav.popBackStack() },
                                        onSaveComplete = { nav.popBackStack() },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("consent_preview") {
                            ScreenContent {
                                val patient = selectedPatient
                                val consentId = nav.currentBackStackEntry?.arguments?.getLong("consentId") ?: -1L
                                if (patient != null && consentId > 0) {
                                    ConsentPreviewScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        consentId = consentId,
                                        onClose = { nav.popBackStack() },
                                        onShare = { /* handled internally */ },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("signature_options") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    SignatureOptionsScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        documentType = "patient_record",
                                        documentId = patient.id,
                                        documentVersion = 1,
                                        documentTitle = "Prontuário clínico",
                                        onClose = { nav.popBackStack() },
                                        onCapture = { modality ->
                                            requestedSignatureModality = modality.name
                                            nav.navigate("signature_capture") { launchSingleTop = true }
                                        },
                                        onImportExternal = {
                                            nav.navigate("signature_import") { launchSingleTop = true }
                                        },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("signature_capture") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    SignatureCaptureScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        documentType = "patient_record",
                                        documentId = patient.id,
                                        documentVersion = 1,
                                        documentTitle = "Prontuário clínico",
                                        initialModality = runCatching { SignatureModality.valueOf(requestedSignatureModality) }
                                            .getOrDefault(SignatureModality.HANDWRITTEN_DIGITAL),
                                        onClose = { nav.popBackStack() },
                                        onSignatureCreated = { nav.popBackStack() },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("signature_preview") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    SignaturePreviewScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        documentType = "patient_record",
                                        documentId = patient.id,
                                        documentVersion = 1,
                                        documentTitle = "Prontuário clínico",
                                        documentContent = "Conteúdo do documento",
                                        onClose = { nav.popBackStack() },
                                        onSignatureCreated = { nav.popBackStack() },
                                        onShareSigned = { /* handled internally */ },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("signature_import") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    SignatureImportScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        onClose = { nav.popBackStack() },
                                        onImportCompleted = { nav.popBackStack() },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                        composable("protection") {
                            ScreenContent {
                                protection()
                                OutlinedButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.back)) }
                            }
                        }
                        composable("about") {
                            ScreenContent {
                                Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineLarge)
                                Text(stringResource(R.string.app_name))
                                Text(stringResource(R.string.version, BuildConfig.VERSION_NAME))
                                Text(stringResource(R.string.license))
                                Text(stringResource(R.string.icon_attribution))
                                val contact = stringResource(R.string.public_contact)
                                if (contact.isNotBlank()) Text(contact)
                                OutlinedButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.back)) }
                            }
                        }
                        composable("patient_record") {
                            ScreenContent {
                                val patient = selectedPatient
                                if (patient != null) {
                                    PatientRecordScreen(
                                        store = demoClinic,
                                        patient = patient,
                                        radiographs = { radiographs(patient.id) },
                                        prescriptions = { prescriptions(patient.id, false) },
                                        exportOdontogram = { exportOdontogram(patient.id, it) },
                                        documentPhotos = { documentPhotos(patient.id) },
                                        onDocuments = { nav.navigate("documents") { launchSingleTop = true } },
                                        onCertificates = { nav.navigate("certificates_list") { launchSingleTop = true } },
                                        onTreatmentPlans = { nav.navigate("treatment_plan_list") { launchSingleTop = true } },
                                        onConsents = { nav.navigate("consent_list") { launchSingleTop = true } },
                                        onSignatures = { nav.navigate("signature_options") { launchSingleTop = true } },
                                        onClose = { selectedPatient = null; nav.popBackStack() },
                                    )
                                } else {
                                    OutlinedButton(onClick = { nav.popBackStack() }) {
                                        Text(stringResource(R.string.back))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}
