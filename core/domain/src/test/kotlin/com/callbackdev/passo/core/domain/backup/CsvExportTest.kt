package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.domain.backup.BackupFixtures.DAY
import com.callbackdev.passo.core.domain.backup.BackupFixtures.session
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CsvExportTest {
    private val bom = Char(0xFEFF).toString()
    private val date = LocalDate.ofEpochDay(DAY)
    private val summary = DailySummary(DAY, 9_412, 6_875.25, 312.46, 71, 38, 9_000, finalized = true)

    private fun lines(csv: String) = csv.removePrefix(bom).split("\r\n").dropLast(1)

    @Test
    fun `days are one row each, with a header that names each unit`() {
        val csv = CsvExport.days(listOf(summary), UnitSystem.METRIC)

        assertThat(csv).startsWith(bom)
        assertThat(csv).endsWith("\r\n")
        assertThat(lines(csv)).containsExactly(
            "date,steps,goal,goal_met,distance_km,active_kcal,active_minutes,brisk_minutes,final",
            "$date,9412,9000,yes,6.875,312.5,71,38,yes",
        ).inOrder()
    }

    @Test
    fun `distances follow the reader's units`() {
        val csv = CsvExport.days(listOf(summary), UnitSystem.IMPERIAL)

        assertThat(lines(csv)[0]).contains("distance_mi")
        assertThat(lines(csv)[1]).contains(",4.272,")
    }

    @Test
    fun `a minute says its recorded day, its local time and its exact instant`() {
        val epochMinute = DAY * 1_440 + 8 * 60 + 12
        val csv = CsvExport.minutes(listOf(MinuteSteps(epochMinute, DAY, 104)), ZoneId.of("Europe/Rome"))

        assertThat(lines(csv)).containsExactly(
            "date,time,steps,utc",
            "$date,10:12,104,${date}T08:12:00Z",
        ).inOrder()
    }

    @Test
    fun `an outing's name is quoted when it must be, and is never a formula`() {
        val named = session(DAY * 86_400_000L + 9 * 3_600_000L).copy(name = "Walk, \"quick\"")
        val formula = session(DAY * 86_400_000L + 18 * 3_600_000L).copy(name = "=HYPERLINK(1)")
        val live = session(DAY * 86_400_000L + 20 * 3_600_000L, state = SessionState.ACTIVE)

        val rows = lines(CsvExport.outings(listOf(formula, live, named), UnitSystem.METRIC, ZoneOffset.UTC))

        assertThat(rows[0]).isEqualTo(
            "date,start,end,name,goal,goal_value,goal_unit,pace,outcome,steps,minutes_in_motion,distance_km,active_kcal",
        )
        assertThat(
            rows[1],
        ).isEqualTo("$date,09:00,09:25,\"Walk, \"\"quick\"\"\",time,25,min,brisk,reached,2600,25.0,1.950,88.0")
        assertThat(rows[2]).contains(",'=HYPERLINK(1),")
        assertThat(rows[3]).contains(",under_way,")
        assertThat(rows[3]).startsWith("$date,20:00,,,")
    }

    @Test
    fun `the file is named for its table and its day`() {
        assertThat(
            CsvExport.fileName(CsvTable.MINUTES, LocalDate.of(2026, 9, 25)),
        ).isEqualTo("passo-minutes-2026-09-25.csv")
    }
}
