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
import br.com.odontolivreval.core.model.ThemeMode
import br.com.odontolivreval.feature.documents.DocumentPdfRequest
import br.com.odontolivreval.feature.documents.DocumentsScreen
import br.com.odontolivreval.feature.patients.PatientRecordScreen
import br.com.odontolivreval.feature.patients.PatientsScreen
import br.com.odontolivreval.feature.schedule.ScheduleScreen
import br.com.odontolivreval.feature.settings.*

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
    fun navigate(destination: Destination) {
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
    // Mantém a seleção durante o fluxo prontuário → documentos → perfil.
    LaunchedEffect(entry?.destination?.route) {
        val currentRoute = nav.currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
        if (currentRoute !in listOf("patient_record", "documents", "profile")) {
            selectedPatient = null
        }
    }
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
                                SettingsScreen(
                                    demoClinic,
                                    state,
                                    onTheme,
                                    onAbout = { nav.navigate("about") { launchSingleTop = true } },
                                    onProfile = { nav.navigate("profile") { launchSingleTop = true } },
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
