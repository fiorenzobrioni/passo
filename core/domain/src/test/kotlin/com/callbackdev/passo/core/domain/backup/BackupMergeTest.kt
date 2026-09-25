package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.domain.backup.BackupFixtures.DAY
import com.callbackdev.passo.core.domain.backup.BackupFixtures.backup
import com.callbackdev.passo.core.domain.backup.BackupFixtures.day
import com.callbackdev.passo.core.domain.backup.BackupFixtures.minutes
import com.callbackdev.passo.core.domain.backup.BackupFixtures.session
import com.callbackdev.passo.core.domain.metrics.DaySummaries
import com.callbackdev.passo.core.domain.metrics.MinuteChange
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupMergeTest {
    private val today = DAY + 3
    private val localProfile = Profile(heightMeters = 1.60, weightKg = 60.0)

    /** What a phone holds: its minutes by day and its summaries, as the merge reads them. */
    private class Phone(val minutes: MutableMap<Long, List<MinuteSteps>> = mutableMapOf()) {
        val summaries = mutableMapOf<Long, DailySummary>()

        fun apply(plan: DayMergePlan) {
            val all = minutes.values.flatten().associateBy { it.epochMinute }.toMutableMap()
            for (minute in plan.minutes) all[minute.epochMinute] = minute
            minutes.clear()
            minutes.putAll(all.values.groupBy { it.localEpochDay })
            for (summary in plan.summaries) summaries[summary.localEpochDay] = summary
        }
    }

    private fun merge(phone: Phone, backup: Backup, profile: Profile = localProfile, goal: Int = 7_000) =
        BackupMerge.days(backup, phone.minutes, phone.summaries, today, profile, goal)

    @Test
    fun `a clean install that imports has the history exactly as it was`() {
        val past = listOf(
            day(DAY, minutes(DAY, 600 to 104, 601 to 98, 1_200 to 12)),
            day(DAY + 1, minutes(DAY + 1, 480 to 60, 481 to 130), goal = 10_000),
        )
        val phone = Phone()

        val plan = merge(phone, backup(past))
        phone.apply(plan)

        assertThat(plan.addedDays).isEqualTo(2)
        assertThat(plan.mergedDays).isEqualTo(0)
        assertThat(phone.summaries.values).containsExactlyElementsIn(past.map { it.summary })
        assertThat(phone.minutes.values.flatten()).containsExactlyElementsIn(past.flatMap { it.minutes })
    }

    @Test
    fun `importing the same file twice changes nothing the second time`() {
        val file = backup(listOf(day(DAY, minutes(DAY, 600 to 104)), day(DAY + 1, minutes(DAY + 1, 700 to 90))))
        val phone = Phone()
        phone.apply(merge(phone, file))
        val after = phone.summaries.toMap()

        val again = merge(phone, file)

        assertThat(again.minutes).isEmpty()
        assertThat(again.summaries).isEmpty()
        assertThat(again.unchangedDays).isEqualTo(2)
        assertThat(phone.summaries).isEqualTo(after)
    }

    @Test
    fun `a minute both phones counted keeps the larger count, never the sum`() {
        val phone = Phone(mutableMapOf(DAY to minutes(DAY, 600 to 50, 601 to 120)))
        phone.summaries[DAY] = DaySummaries.summarize(DAY, listOf(50, 120), localProfile, 7_000, finalized = true)

        val plan = merge(phone, backup(listOf(day(DAY, minutes(DAY, 600 to 80, 601 to 100, 602 to 30)))))

        assertThat(plan.minutes.map { it.epochMinute % 1_440 to it.steps })
            .containsExactly(600L to 80, 602L to 30)
        assertThat(plan.mergedDays).isEqualTo(1)
    }

    @Test
    fun `a frozen day of this phone takes only the added steps' share, with the current profile`() {
        val mine = minutes(DAY, 600 to 50, 601 to 120)
        val frozen = DaySummaries.summarize(DAY, mine.map { it.steps }, localProfile, 7_000, finalized = true)
        val phone = Phone(mutableMapOf(DAY to mine))
        phone.summaries[DAY] = frozen

        val plan = merge(phone, backup(listOf(day(DAY, minutes(DAY, 600 to 80, 602 to 30)))))

        val expected = DaySummaries.withLateSteps(
            frozen,
            listOf(MinuteChange(50, 80), MinuteChange(0, 30)),
            localProfile,
        )
        assertThat(plan.summaries.single()).isEqualTo(expected)
        // The day keeps its own goal: the file does not rewrite how the day was judged.
        assertThat(plan.summaries.single().goalSteps).isEqualTo(7_000)
        assertThat(plan.summaries.single().steps).isEqualTo(80 + 120 + 30)
    }

    @Test
    fun `a day the file wholly covers comes in as the file froze it`() {
        val phone = Phone(mutableMapOf(DAY to minutes(DAY, 600 to 50)))
        phone.summaries[DAY] = DaySummaries.summarize(DAY, listOf(50), localProfile, 7_000, finalized = true)
        val theirs = day(DAY, minutes(DAY, 600 to 90, 601 to 100))

        val plan = merge(phone, backup(listOf(theirs)))

        assertThat(plan.summaries.single()).isEqualTo(theirs.summary)
    }

    @Test
    fun `the file's day still open when it was written is frozen with the file's profile`() {
        val open = day(DAY, minutes(DAY, 600 to 104, 601 to 98), finalized = false)

        val plan = merge(Phone(), backup(listOf(open)))

        val summary = plan.summaries.single()
        assertThat(summary.finalized).isTrue()
        assertThat(summary).isEqualTo(
            DaySummaries.summarize(DAY, listOf(104, 98), BackupFixtures.profile, 9_000, finalized = true),
        )
    }

    @Test
    fun `today's steps from both phones add up minute by minute, with today's goal and profile`() {
        val phone = Phone(mutableMapOf(today to minutes(today, 900 to 100, 901 to 110)))
        phone.summaries[today] = DaySummaries.summarize(today, listOf(100, 110), localProfile, 7_000, finalized = false)
        val morning = day(today, minutes(today, 480 to 120, 481 to 115), finalized = false)

        val plan = merge(phone, backup(listOf(morning)), goal = 7_500)

        val summary = plan.summaries.single()
        assertThat(summary.steps).isEqualTo(100 + 110 + 120 + 115)
        assertThat(summary.goalSteps).isEqualTo(7_500)
        assertThat(summary.finalized).isFalse()
        assertThat(summary).isEqualTo(
            DaySummaries.summarize(today, listOf(100, 110, 120, 115), localProfile, 7_500, finalized = false),
        )
    }

    @Test
    fun `a minute keeps the day this phone recorded it on`() {
        // Recorded here on the next day (a time zone further east); the file has it on DAY.
        val shared = BackupFixtures.minuteOf(DAY, 1_430)
        val phone = Phone(mutableMapOf(DAY + 1 to listOf(MinuteSteps(shared, DAY + 1, 40))))
        phone.summaries[DAY + 1] = DaySummaries.summarize(DAY + 1, listOf(40), localProfile, 7_000, true)

        val plan = merge(phone, backup(listOf(day(DAY, listOf(MinuteSteps(shared, DAY, 70))))))

        assertThat(plan.minutes.single()).isEqualTo(MinuteSteps(shared, DAY + 1, 70))
        assertThat(plan.summaries.map { it.localEpochDay }).contains(DAY + 1)
    }

    @Test
    fun `plans already here are matched, new ones are added once`() {
        val mine = BackupFixtures.plan.copy(id = 31, position = 0, lastUsedAtMillis = null)
        val other = BackupFixtures.plan.copy(id = 5, name = "Hill", position = 1)

        val merge = BackupMerge.plans(local = listOf(mine), incoming = listOf(BackupFixtures.plan, other, other))

        assertThat(merge.matched).containsExactly(BackupFixtures.plan.id, 31L)
        assertThat(merge.toAdd).containsExactly(other)
    }

    @Test
    fun `outings already here are skipped, one under way is closed at its last step`() {
        val done = session(1_000_000)
        val live = session(2_000_000, planId = 9, state = SessionState.ACTIVE)

        val added = BackupMerge.sessions(
            local = listOf(done),
            incoming = listOf(live, done),
            planIds = mapOf(4L to 31L),
        )

        val closed = added.single()
        assertThat(closed.startedAtMillis).isEqualTo(2_000_000)
        assertThat(closed.state).isEqualTo(SessionState.FINISHED)
        assertThat(closed.end).isEqualTo(SessionEnd.CLOSED)
        assertThat(closed.endedAtMillis).isEqualTo(live.lastStepAtMillis)
        // Its plan did not come: the outing keeps its goal, not a plan id of another phone.
        assertThat(closed.planId).isNull()
        assertThat(BackupMerge.sessions(emptyList(), listOf(done), mapOf(4L to 31L)).single().planId).isEqualTo(31L)
    }

    @Test
    fun `the file's settings come in, but not whether this phone counts or has been set up`() {
        val current = UserSettings(trackingEnabled = false, onboardingCompleted = true, dailyGoalSteps = 6_000)
        val incoming = UserSettings(trackingEnabled = true, onboardingCompleted = false, dailyGoalSteps = 11_000)

        val merged = BackupMerge.settings(current, incoming)

        assertThat(merged.dailyGoalSteps).isEqualTo(11_000)
        assertThat(merged.trackingEnabled).isFalse()
        assertThat(merged.onboardingCompleted).isTrue()
    }
}
