package com.callbackdev.passo.core.data.time

import java.time.LocalDate
import java.time.ZoneId

/** Today's local date, as an epoch day, in the time zone in effect right now. */
fun interface TodaySource {
    fun epochDay(): Long

    companion object {
        /** Reads the zone at every call: a time-zone change moves "today" with it. */
        val System = TodaySource { LocalDate.now(ZoneId.systemDefault()).toEpochDay() }
    }
}
