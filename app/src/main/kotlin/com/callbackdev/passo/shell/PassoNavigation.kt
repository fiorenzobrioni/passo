package com.callbackdev.passo.shell

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.callbackdev.passo.R
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.domain.calibration.CalibratedStep
import com.callbackdev.passo.core.model.WayId
import kotlinx.serialization.Serializable

/*
 * The shell's destinations as Navigation 3 keys, Chiaro's shape (its `ChiaroNavigation.kt`,
 * 28 Sep 2026). Until then the three tabs lived in an `AnimatedContent` behind a `BackHandler`,
 * and back from History or Insights swapped the page only when the finger lifted. As keys on a
 * back stack, `NavDisplay` runs that pop while the gesture is still under the finger, like every
 * other page's. Each key is [Serializable], so the stacks survive a rotation and process death.
 */

/** The three tabs, in the bottom bar's order. */
enum class ShellTab(@StringRes val label: Int, val icon: ImageVector) {
    TODAY(R.string.nav_today, PassoIcons.Today),
    HISTORY(R.string.nav_history, PassoIcons.History),
    INSIGHTS(R.string.nav_insights, PassoIcons.Trophy),
}

@Serializable
data object TodayKey : NavKey

@Serializable
data object HistoryKey : NavKey

@Serializable
data object InsightsKey : NavKey

/** Settings, from each tab's gear. */
@Serializable
data object SettingsKey : NavKey

/** The Outings page, from Today's button and from Settings (PLANNING.md §11 Phase 10). */
@Serializable
data object SessionsKey : NavKey

/** The guide, from the top of Settings and from Today's first-day card. */
@Serializable
data object GuideKey : NavKey

/** Measuring the walking or the running step, from Settings (PLANNING.md §11 Phase 7). */
@Serializable
data class CalibrationKey(val step: CalibratedStep) : NavKey

/** The editor of one outing; a new one when [planId] is null. */
@Serializable
data class PlanEditorKey(val planId: Long?) : NavKey

/** The Ways, from Insights and from a stage's notification (PLANNING.md §11 Phase 11). */
@Serializable
data object WaysKey : NavKey

/** One way: the journey [journeyId], or with none the way itself (under way, or ready to start). */
@Serializable
data class WayKey(val way: WayId, val journeyId: Long?) : NavKey

/** The root of [this] tab's stack. */
val ShellTab.root: NavKey
    get() = when (this) {
        ShellTab.TODAY -> TodayKey
        ShellTab.HISTORY -> HistoryKey
        ShellTab.INSIGHTS -> InsightsKey
    }

/**
 * One back stack per tab, Navigation 3's "multiple back stacks" recipe as Chiaro uses it. A tab
 * keeps its stack, and through the per-stack decorator its saved state (the week History was on,
 * how far Insights was scrolled), while another tab is on screen: the promise the old
 * `rememberSaveableStateHolder` kept.
 *
 * What is on screen is `[Today's stack] + [the selected tab's stack]` ("exit through home"): back
 * from History's or Insights' root lands on Today, back from Today's root leaves the app. The
 * same rule the old `BackHandler` kept, now with Today actually there for the gesture to reveal.
 */
@Stable
class PassoNavigationState(
    private val selectedName: MutableState<String>,
    val backStacks: Map<ShellTab, NavBackStack<NavKey>>,
) {
    /** The selected tab; saved by enum name, so it survives process death. */
    var selected: ShellTab
        get() = ShellTab.entries.firstOrNull { it.name == selectedName.value } ?: ShellTab.TODAY
        private set(value) {
            selectedName.value = value.name
        }

    private val currentStack: NavBackStack<NavKey>
        get() = backStacks.getValue(selected)

    /** The key on top of what is on screen. */
    val currentKey: NavKey?
        get() = currentStack.lastOrNull()

    /**
     * Whether the bottom bar is drawn: only on a tab's own page. Every page opened over a tab
     * (Settings, the guide, the outings, the calibration) covers the bar, as it always has.
     */
    val showsBottomBar: Boolean
        get() = currentStack.size == 1

    /**
     * Whether the move being animated is a tap on the bottom bar, which keeps Material's fade
     * through; every other move, back included, slides. Not state: it is read once, when
     * `NavDisplay` picks the transition, and must not recompose anything.
     */
    var lastMoveWasTabTap: Boolean = false
        private set

    /**
     * Opens [key] on top of the selected tab's stack. A second tap on the same door while the
     * first page is still arriving does not stack it twice.
     */
    fun navigate(key: NavKey) {
        lastMoveWasTabTap = false
        if (currentKey != key) currentStack.add(key)
    }

    /** Selects [tab] from the bar, keeping every other tab's stack as it was. Reselecting is a no-op. */
    fun switchTab(tab: ShellTab) {
        if (tab == selected) return
        lastMoveWasTabTap = true
        selected = tab
    }

    /**
     * Back: pops the selected tab's stack, or from a tab's root returns to Today. At Today's root
     * there is nothing to pop, and the system's back leaves the app.
     */
    fun goBack() {
        lastMoveWasTabTap = false
        if (currentStack.size > 1) {
            currentStack.removeAt(currentStack.lastIndex)
        } else if (selected != ShellTab.TODAY) {
            selected = ShellTab.TODAY
        }
    }

    /** The stacks on screen right now: Today's, then the selected tab's. */
    fun tabsInUse(): List<ShellTab> =
        if (selected == ShellTab.TODAY) listOf(ShellTab.TODAY) else listOf(ShellTab.TODAY, selected)
}

/**
 * The shell's navigation state, remembered. Every stack is a [rememberNavBackStack], so its
 * contents survive a configuration change and process death; the selected tab is a plain
 * saveable string, as it was when it was the `Tabs` enum.
 */
@Composable
fun rememberPassoNavigationState(): PassoNavigationState {
    val selectedName = rememberSaveable { mutableStateOf(ShellTab.TODAY.name) }
    val backStacks = ShellTab.entries.associateWith { tab -> rememberNavBackStack(tab.root) }
    return remember { PassoNavigationState(selectedName, backStacks) }
}

/**
 * Decorates each tab's entries with its own saveable-state holder, then flattens the stacks in
 * use into the list `NavDisplay` draws. Per stack rather than over the visible list is the point:
 * a hidden tab's entries stay decorated, so what they saved is still there when the reader comes
 * back. No ViewModel decorator, as before: the ViewModels stay scoped to the activity.
 */
@Composable
fun PassoNavigationState.rememberDecoratedEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val decorated = backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>()),
            entryProvider = entryProvider,
        )
    }
    return tabsInUse().flatMap { decorated.getValue(it) }
}
