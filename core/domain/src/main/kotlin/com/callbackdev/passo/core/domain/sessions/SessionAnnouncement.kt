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
     * The goal: [steps] walked, and for an outing with a pace to keep, [zoneMinutes] of
     * [movingMinutes] at it.
     */
    data class GoalReached(val goal: SessionAmount, val steps: Int, val zoneMinutes: Int?, val movingMinutes: Int) :
        SessionAnnouncement

    companion object {
        fun started(session: Session): Started = Started(session.goal(), session.intensity, session.restOfDay)

        /** What [milestone] says for [session] as it stands, the pace read as [cadence]. */
        fun milestone(session: Session, milestone: SessionMilestone, cadence: Int?): SessionAnnouncement =
            if (milestone == SessionMilestone.GOAL) {
                goal(session)
            } else {
                Milestone(milestone, session.remaining(), cadence, verdict(session.intensity, cadence))
            }

        fun goal(session: Session): GoalReached {
            val moving = (session.totals.movingMillis / MILLIS_PER_MINUTE).toInt()
            val zone = (session.totals.zoneMillis / MILLIS_PER_MINUTE).toInt()
            return GoalReached(
                goal = session.goal(),
                steps = session.totals.steps,
                zoneMinutes = zone.takeIf { session.intensity.cadenceFloor != null },
                movingMinutes = moving,
            )
        }

        fun verdict(intensity: SessionIntensity, cadence: Int?): PaceVerdict {
            val floor = intensity.cadenceFloor ?: return PaceVerdict.NONE
            return when {
                cadence == null -> PaceVerdict.NONE
                cadence >= floor -> PaceVerdict.ON_PACE
                else -> PaceVerdict.BELOW
            }
        }

        private const val MILLIS_PER_MINUTE = 60_000L
    }
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
