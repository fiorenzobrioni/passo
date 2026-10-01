package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.ways.WayRepository
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.designsystem.ways.placeNoteRes
import com.callbackdev.passo.core.designsystem.ways.wayNameRes
import com.callbackdev.passo.core.domain.ways.WayAnnouncement
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.WayStop
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayJourney
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A stage reached on the way under way (PLANNING.md §11 Phase 11), told once.
 *
 * Nothing here runs on a timer, and nothing wakes the phone: it watches the days the tracking
 * service writes anyway (in batches, PLANNING.md §4.5), and only while a way is under way. So
 * a stage reached on a walk is told with the batch that wrote it, a few minutes later at most
 * with the screen off, and a stage reached while the phone was off is told when it next writes.
 */
@Singleton
class WayNotifier
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val ways: WayRepository,
    private val preferences: UserPreferencesDataSource,
) {
    private val notifications = WayNotifications(context)
    private val started = AtomicBoolean(false)

    /** Watches for as long as the process lives. Called once, by the application. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        notifications.ensureChannel()
        Background.launch {
            ways.active
                .flatMapLatest { journey ->
                    if (journey == null) {
                        flowOf(null)
                    } else {
                        ways.observeDaysFrom(journey.startEpochDay).map { journey to it }
                    }
                }
                .collect { current -> current?.let { (journey, days) -> guarded { check(journey, days) } } }
        }
    }

    private suspend fun check(journey: WayJourney, days: Map<Long, Double>) {
        val way = Ways.of(journey.way)
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val progress = WayProgress.of(way, journey, days, today)
        val stage = WayAnnouncement.next(way, progress.walkedMeters, journey.toldMeters)
        // Claimed before it is told: two writes close together cannot tell it twice.
        if (stage != null && ways.claimTold(journey.id, stage.distanceMeters)) {
            notifications.post(notifications.stage(stage, progress, preferences.current().settings.units))
        }
        progress.finishedEpochDay?.let { ways.finish(journey.id, it) }
    }

    /** A failure costs one notification, logged, never the process. */
    private suspend fun guarded(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Telling a stage failed", e)
        }
    }

    private companion object {
        const val TAG = "PassoWays"
        val Background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

/**
 * The ways' notifications: a stage reached, and the end. A channel of their own, `ways`, at the
 * default importance: the reader started the way, and the channel is theirs to silence.
 */
internal class WayNotifications(private val context: Context) {
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.ways_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.ways_channel_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun stage(stage: WayStop, progress: WayProgress, units: UnitPreference): Notification {
        val res = context.resources
        val format = context.measureFormatter(units)
        val place = res.getString(placeNameRes(stage.key))
        val way = res.getString(wayNameRes(progress.way.id))
        val next = progress.nextStage
        val title: String
        val text: String
        if (next == null) {
            // Counted like the days of a trip: the start day is the first.
            val end = progress.finishedEpochDay ?: progress.last.epochDay
            val days = (end - progress.journey.startEpochDay + 1).toInt()
            title = res.getString(R.string.way_finished_title, place)
            text = res.getQuantityString(
                R.plurals.way_finished_text,
                days,
                way,
                res.format(format.distance(progress.way.lengthMeters.toDouble())),
                days,
            )
        } else {
            title = res.getString(R.string.way_stage_title, place)
            text = res.getString(
                R.string.way_stage_text,
                way,
                res.format(format.distance(progress.walkedMeters)),
                res.format(format.distance(progress.way.lengthMeters.toDouble())),
                res.getString(placeNameRes(next.key)),
                res.format(format.distance(progress.metersTo(next))),
            )
        }
        val note = placeNoteRes(stage.key)?.let(res::getString)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentIntent(context.openAppIntent(REQUEST_WAY) { putExtra(WayIntents.EXTRA_OPEN_WAYS, true) })
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOfNotNull(text, note).joinToString("\n")))
            .build()
    }

    /** Posts [notification]; a no-op without the notification permission. */
    fun post(notification: Notification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(ID_WAY, notification)
    }

    companion object {
        const val CHANNEL_ID = "ways"
        const val ID_WAY = 6
        private const val REQUEST_WAY = 16
    }
}

/** What a stage's notification asks of the app when touched. */
object WayIntents {
    /** On the app's launch intent: open the Ways page. */
    const val EXTRA_OPEN_WAYS = "com.callbackdev.passo.extra.OPEN_WAYS"
}
