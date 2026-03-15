package com.rahees.quickscan.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rahees.quickscan.util.settingsDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val THEME_KEY = stringPreferencesKey("theme")
private val VIBRATE_KEY = booleanPreferencesKey("vibrate_on_scan")
private val AUTO_COPY_KEY = booleanPreferencesKey("auto_copy_clipboard")
private val PLAY_SOUND_KEY = booleanPreferencesKey("play_sound_on_scan")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val theme by context.settingsDataStore.data
        .map { it[THEME_KEY] ?: "system" }
        .collectAsState(initial = "system")

    val vibrateOnScan by context.settingsDataStore.data
        .map { it[VIBRATE_KEY] ?: true }
        .collectAsState(initial = true)

    val autoCopy by context.settingsDataStore.data
        .map { it[AUTO_COPY_KEY] ?: false }
        .collectAsState(initial = false)

    val playSound by context.settingsDataStore.data
        .map { it[PLAY_SOUND_KEY] ?: false }
        .collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Theme section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val themeOptions = listOf("system" to "System Default", "light" to "Light", "dark" to "Dark")
            themeOptions.forEach { (value, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(
                            selected = theme == value,
                            onClick = {
                                scope.launch {
                                    context.settingsDataStore.edit { it[THEME_KEY] = value }
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        scope.launch {
                            context.settingsDataStore.edit { it[THEME_KEY] = value }
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Scan behavior section
            Text(
                text = "Scan Behavior",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Vibrate on Scan") },
                supportingContent = { Text("Vibrate when a barcode is detected") },
                trailingContent = {
                    Switch(
                        checked = vibrateOnScan,
                        onCheckedChange = { checked ->
                            scope.launch {
                                context.settingsDataStore.edit { it[VIBRATE_KEY] = checked }
                            }
                        }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Auto-copy to Clipboard") },
                supportingContent = { Text("Automatically copy scanned content") },
                trailingContent = {
                    Switch(
                        checked = autoCopy,
                        onCheckedChange = { checked ->
                            scope.launch {
                                context.settingsDataStore.edit { it[AUTO_COPY_KEY] = checked }
                            }
                        }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Play Sound on Scan") },
                supportingContent = { Text("Play a beep sound when scanning") },
                trailingContent = {
                    Switch(
                        checked = playSound,
                        onCheckedChange = { checked ->
                            scope.launch {
                                context.settingsDataStore.edit { it[PLAY_SOUND_KEY] = checked }
                            }
                        }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Language section
            Text(
                text = "Language",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            var showLanguageDialog by remember { mutableStateOf(false) }

            val currentLocale = remember {
                val locales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.resources.configuration.locales
                } else {
                    @Suppress("DEPRECATION")
                    android.os.LocaleList.forLanguageTags(
                        context.resources.configuration.locales.toLanguageTags()
                    )
                }
                locales.get(0)?.displayLanguage ?: "System Default"
            }

            ListItem(
                headlineContent = { Text("App Language") },
                supportingContent = { Text(currentLocale) },
                modifier = Modifier.clickable {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                    } else {
                        showLanguageDialog = true
                    }
                }
            )

            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text("Language") },
                    text = {
                        Text("On this Android version, please change the app language in your device's system settings:\nSettings > Apps > QuickScan > Language")
                    },
                    confirmButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About section
            Text(
                text = "About",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("QuickScan") },
                supportingContent = { Text("QR & Barcode Scanner") }
            )

            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text("1.0.0") }
            )
        }
    }
}
