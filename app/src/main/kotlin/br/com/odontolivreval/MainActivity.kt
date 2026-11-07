package br.com.odontolivreval

import android.content.Intent
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.odontolivreval.core.designsystem.OdontoTheme
import br.com.odontolivreval.feature.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    private lateinit var lock: DeviceLock
    private lateinit var backupModel: BackupViewModel

    private val exportBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        backupModel.select(BackupAction.EXPORT, it)
    }

    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        backupModel.select(BackupAction.RESTORE, it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lock = DeviceLock(this)
        backupModel = ViewModelProvider(this)[BackupViewModel::class.java]
        applyCaptureProtection(lock.enabled)
        enableEdgeToEdge()
        val odontoApplication = application as OdontoApplication
        setContent {
            val themeModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(odontoApplication.themes))
            val state by themeModel.state.collectAsStateWithLifecycle()
            val backup by backupModel.state.collectAsStateWithLifecycle()
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
                            ) {
                                ClinicProtectionScreen(
                                    lock = lock,
                                    backup = backup,
                                    onExport = { exportBackup.launch("OdontoLivreVal.odl") },
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

    override fun onResume() {
        super.onResume()
        lock.refreshCredentialStatus()
    }

    override fun onStop() {
        lock.lock()
        super.onStop()
    }

    override fun onDestroy() {
        lock.dispose()
        super.onDestroy()
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
