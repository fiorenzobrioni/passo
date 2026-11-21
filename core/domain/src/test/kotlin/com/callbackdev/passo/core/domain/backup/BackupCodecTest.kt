package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.domain.backup.BackupFixtures.DAY
import com.callbackdev.passo.core.domain.backup.BackupFixtures.backup
import com.callbackdev.passo.core.domain.backup.BackupFixtures.day
import com.callbackdev.passo.core.domain.backup.BackupFixtures.minutes
import com.callbackdev.passo.core.domain.backup.BackupFixtures.session
import com.callbackdev.passo.core.domain.routes.Splits
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.AppPalette
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionMarks
import com.callbackdev.passo.core.model.SessionPause
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.StepLengthSample
import com.callbackdev.passo.core.model.StepPoint
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

    private val learned = BackupFixtures.profile.copy(
        stepLengthMode = StepLengthMode.LEARNED,
        walkingStepLengthMeters = 0.742,
        learnedStep = LearnedStep(
            points = listOf(StepPoint(94.5, 0.76), StepPoint(116.0, 0.81), StepPoint(161.0, 1.04)),
            usualCadence = 104,
            learntMeters = 14_230.0,
            outings = 6,
            throughMillis = 1_790_000_000_000,
            replaced = StepLengthMode.CALIBRATED,
            running = false,
        ),
    )

    @Test
    fun `a learnt step reads back with its curve and its way back`() {
        val withCurve = full.copy(profile = learned)

        assertThat(BackupCodec.decode(BackupCodec.encode(withCurve))).isEqualTo(BackupRead.Ok(withCurve))
        assertThat(BackupCodec.encode(withCurve)).contains("\"stepLengthMode\":\"LEARNED\"")
    }

    @Test
    fun `a file from before the step learnt reads without a curve`() {
        val read = BackupCodec.decode(BackupCodec.encode(full)) as BackupRead.Ok

        assertThat(read.backup.profile.learnedStep).isNull()
        assertThat(read.backup.stepFallback).isFalse()
    }

    @Test
    fun `a file of this version imported by 1_1_0 falls back to the height's estimate`() {
        // What 1.1.0 does with this file: it skips the curve it does not know, and reads the mode
        // `LEARNED`, unknown to it, as the height's estimate (tag v1.1.0, BackupCodec).
        val text = BackupCodec.encode(full.copy(profile = learned))
            .replace(Regex("\"learnedStep\":\\{.*?\\]\\s*,.*?\\}"), "\"skipped\":0")
        val read = BackupCodec.decode(text) as BackupRead.Ok

        assertThat(text).doesNotContain("learnedStep")
        assertThat(read.backup.profile.stepLengthMode).isEqualTo(StepLengthMode.AUTO)
        assertThat(read.backup.profile.learnedStep).isNull()
    }

    @Test
    fun `a mode it does not know falls back to the height's estimate and says so`() {
        val text = BackupCodec.encode(
            full,
        ).replace("\"stepLengthMode\":\"CALIBRATED\"", "\"stepLengthMode\":\"DREAMED\"")
        val read = BackupCodec.decode(text) as BackupRead.Ok

        assertThat(read.backup.profile.stepLengthMode).isEqualTo(StepLengthMode.AUTO)
        assertThat(read.backup.stepFallback).isTrue()
        assertThat((BackupCodec.decode(BackupCodec.encode(full)) as BackupRead.Ok).backup.stepFallback).isFalse()
    }

    @Test
    fun `an outing measured with GPS reads back with its numbers and what it said of the step`() {
        val plan = SessionPlans.NEW.copy(id = 11, gps = true, position = 1)
        val outing = session(1_790_000_000_000, planId = 11).copy(
            gps = true,
            stepsMeters = 5_050.0,
            measuredShare = 0.96,
        )
        val samples = listOf(StepLengthSample(110, 330, 252.0), StepLengthSample(128, 256, 216.5))
        val measured = full.copy(
            plans = full.plans + plan,
            sessions = full.sessions + outing,
            stepLengthSamples = mapOf(outing.startedAtMillis to samples),
        )

        assertThat(BackupCodec.decode(BackupCodec.encode(measured))).isEqualTo(BackupRead.Ok(measured))
    }

    @Test
    fun `the file holds an outing's numbers, never where it went`() {
        val outing = session(1_790_000_000_000).copy(gps = true, stepsMeters = 5_050.0, measuredShare = 0.96)
        val measured = full.copy(
            sessions = listOf(outing),
            stepLengthSamples = mapOf(outing.startedAtMillis to listOf(StepLengthSample(110, 330, 252.0))),
        )

        val text = BackupCodec.encode(measured)

        assertThat(text).contains("\"gps\"")
        assertThat(text).contains("\"measuredShare\"")
        assertThat(text).doesNotContainMatch("(?i)latitude|longitude|\"lat\"|\"lon\"|route|chunk")
    }

    @Test
    fun `an outing without GPS writes none of it, and a file from before reads as it always did`() {
        val text = BackupCodec.encode(full)
        val read = (BackupCodec.decode(text) as BackupRead.Ok).backup

        assertThat(read.sessions.none { it.gps }).isTrue()
        assertThat(read.sessions.all { it.stepsMeters == null && it.measuredShare == null }).isTrue()
        assertThat(read.stepLengthSamples).isEmpty()
        assertThat(read.plans.none { it.gps }).isTrue()
    }

    @Test
    fun `what cannot be a measure is dropped, and the outing kept`() {
        val outing = session(1_790_000_000_000).copy(gps = true, stepsMeters = 5_050.0, measuredShare = 0.96)
        val measured = full.copy(
            sessions = listOf(outing),
            stepLengthSamples = mapOf(outing.startedAtMillis to listOf(StepLengthSample(110, 330, 252.0))),
        )
        val broken = BackupCodec.encode(measured)
            .replace(Regex("\"measuredShare\"\\s*:\\s*0\\.96"), "\"measuredShare\": 7.5")
            .replace(Regex("\"steps\"\\s*:\\s*330"), "\"steps\": 0")
        assertThat(broken).contains("7.5")

        val read = (BackupCodec.decode(broken) as BackupRead.Ok).backup

        // A share is between nothing and all; a sample with no steps says nothing of a step.
        assertThat(read.sessions.single().measuredShare).isEqualTo(1.0)
        assertThat(read.stepLengthSamples).isEmpty()
    }

    @Test
    fun `a leg no outing can have is dropped, and its splits are cut from the rest`() {
        val legs = listOf(
            OutingLeg(0, 100.0, 70_000, 130.0, endedAtMillis = 1_790_000_070_000),
            OutingLeg(1, 100.0, 70_000, 130.0, endedAtMillis = 1_790_000_140_000),
        )
        val outing = session(1_790_000_000_000).copy(gps = true, stepsMeters = 210.0, measuredShare = 0.96, legs = legs)
        val text = BackupCodec.encode(full.copy(sessions = listOf(outing)))
        // A damaged or hand-edited file: a leg of ten thousand billion metres.
        val broken = text.replaceFirst(Regex("\"meters\"\\s*:\\s*100(\\.0)?"), "\"meters\": 1e13")
        assertThat(broken).contains("1e13")

        val read = (BackupCodec.decode(broken) as BackupRead.Ok).backup.sessions.single()

        assertThat(read.legs.map { it.meters }).containsExactly(100.0)
        assertThat(read.legs.single().index).isEqualTo(0)
        assertThat(Splits.of(read.legs, 1_000.0).single().meters).isEqualTo(100.0)
    }

    @Test
    fun `an interval outing and its plan read back with their sets and splits`() {
        val plan = SessionPlans.JAPANESE_WALKING.copy(id = 9, intervals = IntervalSets(2, 3, 6), goalValue = 30)
        val outing = session(1_790_000_000_000, planId = 9).copy(
            goalKind = SessionGoalKind.INTERVALS,
            goalValue = 30,
            milestones = emptySet(),
            intervals = IntervalSets(2, 3, 6),
            splits = listOf(
                IntervalSplit(0, fast = false, steps = 180, movingMillis = 120_000, zoneMillis = 0),
                IntervalSplit(1, fast = true, steps = 330, movingMillis = 180_000, zoneMillis = 170_000),
            ),
        )
        val withIntervals = full.copy(plans = full.plans + plan, sessions = full.sessions + outing)

        assertThat(BackupCodec.decode(BackupCodec.encode(withIntervals))).isEqualTo(BackupRead.Ok(withIntervals))
    }

    @Test
    fun `a backup with open outings reads back whole, pauses included`() {
        val start = 1_790_000_000_000
        val plan = SessionPlans.OPEN_WALK.copy(id = 11)
        val outing = session(start, planId = 11).copy(
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            milestones = emptySet(),
            reachedAtMillis = null,
            endedAtMillis = start + 60 * 60_000,
            pauses = listOf(
                SessionPause(start + 10 * 60_000, start + 15 * 60_000),
                SessionPause(start + 25 * 60_000, start + 28 * 60_000),
            ),
            // An open outing is walked by a Passo that keeps the pauses, always.
            pausesKept = true,
        )
        val withOpen = full.copy(plans = full.plans + plan, sessions = full.sessions + outing)

        assertThat(BackupCodec.decode(BackupCodec.encode(withOpen))).isEqualTo(BackupRead.Ok(withOpen))
        assertThat(BackupCodec.encode(withOpen)).contains("\"goalKind\":\"OPEN\"")
    }

    @Test
    fun `a backup carries the marks - a plan's choice, and an open outing's mark, unit and count told`() {
        val start = 1_790_000_000_000
        val chosen = SessionPlans.OPEN_WALK.copy(id = 12, gps = true, marks = SessionMarks.TIME)
        val following = SessionPlans.OPEN_WALK.copy(id = 13, position = 1)
        val outing = session(start, planId = 12).copy(
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            milestones = emptySet(),
            reachedAtMillis = null,
            marks = SessionMarks.DISTANCE,
            markMiles = true,
            toldMarks = 3,
            pausesKept = true,
        )
        val withMarks = full.copy(plans = full.plans + chosen + following, sessions = full.sessions + outing)

        val text = BackupCodec.encode(withMarks)
        assertThat(BackupCodec.decode(text)).isEqualTo(BackupRead.Ok(withMarks))
        assertThat(text).contains("\"marks\":\"TIME\"")
        assertThat(text).contains("\"markMiles\":true")
        assertThat(text).contains("\"toldMarks\":3")
    }

    @Test
    fun `a file from before the marks reads a plan that follows its switch, and outings with none`() {
        val start = 1_790_000_000_000
        val text = BackupCodec.encode(
            full.copy(
                plans = listOf(SessionPlans.OPEN_WALK.copy(id = 14, gps = true)),
                sessions = listOf(
                    session(start).copy(goalKind = SessionGoalKind.OPEN, goalValue = 0, milestones = emptySet()),
                ),
            ),
        )
        assertThat(text).doesNotContain("\"marks\"")
        val read = (BackupCodec.decode(text) as BackupRead.Ok).backup
        assertThat(read.plans.single().marks).isNull()
        assertThat(read.plans.single().marksInForce).isEqualTo(SessionMarks.DISTANCE)
        assertThat(read.sessions.single().marks).isEqualTo(SessionMarks.NONE)
        // A mark on a goal outing, or one this build does not know, is none.
        val odd = text.replace(
            "\"goalKind\":\"OPEN\",\"goalValue\":0,",
            "\"goalKind\":\"OPEN\",\"goalValue\":0,\"marks\":\"LAPS\",",
        )
        assertThat(odd).contains("LAPS")
        assertThat(
            (BackupCodec.decode(odd) as BackupRead.Ok).backup.sessions.single().marks,
        ).isEqualTo(SessionMarks.NONE)
    }

    @Test
    fun `an outing's pauses known are carried, and a file from before proves them where it can`() {
        val start = 1_790_000_000_000
        val kept = session(start).copy(pausesKept = true)
        val text = BackupCodec.encode(full.copy(sessions = listOf(kept)))
        assertThat(text).contains("\"pausesKept\":true")
        assertThat((BackupCodec.decode(text) as BackupRead.Ok).backup.sessions.single().pausesKept).isTrue()

        val day = 86_400_000L
        // A file from before the field: a goal outing with no pause cannot tell, an open one or one
        // with a pause can only come from a Passo that kept them, as the database's migration says.
        val before = BackupCodec.encode(
            full.copy(
                sessions = listOf(
                    session(start),
                    session(start + day).copy(goalKind = SessionGoalKind.OPEN, goalValue = 0, milestones = emptySet()),
                    session(start + 2 * day).copy(
                        pauses = listOf(SessionPause(start + 2 * day + 60_000, start + 2 * day + 120_000)),
                    ),
                ),
            ),
        ).replace("\"pausesKept\":false,", "").replace(",\"pausesKept\":false", "")
        assertThat(before).doesNotContain("pausesKept")
        val read = (BackupCodec.decode(before) as BackupRead.Ok).backup.sessions.sortedBy { it.startedAtMillis }
        assertThat(read.map { it.pausesKept }).containsExactly(false, true, true).inOrder()
    }

    @Test
    fun `a pause no outing can have is dropped, and the outing kept`() {
        val start = 1_790_000_000_000
        val outing = session(start).copy(
            pauses = listOf(
                SessionPause(start + 5 * 60_000, start + 6 * 60_000),
                // Ends before it begins, and before the outing began: not pauses.
                SessionPause(start + 9 * 60_000, start + 8 * 60_000),
                SessionPause(start - 60_000, start + 60_000),
            ),
        )
        val read = BackupCodec.decode(BackupCodec.encode(full.copy(sessions = listOf(outing))))
        val sessions = (read as BackupRead.Ok).backup.sessions
        assertThat(sessions.single().pauses).containsExactly(SessionPause(start + 5 * 60_000, start + 6 * 60_000))
    }

    @Test
    fun `a backup carries the usual rounds - their names, their walks and the pairs said not the same`() {
        val rounds = full.copy(
            rounds = listOf(BackupRound("Park", listOf(1_790_000_000_000, 1_790_090_000_000, 1_790_180_000_000))),
            roundsApart = listOf(1_790_180_000_000L to 1_790_270_000_000L),
        )
        val text = BackupCodec.encode(rounds)
        assertThat(BackupCodec.decode(text)).isEqualTo(BackupRead.Ok(rounds))
        // Names and outings' starts: no place.
        assertThat(text).doesNotContain("latitude")
    }

    @Test
    fun `a file from before the rounds has none, and a round of one walk or a pair of one outing is dropped`() {
        val text = BackupCodec.encode(full).replace(
            "\"rounds\":[]",
            "\"rounds\":[{\"name\":\" \",\"walks\":[5,5]},{\"walks\":[1,2]}]",
        ).replace("\"roundsApart\":[]", "\"roundsApart\":[{\"first\":3,\"second\":3},{\"first\":1,\"second\":4}]")
        val read = (BackupCodec.decode(text) as BackupRead.Ok).backup
        assertThat(read.rounds).containsExactly(BackupRound(null, listOf(1L, 2L)))
        assertThat(read.roundsApart).containsExactly(1L to 4L)
        val before = BackupCodec.encode(full).replace(",\"rounds\":[]", "").replace(",\"roundsApart\":[]", "")
        assertThat((BackupCodec.decode(before) as BackupRead.Ok).backup.rounds).isEmpty()
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
