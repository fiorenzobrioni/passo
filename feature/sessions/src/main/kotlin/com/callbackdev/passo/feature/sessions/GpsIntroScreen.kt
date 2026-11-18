package com.callbackdev.passo.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.format.unorphaned
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.balanced
import com.callbackdev.passo.core.designsystem.theme.padding
import com.callbackdev.passo.core.designsystem.theme.pageGutter

/**
 * Passo's own words before Android asks for the location (PLANNING.md §11 Phase 15;
 * docs/adr/0016-location.md, decision 2): shown once, the first time an outing with «Measure with
 * GPS» starts, never in the first run. One sentence of what it is for, then the three things a
 * reader weighs: when location is read, that it is never sent, what it costs. The system's dialog
 * follows «Continue»; «Not now» starts the outing all the same, from the steps, and is not asked
 * again by itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsIntroScreen(onContinue: () -> Unit, onNotNow: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.sessions_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(pageGutter(sideInsets = false))
                        .padding(horizontal = ScreenMargin, vertical = 12.dp),
                ) {
                    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag(GpsIntroTags.CONTINUE)) {
                        Text(stringResource(R.string.gps_intro_continue))
                    }
                    TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth().testTag(GpsIntroTags.NOT_NOW)) {
                        Text(stringResource(R.string.gps_intro_not_now))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(pageGutter(sideInsets = false))
                .padding(horizontal = ScreenMargin + 4.dp, vertical = 8.dp)
                .testTag(GpsIntroTags.PAGE),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    PassoIcons.Gps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    stringResource(R.string.gps_intro_title),
                    style = MaterialTheme.typography.headlineMedium.balanced(),
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.gps_intro_lead).unorphaned(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Point(PassoIcons.Outing, R.string.gps_intro_when_title, R.string.gps_intro_when_body)
            Point(PassoIcons.Shield, R.string.gps_intro_sent_title, R.string.gps_intro_sent_body)
            Point(PassoIcons.Battery, R.string.gps_intro_cost_title, R.string.gps_intro_cost_body)
            Text(
                stringResource(R.string.gps_intro_next).unorphaned(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One thing to weigh: what it is, in a line, and the sentence that says what it means. */
@Composable
private fun Point(icon: ImageVector, title: Int, body: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
        // One stop for TalkBack: the line and its sentence are read together.
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp).size(24.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(body).unorphaned(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hooks for the UI tests. */
object GpsIntroTags {
    const val PAGE = "gps_intro_page"
    const val CONTINUE = "gps_intro_continue"
    const val NOT_NOW = "gps_intro_not_now"
}
