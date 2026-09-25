package com.callbackdev.passo.shell

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.passo.R
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.tracking.TrackingReadiness
import com.callbackdev.passo.feature.history.HistoryRoute
import com.callbackdev.passo.feature.history.HistoryTarget
import com.callbackdev.passo.feature.insights.InsightsRoute
import com.callbackdev.passo.feature.onboarding.NoSensorScreen
import com.callbackdev.passo.feature.onboarding.OnboardingRoute
import com.callbackdev.passo.feature.settings.SettingsRoute
import com.callbackdev.passo.feature.today.TodayRoute
import kotlinx.serialization.Serializable

/** The three tabs, Today, History and Insights, under one bottom bar. */
@Serializable
data object TabsKey : NavKey

/** Settings, from each tab's gear. */
@Serializable
data object SettingsKey : NavKey

/**
 * The shell (PLANNING.md §11 Phase 3). Three questions before a page: can this phone count at
 * all (no step counter is a dead end, explained), has the first run been through (the
 * onboarding), and which page: the three tabs under the bottom bar (Phase 5), or Settings
 * over them.
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
    val backStack = rememberNavBackStack(TabsKey)
    val reduced = reducedMotion()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
            transitionSpec = { forward(reduced) },
            popTransitionSpec = { backward(reduced) },
            predictivePopTransitionSpec = { backward(reduced) },
            entryProvider = entryProvider {
                entry<TabsKey> { Tabs(onOpenSettings = { backStack.add(SettingsKey) }) }
                entry<SettingsKey> {
                    SettingsRoute(onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) })
                }
            },
        )
    }
}

private enum class Tab(@StringRes val label: Int, val icon: ImageVector) {
    TODAY(R.string.nav_today, PassoIcons.Today),
    HISTORY(R.string.nav_history, PassoIcons.History),
    INSIGHTS(R.string.nav_insights, PassoIcons.Trophy),
}

/**
 * Today, History and Insights under Material's bottom bar. Each tab keeps its own place (the
 * week History was on, how far Insights was scrolled) while another is shown. Back from History
 * or Insights goes to Today, the app's first page, and from Today leaves the app. A record in
 * Insights opens its day, week or month in History.
 */
@Composable
private fun Tabs(onOpenSettings: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var historyTarget by remember { mutableStateOf<HistoryTarget?>(null) }
    val saveable = rememberSaveableStateHolder()
    val reduced = reducedMotion()
    BackHandler(enabled = tab != Tab.TODAY) { tab = Tab.TODAY }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                        modifier = Modifier.testTag("tab_${item.name.lowercase()}"),
                    )
                }
            }
        },
    ) { padding ->
        val bottom = padding.calculateBottomPadding()
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeThrough(reduced) },
            label = "tab",
        ) { current ->
            saveable.SaveableStateProvider(current.name) {
                when (current) {
                    Tab.TODAY -> TodayRoute(onOpenSettings = onOpenSettings, bottomPadding = bottom)

                    Tab.HISTORY -> HistoryRoute(
                        onOpenSettings = onOpenSettings,
                        target = historyTarget,
                        onTargetShown = { historyTarget = null },
                        bottomPadding = bottom,
                    )

                    Tab.INSIGHTS -> InsightsRoute(
                        onOpenSettings = onOpenSettings,
                        onOpenPeriod = { scale, date ->
                            historyTarget = HistoryTarget(scale, date)
                            tab = Tab.HISTORY
                        },
                        bottomPadding = bottom,
                    )
                }
            }
        }
    }
}

/*
 * Material's fade through between tabs: the page going out fades in 90 ms, the one coming in
 * fades and grows from 92% in 210 ms after it, so the two are never read together. Under
 * reduced motion, the 100 ms fade.
 */
private fun fadeThrough(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(PassoMotion.fade()) togetherWith fadeOut(PassoMotion.fade())
    val enter = tween<Float>(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS, easing = LinearOutSlowInEasing)
    return (fadeIn(enter) + scaleIn(enter, initialScale = 0.92f)) togetherWith
        fadeOut(tween(FADE_OUT_MILLIS, easing = FastOutLinearInEasing))
}

private const val FADE_OUT_MILLIS = 90
private const val FADE_IN_MILLIS = 210

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
