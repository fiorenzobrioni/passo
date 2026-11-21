package com.callbackdev.passo

import android.content.Intent
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
import com.callbackdev.passo.core.data.widget.WidgetUpdates
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.widget.WidgetEvent
import com.callbackdev.passo.core.model.AppFont
import com.callbackdev.passo.core.model.AppPalette
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.tracking.GoalNotifier
import com.callbackdev.passo.core.tracking.OutingStarter
import com.callbackdev.passo.core.tracking.SessionControl
import com.callbackdev.passo.core.tracking.StepTracking
import com.callbackdev.passo.core.tracking.TrackingControl
import com.callbackdev.passo.core.tracking.TrackingReadiness
import com.callbackdev.passo.shell.AppDoor
import com.callbackdev.passo.shell.PassoRoot
import com.callbackdev.passo.shell.isFreshTouch
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The one activity. It wears the reader's appearance (theme, palette, typeface), hands the
 * pages to [PassoRoot], and on every return (re)starts tracking unless the reader paused it:
 * opening the app is the documented way back after a force stop (PLANNING.md §4.2). A paused
 * widget's tap lands here too, asking to resume ([TrackingControl.EXTRA_RESUME]), and a launcher
 * shortcut, asking to start an outing ([SessionControl.EXTRA_START_PLAN]); and every touch from
 * outside the app names the page it opens ([AppDoor]: Today, an outing, a week, a way).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var trackingControl: TrackingControl

    @Inject lateinit var widgets: WidgetUpdates

    @Inject lateinit var goals: GoalNotifier

    @Inject lateinit var outings: OutingStarter

    private var readiness by mutableStateOf(TrackingReadiness.READY)

    /** The page a touch from outside asked for, until the pages have opened it. */
    private var door by mutableStateOf<AppDoor?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readiness = StepTracking.readiness(this)
        // Not again after a rotation, nor after the process was ended: that touch was answered.
        if (savedInstanceState == null) answer(intent)
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
                PassoRoot(
                    readiness = readiness,
                    onboardingCompleted = settings?.onboardingCompleted,
                    outings = outings,
                    door = door,
                    onDoorOpened = { door = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        answer(intent)
    }

    override fun onResume() {
        super.onResume()
        // Also on the way back from the system's settings, where the permission may have changed.
        readiness = StepTracking.readiness(this)
        // A revoked permission kills the process without a word to the widgets: they learn it
        // here, once a process and whenever it changed, not at every return to the app (the
        // service tells them itself when it starts or stops).
        if (readiness != toldReadiness) {
            toldReadiness = readiness
            widgets.notify(WidgetEvent.TRACKING_STATE)
        }
        lifecycleScope.launch {
            if (readiness == TrackingReadiness.READY && settingsRepository.settings.first().trackingEnabled) {
                StepTracking.start(this@MainActivity, fromApp = true)
            }
            // Back from the system's settings, where the goals' notifications may have been
            // turned on again: the evening reminder is armed once they can show.
            goals.accessMayHaveChanged()
        }
    }

    /**
     * What a touch from outside asks for, once per touch: a resume, an outing to start, a page.
     * Not an intent Android hands back from Recents ([isFreshTouch]): that touch was answered when
     * it was made.
     */
    private fun answer(intent: Intent?) {
        if (intent == null || !intent.isFreshTouch) return
        resumeIfAsked(intent)
        startOutingIfAsked(intent)
        AppDoor.take(intent)?.let { door = it }
    }

    /**
     * A launcher shortcut's outing. Only while counting: an outing is measured from the steps,
     * and a paused count has none; Today then says why, with its button. Either way Today comes
     * forward, wherever the app was left: the card, or the reason, is there. Through the same
     * door as a start from a page ([OutingStarter]): a plan measured with GPS asks for its
     * permission here too, the first time.
     */
    private fun startOutingIfAsked(intent: Intent) {
        val planId = intent.getLongExtra(SessionControl.EXTRA_START_PLAN, 0L)
        if (planId == 0L) return
        intent.removeExtra(SessionControl.EXTRA_START_PLAN)
        door = AppDoor.Today
        lifecycleScope.launch {
            if (settingsRepository.settings.first().trackingEnabled) outings.start(planId)
        }
    }

    /** The widget's and the tile's "tap to resume" (PLANNING.md §7). */
    private fun resumeIfAsked(intent: Intent) {
        if (!intent.getBooleanExtra(TrackingControl.EXTRA_RESUME, false)) return
        intent.removeExtra(TrackingControl.EXTRA_RESUME)
        lifecycleScope.launch {
            if (!settingsRepository.settings.first().trackingEnabled) trackingControl.resume()
        }
    }

    private companion object {
        /** The readiness the widgets were last told of, in this process. */
        var toldReadiness: TrackingReadiness? = null
    }
}
