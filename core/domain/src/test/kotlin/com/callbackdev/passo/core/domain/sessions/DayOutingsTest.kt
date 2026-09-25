package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.domain.walks.WalkType
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class DayOutingsTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 9, 25)
    private fun millisAt(hour: Int, minute: Int) = day.atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun walk(from: Int, to: Int) = Walk(from, to, (to - from) * 100, 0.0, 0.0, 100, WalkType.WALK)

    private fun session(startMillis: Long, endMillis: Long?) = Session(
        planId = null,
        name = null,
        goalKind = SessionGoalKind.TIME,
        goalValue = 20,
        intensity = SessionIntensity.BRISK,
        milestones = emptySet(),
        vibrate = true,
        localEpochDay = day.toEpochDay(),
        startedAtMillis = startMillis,
        state = if (endMillis == null) SessionState.ACTIVE else SessionState.FINISHED,
        endedAtMillis = endMillis,
    )

    @Test
    fun `an outing replaces the walk found in its minutes`() {
        val morning = walk(8 * 60, 8 * 60 + 25)
        val evening = walk(18 * 60 + 2, 18 * 60 + 24)
        val outing = session(millisAt(18, 0), millisAt(18, 20))
        val list = DayOutings.of(listOf(morning, evening), listOf(outing), zone, millisAt(20, 0))
        assertThat(list).hasSize(2)
        assertThat((list[0] as Outing.Detected).walk).isEqualTo(morning)
        val planned = list[1] as Outing.Planned
        assertThat(planned.startMinute).isEqualTo(18 * 60)
        assertThat(planned.endMinute).isEqualTo(18 * 60 + 20)
    }

    @Test
    fun `an outing under way reaches now`() {
        val outing = session(millisAt(9, 0), null)
        val planned = DayOutings.of(null, listOf(outing), zone, millisAt(9, 12)).single() as Outing.Planned
        assertThat(planned.endMinute).isEqualTo(9 * 60 + 12)
    }

    @Test
    fun `with walk detection off, only the outings are listed`() {
        val outing = session(millisAt(9, 0), millisAt(9, 30))
        assertThat(DayOutings.of(null, listOf(outing), zone, millisAt(10, 0))).hasSize(1)
    }
}
