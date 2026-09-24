package com.callbackdev.passo

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.tracking.StepTracking
import com.callbackdev.passo.core.tracking.TrackingReadiness
import com.callbackdev.passo.feature.onboarding.TrackingSetupScreen
import com.callbackdev.passo.feature.onboarding.TrackingSetupState
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

/**
 * The one activity. In Phase 1 it only gets tracking going: the permissions, the phone without
 * a step counter, and a line saying counting is on. Opening the app (re)starts the service,
 * which is the documented way back after a force stop (PLANNING.md §4.2). The navigation shell
 * arrives in Phase 3.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var trackingRepository: TrackingRepository

    private var readiness by mutableStateOf(TrackingReadiness.PERMISSION_NEEDED)

    // The system stops showing its dialog after the second refusal; from then on only the
    // app's settings page can grant the permission.
    private var askInSettings by mutableStateOf(false)

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshTracking()
        if (readiness == TrackingReadiness.PERMISSION_NEEDED &&
            !shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            askInSettings = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PassoTheme {
                val stepsFlow = remember { trackingRepository.observeStepsOn(LocalDate.now().toEpochDay()) }
                val stepsToday by stepsFlow.collectAsStateWithLifecycle(initialValue = null)
                val state = when (readiness) {
                    TrackingReadiness.NO_SENSOR -> TrackingSetupState.NoSensor
                    TrackingReadiness.PERMISSION_NEEDED -> TrackingSetupState.PermissionNeeded(askInSettings)
                    TrackingReadiness.READY -> TrackingSetupState.Tracking(stepsToday)
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TrackingSetupScreen(
                        state = state,
                        onAllow = ::requestPermissions,
                        onOpenSettings = ::openAppSettings,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Also on the way back from the settings page, where the permission may have changed.
        refreshTracking()
    }

    private fun refreshTracking() {
        readiness = StepTracking.readiness(this)
        if (readiness == TrackingReadiness.READY) StepTracking.start(this)
    }

    private fun requestPermissions() {
        permissionRequest.launch(
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS),
        )
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }
}
