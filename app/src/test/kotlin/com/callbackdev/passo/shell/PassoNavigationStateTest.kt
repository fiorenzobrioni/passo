package com.callbackdev.passo.shell

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.callbackdev.passo.core.domain.calibration.CalibratedStep
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The shell's back stacks, for Passo's three tabs. The
 * animation is a device check; what can be checked here is what back DOES: the page each gesture
 * lands on, the tab it leaves the reader in, and when the bar is drawn.
 */
@RunWith(RobolectricTestRunner::class)
class PassoNavigationStateTest {
    private fun state() = PassoNavigationState(
        selectedName = mutableStateOf(ShellTab.TODAY.name),
        backStacks = ShellTab.entries.associateWith { NavBackStack<NavKey>(it.root) },
    )

    /** What `NavDisplay` is handed: the stacks in use, flattened. */
    private fun PassoNavigationState.onScreen(): List<NavKey> = tabsInUse().flatMap { backStacks.getValue(it) }

    @Test
    fun `a fresh shell shows Today alone, with the bar`() {
        val nav = state()
        assertThat(nav.onScreen()).containsExactly(TodayKey)
        assertThat(nav.showsBottomBar).isTrue()
    }

    @Test
    fun `back at Today's root is left to the system`() {
        val nav = state()
        nav.goBack()
        assertThat(nav.selected).isEqualTo(ShellTab.TODAY)
        assertThat(nav.onScreen()).containsExactly(TodayKey)
    }

    @Test
    fun `a tab sits over Today, so back from its root reveals Today`() {
        val nav = state()
        nav.switchTab(ShellTab.HISTORY)
        assertThat(nav.onScreen()).containsExactly(TodayKey, HistoryKey).inOrder()
        assertThat(nav.lastMoveWasTabTap).isTrue()

        nav.goBack()
        assertThat(nav.selected).isEqualTo(ShellTab.TODAY)
        assertThat(nav.onScreen()).containsExactly(TodayKey)
        assertThat(nav.lastMoveWasTabTap).isFalse()
    }

    @Test
    fun `from History to Insights, Today stays underneath`() {
        val nav = state()
        nav.switchTab(ShellTab.HISTORY)
        nav.switchTab(ShellTab.INSIGHTS)
        assertThat(nav.onScreen()).containsExactly(TodayKey, InsightsKey).inOrder()
        nav.goBack()
        assertThat(nav.selected).isEqualTo(ShellTab.TODAY)
    }

    @Test
    fun `settings covers the bar, and back returns to the tab it was opened from`() {
        val nav = state()
        nav.switchTab(ShellTab.INSIGHTS)
        nav.navigate(SettingsKey)
        assertThat(nav.onScreen()).containsExactly(TodayKey, InsightsKey, SettingsKey).inOrder()
        assertThat(nav.showsBottomBar).isFalse()

        nav.goBack()
        assertThat(nav.selected).isEqualTo(ShellTab.INSIGHTS)
        assertThat(nav.currentKey).isEqualTo(InsightsKey)
        assertThat(nav.showsBottomBar).isTrue()
    }

    @Test
    fun `pages opened from Settings go back through Settings`() {
        val nav = state()
        nav.navigate(SettingsKey)
        nav.navigate(CalibrationKey(CalibratedStep.WALKING))
        nav.goBack()
        assertThat(nav.currentKey).isEqualTo(SettingsKey)
        nav.navigate(GuideKey)
        nav.goBack()
        nav.goBack()
        assertThat(nav.onScreen()).containsExactly(TodayKey)
    }

    @Test
    fun `a tab keeps its own stack while another is shown`() {
        val nav = state()
        nav.navigate(SessionsKey)
        nav.navigate(PlanEditorKey(null))
        // Only reachable through the bar, which the pages above cover: here straight to the state.
        nav.goBack()
        nav.goBack()
        nav.switchTab(ShellTab.HISTORY)
        nav.switchTab(ShellTab.TODAY)
        assertThat(nav.onScreen()).containsExactly(TodayKey)
    }

    @Test
    fun `reselecting the tab on screen does nothing`() {
        val nav = state()
        nav.switchTab(ShellTab.TODAY)
        assertThat(nav.lastMoveWasTabTap).isFalse()
        assertThat(nav.onScreen()).containsExactly(TodayKey)
    }

    @Test
    fun `a second tap on the same door does not stack the page twice`() {
        val nav = state()
        nav.navigate(SettingsKey)
        nav.navigate(SettingsKey)
        assertThat(nav.onScreen()).containsExactly(TodayKey, SettingsKey).inOrder()
    }
}
