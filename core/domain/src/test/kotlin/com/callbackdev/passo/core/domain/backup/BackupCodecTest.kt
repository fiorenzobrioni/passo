package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.domain.backup.BackupFixtures.DAY
import com.callbackdev.passo.core.domain.backup.BackupFixtures.backup
import com.callbackdev.passo.core.domain.backup.BackupFixtures.day
import com.callbackdev.passo.core.domain.backup.BackupFixtures.minutes
import com.callbackdev.passo.core.domain.backup.BackupFixtures.session
import com.callbackdev.passo.core.model.AppPalette
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class BackupCodecTest {
    private val full = backup(
        days = listOf(
            day(DAY, minutes(DAY, 600 to 104, 601 to 98, 1_200 to 12)),
            day(DAY + 1, minutes(DAY + 1, 480 to 60), finalized = false),
        ),
        profile = BackupFixtures.profile.copy(
            sex = Sex.FEMALE,
            stepLengthMode = StepLengthMode.CALIBRATED,
            walkingStepLengthMeters = 0.742,
            runningStepLengthMeters = 0.98,
        ),
        sessions = listOf(session(1_790_000_000_000)),
    ).copy(
        settings = UserSettings(
            dailyGoalSteps = 9_000,
            units = UnitPreference.IMPERIAL,
            firstDayOfWeek = DayOfWeek.SUNDAY,
            palette = AppPalette.PAPER,
            eveningReminder = true,
            eveningReminderTime = LocalTime.of(21, 15),
            eveningReminderThresholdPercent = 75,
            minWalkMinutes = 5,
            startOutingButton = false,
        ),
        diagnostics = listOf(DiagnosticsEvent(1_790_000_000_000, DiagnosticsType.BOOT, "count=12")),
    )

    @Test
    fun `a backup reads back as it was written`() {
        val read = BackupCodec.decode(BackupCodec.encode(full))

        assertThat(read).isEqualTo(BackupRead.Ok(full))
    }

    @Test
    fun `the file says what it is, and a day reads without the app`() {
        val text = BackupCodec.encode(full)

        assertThat(text).contains("\"format\":\"passo-backup\"")
        assertThat(text).contains("\"version\":1")
        assertThat(text).contains("\"date\":\"${LocalDate.ofEpochDay(DAY)}\"")
        assertThat(text).contains("[${DAY * 1_440 + 600},104]")
        assertThat(text).contains("\"eveningReminderTime\":\"21:15\"")
        // What only a phone decides is not in it.
        assertThat(text).doesNotContain("trackingEnabled")
        assertThat(text).doesNotContain("onboardingCompleted")
    }

    @Test
    fun `a file that is not Passo's says so`() {
        assertThat(BackupCodec.decode("")).isEqualTo(BackupRead.NotPasso)
        assertThat(BackupCodec.decode("date,steps\n2026-09-25,8000")).isEqualTo(BackupRead.NotPasso)
        assertThat(BackupCodec.decode("[1,2,3]")).isEqualTo(BackupRead.NotPasso)
        assertThat(BackupCodec.decode("""{"format":"something-else","version":1}""")).isEqualTo(BackupRead.NotPasso)
    }

    @Test
    fun `a file from a newer Passo asks for an update`() {
        assertThat(BackupCodec.decode("""{"format":"passo-backup","version":2,"days":[]}"""))
            .isEqualTo(BackupRead.TooNew(2))
    }

    @Test
    fun `a file cut short or broken by hand is damaged`() {
        val text = BackupCodec.encode(full)

        assertThat(BackupCodec.decode("""{"format":"passo-backup"}""")).isEqualTo(BackupRead.Damaged)
        assertThat(BackupCodec.decode("""{"format":"passo-backup","version":1,"days":"many"}"""))
            .isEqualTo(BackupRead.Damaged)
        assertThat(BackupCodec.decode(text.take(text.length / 2))).isEqualTo(BackupRead.Damaged)
    }

    @Test
    fun `what a newer build adds is skipped, and what cannot be read falls back`() {
        val text = """
            {"format":"passo-backup","version":1,"somethingNew":{"a":1},
             "settings":{"units":"PARSECS","theme":"DARK","dailyGoalSteps":3,"eveningReminderTime":"25:99"},
             "profile":{"sex":"OTHER","stepLengthMode":"GUESSED"},
             "days":[
               {"date":"2026-09-20","steps":100,"goalSteps":9000,"minutes":[[29000000,60],[29000001,-4],[29000002],[29000003,40]]},
               {"date":"not a date","steps":5}
             ],
             "plans":[{"id":1,"goalKind":"SWIM","goalValue":10,"intensity":"FREE"}],
             "outings":[{"goalKind":"STEPS","goalValue":1000,"date":"2026-09-20","startedAtMillis":5,"state":"LEVITATING"}],
             "diagnostics":[{"atMillis":1,"type":"COSMIC_RAY","detail":"?"}]}
        """.trimIndent()

        val read = BackupCodec.decode(text) as BackupRead.Ok
        val backup = read.backup

        assertThat(backup.settings.units).isEqualTo(UserSettings().units)
        assertThat(backup.settings.theme.name).isEqualTo("DARK")
        assertThat(backup.settings.eveningReminderTime).isEqualTo(UserSettings().eveningReminderTime)
        assertThat(backup.profile.sex).isNull()
        assertThat(backup.profile.stepLengthMode).isEqualTo(StepLengthMode.AUTO)
        assertThat(backup.days).hasSize(1)
        assertThat(backup.days.single().minutes.map { it.steps }).containsExactly(60, 40).inOrder()
        assertThat(backup.days.single().summary.localEpochDay).isEqualTo(LocalDate.parse("2026-09-20").toEpochDay())
        assertThat(backup.plans).isEmpty()
        assertThat(backup.sessions.single().state.name).isEqualTo("FINISHED")
        assertThat(backup.diagnostics).isEmpty()
    }

    @Test
    fun `a day or a minute repeated by hand is kept once, the fuller one`() {
        val text = """
            {"format":"passo-backup","version":1,"days":[
              {"date":"2026-09-20","steps":100,"goalSteps":9000,"minutes":[[29000000,60],[29000000,80]]},
              {"date":"2026-09-20","steps":40,"goalSteps":9000,"minutes":[[29000000,40]]}
            ]}
        """.trimIndent()

        val day = (BackupCodec.decode(text) as BackupRead.Ok).backup.days.single()

        assertThat(day.summary.steps).isEqualTo(100)
        assertThat(day.minutes.single().steps).isEqualTo(80)
    }

    @Test
    fun `the file is named for the day it was written`() {
        assertThat(BackupCodec.fileName(LocalDate.of(2026, 9, 25))).isEqualTo("passo-backup-2026-09-25.json")
    }
}
