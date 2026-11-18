package com.callbackdev.passo.core.domain.routes

/** Whether an outing can be measured with GPS on this phone, right now (docs/adr/0016-location.md). */
enum class GpsReadiness {
    /** The precise permission is granted and location is on: the satellites can be read. */
    READY,

    /** Neither location permission is granted. */
    PERMISSION_NEEDED,

    /**
     * The reader granted the approximate location alone: a point somewhere within a few
     * kilometres measures no walk, so the outing counts from the steps.
     */
    APPROXIMATE_ONLY,

    /** The permission is there, and location is turned off on the phone. */
    LOCATION_OFF,

    /** The phone has no satellite receiver: «Measure with GPS» is not offered at all. */
    NO_RECEIVER,
}

/**
 * What the reader last answered when Passo asked for the location permission. Kept so that a
 * refusal is told once and never asked again by itself, while a grant the system took back
 * («Only this time», which lasts as long as Android says) is simply asked for again.
 */
enum class GpsAnswer {
    NEVER_ASKED,
    GRANTED,
    REFUSED,
}

/** What happens before an outing starts, when its plan asks to be measured with GPS. */
enum class GpsStartStep {
    /** Nothing to ask: the outing starts, with GPS if the phone is ready and on the steps if not. */
    START,

    /** The first time: Passo's own screen, saying when location is read, then the system's request. */
    EXPLAIN,

    /** Granted before and taken back since: the system's request alone, Passo's screen being said. */
    ASK,
}

/**
 * The one door every start of an outing goes through (PLANNING.md §11 Phase 15, «One door for
 * every start»): the Outings page, a launcher's shortcut and, later, a walk's page all ask here,
 * so no start with GPS skips Passo's own screen and none asks twice.
 */
object GpsStart {
    /**
     * The step before starting a plan whose `gps` is [planAsksGps].
     *
     * The permission is asked in context only: never for a plan that does not measure, never
     * after a refusal (the outing's card then says once what it costs and the way back), and
     * never where asking cannot help (no receiver, location turned off, approximate alone: each
     * is the card's to say, with its own way out).
     */
    fun step(planAsksGps: Boolean, readiness: GpsReadiness, answer: GpsAnswer): GpsStartStep = when {
        !planAsksGps -> GpsStartStep.START

        readiness != GpsReadiness.PERMISSION_NEEDED -> GpsStartStep.START

        else -> when (answer) {
            GpsAnswer.NEVER_ASKED -> GpsStartStep.EXPLAIN
            GpsAnswer.GRANTED -> GpsStartStep.ASK
            GpsAnswer.REFUSED -> GpsStartStep.START
        }
    }

    /**
     * What to remember of the system's answer: a grant only when the precise location came with
     * it. Approximate alone is the reader saying no to what was asked.
     */
    fun answer(preciseGranted: Boolean): GpsAnswer = if (preciseGranted) GpsAnswer.GRANTED else GpsAnswer.REFUSED
}
