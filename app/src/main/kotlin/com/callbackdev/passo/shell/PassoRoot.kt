package com.callbackdev.passo.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.tracking.TrackingReadiness
import com.callbackdev.passo.feature.guide.GuideRoute
import com.callbackdev.passo.feature.history.HistoryRoute
import com.callbackdev.passo.feature.history.HistoryTarget
import com.callbackdev.passo.feature.insights.InsightsRoute
import com.callbackdev.passo.feature.onboarding.NoSensorScreen
import com.callbackdev.passo.feature.onboarding.OnboardingRoute
import com.callbackdev.passo.feature.sessions.PlanEditorRoute
import com.callbackdev.passo.feature.sessions.SessionsRoute
import com.callbackdev.passo.feature.settings.SettingsRoute
import com.callbackdev.passo.feature.settings.calibration.CalibrationRoute
import com.callbackdev.passo.feature.today.TodayRoute
import com.callbackdev.passo.feature.ways.WayRoute
import com.callbackdev.passo.feature.ways.WaysRoute

/**
 * The shell (PLANNING.md §11 Phase 3). Three questions before a page: can this phone count at
 * all (no step counter is a dead end, explained), has the first run been through (the
 * onboarding), and which page: the three tabs under the bottom bar (Phase 5), or Settings
 * over them.
 *
 * @param onboardingCompleted null until the settings are read: a bare surface for that frame,
 *   because flashing the wrong screen at the reader is worse than one blank frame.
 * @param openWays a stage's notification was touched: the Ways page opens, once ([onWaysOpened]).
 */
@Composable
fun PassoRoot(
    readiness: TrackingReadiness,
    onboardingCompleted: Boolean?,
    openWays: Boolean = false,
    onWaysOpened: () -> Unit = {},
) {
    when {
        readiness == TrackingReadiness.NO_SENSOR -> NoSensorScreen()
        onboardingCompleted == null -> Surface(Modifier.fillMaxSize()) { }
        !onboardingCompleted -> OnboardingRoute()
        else -> MainPages(openWays, onWaysOpened)
    }
}

/**
 * The pages (Chiaro's shell shape, 28 Sep 2026): one `NavDisplay` over one back stack per tab
 * ([PassoNavigationState]), so every back, from a tab to Today included, is previewed under the
 * finger. A tap on the bar keeps Material's fade through; everything else wears the page
 * transition that predictive back seeks.
 *
 * The bar is drawn OVER the display rather than in a `Scaffold` around each tab, as in Chiaro: a
 * bar in the layout would reshape the pages each time it came or went. The tab pages already
 * scroll under the bar and leave its height free at the bottom ([bottomPadding]), measured from
 * the bar itself, so they are laid out to the pixel where the old `Scaffold` put them.
 */
@Composable
private fun MainPages(openWays: Boolean, onWaysOpened: () -> Unit) {
    val nav = rememberPassoNavigationState()
    LaunchedEffect(openWays) {
        if (openWays) {
            nav.navigate(WaysKey)
            onWaysOpened()
        }
    }
    val reduced = reducedMotion()
    val density = LocalDensity.current
    // A record in Insights opens its day, week or month in History, once.
    var historyTarget by remember { mutableStateOf<HistoryTarget?>(null) }
    // The bar's measured height, system inset included; until the first measure, Material's 80dp.
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var barHeight by remember { mutableStateOf(BAR_HEIGHT + inset) }

    val provider = entryProvider<NavKey> {
        val open = { key: NavKey -> nav.navigate(key) }
        entry<TodayKey> {
            TodayRoute(
                onOpenSettings = { open(SettingsKey) },
                onOpenSessions = { open(SessionsKey) },
                onOpenGuide = { open(GuideKey) },
                bottomPadding = barHeight,
            )
        }
        entry<HistoryKey> {
            HistoryRoute(
                onOpenSettings = { open(SettingsKey) },
                target = historyTarget,
                onTargetShown = { historyTarget = null },
                bottomPadding = barHeight,
            )
        }
        entry<InsightsKey> {
            InsightsRoute(
                onOpenSettings = { open(SettingsKey) },
                onOpenPeriod = { scale, date ->
                    historyTarget = HistoryTarget(scale, date)
                    nav.switchTab(ShellTab.HISTORY)
                },
                onOpenWays = { open(WaysKey) },
                bottomPadding = barHeight,
            )
        }
        entry<SettingsKey> {
            SettingsRoute(
                onBack = nav::goBack,
                onCalibrate = { open(CalibrationKey(it)) },
                onOpenSessions = { open(SessionsKey) },
                onOpenGuide = { open(GuideKey) },
            )
        }
        entry<GuideKey> { GuideRoute(onBack = nav::goBack) }
        entry<CalibrationKey> { key -> CalibrationRoute(step = key.step, onDone = nav::goBack) }
        entry<SessionsKey> {
            SessionsRoute(onBack = nav::goBack, onEdit = { open(PlanEditorKey(it)) }, onOpenWays = { open(WaysKey) })
        }
        entry<PlanEditorKey> { key -> PlanEditorRoute(planId = key.planId, onDone = nav::goBack) }
        entry<WaysKey> { WaysRoute(onBack = nav::goBack, onOpenWay = { way, journey -> open(WayKey(way, journey)) }) }
        entry<WayKey> { key -> WayRoute(way = key.way, journeyId = key.journeyId, onBack = nav::goBack) }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                entries = nav.rememberDecoratedEntries(provider),
                modifier = Modifier.fillMaxSize(),
                onBack = nav::goBack,
                transitionSpec = { if (nav.lastMoveWasTabTap) fadeThrough(reduced) else forward(reduced) },
                popTransitionSpec = { if (nav.lastMoveWasTabTap) fadeThrough(reduced) else backward(reduced) },
                predictivePopTransitionSpec = { backward(reduced) },
            )
            AnimatedVisibility(
                visible = nav.showsBottomBar,
                enter = if (reduced) fadeIn(PassoMotion.fade()) else slideInVertically(tween(NAV_MILLIS)) { it },
                exit = if (reduced) fadeOut(PassoMotion.fade()) else slideOutVertically(tween(NAV_MILLIS)) { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                NavigationBar(
                    modifier = Modifier.onSizeChanged { size ->
                        if (size.height > 0) barHeight = with(density) { size.height.toDp() }
                    },
                    // On an open foldable the three tabs stand over the page's column, not
                    // spread across the inner screen; the bar's ground still spans it.
                    windowInsets = NavigationBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                ) {
                    ShellTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = item == nav.selected,
                            onClick = { nav.switchTab(item) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            modifier = Modifier.testTag("tab_${item.name.lowercase()}"),
                        )
                    }
                }
            }
        }
    }
}

/** Material's navigation bar height, before the bar has been measured once. */
private val BAR_HEIGHT = 80.dp

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
