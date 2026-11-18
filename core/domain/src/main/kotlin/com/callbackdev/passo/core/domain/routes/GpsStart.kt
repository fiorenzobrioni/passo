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
 * («Only this time», which lasts as long as Android says) is simply asked for again. A refusal
 * is forgotten when the reader turns «Measure with GPS» on again ([GpsStart.asksAgain]): that
 * is them asking, not Passo.
 */
enum class GpsAnswer {
    NEVER_ASKED,
    GRANTED,
    REFUSED,
}

/** What the editor says under «Measure with GPS». */
enum class GpsSwitchNote {
    /** What it gives and what it costs. */
    PLAIN,

    /** The same, and that Passo asks for the location when the outing starts. */
    ASKS_AT_START,

    /**
     * The switch is on and the reader refused the permission: the outing counts from the steps,
     * and turning the switch off and on is the way to be asked again.
     */
    NOT_ALLOWED,
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
     * after a refusal (the outing's card then says once what it costs and the way back, and the
     * editor's switch says it for as long as it lasts), and never where asking cannot help (no
     * receiver, location turned off: each is the card's to say, with its own way out).
     *
     * Approximate alone is a refusal of what was asked, or the reader's own choice in the
     * system's page after a grant: neither is asked again by itself. Where Passo has no answer
     * to remember (the approximate one given outside it, or a refusal the reader took back at
     * the switch), it asks as it does the first time, for the precise one.
     */
    fun step(planAsksGps: Boolean, readiness: GpsReadiness, answer: GpsAnswer): GpsStartStep = when {
        !planAsksGps -> GpsStartStep.START

        readiness == GpsReadiness.PERMISSION_NEEDED -> when (answer) {
            GpsAnswer.NEVER_ASKED -> GpsStartStep.EXPLAIN
            GpsAnswer.GRANTED -> GpsStartStep.ASK
            GpsAnswer.REFUSED -> GpsStartStep.START
        }

        readiness == GpsReadiness.APPROXIMATE_ONLY && answer == GpsAnswer.NEVER_ASKED -> GpsStartStep.EXPLAIN

        else -> GpsStartStep.START
    }

    /**
     * Whether a plan saved with «Measure with GPS» [isOn], which [wasOn] before the reader last
     * touched it, is the reader asking to be asked again (docs/adr/0016-location.md, decision
     * 2, amended): the switch turned on by their own hand, in a new plan or off and on in an
     * old one, on a phone where the permission is what is missing and Passo would not ask by
     * itself. The remembered answer is then forgotten, and the next start of an outing with GPS
     * asks as the first did: in context, at the start, never from the editor.
     *
     * Without it a refusal had one way back, the line its first outing's card said once: every
     * outing after it counted from the steps without a word, under a switch that stayed on.
     */
    fun asksAgain(wasOn: Boolean, isOn: Boolean, readiness: GpsReadiness, answer: GpsAnswer): Boolean =
        isOn && !wasOn && permissionMissing(readiness) && step(true, readiness, answer) == GpsStartStep.START

    /**
     * What the editor's switch says under it, so that it is never on over an outing that will
     * not be measured without a word of why.
     */
    fun switchNote(wasOn: Boolean, isOn: Boolean, readiness: GpsReadiness, answer: GpsAnswer): GpsSwitchNote = when {
        !isOn || !permissionMissing(readiness) -> GpsSwitchNote.PLAIN
        step(true, readiness, answer) != GpsStartStep.START -> GpsSwitchNote.ASKS_AT_START
        asksAgain(wasOn, isOn, readiness, answer) -> GpsSwitchNote.ASKS_AT_START
        else -> GpsSwitchNote.NOT_ALLOWED
    }

    private fun permissionMissing(readiness: GpsReadiness): Boolean =
        readiness == GpsReadiness.PERMISSION_NEEDED || readiness == GpsReadiness.APPROXIMATE_ONLY

    /**
     * What to remember of the system's answer: a grant only when the precise location came with
     * it. Approximate alone is the reader saying no to what was asked.
     */
    fun answer(preciseGranted: Boolean): GpsAnswer = if (preciseGranted) GpsAnswer.GRANTED else GpsAnswer.REFUSED
}
