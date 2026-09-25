package com.callbackdev.passo.core.tracking

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.callbackdev.passo.core.data.prefs.UserPreferences
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.LiveToday
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.byDayWithLive
import com.callbackdev.passo.core.data.tracking.withPending
import com.callbackdev.passo.core.domain.goals.EveningReminder
import com.callbackdev.passo.core.domain.goals.GoalSchedule
import com.callbackdev.passo.core.domain.goals.WeeklySummary
import com.callbackdev.passo.core.domain.insights.Insights
import com.callbackdev.passo.core.domain.settings.firstDayOfWeek
import com.callbackdev.passo.core.domain.today.DayCurve
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Goal reached, the evening reminder and the weekly summary (PLANNING.md §8, §11 Phase 6):
 * when each is due, and what each says.
 *
 * Nothing here runs on a timer. "Goal reached" rides on the sensor events the tracking service
 * already receives; the reminder and the summary are one alarm each ([GoalAlarms]), armed again
 * whenever what they depend on changes: a setting ([start]), the goal met (the reminder moves
 * to tomorrow), the clock or the zone ([GoalAlarmReceiver]), and each firing.
 */
@Singleton
class GoalNotifier
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesDataSource,
    private val tracking: TrackingRepository,
    private val liveSteps: LiveSteps,
    private val link: TrackerLink,
) {
    private val notifications = GoalNotifications(context)
    private val alarms = GoalAlarms(context)
    private val scheduleLock = Mutex()
    private val started = AtomicBoolean(false)

    /**
     * Keeps the alarms in step with the settings for as long as the process lives: armed at
     * every start of the process (a boot or an update starts it, and a reboot clears every
     * alarm) and again at every change that moves them. Called once, by the application.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        // Made up front, so Settings can open its page before the first notification.
        notifications.ensureChannel()
        Background.launch {
            preferences.data
                .map { ScheduleInputs(it.settings) }
                .distinctUntilChanged()
                .collect { guarded("Arming the goal alarms") { reschedule() } }
        }
    }

    /** Arms, moves or cancels both alarms for what the settings and today say now. */
    suspend fun reschedule() = scheduleLock.withLock {
        val settings = preferences.current().settings
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        if (settings.eveningReminder && settings.trackingEnabled) {
            val today = LocalDate.now(zone)
            // A day already at its goal has nothing left to remind: no wake for it.
            val met = todaySteps(today.toEpochDay()) >= settings.dailyGoalSteps
            val earliest = if (met) today.plusDays(1) else null
            alarms.setEveningReminder(GoalSchedule.nextReminder(now, zone, settings.eveningReminderTime, earliest))
        } else {
            alarms.cancelEveningReminder()
        }
        if (settings.weeklySummary) {
            alarms.setWeeklySummary(GoalSchedule.nextWeeklySummary(now, zone, weekStart(settings)))
        } else {
            alarms.cancelWeeklySummary()
        }
    }

    /**
     * The tracking service saw today's count reach the goal. Told once a day: the day is claimed
     * in the store first, so a reboot, a clock change or a second caller cannot tell it again,
     * and it is claimed with the notification off too, so turning it on later that day does not
     * announce the morning (`GoalReached`). On a scope of its own: the service may stop meanwhile.
     */
    fun goalReached(epochDay: Long) {
        Background.launch { tellGoalReached(epochDay) }
    }

    internal suspend fun tellGoalReached(epochDay: Long) = guarded("Telling the goal reached") {
        if (!preferences.claimGoalNoticeDay(epochDay)) return@guarded
        reschedule()
        val current = preferences.current()
        if (!current.settings.goalReachedNotification) return@guarded
        val content = goalReachedContent(epochDay, current)
        notifications.ensureChannel()
        notifications.post(
            GoalNotifications.ID_GOAL_REACHED,
            notifications.goalReached(content, current.settings.units),
        )
    }

    /**
     * The evening reminder's alarm. The next one is armed first, whatever this one decides. It
     * speaks only if Passo is counting right now (a paused or stopped count would make it lie),
     * after catching up with the sensor, and only below the reader's share of the goal.
     */
    suspend fun eveningReminder(dueAt: Instant?) = guarded("The evening reminder") {
        reschedule()
        val settings = preferences.current().settings
        if (!settings.eveningReminder || countingState(settings) != CountingState.COUNTING) return@guarded
        val zone = ZoneId.systemDefault()
        if (dueAt != null && !GoalSchedule.reminderStillTimely(dueAt, Instant.now(), zone)) return@guarded
        withTimeoutOrNull(CATCH_UP_TIMEOUT_MILLIS) { link.catchUp() }
        val steps = todaySteps(LocalDate.now(zone).toEpochDay())
        val nudge = EveningReminder.check(steps, settings.dailyGoalSteps, settings.eveningReminderThresholdPercent)
            ?: return@guarded
        notifications.ensureChannel()
        notifications.post(GoalNotifications.ID_EVENING_REMINDER, notifications.eveningReminder(nudge, settings.units))
    }

    /** The weekly summary's alarm: the week that has just ended, as History would show it. */
    suspend fun weeklySummary(dueAt: Instant?) = guarded("The weekly summary") {
        reschedule()
        val settings = preferences.current().settings
        if (!settings.weeklySummary) return@guarded
        val zone = ZoneId.systemDefault()
        val weekStart = weekStart(settings)
        if (dueAt != null && !GoalSchedule.summaryStillTimely(dueAt, Instant.now(), zone, weekStart)) {
            return@guarded
        }
        val days = tracking.observeAllSummaries().first().byDayWithLive(liveSteps.today.value, settings.dailyGoalSteps)
        val summary = WeeklySummary.ofLastWeek(
            days = days,
            today = LocalDate.now(zone),
            firstDayOfWeek = weekStart,
            firstRecordedDay = days.keys.minOrNull()?.let(LocalDate::ofEpochDay),
            currentGoal = settings.dailyGoalSteps,
        ) ?: return@guarded
        notifications.ensureChannel()
        notifications.post(GoalNotifications.ID_WEEKLY_SUMMARY, notifications.weeklySummary(summary, settings.units))
    }

    /** Today's count: the service's live one when it runs, the stored one otherwise. */
    private suspend fun todaySteps(epochDay: Long): Int {
        val live = liveSteps.today.value?.takeIf { it.localEpochDay == epochDay }
        return live?.steps ?: tracking.stepsOn(epochDay)
    }

    /**
     * The minute the goal was met (from today's minutes, stored and buffered: with the screen off
     * the service hears of it up to ten minutes late) and the streak it extends.
     */
    private suspend fun goalReachedContent(epochDay: Long, current: UserPreferences): GoalReachedContent {
        val zone = ZoneId.systemDefault()
        val goal = current.settings.dailyGoalSteps
        val live = liveSteps.today.value?.takeIf { it.localEpochDay == epochDay }
        val minutes = tracking.minutesOn(listOf(epochDay))[epochDay].orEmpty()
            .withPending(live?.pending.orEmpty())
            .map { DayMinute(minuteOfDay(it.epochMinute, it.localEpochDay, zone), it.steps) }
        val now = LocalTime.now(zone)
        val reachedAt = DayCurve.minuteReaching(minutes, goal) ?: (now.hour * MINUTES_PER_HOUR + now.minute)
        // Today's row is written once a minute and may not hold the goal yet: today is met.
        val count = maxOf(live?.steps ?: tracking.stepsOn(epochDay), goal)
        val days = tracking.observeAllSummaries().first().byDayWithLive(LiveToday(epochDay, count, emptyList()), goal)
        val streak = Insights.of(days, LocalDate.ofEpochDay(epochDay), weekStart(current.settings)).currentStreak.days
        return GoalReachedContent(goalSteps = goal, reachedAtMinute = reachedAt, streakDays = streak)
    }

    private fun countingState(settings: UserSettings): CountingState = CountingState.of(
        hasSensor = StepTracking.hasStepCounter(context),
        onboarded = settings.onboardingCompleted,
        hasPermission = StepTracking.hasActivityRecognition(context),
        enabled = settings.trackingEnabled,
        serviceRunning = liveSteps.serviceRunning.value,
    )

    // The first day of the week from the phone's region, as History reads it.
    private fun weekStart(settings: UserSettings): DayOfWeek =
        firstDayOfWeek(settings.firstDayOfWeek, Resources.getSystem().configuration.locales[0])

    /** A failure costs one notification or one arming, logged, never the caller (a receiver, the service). */
    private suspend fun guarded(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$what failed", e)
        }
    }

    /** What the alarms depend on among the settings: a change in anything else arms nothing. */
    private data class ScheduleInputs(
        val eveningReminder: Boolean,
        val time: LocalTime,
        val weeklySummary: Boolean,
        val firstDayOfWeek: DayOfWeek?,
        val trackingEnabled: Boolean,
        val goal: Int,
    ) {
        constructor(settings: UserSettings) : this(
            eveningReminder = settings.eveningReminder,
            time = settings.eveningReminderTime,
            weeklySummary = settings.weeklySummary,
            firstDayOfWeek = settings.firstDayOfWeek,
            trackingEnabled = settings.trackingEnabled,
            goal = settings.dailyGoalSteps,
        )
    }

    private companion object {
        const val TAG = "PassoGoals"

        /** The service bounds its flush at 1.5 s; this bounds the call around it. */
        const val CATCH_UP_TIMEOUT_MILLIS = 3_000L
        const val MINUTES_PER_HOUR = 60

        /** Outlives any one caller: the settings are watched for the whole process. */
        val Background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
