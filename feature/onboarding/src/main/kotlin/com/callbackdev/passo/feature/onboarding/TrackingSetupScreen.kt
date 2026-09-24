package com.callbackdev.passo.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import java.text.NumberFormat

/** Where tracking stands, as far as the setup screen is concerned. */
sealed interface TrackingSetupState {
    /** No hardware step counter: a dead end, explained (PLANNING.md §4.6). */
    data object NoSensor : TrackingSetupState

    /** The permission is missing; [askInSettings] once the system will no longer ask. */
    data class PermissionNeeded(val askInSettings: Boolean) : TrackingSetupState

    /** Running. [stepsToday] is null until the stored count has been read. */
    data class Tracking(val stepsToday: Int?) : TrackingSetupState
}

/**
 * Phase 1's only screen: asks for the permissions tracking needs, explains a phone without a
 * step counter, and otherwise says that counting is on. The real onboarding (welcome,
 * profile, goal, manufacturer tips) replaces it in Phase 3.
 */
@Composable
fun TrackingSetupScreen(
    state: TrackingSetupState,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            TrackingSetupState.NoSensor -> {
                Title(stringResource(R.string.setup_no_sensor_title))
                Body(stringResource(R.string.setup_no_sensor_body))
            }

            is TrackingSetupState.PermissionNeeded -> {
                Title(stringResource(R.string.setup_permission_title))
                Body(stringResource(R.string.setup_permission_body))
                Spacer(Modifier.height(8.dp))
                if (state.askInSettings) {
                    Body(stringResource(R.string.setup_permission_denied_body))
                    Button(onClick = onOpenSettings) { Text(stringResource(R.string.setup_permission_open_settings)) }
                } else {
                    Button(onClick = onAllow) { Text(stringResource(R.string.setup_permission_allow)) }
                }
            }

            is TrackingSetupState.Tracking -> {
                Title(stringResource(R.string.setup_tracking_title))
                if (state.stepsToday != null) {
                    val locale = LocalConfiguration.current.locales[0]
                    Text(
                        text = pluralStringResource(
                            R.plurals.setup_tracking_steps_today,
                            state.stepsToday,
                            NumberFormat.getIntegerInstance(locale).format(state.stepsToday),
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Body(stringResource(R.string.setup_tracking_body))
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
private fun PermissionPreview() {
    PassoTheme(dynamicColor = false) {
        TrackingSetupScreen(TrackingSetupState.PermissionNeeded(askInSettings = false), onAllow = {
        }, onOpenSettings = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingPreview() {
    PassoTheme(dynamicColor = false) {
        TrackingSetupScreen(TrackingSetupState.Tracking(stepsToday = 6_240), onAllow = {}, onOpenSettings = {})
    }
}
