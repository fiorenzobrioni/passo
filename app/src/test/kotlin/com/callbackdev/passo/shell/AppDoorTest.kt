package com.callbackdev.passo.shell

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.tracking.AppDoors
import com.callbackdev.passo.core.tracking.SessionControl
import com.callbackdev.passo.core.tracking.openingToday
import com.callbackdev.passo.feature.history.HistoryTarget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * The doors into the app from outside it (PLANNING.md §11 Phase 23, step 23.1): what each touch's
 * intent asks for, read once, and the pages it then leaves on screen.
 */
@RunWith(RobolectricTestRunner::class)
class AppDoorTest {
    private fun state() = PassoNavigationState(
        selectedName = mutableStateOf(ShellTab.TODAY.name),
        backStacks = ShellTab.entries.associateWith { NavBackStack<NavKey>(it.root) },
    )

    private fun PassoNavigationState.onScreen(): List<NavKey> = tabsInUse().flatMap { backStacks.getValue(it) }

    /** The reader left the app deep in another tab, over a page of Today's too. */
    private fun leftDeep() = state().apply {
        navigate(SettingsKey)
        switchTab(ShellTab.INSIGHTS)
        navigate(WaysKey())
        navigate(WayKey(WayId.CAMINO_FRANCES, null))
    }

    @Test
    fun `each surface's extras are read as the page they name`() {
        assertThat(AppDoor.take(Intent().openingToday())).isEqualTo(AppDoor.Today)
        assertThat(AppDoor.take(Intent().putExtra(SessionControl.EXTRA_OPEN_OUTING, 7L)))
            .isEqualTo(AppDoor.Outing(7L))
        assertThat(AppDoor.take(Intent().putExtra(AppDoors.EXTRA_OPEN_WEEK, LocalDate.of(2026, 11, 9).toEpochDay())))
            .isEqualTo(AppDoor.Week(LocalDate.of(2026, 11, 9)))
        val way = Intent()
            .putExtra(AppDoors.EXTRA_OPEN_WAY, WayId.VIA_FRANCIGENA.name)
            .putExtra(AppDoors.EXTRA_OPEN_JOURNEY, 42L)
        assertThat(AppDoor.take(way)).isEqualTo(AppDoor.Way(WayId.VIA_FRANCIGENA, 42L))
    }

    @Test
    fun `the launcher's own intent, and an unknown way, ask for nothing`() {
        assertThat(AppDoor.take(Intent(Intent.ACTION_MAIN))).isNull()
        // A way a later version wrote, read by this one: nothing, rather than a wrong page.
        assertThat(AppDoor.take(Intent().putExtra(AppDoors.EXTRA_OPEN_WAY, "NOT_A_WAY"))).isNull()
        // Any app may start the launcher's activity: a day no calendar holds opens nothing, and
        // takes nothing down.
        assertThat(AppDoor.take(Intent().putExtra(AppDoors.EXTRA_OPEN_WEEK, Long.MAX_VALUE))).isNull()
    }

    @Test
    fun `a touch is answered once - its extras are taken off`() {
        val intent = Intent().openingToday().putExtra(SessionControl.EXTRA_OPEN_OUTING, 7L)
        assertThat(AppDoor.take(intent)).isEqualTo(AppDoor.Outing(7L))
        assertThat(intent.hasExtra(AppDoors.EXTRA_OPEN_TODAY)).isFalse()
        assertThat(intent.hasExtra(SessionControl.EXTRA_OPEN_OUTING)).isFalse()
        assertThat(AppDoor.take(intent)).isNull()
    }

    @Test
    fun `an intent Android hands back from Recents is not a touch`() {
        assertThat(Intent().openingToday().isFreshTouch).isTrue()
        val again = Intent().openingToday().addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        assertThat(again.isFreshTouch).isFalse()
    }

    @Test
    fun `Today's door opens Today itself, wherever the app was left`() {
        val nav = leftDeep()
        assertThat(nav.open(AppDoor.Today)).isNull()
        assertThat(nav.onScreen()).containsExactly(TodayKey)
        assertThat(nav.showsBottomBar).isTrue()
        // The other tab is as it was, as a tap on the bar keeps it.
        nav.switchTab(ShellTab.INSIGHTS)
        assertThat(nav.currentKey).isEqualTo(WayKey(WayId.CAMINO_FRANCES, null))
    }

    @Test
    fun `an outing's door opens its page over Today, and back lands on Today`() {
        val nav = leftDeep()
        nav.open(AppDoor.Outing(7L))
        assertThat(nav.onScreen()).containsExactly(TodayKey, OutingKey(7L)).inOrder()
        nav.goBack()
        assertThat(nav.onScreen()).containsExactly(TodayKey)
    }

    @Test
    fun `a way's door opens that way over Today`() {
        val nav = leftDeep()
        nav.open(AppDoor.Way(WayId.VIA_FRANCIGENA, 42L))
        assertThat(nav.onScreen()).containsExactly(TodayKey, WayKey(WayId.VIA_FRANCIGENA, 42L)).inOrder()
    }

    @Test
    fun `Passo's words before the location's request stay over the page a door opens`() {
        val nav = state()
        nav.navigate(SessionsKey)
        nav.navigate(GpsIntroKey)

        nav.open(AppDoor.Today)

        // The outing waiting on the answer is not left unstarted behind a page that cleared them.
        assertThat(nav.onScreen()).containsExactly(TodayKey, GpsIntroKey).inOrder()
    }

    @Test
    fun `a week's door opens History's own page on that week, with Today beneath`() {
        val nav = leftDeep()
        nav.switchTab(ShellTab.HISTORY)
        nav.navigate(OutingKey(3L))

        val target = nav.open(AppDoor.Week(LocalDate.of(2026, 11, 9)))

        assertThat(target).isEqualTo(HistoryTarget(PeriodScale.WEEK, LocalDate.of(2026, 11, 9)))
        assertThat(nav.onScreen()).containsExactly(TodayKey, HistoryKey).inOrder()
        assertThat(nav.showsBottomBar).isTrue()
        nav.goBack()
        assertThat(nav.onScreen()).containsExactly(TodayKey)
    }
}
