package com.rahees.quickscan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rahees.quickscan.navigation.NavGraph
import com.rahees.quickscan.ui.components.RateAppDialog
import com.rahees.quickscan.ui.theme.QuickScanTheme
import com.rahees.quickscan.util.settingsDataStore
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsDataStore.data.map { prefs ->
                prefs[stringPreferencesKey("theme")] ?: "system"
            }.collectAsState(initial = "system")

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            var showRateDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val prefs = settingsDataStore.data.first()
                val launchCount = (prefs[intPreferencesKey("launch_count")] ?: 0) + 1
                val neverRate = prefs[booleanPreferencesKey("never_rate")] ?: false
                settingsDataStore.edit { it[intPreferencesKey("launch_count")] = launchCount }
                if (launchCount >= 5 && !neverRate) {
                    showRateDialog = true
                }
            }

            QuickScanTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()

                    if (showRateDialog) {
                        RateAppDialog(
                            onRateNow = {
                                showRateDialog = false
                                try {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id=$packageName")
                                        )
                                    )
                                } catch (_: Exception) {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                                        )
                                    )
                                }
                            },
                            onLater = { showRateDialog = false },
                            onNever = {
                                showRateDialog = false
                                lifecycleScope.launch {
                                    settingsDataStore.edit { prefs ->
                                        prefs[booleanPreferencesKey("never_rate")] = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
