package com.callbackdev.passo.core.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.sessions.PaceVerdict
import com.callbackdev.passo.core.domain.sessions.SessionAmount
import com.callbackdev.passo.core.domain.sessions.SessionAnnouncement
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
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

    @Test
    @Config(qualifiers = "en-rUS")
    fun `an end by stillness says what was walked, and that it can be resumed`() {
        val still = walk.copy(totals = walk.totals.copy(steps = 764))
        assertThat(say(SessionAnnouncement.endedStill(still), still))
            .isEqualTo("Outing ended after a long stop, at 764 steps. You can resume it from the notification.")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian, an end by stillness`() {
        val still = walk.copy(totals = walk.totals.copy(steps = 764))
        assertThat(say(SessionAnnouncement.endedStill(still), still))
            .isEqualTo("Uscita chiusa dopo una lunga sosta, a 764 passi. Puoi riprenderla dalla notifica.")
    }

    private val milan = Ways.of(WayId.MILAN_DUOMO_NAVIGLI)

    private fun milanWalk(from: Int, walked: Double) = checkNotNull(
        SessionPlans.startWalk(milan, from, SessionVoice.HEADPHONES, nowMillis = 0, localEpochDay = 0),
    ).copy(totals = SessionTotals(steps = 2_140, distanceMeters = walked))

    @Test
    @Config(qualifiers = "en-rUS")
    fun `a city walk names its places, says the one reached and the one ahead`() {
        val start = milanWalk(from = 0, walked = 0.0)
        assertThat(say(checkNotNull(SessionAnnouncement.walkStarted(start)), start)).isEqualTo(
            "A walk in Milan: off you go. Here: The Duomo. " +
                "Milan began its cathedral in 1386, and building went on for nearly six centuries.",
        )
        val continued = milanWalk(from = 3_216, walked = 0.0)
        assertThat(say(checkNotNull(SessionAnnouncement.walkStarted(continued)), continued)).isEqualTo(
            "A walk in Milan: on we go. From here: Sforza Castle. Next: Sempione Park, in 430 metres.",
        )
        val atScala = milanWalk(from = 0, walked = 500.0)
        val reached = SessionAnnouncement.placesReached(atScala, milan.stops.subList(1, 3))
        assertThat(say(reached, atScala)).isEqualTo(
            "Behind you: Galleria Vittorio Emanuele II. Here: La Scala. " +
                "La Scala opened in 1778; Verdi’s Otello and Puccini’s Turandot had their first nights here. " +
                "Next: Via Montenapoleone, in 640 metres.",
        )
        // The page's "Hear it": the next place, as it will be told.
        assertThat(context.spokenWalkSample(milan, 0, UnitPreference.METRIC)).isEqualTo(
            "Here: Galleria Vittorio Emanuele II. " +
                "Opened in 1867, it is one of the oldest covered shopping arcades in the world. " +
                "Next: La Scala, in 290 metres.",
        )
        val end = milanWalk(from = 0, walked = 9_326.0)
        assertThat(say(SessionAnnouncement.goal(end), end)).isEqualTo("The walk is done, in 2,140 steps. Well done.")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian, a city walk`() {
        val start = milanWalk(from = 0, walked = 0.0)
        assertThat(say(checkNotNull(SessionAnnouncement.walkStarted(start)), start)).isEqualTo(
            "Passeggiata a Milano: si parte. Qui: Il Duomo. " +
                "Milano iniziò il suo Duomo nel 1386, e il cantiere durò quasi sei secoli.",
        )
        val atScala = milanWalk(from = 0, walked = 500.0)
        val reached = SessionAnnouncement.placesReached(atScala, milan.stops.subList(2, 3))
        assertThat(say(reached, atScala)).isEqualTo(
            "Qui: Teatro alla Scala. " +
                "La Scala aprì nel 1778; qui debuttarono l’Otello di Verdi e la Turandot di Puccini. " +
                "Più avanti: Via Montenapoleone, tra 640 metri.",
        )
    }

    // --- The interval walk (Phase 13) ----------------------------------------------------------

    private val intervals = checkNotNull(SessionPlans.start(SessionPlans.JAPANESE_WALKING, 0, 0, 0, 8_000)).copy(
        voice = SessionVoice.HEADPHONES,
        totals = SessionTotals(steps = 3_150, movingMillis = 30 * 60_000L),
        splits = (0 until 10).map { index ->
            val fast = index % 2 == 1
            // The third fast interval below the brisk 100.
            val cadence = if (index == 5) {
                94
            } else if (fast) {
                112
            } else {
                90
            }
            IntervalSplit(index, fast, steps = cadence * 3, movingMillis = 3 * 60_000L)
        },
    )

    @Test
    @Config(qualifiers = "en-rUS")
    fun `an interval walk says its sets, its changes short, and its fast intervals at the goal`() {
        assertThat(say(SessionAnnouncement.started(intervals), intervals)).isEqualTo(
            "Japanese walking: off you go. 5 sets of 3 minutes slow and 3 minutes fast. Start slow.",
        )
        val fast = SessionAnnouncement.IntervalChanged(fast = true, minutes = 3, lastSet = false)
        assertThat(say(fast, intervals)).isEqualTo("Fast, 3 minutes.")
        assertThat(say(fast.copy(lastSet = true), intervals)).isEqualTo("Last fast interval, 3 minutes.")
        assertThat(say(fast.copy(fast = false), intervals)).isEqualTo("Slow.")
        assertThat(say(SessionAnnouncement.goal(intervals), intervals)).isEqualTo(
            "Goal reached: 30 minutes, 3,150 steps. 4 fast intervals of 5 at pace. Well done.",
        )
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian, an interval walk`() {
        assertThat(say(SessionAnnouncement.started(intervals), intervals)).isEqualTo(
            "Camminata giapponese: si parte. 5 serie di 3 minuti a passo lento e 3 minuti a passo veloce. " +
                "Si comincia piano.",
        )
        val fast = SessionAnnouncement.IntervalChanged(fast = true, minutes = 3, lastSet = false)
        assertThat(say(fast, intervals)).isEqualTo("Veloce, 3 minuti.")
        assertThat(say(fast.copy(lastSet = true), intervals)).isEqualTo("Ultima serie veloce, 3 minuti.")
        assertThat(say(fast.copy(fast = false), intervals)).isEqualTo("Lento.")
        assertThat(say(SessionAnnouncement.goal(intervals), intervals)).isEqualTo(
            "Obiettivo raggiunto: 30 minuti, 3.150 passi. 4 intervalli veloci su 5 al ritmo. Ben fatto.",
        )
    }
}
