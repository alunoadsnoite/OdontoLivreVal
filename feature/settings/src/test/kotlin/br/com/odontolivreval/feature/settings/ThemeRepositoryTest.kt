package br.com.odontolivreval.feature.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.odontolivreval.core.model.ThemeMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ThemeRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun defaultAndUnknownValuesUseSystem() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("theme.preferences_pb") }
            val repo = DataStoreThemeRepository(store)
            assertEquals(ThemeMode.SYSTEM, repo.theme.first())
            store.edit { it[stringPreferencesKey("theme")] = "unknown" }
            assertEquals(ThemeMode.SYSTEM, repo.theme.first())
        } finally { scope.cancel() }
    }
    @Test fun selectionSurvivesClosingAndReopeningStore() = runTest {
        for (mode in ThemeMode.entries) {
            val file = folder.root.resolve("${mode.key}.preferences_pb")
            val job = SupervisorJob()
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
            DataStoreThemeRepository(store).setTheme(mode)
            job.cancelAndJoin()
            val reopenedJob = SupervisorJob()
            try {
                val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(reopenedJob + Dispatchers.IO)) { file }
                assertEquals(mode, DataStoreThemeRepository(reopened).theme.first())
            } finally { reopenedJob.cancelAndJoin() }
        }
    }
}
