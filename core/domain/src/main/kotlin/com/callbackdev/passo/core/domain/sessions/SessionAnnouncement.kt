package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionVoice

/** The pace now against the outing's own, as a spoken signal says it. */
enum class PaceVerdict {
    /** At or above the outing's cadence. */
    ON_PACE,

    /** Below it. */
    BELOW,

    /** Nothing to say: no cadence to keep, or not yet a pace. */
    NONE,
}

/**
 * What an outing says aloud (Phase 10, second iteration: the voice), one sentence each, in the
 * order the reader lives it: the start, each signal they chose with what is left and how the
 * pace is going, the goal with what it came to. The words are the app's (resources); this is
 * what they say.
 */
sealed interface SessionAnnouncement {
    /** It started: what it is. Heard first, it also says the voice works through these ears. */
    data class Started(val goal: SessionAmount, val intensity: SessionIntensity, val restOfDay: Boolean) :
        SessionAnnouncement

    /** A share of the goal: [left] to go, and the pace. */
    data class Milestone(
        val milestone: SessionMilestone,
        val left: SessionAmount,
        val cadence: Int?,
        val pace: PaceVerdict,
    ) : SessionAnnouncement

    /**
     * The goal: [steps] walked, how the pace was kept, and whether the day's own goal came with
     * it. What varies from one outing to the next is what happened, never a phrase drawn at
     * random: the reader learns the shape of these sentences as they learn the vibrations.
     */
    data class GoalReached(
        val goal: SessionAmount,
        val steps: Int,
        val pace: PaceSummary,
        val dayGoalReached: Boolean,
    ) : SessionAnnouncement

    companion object {
        fun started(session: Session): Started = Started(session.goal(), session.intensity, session.restOfDay)

        /**
         * What [milestone] says for [session] as it stands, the pace read as [cadence]; at the
         * goal, [dayGoalReached] says whether this outing also brought today's goal.
         */
        fun milestone(
            session: Session,
            milestone: SessionMilestone,
            cadence: Int?,
            dayGoalReached: Boolean = false,
        ): SessionAnnouncement = if (milestone == SessionMilestone.GOAL) {
            goal(session, dayGoalReached)
        } else {
            Milestone(milestone, session.remaining(), cadence, verdict(session.intensity, cadence))
        }

        fun goal(session: Session, dayGoalReached: Boolean = false): GoalReached = GoalReached(
            goal = session.goal(),
            steps = session.totals.steps,
            pace = paceSummary(session),
            dayGoalReached = dayGoalReached || session.restOfDay,
        )

        /**
         * How an outing with a pace to keep kept it: "almost all of it" from [MOSTLY_AT_PACE] of
         * its time in motion, the minutes otherwise.
         */
        fun paceSummary(session: Session): PaceSummary {
            val moving = session.totals.movingMillis
            if (session.intensity.cadenceFloor == null || moving < MILLIS_PER_MINUTE) return PaceSummary.None
            if (session.totals.zoneMillis >= moving * MOSTLY_AT_PACE) return PaceSummary.Mostly
            return PaceSummary.Part(
                zoneMinutes = (session.totals.zoneMillis / MILLIS_PER_MINUTE).toInt(),
                movingMinutes = (moving / MILLIS_PER_MINUTE).toInt(),
            )
        }

        /**
         * Whether an outing's last [sessionSteps] took today's count across [dailyGoalSteps]:
         * the day was below it without them and is at it with them.
         */
        fun broughtDayGoal(todaySteps: Int, sessionSteps: Int, dailyGoalSteps: Int): Boolean =
            todaySteps >= dailyGoalSteps && todaySteps - sessionSteps < dailyGoalSteps

        fun verdict(intensity: SessionIntensity, cadence: Int?): PaceVerdict {
            val floor = intensity.cadenceFloor ?: return PaceVerdict.NONE
            return when {
                cadence == null -> PaceVerdict.NONE
                cadence >= floor -> PaceVerdict.ON_PACE
                else -> PaceVerdict.BELOW
            }
        }

        private const val MILLIS_PER_MINUTE = 60_000L

        /** Nine tenths: a traffic light or two below the pace still reads as "almost all". */
        private const val MOSTLY_AT_PACE = 0.9
    }
}

/** How an outing kept its pace, as its goal's sentence says it. */
sealed interface PaceSummary {
    /** No pace to keep, or too short to say. */
    data object None : PaceSummary

    /** Nine tenths of it or more at the pace. */
    data object Mostly : PaceSummary

    /** [zoneMinutes] of [movingMinutes] at the pace. */
    data class Part(val zoneMinutes: Int, val movingMinutes: Int) : PaceSummary
}

/**
 * Whether a signal is spoken now, and so heard only by whom it should be: through headphones
 * whenever the outing speaks; through the phone's speaker only when the reader asked for it
 * ([SessionVoice.ALWAYS]) and the phone is not silenced (silent or vibrate mode means "not out
 * loud", as it does for every other sound).
 */
object SpeechRoute {
    fun speaks(voice: SessionVoice, headphones: Boolean, ringerNormal: Boolean): Boolean = when (voice) {
        SessionVoice.OFF -> false
        SessionVoice.HEADPHONES -> headphones
        SessionVoice.ALWAYS -> headphones || ringerNormal
    }
}
