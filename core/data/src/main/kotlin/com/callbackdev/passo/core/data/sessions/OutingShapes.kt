package com.callbackdev.passo.core.data.sessions

import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.time.TodaySource
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.withPending
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.sessions.OutingMinutes
import com.callbackdev.passo.core.domain.sessions.OutingShape
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The shape of an outing as its card and its page draw it (PLANNING.md §11 Phase 21, step 21.3):
 * its minutes from `minute_steps` with its pauses as gaps, its route so far, and the goal of its
 * day. One source for every screen that shows it, so the card on Today, on the Outings page and the
 * outing's page say the same.
 *
 * Nothing here runs on its own: each part is read only while someone collects it, which a page
 * does only while it is on screen (`WhileSubscribed`), so with the screen off nothing is read
 * (§9). The route, the costliest part, is read again only when its point count changes, off the
 * main thread, as the outing's page reads it; the minutes move with the service's live count, a
 * batch fresher than what is written.
 */
@Singleton
class OutingShapes
@Inject
constructor(
    private val tracking: TrackingRepository,
    private val routes: RouteRepository,
    private val liveSteps: LiveSteps,
    private val settings: SettingsRepository,
    private val todaySource: TodaySource,
) {
    /**
     * The shape of the outing [outing] holds, as it changes; null while it holds none. A new outing
     * starts its reads anew; the same outing moving on (its steps, its pauses) only recomputes.
     *
     * @param minutesOn the stored minutes of a day: Today passes the flow its day's chart already
     *   reads, any other page the repository's.
     * @param withRoute read the route too; the outing's page, which reads it for its map, does not.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun of(
        outing: Flow<Session?>,
        minutesOn: (Long) -> Flow<List<MinuteSteps>> = tracking::observeMinutesOn,
        withRoute: Boolean = true,
        clock: () -> Long = System::currentTimeMillis,
    ): Flow<OutingShape?> = channelFlow {
        // The outing is collected once: its identity starts the reads, its latest state feeds them.
        val latest = MutableStateFlow<Session?>(null)
        launch { outing.collect { latest.value = it } }
        latest
            .map { session -> session?.let { key(it, clock()) } }
            .distinctUntilChanged()
            .flatMapLatest { key ->
                if (key == null) {
                    flowOf(null)
                } else {
                    shape(key, latest.filterNotNull(), minutesOn, withRoute, clock)
                }
            }
            .distinctUntilChanged()
            .collect { send(it) }
    }

    private fun shape(
        key: Key,
        outing: Flow<Session>,
        minutesOn: (Long) -> Flow<List<MinuteSteps>>,
        withRoute: Boolean,
        clock: () -> Long,
    ): Flow<OutingShape> {
        val session = outing.map { it.takeIf { s -> s.id == key.id } }.filterNotNull()
        // The days it spans: its own, and the next ones up to now or its end, across midnight.
        val days = (key.day..maxOf(key.day, key.lastDay)).toList().take(MAX_DAYS)
        val stored = combine(days.map(minutesOn)) { lists -> lists.flatMap { it } }
        val rows = combine(stored, liveSteps.today) { written, live ->
            if (live != null && live.localEpochDay in days) written.withPending(live.pending) else written
        }
        val minutes = combine(session, rows) { s, r -> OutingMinutes.of(s, r, clock()) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
        val route = if (withRoute && key.gps) route(key) else flowOf(null)
        val today = key.day == todaySource.epochDay()
        // The day's goal as Today shows it: today's, which a change of goal moves; any other day's,
        // as that day stored it.
        val goal = if (today) {
            settings.settings.map { it.dailyGoalSteps }
        } else {
            combine(tracking.observeSummary(key.day), settings.settings) { summary, now ->
                summary?.goalSteps ?: now.dailyGoalSteps
            }
        }.distinctUntilChanged()
        return combine(minutes, route, goal) { m, r, g -> OutingShape(key.id, m, r, g, today) }
    }

    /** The route so far, read again only when its point count changes; null until it has a line. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun route(key: Key): Flow<RouteShape?> = routes.observePointCount(key.id)
        .distinctUntilChanged()
        .mapLatest { count ->
            if (count == null || count < 2) {
                null
            } else {
                withContext(Dispatchers.Default) {
                    routes.route(key.id, key.startedAtMillis)
                        ?.let { RouteShape.of(it.points) }
                        // Its lines and its frame made here, off the main thread, not on a frame.
                        ?.takeIf { it.lines.isNotEmpty() }
                        ?.also { it.frame }
                }
            }
        }

    /**
     * What starts an outing's reads anew: which outing it is, whether it measures with GPS, and
     * the last day it reaches, which moves at midnight.
     */
    private data class Key(val id: Long, val startedAtMillis: Long, val day: Long, val gps: Boolean, val lastDay: Long)

    private fun key(session: Session, nowMillis: Long): Key {
        val end = session.endedAtMillis ?: maxOf(nowMillis, session.lastEventAtMillis)
        val lastDay = Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        return Key(session.id, session.startedAtMillis, session.localEpochDay, session.gps, lastDay)
    }

    private companion object {
        /** An outing spans at most its own day and the next two: twelve hours, and «Keep going». */
        const val MAX_DAYS = 3
    }
}
