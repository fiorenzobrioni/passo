package com.callbackdev.passo.shell

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.tracking.TrackingReadiness
import com.callbackdev.passo.feature.onboarding.NoSensorScreen
import com.callbackdev.passo.feature.onboarding.OnboardingRoute
import com.callbackdev.passo.feature.settings.SettingsRoute
import com.callbackdev.passo.feature.today.TodayRoute
import kotlinx.serialization.Serializable

/** Today, the app's one tab until History and Insights arrive (Phase 5). */
@Serializable
data object TodayKey : NavKey

/** Settings, from Today's gear. */
@Serializable
data object SettingsKey : NavKey

/**
 * The shell (PLANNING.md §11 Phase 3). Three questions before a page: can this phone count at
 * all (no step counter is a dead end, explained), has the first run been through (the
 * onboarding), and which page. The bottom bar arrives with its second tab in Phase 5: a bar
 * with two tabs that lead nowhere would be the screen lying about the app.
 *
 * @param onboardingCompleted null until the settings are read: a bare surface for that frame,
 *   because flashing the wrong screen at the reader is worse than one blank frame.
 */
@Composable
fun PassoRoot(readiness: TrackingReadiness, onboardingCompleted: Boolean?) {
    when {
        readiness == TrackingReadiness.NO_SENSOR -> NoSensorScreen()
        onboardingCompleted == null -> Surface(Modifier.fillMaxSize()) { }
        !onboardingCompleted -> OnboardingRoute()
        else -> MainPages()
    }
}

@Composable
private fun MainPages() {
    val backStack = rememberNavBackStack(TodayKey)
    val reduced = reducedMotion()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            transitionSpec = { forward(reduced) },
            popTransitionSpec = { backward(reduced) },
            predictivePopTransitionSpec = { backward(reduced) },
            entryProvider = entryProvider {
                entry<TodayKey> { TodayRoute(onOpenSettings = { backStack.add(SettingsKey) }) }
                entry<SettingsKey> {
                    SettingsRoute(onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) })
                }
            },
        )
    }
}

/*
 * Chiaro's page transition, value for value (itself Saldo's): 300 ms, the incoming page sliding
 * a sixth of the width and fading in, the outgoing one a sixth the other way. A tween rather
 * than a spring because predictive back seeks it with the finger, and a seek needs a curve of
 * known length. Reduced motion: the 100 ms fade.
 */
private const val NAV_MILLIS = 300
private const val SLIDE_DIVISOR = 6

private fun forward(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(PassoMotion.fade()) togetherWith fadeOut(PassoMotion.fade())
    val curve = tween<Float>(NAV_MILLIS, easing = FastOutSlowInEasing)
    return (
        fadeIn(curve) + slideInHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) {
            it / SLIDE_DIVISOR
        }
        ) togetherWith
        (fadeOut(curve) + slideOutHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { -it / SLIDE_DIVISOR })
}

private fun backward(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(PassoMotion.fade()) togetherWith fadeOut(PassoMotion.fade())
    val curve = tween<Float>(NAV_MILLIS, easing = FastOutSlowInEasing)
    return (
        fadeIn(curve) + slideInHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) {
            -it / SLIDE_DIVISOR
        }
        ) togetherWith
        (fadeOut(curve) + slideOutHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { it / SLIDE_DIVISOR })
}
