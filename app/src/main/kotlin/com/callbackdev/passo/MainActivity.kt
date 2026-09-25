package com.callbackdev.passo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.model.AppFont
import com.callbackdev.passo.core.model.AppPalette
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.tracking.StepTracking
import com.callbackdev.passo.core.tracking.TrackingReadiness
import com.callbackdev.passo.shell.PassoRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The one activity. It wears the reader's appearance (theme, palette, typeface), hands the
 * pages to [PassoRoot], and on every return (re)starts tracking unless the reader paused it:
 * opening the app is the documented way back after a force stop (PLANNING.md §4.2).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    private var readiness by mutableStateOf(TrackingReadiness.READY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readiness = StepTracking.readiness(this)
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = when (settings?.theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }
            // The bars' ink follows the applied theme, not the system's: a reader who forces
            // light on a dark phone would otherwise get white icons on a white page.
            val view = LocalView.current
            DisposableEffect(dark) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
                onDispose { }
            }
            PassoTheme(
                darkTheme = dark,
                dynamicColor = settings?.dynamicColor ?: false,
                palette = settings?.palette ?: AppPalette.VIVID,
                font = settings?.font ?: AppFont.GOOGLE_SANS,
            ) {
                PassoRoot(readiness = readiness, onboardingCompleted = settings?.onboardingCompleted)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Also on the way back from the system's settings, where the permission may have changed.
        readiness = StepTracking.readiness(this)
        lifecycleScope.launch {
            if (readiness == TrackingReadiness.READY && settingsRepository.settings.first().trackingEnabled) {
                StepTracking.start(this@MainActivity)
            }
        }
    }
}
