package br.com.odontolivreval

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import br.com.odontolivreval.core.model.ThemeMode
import br.com.odontolivreval.feature.settings.*

private enum class Destination(@param:StringRes val label: Int) {
    HOME(R.string.home), PATIENTS(R.string.patients), SCHEDULE(R.string.schedule),
    ODONTOGRAM(R.string.odontogram), SETTINGS(R.string.settings)
}

@Composable
fun OdontoApp(state: SettingsState, onTheme: (ThemeMode) -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Destination.HOME.name
    fun navigate(destination: Destination) {
        nav.navigate(destination.name) {
            popUpTo(Destination.HOME.name) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 600.dp
            Column(Modifier.fillMaxSize()) {
                // FlowRow wraps labels at large font scales and on narrow screens.
                FlowRow(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Destination.entries.forEach { destination ->
                        FilterChip(selected = route == destination.name, onClick = { navigate(destination) },
                            label = { Text(stringResource(destination.label)) })
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = if (wide) 840.dp else 600.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NavHost(navController = nav, startDestination = Destination.HOME.name) {
                            composable(Destination.HOME.name) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                                    Text(stringResource(R.string.welcome), style = MaterialTheme.typography.titleLarge)
                                    Text(stringResource(R.string.intro))
                                    Destination.entries.filter { it != Destination.HOME }.forEach { destination ->
                                        OutlinedButton(onClick = { navigate(destination) }) { Text(stringResource(destination.label)) }
                                    }
                                }
                            }
                            Destination.entries.filter { it != Destination.HOME && it != Destination.SETTINGS }.forEach { destination ->
                                composable(destination.name) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(stringResource(destination.label), style = MaterialTheme.typography.headlineLarge)
                                        Text(stringResource(R.string.development), style = MaterialTheme.typography.titleLarge)
                                        Text(stringResource(R.string.no_clinical_data))
                                    }
                                }
                            }
                            composable(Destination.SETTINGS.name) { SettingsScreen(state, onTheme) { nav.navigate("about") { launchSingleTop = true } } }
                            composable("about") {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineLarge)
                                    Text(stringResource(R.string.app_name))
                                    Text(stringResource(R.string.version, BuildConfig.VERSION_NAME))
                                    Text(stringResource(R.string.license))
                                    val contact = stringResource(R.string.public_contact)
                                    if (contact.isNotBlank()) Text(contact)
                                    OutlinedButton(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.back)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
