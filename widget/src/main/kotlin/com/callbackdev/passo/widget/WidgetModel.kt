package com.callbackdev.passo.widget

import android.content.Context
import android.util.Log
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.withPending
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.HourlySteps
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.TypicalDay
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.core.tracking.StepTracking
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a widget knows when it draws: the same numbers Today shows, computed the same way
 * ([TodayOverview] over the stored minutes plus the service's buffer), read at render time and
 * never invented. [day] is null when there is no day to draw ([CountingState.hasCount]).
 */
data class WidgetModel(
    val look: WidgetLook,
    val settings: UserSettings,
    val state: CountingState,
    val day: WidgetDay?,
    /** The day could not be read (an error, or a read that did not finish in time): said, not waited on. */
    val unavailable: Boolean = false,
) {
    companion object {
        /** A card that says it could not read the day, in the default dress. */
        fun unavailable(): WidgetModel =
            WidgetModel(WidgetLook(), UserSettings(), CountingState.COUNTING, null, unavailable = true)
    }
}

/**
 * Today, as a widget draws it.
 *
 * @property nowMinute the minute of the day the model was read at: the hour the bars highlight.
 */
data class WidgetDay(val overview: TodayOverview, val hourly: HourlySteps, val nowMinute: Int)

/**
 * Reads a [WidgetModel]. The typical day is computed once per day and zone (it rests on past
 * days only, so it cannot change before midnight) rather than at every repaint: the widgets
 * repaint up to once a minute while the screen is on, Today does it only while it is open.
 */
@Singleton
class WidgetModelLoader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesDataSource,
    private val tracking: TrackingRepository,
    private val liveSteps: LiveSteps,
    private val looks: WidgetLookStore,
) {
    private val typicalLock = Mutex()
    private var typicalKey: Pair<Long, ZoneId>? = null
    private var typical: TypicalDay? = null

    suspend fun load(appWidgetId: Int): WidgetModel {
        val stored = preferences.current()
        val settings = stored.settings
        val look = looks.lookFor(appWidgetId)
        val state = CountingState.of(
            hasSensor = StepTracking.hasStepCounter(context),
            onboarded = settings.onboardingCompleted,
            hasPermission = StepTracking.hasActivityRecognition(context),
            enabled = settings.trackingEnabled,
            serviceRunning = liveSteps.serviceRunning.value,
        )
        if (!state.hasCount) return WidgetModel(look, settings, state, null)

        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val day = now.toLocalDate().toEpochDay()
        val live = liveSteps.today.value?.takeIf { it.localEpochDay == day }
        val minutes = tracking.minutesOn(listOf(day))[day].orEmpty()
            .withPending(live?.pending.orEmpty())
            .map { DayMinute(minuteOfDay(it.epochMinute, it.localEpochDay, zone), it.steps) }
        val nowMinute = now.hour * MINUTES_PER_HOUR + now.minute
        val overview = TodayOverview.of(
            minutes = minutes,
            profile = stored.profile,
            goalSteps = settings.dailyGoalSteps,
            nowMinute = nowMinute.toDouble(),
            typical = if (settings.typicalDayLine) typicalFor(day, zone) else null,
            liveSteps = live?.steps,
        )
        return WidgetModel(look, settings, state, WidgetDay(overview, HourlySteps.of(minutes), nowMinute))
    }

    /**
     * [load] for a card that is waiting to be drawn: never longer than [LOAD_TIMEOUT_MILLIS] and
     * never an exception, because Glance shows its loading spinner until `provideContent` is
     * reached and a card stuck on it says nothing to anybody (device report, 25 Sep 2026). A
     * failure is logged under [TAG] and drawn as a card that says the day could not be read, and
     * the next repaint tries again.
     */
    suspend fun loadForCard(appWidgetId: Int): WidgetModel =
        guardedLoad(LOAD_TIMEOUT_MILLIS, "widget $appWidgetId") { load(appWidgetId) }

    private suspend fun typicalFor(day: Long, zone: ZoneId): TypicalDay? = typicalLock.withLock {
        val key = day to zone
        if (typicalKey != key) {
            typical = tracking.typicalDay(day, zone)
            typicalKey = key
        }
        typical
    }

    companion object {
        /** The log tag of everything the widgets report: `adb logcat -s PassoWidget`. */
        const val TAG: String = "PassoWidget"

        /** A read is a few queries on the phone's own storage; ten seconds is something wrong. */
        const val LOAD_TIMEOUT_MILLIS: Long = 10_000L
        private const val MINUTES_PER_HOUR = 60
    }
}

/**
 * [load], bounded by [timeoutMillis] and never throwing but for cancellation: a failure is logged
 * as [what] and becomes [WidgetModel.unavailable]. Split off the loader so a test can pin it.
 */
internal suspend fun guardedLoad(timeoutMillis: Long, what: String, load: suspend () -> WidgetModel): WidgetModel =
    try {
        withTimeout(timeoutMillis) { load() }
    } catch (e: TimeoutCancellationException) {
        logWidgetFailure("Reading today for $what took over $timeoutMillis ms", e)
        WidgetModel.unavailable()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logWidgetFailure("Reading today for $what failed", e)
        WidgetModel.unavailable()
    }

/** The log line of a failed read; quiet on a JVM without Android's log, where the tests run. */
private fun logWidgetFailure(message: String, error: Throwable) {
    runCatching { Log.w(WidgetModelLoader.TAG, message, error) }
}
