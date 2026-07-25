package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.di.AppContainer
import com.example.ui.lock.AppLockGate
import com.example.ui.navigation.MyNotesNavigation
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.theme.MyNotesTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = AppContainer.settingsRepository!!
        // Read the persisted settings once up front so the theme / lock state is
        // correct on the very first frame (no flash of the wrong theme or content).
        val initialSettings = runBlocking { settingsRepository.settings.first() }

        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = initialSettings)

            // Optionally hide the content from the recent-apps preview / screenshots.
            LaunchedEffect(settings.hideFromRecents) {
                if (settings.hideFromRecents) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            MyNotesTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (!settings.onboardingComplete) {
                        OnboardingScreen(
                            onFinish = {
                                AppContainer.applicationScope.launch {
                                    settingsRepository.setOnboardingComplete(true)
                                }
                            },
                        )
                    } else {
                        AppLockGate(enabled = settings.appLockEnabled) {
                            MyNotesNavigation()
                        }
                    }
                }
            }
        }
    }
}
