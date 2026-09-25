package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.UnitSystem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The three tables a spreadsheet can take, one per file. */
enum class CsvTable {
    /** One row per recorded day: steps, goal, estimates. */
    DAYS,

    /** One row per minute with steps: the finest the history goes. */
    MINUTES,

    /** One row per outing. */
    OUTINGS,
}

/**
 * The spreadsheet export (ADR 0011): CSV as RFC 4180 writes it (commas, a dot for decimals,
 * CRLF line ends, quotes only where a field needs them), UTF-8 with a byte-order mark so that
 * Excel reads an accented outing name right. The headers are fixed English identifiers with the
 * unit in their name (`distance_km`), the same in every language, so a formula or a script
 * written once keeps working. Distances follow the reader's units; everything else has one.
 *
 * Times of day are local, in [ZoneId] the phone is in now; `utc` gives the exact instant for a
 * minute recorded somewhere else.
 */
object CsvExport {
    /** The byte-order mark, written as its code so no editor or formatter can drop it unseen. */
    private val BOM = Char(0xFEFF).toString()
    private const val EOL = "\r\n"
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun days(days: List<DailySummary>, units: UnitSystem): String = table(
        header = listOf(
            "date",
            "steps",
            "goal",
            "goal_met",
            distanceHeader(units),
            "active_kcal",
            "active_minutes",
            "brisk_minutes",
            "final",
        ),
        rows = days.sortedBy { it.localEpochDay }.map { day ->
            listOf(
                LocalDate.ofEpochDay(day.localEpochDay).toString(),
                day.steps.toString(),
                day.goalSteps.toString(),
                day.goalReached.yesNo(),
                distance(day.distanceMeters, units),
                decimal(day.activeKcal, 1),
                day.activeMinutes.toString(),
                day.briskMinutes.toString(),
                day.finalized.yesNo(),
            )
        },
    )

    fun minutes(minutes: List<MinuteSteps>, zone: ZoneId): String = table(
        header = listOf("date", "time", "steps", "utc"),
        rows = minutes.sortedBy { it.epochMinute }.map { minute ->
            val instant = Instant.ofEpochSecond(minute.epochMinute * SECONDS_PER_MINUTE)
            listOf(
                LocalDate.ofEpochDay(minute.localEpochDay).toString(),
                TIME.format(instant.atZone(zone)),
                minute.steps.toString(),
                instant.toString(),
            )
        },
    )

    fun outings(sessions: List<Session>, units: UnitSystem, zone: ZoneId): String = table(
        header = listOf(
            "date",
            "start",
            "end",
            "name",
            "goal",
            "goal_value",
            "goal_unit",
            "pace",
            "outcome",
            "steps",
            "minutes_in_motion",
            distanceHeader(units),
            "active_kcal",
        ),
        rows = sessions.sortedBy { it.startedAtMillis }.map { session ->
            val (goalValue, goalUnit) = goal(session, units)
            listOf(
                LocalDate.ofEpochDay(session.localEpochDay).toString(),
                clock(session.startedAtMillis, zone),
                session.endedAtMillis?.let { clock(it, zone) }.orEmpty(),
                session.name.orEmpty(),
                if (session.restOfDay) "rest_of_day" else session.goalKind.name.lowercase(Locale.ROOT),
                goalValue,
                goalUnit,
                session.intensity.name.lowercase(Locale.ROOT),
                outcome(session),
                session.totals.steps.toString(),
                decimal(session.totals.movingMillis / MILLIS_PER_MINUTE, 1),
                distance(session.totals.distanceMeters, units),
                decimal(session.totals.activeKcal, 1),
            )
        },
    )

    /** The name offered for the file: what it holds and the day it was written, `passo-days-2026-09-25.csv`. */
    fun fileName(table: CsvTable, date: LocalDate): String = "passo-${table.name.lowercase(Locale.ROOT)}-$date.csv"

    private fun table(header: List<String>, rows: List<List<String>>): String = buildString {
        append(BOM)
        append(header.joinToString(",") { field(it) }).append(EOL)
        for (row in rows) append(row.joinToString(",") { field(it) }).append(EOL)
    }

    /**
     * A field as RFC 4180 wants it, and never a formula: a name the reader typed as "=1+1" or
     * "-20 min" is text in a spreadsheet, not something it runs.
     */
    private fun field(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in FORMULA_STARTS) "'$value" else value
        val quoted = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (quoted) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    private fun goal(session: Session, units: UnitSystem): Pair<String, String> = when (session.goalKind) {
        SessionGoalKind.DISTANCE -> distance(session.goalValue.toDouble(), units) to distanceUnit(units)
        SessionGoalKind.TIME -> session.goalValue.toString() to "min"
        SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> session.goalValue.toString() to "steps"
    }

    private fun outcome(session: Session): String = when {
        session.live -> "under_way"

        session.reached -> "reached"

        else -> when (session.end) {
            SessionEnd.GOAL -> "reached"
            SessionEnd.STOPPED -> "stopped"
            SessionEnd.IDLE -> "idle"
            SessionEnd.CLOSED, null -> "closed"
        }
    }

    private fun distanceHeader(units: UnitSystem) = "distance_${distanceUnit(units)}"

    private fun distanceUnit(units: UnitSystem) = if (units == UnitSystem.IMPERIAL) "mi" else "km"

    private fun distance(meters: Double, units: UnitSystem): String = decimal(
        if (units == UnitSystem.IMPERIAL) UnitConversions.metersToMiles(meters) else meters / METERS_PER_KM,
        3,
    )

    private fun clock(millis: Long, zone: ZoneId): String = TIME.format(Instant.ofEpochMilli(millis).atZone(zone))

    private fun decimal(value: Double, decimals: Int): String = String.format(Locale.ROOT, "%.${decimals}f", value)

    private fun Boolean.yesNo() = if (this) "yes" else "no"

    private val FORMULA_STARTS = setOf('=', '+', '-', '@')
    private const val SECONDS_PER_MINUTE = 60L
    private const val MILLIS_PER_MINUTE = 60_000.0
    private const val METERS_PER_KM = 1_000.0
}
