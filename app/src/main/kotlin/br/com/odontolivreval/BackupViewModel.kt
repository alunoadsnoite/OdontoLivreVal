package br.com.odontolivreval

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.odontolivreval.core.demo.ClinicStorageException
import br.com.odontolivreval.core.demo.DemoClinicStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class BackupAction { EXPORT, RESTORE }
internal enum class BackupResult { EXPORTED, RESTORED, EXPORT_FAILED, RESTORE_FAILED }
internal data class BackupRequest(val action: BackupAction, val uri: Uri)
internal data class BackupState(
    val request: BackupRequest? = null,
    val busy: Boolean = false,
    val result: BackupResult? = null,
    val revision: Int = 0,
)

internal class BackupViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(BackupState())
    val state = mutableState.asStateFlow()

    fun select(action: BackupAction, uri: Uri?) {
        if (uri == null || state.value.busy) return
        mutableState.value = state.value.copy(request = BackupRequest(action, uri), result = null)
    }

    fun dismiss() {
        if (!state.value.busy) mutableState.value = state.value.copy(request = null)
    }

    fun run(store: DemoClinicStore, password: CharArray) {
        val current = state.value
        val request = current.request
        if (current.busy || request == null) {
            password.fill('\u0000')
            return
        }
        mutableState.value = current.copy(busy = true, result = null)
        viewModelScope.launch {
            try {
                mutableState.value = when (request.action) {
                    BackupAction.EXPORT -> {
                        store.exportBackup(request.uri, password)
                        current.copy(request = null, result = BackupResult.EXPORTED)
                    }
                    BackupAction.RESTORE -> {
                        store.restoreBackup(request.uri, password)
                        current.copy(request = null, result = BackupResult.RESTORED, revision = current.revision + 1)
                    }
                }
            } catch (_: ClinicStorageException) {
                val result = when (request.action) {
                    BackupAction.EXPORT -> BackupResult.EXPORT_FAILED
                    BackupAction.RESTORE -> BackupResult.RESTORE_FAILED
                }
                mutableState.value = current.copy(result = result)
            } finally {
                password.fill('\u0000')
            }
        }
    }
}
