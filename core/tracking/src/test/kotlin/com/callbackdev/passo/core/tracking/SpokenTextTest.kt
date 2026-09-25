package com.callbackdev.passo.core.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.sessions.PaceVerdict
import com.callbackdev.passo.core.domain.sessions.SessionAmount
import com.callbackdev.passo.core.domain.sessions.SessionAnnouncement
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** What an outing says aloud: every amount in words, in the app's language. */
@RunWith(AndroidJUnit4::class)
class SpokenTextTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val walk = Session(
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
        totals = SessionTotals(steps = 2_140, movingMillis = 20 * 60_000L, zoneMillis = 17 * 60_000L),
    )

    private fun say(announcement: SessionAnnouncement, session: Session = walk) =
        context.spoken(announcement, session, UnitPreference.METRIC)

    @Test
    @Config(qualifiers = "en-rUS")
    fun `in English`() {
        assertThat(
            say(SessionAnnouncement.started(walk)),
        ).isEqualTo("Brisk walk: off you go. 20 minutes at a brisk pace.")
        val half = SessionAnnouncement.Milestone(
            SessionMilestone.HALF,
            SessionAmount(SessionGoalKind.TIME, 10.0),
            108,
            PaceVerdict.ON_PACE,
        )
        assertThat(say(half)).isEqualTo("Halfway. 10 minutes to go. 108 steps a minute: on pace.")
        val slow = half.copy(
            milestone = SessionMilestone.THREE_QUARTERS,
            left = SessionAmount(SessionGoalKind.TIME, 5.0),
            cadence = 92,
            pace = PaceVerdict.BELOW,
        )
        assertThat(
            say(slow),
        ).isEqualTo("Three quarters done. 5 minutes to go. 92 steps a minute: pick up the pace a little.")
        assertThat(
            say(SessionAnnouncement.goal(walk)),
        ).isEqualTo("Goal reached: 20 minutes, 2,140 steps. 17 of 20 minutes at your pace. Well done.")
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `the goal says what happened, and a steps goal its steps once`() {
        val kept = walk.copy(totals = walk.totals.copy(zoneMillis = 19 * 60_000L + 30_000))
        assertThat(say(SessionAnnouncement.goal(kept, dayGoalReached = true))).isEqualTo(
            "Goal reached: 20 minutes, 2,140 steps. Almost all of it at your pace. Today’s goal is reached too. Well done.",
        )
        val rest = walk.copy(
            goalKind = SessionGoalKind.STEPS,
            goalValue = 2_100,
            restOfDay = true,
            intensity = SessionIntensity.FREE,
        )
        assertThat(say(SessionAnnouncement.goal(rest)))
            .isEqualTo("Goal reached: 2,140 steps. Today’s goal is reached too. Well done.")
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `distances and steps in words`() {
        val km = SessionAnnouncement.Milestone(
            SessionMilestone.QUARTER,
            SessionAmount(SessionGoalKind.DISTANCE, 2_250.0),
            null,
            PaceVerdict.NONE,
        )
        assertThat(say(km)).isEqualTo("A quarter done. 2.25 kilometres to go.")
        val steps = km.copy(left = SessionAmount(SessionGoalKind.STEPS, 1_200.0))
        assertThat(say(steps)).isEqualTo("A quarter done. 1,200 steps to go.")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian, with the verb agreeing`() {
        assertThat(
            say(SessionAnnouncement.started(walk)),
        ).isEqualTo("Camminata svelta: si parte. 20 minuti a passo svelto.")
        val one = SessionAnnouncement.Milestone(
            SessionMilestone.THREE_QUARTERS,
            SessionAmount(SessionGoalKind.TIME, 1.0),
            104,
            PaceVerdict.ON_PACE,
        )
        assertThat(say(one)).isEqualTo("Tre quarti fatti. Manca 1 minuto. 104 passi al minuto: sei a ritmo.")
        val ten = one.copy(milestone = SessionMilestone.HALF, left = SessionAmount(SessionGoalKind.TIME, 10.0))
        assertThat(say(ten)).isEqualTo("Metà strada. Mancano 10 minuti. 104 passi al minuto: sei a ritmo.")
        assertThat(
            say(SessionAnnouncement.goal(walk)),
        ).isEqualTo("Obiettivo raggiunto: 20 minuti, 2.140 passi. 17 su 20 minuti al tuo ritmo. Ben fatto.")
        val slow = ten.copy(cadence = 92, pace = PaceVerdict.BELOW)
        assertThat(say(slow)).isEqualTo("Metà strada. Mancano 10 minuti. 92 passi al minuto: accelera un po’.")
    }
}
