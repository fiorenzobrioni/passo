package com.callbackdev.passo.core.model

/**
 * Steps attributed to one minute.
 *
 * @property epochMinute the UTC minute (epoch millis / 60 000).
 * @property localEpochDay the local date of that minute in the time zone in effect when it was
 *   written. Stored, never recomputed: a later time-zone change does not move a recorded day.
 */
data class MinuteSteps(val epochMinute: Long, val localEpochDay: Long, val steps: Int)
