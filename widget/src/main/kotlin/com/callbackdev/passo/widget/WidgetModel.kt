package com.callbackdev.passo.widget

import android.content.Context
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a widget knows when it draws: the same numbers Today shows, computed the same way
 * ([TodayOverview] over the stored minutes plus the service's buffer), read at render time and
 * never invented. [day] is null when there is no day to draw ([CountingState.hasCount]).
 */
data class WidgetModel(val look: WidgetLook, val settings: UserSettings, val state: CountingState, val day: WidgetDay?)

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

    private suspend fun typicalFor(day: Long, zone: ZoneId): TypicalDay? = typicalLock.withLock {
        val key = day to zone
        if (typicalKey != key) {
            typical = tracking.typicalDay(day, zone)
            typicalKey = key
        }
        typical
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}
