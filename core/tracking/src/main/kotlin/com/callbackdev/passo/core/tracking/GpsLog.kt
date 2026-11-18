package com.callbackdev.passo.core.tracking

/**
 * What the receiver and the service's `location` type did during one outing with GPS, for its
 * one row in the diagnostics log (`DiagnosticsType.GPS`; PLANNING.md §11 Phase 15). Counts and
 * delays only, never a coordinate: the log travels in the database Android's backup carries.
 *
 * It answers what only a phone can say (docs/adr/0016-location.md, «To be measured and proven on
 * a device»): whether the service is given the type each time it asks, whether fixes keep coming
 * with the screen off, and whether the phone batches them or hands them over one by one.
 */
internal class GpsLog {
    private var fixes = 0
    private var good = 0
    private var screenOffFixes = 0
    private var batches = 0
    private var largestBatch = 0
    private var longestGapMillis = 0L
    private var firstFixAfterMillis: Long? = null
    private var sleeps = 0
    private var typeAdded = 0
    private var typeRefused = 0
    private var updatesRefused = 0
    private var lastRefusal: String? = null

    private var askedAtElapsedMillis: Long? = null
    private var lastFixElapsedMillis: Long? = null

    /** Whether there is anything to say: an outing that never asked for the receiver logs nothing. */
    val used: Boolean get() = typeAdded + typeRefused + updatesRefused > 0

    /** The service asked for its `location` type, and was given it or not ([refusal] says why not). */
    fun onType(added: Boolean, refusal: Throwable? = null) {
        if (added) {
            typeAdded++
        } else {
            typeRefused++
            lastRefusal = refusal?.javaClass?.simpleName
        }
    }

    /** The updates were asked for at [elapsedMillis]: the first fix after it is timed from here. */
    fun onAsked(elapsedMillis: Long, granted: Boolean) {
        if (!granted) {
            updatesRefused++
            return
        }
        if (askedAtElapsedMillis == null) askedAtElapsedMillis = elapsedMillis
    }

    /** The receiver was put to sleep, or stopped: a gap across it is not the phone's delay. */
    fun onStopped(asleep: Boolean) {
        if (asleep) sleeps++
        lastFixElapsedMillis = null
        askedAtElapsedMillis = null
    }

    /**
     * One delivery: each fix's own elapsed time and accuracy, oldest first. A fix is counted good
     * at [GOOD_ACCURACY_METERS] or better, the accuracy the route's filter will ask for.
     */
    fun onFixes(elapsedMillis: List<Long>, accuracyMeters: List<Float>, screenOn: Boolean) {
        if (elapsedMillis.isEmpty()) return
        batches++
        largestBatch = maxOf(largestBatch, elapsedMillis.size)
        fixes += elapsedMillis.size
        good += accuracyMeters.count { it <= GOOD_ACCURACY_METERS }
        if (!screenOn) screenOffFixes += elapsedMillis.size
        for (at in elapsedMillis) {
            val asked = askedAtElapsedMillis
            if (firstFixAfterMillis == null && asked != null) firstFixAfterMillis = (at - asked).coerceAtLeast(0)
            lastFixElapsedMillis?.let { last -> longestGapMillis = maxOf(longestGapMillis, at - last) }
            lastFixElapsedMillis = maxOf(at, lastFixElapsedMillis ?: at)
        }
    }

    /** The row, and a clean slate for the next outing. */
    fun drain(wakeUpCounter: Boolean): String {
        val row = buildString {
            append("fixes=$fixes good=$good screenOff=$screenOffFixes ")
            append("batches=$batches largestBatch=$largestBatch longestGap=${longestGapMillis}ms ")
            append("firstFix=${firstFixAfterMillis?.let { "${it}ms" } ?: "none"} sleeps=$sleeps ")
            append("typeAdded=$typeAdded typeRefused=$typeRefused updatesRefused=$updatesRefused")
            lastRefusal?.let { append(" refusal=$it") }
            append(" wakeUp=$wakeUpCounter")
        }
        reset()
        return row
    }

    private fun reset() {
        fixes = 0
        good = 0
        screenOffFixes = 0
        batches = 0
        largestBatch = 0
        longestGapMillis = 0L
        firstFixAfterMillis = null
        sleeps = 0
        typeAdded = 0
        typeRefused = 0
        updatesRefused = 0
        lastRefusal = null
        askedAtElapsedMillis = null
        lastFixElapsedMillis = null
    }

    private companion object {
        /** docs/adr/0016-location.md, decision 5: a fix is accepted at 25 m of accuracy or better. */
        const val GOOD_ACCURACY_METERS = 25f
    }
}
