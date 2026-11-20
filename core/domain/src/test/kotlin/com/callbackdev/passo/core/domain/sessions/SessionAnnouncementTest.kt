package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
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
    fun `it speaks through headphones, and out loud only when asked and not silenced`() {
        assertThat(SpeechRoute.speaks(SessionVoice.OFF, headphones = true, ringerNormal = true)).isFalse()
        assertThat(SpeechRoute.speaks(SessionVoice.HEADPHONES, headphones = true, ringerNormal = false)).isTrue()
        assertThat(SpeechRoute.speaks(SessionVoice.HEADPHONES, headphones = false, ringerNormal = true)).isFalse()
        assertThat(SpeechRoute.speaks(SessionVoice.ALWAYS, headphones = false, ringerNormal = true)).isTrue()
        assertThat(SpeechRoute.speaks(SessionVoice.ALWAYS, headphones = false, ringerNormal = false)).isFalse()
        assertThat(SpeechRoute.speaks(SessionVoice.ALWAYS, headphones = true, ringerNormal = false)).isTrue()
    }
}
