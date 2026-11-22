package br.com.odontolivreval.feature.settings

import br.com.odontolivreval.core.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test fun selectionUpdatesStateAndWriteFailurePreservesSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = object : ThemeRepository {
                override val theme = MutableStateFlow(ThemeMode.SYSTEM)
                var fail = false
                override suspend fun setTheme(mode: ThemeMode) {
                    if (fail) throw IOException("test")
                    theme.value = mode
                }
            }
            val vm = SettingsViewModel(repo)
            runCurrent()
            assertEquals(ThemeMode.SYSTEM, vm.state.value.theme)
            vm.selectTheme(ThemeMode.DARK)
            runCurrent()
            assertEquals(ThemeMode.DARK, vm.state.value.theme)
            repo.fail = true
            vm.selectTheme(ThemeMode.LIGHT)
            runCurrent()
            assertTrue(vm.state.value.writeFailed)
            assertEquals(ThemeMode.DARK, vm.state.value.theme)
            repo.fail = false
            vm.selectTheme(ThemeMode.LIGHT)
            runCurrent()
            assertFalse(vm.state.value.writeFailed)
            assertEquals(ThemeMode.LIGHT, vm.state.value.theme)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun clearResetsWriteFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = object : ThemeRepository {
                override val theme = MutableStateFlow(ThemeMode.SYSTEM)
                var fail = false
                override suspend fun setTheme(mode: ThemeMode) {
                    if (fail) throw IOException("test")
                    theme.value = mode
                }
            }
            val vm = SettingsViewModel(repo)
            runCurrent()
            repo.fail = true
            vm.selectTheme(ThemeMode.LIGHT)
            runCurrent()
            assertTrue(vm.state.value.writeFailed)
            vm.clear()
            runCurrent()
            assertFalse(vm.state.value.writeFailed)
        } finally { Dispatchers.resetMain() }
    }
}
