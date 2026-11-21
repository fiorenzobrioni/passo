package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMarks
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionAnnouncementTest {
    private val session = Session(
        planId = 1,
        name = null,
        goalKind = SessionGoalKind.TIME,
        goalValue = 20,
        intensity = SessionIntensity.BRISK,
        milestones = setOf(SessionMilestone.HALF),
        vibrate = true,
        voice = SessionVoice.HEADPHONES,
        localEpochDay = 0,
        startedAtMillis = 0,
        totals = SessionTotals(steps = 1_050, movingMillis = 10 * 60_000L + 5_000, zoneMillis = 9 * 60_000L),
    )

    @Test
    fun `the start says what the outing is`() {
        assertThat(SessionAnnouncement.started(session))
            .isEqualTo(
                SessionAnnouncement.Started(SessionAmount(SessionGoalKind.TIME, 20.0), SessionIntensity.BRISK, false),
            )
    }

    @Test
    fun `a milestone says what is left and how the pace is going`() {
        val half = SessionAnnouncement.milestone(session, SessionMilestone.HALF, cadence = 108)
        assertThat(half).isEqualTo(
            SessionAnnouncement.Milestone(
                SessionMilestone.HALF,
                SessionAmount(SessionGoalKind.TIME, 10.0),
                108,
                PaceVerdict.ON_PACE,
            ),
        )
        val slow = SessionAnnouncement.milestone(
            session,
            SessionMilestone.HALF,
            cadence = 92,
        ) as SessionAnnouncement.Milestone
        assertThat(slow.pace).isEqualTo(PaceVerdict.BELOW)
        val early = SessionAnnouncement.milestone(
            session,
            SessionMilestone.HALF,
            cadence = null,
        ) as SessionAnnouncement.Milestone
        assertThat(early.pace).isEqualTo(PaceVerdict.NONE)
    }

    @Test
    fun `a free outing has no pace to judge`() {
        assertThat(SessionAnnouncement.verdict(SessionIntensity.FREE, 70)).isEqualTo(PaceVerdict.NONE)
        assertThat(SessionAnnouncement.verdict(SessionIntensity.RUN, 150)).isEqualTo(PaceVerdict.ON_PACE)
    }

    @Test
    fun `the goal says what it came to, from what happened`() {
        val goal = SessionAnnouncement.milestone(session, SessionMilestone.GOAL, cadence = 108)
        assertThat(goal).isEqualTo(
            SessionAnnouncement.GoalReached(
                SessionAmount(SessionGoalKind.TIME, 20.0),
                1_050,
                PaceSummary.Part(9, 10),
                false,
            ),
        )
        val kept = session.copy(totals = session.totals.copy(zoneMillis = session.totals.movingMillis - 30_000))
        assertThat(SessionAnnouncement.goal(kept).pace).isEqualTo(PaceSummary.Mostly)
        assertThat(
            SessionAnnouncement.goal(session.copy(intensity = SessionIntensity.FREE)).pace,
        ).isEqualTo(PaceSummary.None)
        assertThat(SessionAnnouncement.goal(session.copy(restOfDay = true)).dayGoalReached).isTrue()
    }

    @Test
    fun `the day's goal is news only when this outing brought it`() {
        assertThat(
            SessionAnnouncement.broughtDayGoal(todaySteps = 8_300, sessionSteps = 2_000, dailyGoalSteps = 8_000),
        ).isTrue()
        assertThat(
            SessionAnnouncement.broughtDayGoal(todaySteps = 9_500, sessionSteps = 1_000, dailyGoalSteps = 8_000),
        ).isFalse()
        assertThat(
            SessionAnnouncement.broughtDayGoal(todaySteps = 7_000, sessionSteps = 2_000, dailyGoalSteps = 8_000),
        ).isFalse()
    }

    @Test
    fun `an open outing starts with no goal to say`() {
        val open = session.copy(goalKind = SessionGoalKind.OPEN, goalValue = 0, intensity = SessionIntensity.FREE)
        assertThat(SessionAnnouncement.started(open))
            .isEqualTo(SessionAnnouncement.Started(null, SessionIntensity.FREE, false))
    }

    @Test
    fun `an open outing stopped says what it came to, a goal outing stopped says nothing`() {
        val open = session.copy(
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            totals = SessionTotals(steps = 4_100, movingMillis = 41 * 60_000L + 20_000, distanceMeters = 3_210.0),
        )
        assertThat(SessionAnnouncement.openStopped(open))
            .isEqualTo(SessionAnnouncement.OpenStopped(41, 3_210.0, measured = false, paceSecondsPerKm = null))
        assertThat(SessionAnnouncement.openStopped(session)).isNull()

        val measured = open.copy(gps = true, measuredShare = 0.97)
        val said = checkNotNull(SessionAnnouncement.openStopped(measured))
        assertThat(said.measured).isTrue()
        assertThat(said.paceSecondsPerKm).isNotNull()
        // A start pressed and stopped at once still says a minute, not none.
        assertThat(SessionAnnouncement.openStopped(open.copy(totals = SessionTotals(steps = 40)))?.movingMinutes)
            .isEqualTo(1)
    }

    @Test
    fun `an estimated kilometre says about, its time in motion, and no pace`() {
        val open = session.copy(
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            marks = SessionMarks.DISTANCE,
            totals = SessionTotals(steps = 2_900, movingMillis = 23 * 60_000L + 40_000, distanceMeters = 2_030.0),
        )
        assertThat(SessionAnnouncement.mark(open, 2)).isEqualTo(
            SessionAnnouncement.DistanceMark(count = 2, miles = false, movingMinutes = 24, measured = false),
        )
    }

    @Test
    fun `a measured kilometre says the pace of that kilometre, not the walk's`() {
        // Two kilometres from twenty legs: the first at 11 minutes, the second at 9.
        val legs = (0 until 20).map { index ->
            OutingLeg(
                index = index,
                meters = 100.0,
                movingMillis = if (index < 10) 66_000L else 54_000L,
                steps = 140.0,
                endedAtMillis = (index + 1) * 60_000L,
            )
        }
        val open = session.copy(
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            marks = SessionMarks.DISTANCE,
            gps = true,
            measuredShare = 0.98,
            legs = legs,
            totals = SessionTotals(steps = 2_800, movingMillis = 20 * 60_000L + 30_000, distanceMeters = 2_000.0),
        )
        val said = SessionAnnouncement.mark(open, 2) as SessionAnnouncement.DistanceMark
        assertThat(said.measured).isTrue()
        assertThat(checkNotNull(said.paceSecondsPerKm)).isWithin(0.5).of(540.0)
        // The time to the mark is its legs', not the walk's: twenty minutes, the half minute since not in it.
        assertThat(said.movingMinutes).isEqualTo(20)
        // Under nine tenths measured, an estimate: no pace.
        val partly = SessionAnnouncement.mark(open.copy(measuredShare = 0.7), 2) as SessionAnnouncement.DistanceMark
        assertThat(partly.measured).isFalse()
        assertThat(partly.paceSecondsPerKm).isNull()
    }

    @Test
    fun `a time mark says its ten minutes and the steps, and an outing with no marks says nothing`() {
        val open = session.copy(goalKind = SessionGoalKind.OPEN, goalValue = 0, marks = SessionMarks.TIME)
        assertThat(SessionAnnouncement.mark(open, 3)).isEqualTo(SessionAnnouncement.TimeMark(30, 1_050))
        assertThat(SessionAnnouncement.mark(session, 1)).isNull()
    }

    @Test
    fun `it speaks through headphones, and out loud only when asked and not silenced`() {
        assertThat(SpeechRoute.speaks(SessionVoice.OFF, headphones = true, ringerNormal = true)).isFalse()
        assertThat(SpeechRoute.speaks(SessionVoice.HEADPHONES, headphones = true, ringerNormal = false)).isTrue()
        assertThat(SpeechRoute.speaks(SessionVoice.HEADPHONES, headphones = false, ringerNormal = true)).isFalse()
        assertThat(SpeechRoute.speaks(SessionVoice.ALWAYS, headphones = false, ringerNormal = true)).isTrue()
        assertThat(SpeechRoute.speaks(SessionVoice.ALWAYS, headphones = false, ringerNormal = false)).isFalse()
        assertThat(SpeechRoute.speaks(SessionVoice.ALWAYS, headphones = true, ringerNormal = false)).isTrue()
    }
}
