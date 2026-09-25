package com.callbackdev.passo.core.tracking

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.callbackdev.passo.core.data.prefs.UserPreferences
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.LiveToday
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.withPending
import com.callbackdev.passo.core.data.widget.WidgetUpdates
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.tracking.StepLedger
import com.callbackdev.passo.core.domain.widget.WidgetEvent
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.MinuteSteps
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Keeps the step counter read for as long as the phone is on (PLANNING.md §4.1, §4.2).
 *
 * A foreground service of type `health`, because background apps get no sensor events and
 * `ACTION_SHUTDOWN` reaches only receivers registered at runtime: without a live process the
 * last steps before a power-off would be lost at the reboot.
 *
 * Battery (§9): the sensor is the non-wake-up one; with the screen off it batches for up to
 * [SCREEN_OFF_LATENCY_US] and nothing here runs on a timer. The ticker and the notification
 * updates live only while the screen is on. Everything runs on the main thread, so the ledger
 * needs no locking; the database work runs on Room's own executor.
 *
 * The home-screen widgets hear from here what moves them ([WidgetUpdates], PLANNING.md §7): the
 * screen coming on (after the flush, so the batch of the screen-off is in), the count, a new
 * day, and the service itself starting and stopping. Whether that repaints is the widgets'
 * policy; this side only reports, and reports nothing on a timer.
 */
@AndroidEntryPoint
class StepTrackingService : Service() {
    @Inject lateinit var repository: TrackingRepository

    @Inject lateinit var liveSteps: LiveSteps

    @Inject lateinit var widgets: WidgetUpdates

    @Inject lateinit var preferencesSource: UserPreferencesDataSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val persistMutex = Mutex()

    private lateinit var snapshots: SystemSnapshots
    private lateinit var notifications: TrackingNotifications
    private lateinit var sensorSource: StepSensorSource
    private lateinit var powerManager: PowerManager

    private var ledger: StepLedger? = null
    private var started = false
    private var receiversRegistered = false
    private var interactive = false
    private var storedToday: StoredDay? = null

    // The goal, the profile and the units, for the expanded notification: held as they change,
    // never read on a timer.
    private var preferences: UserPreferences? = null

    // Today's stored minutes, read for the notification only when it is drawn, and read again
    // only after a write ([storedVersion]): at most one query a minute, with the screen on.
    private var storedVersion = 0L
    private var minutesCache: StoredMinutes? = null

    // Today's steps drained from the ledger and being written: counted on screen until the
    // stored total is read back with them in it, so the number never dips during a write.
    private var inFlightToday = 0
    private var pendingFlush: CompletableDeferred<Unit>? = null
    private var screenJob: Job? = null
    private var tickerJob: Job? = null
    private var notifyJob: Job? = null
    private var renderJob: Job? = null

    // What the notification last said. The app starts the service again every time it is opened
    // (to be sure it counts), and startForeground posts the notification again: with this it
    // keeps its expanded form instead of falling back to the count until the next step.
    private var shownContent: NotificationContent? = null
    private var lastNotifyElapsed = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        snapshots = SystemSnapshots(contentResolver)
        notifications = TrackingNotifications(this).also { it.ensureChannel() }
        sensorSource = StepSensorSource(
            sensorManager = requireNotNull(getSystemService(SensorManager::class.java)) { "No SensorManager" },
            handler = Handler(Looper.getMainLooper()),
        )
        powerManager = requireNotNull(getSystemService(PowerManager::class.java)) { "No PowerManager" }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Also reached on a sticky restart after the process was killed, when the permission
        // may be gone: stop instead of failing in startForeground.
        if (StepTracking.readiness(this) != TrackingReadiness.READY) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Only today's: after midnight the last one would show yesterday's numbers.
        val content = displayedToday()?.let { steps -> shownContent ?: NotificationContent(steps) }
            ?: NotificationContent(null)
        try {
            ServiceCompat.startForeground(
                this,
                TrackingNotifications.NOTIFICATION_ID,
                notifications.build(content),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "The health service type was refused", e)
            stopSelf()
            return START_NOT_STICKY
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "Foreground start not allowed from here", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!started) {
            started = true
            startTracking()
        } else if (interactive) {
            // Opened from the app, so someone is looking: the numbers catch up at once.
            notifyNow()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        liveSteps.publish(null)
        liveSteps.setServiceRunning(false)
        unregisterReceivers()
        sensorSource.close()
        // Whatever is still buffered is written on a scope that outlives this one. If the
        // process dies first, the stored state is older than the counter and the next sample
        // recomputes the delta: nothing is lost, only the minutes may shift.
        // A stopped service cannot repaint at the next screen-on, so the widgets hear it now,
        // after the last write, and show the count it stopped at.
        val batch = ledger?.drain()
        if (batch != null) {
            FinalWrites.launch {
                persistMutex.withLock { repository.persist(batch, System.currentTimeMillis()) }
                widgets.notify(WidgetEvent.TRACKING_STATE)
            }
        } else {
            widgets.notify(WidgetEvent.TRACKING_STATE)
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun startTracking() {
        scope.launch {
            // A state restored from a backup of another installation is not this counter's.
            val adoption = repository.adoptTrackerState(installedAtMillis())
            val ledger = StepLedger(adoption.state)
            this@StepTrackingService.ledger = ledger
            ledger.note(
                DiagnosticsEvent(System.currentTimeMillis(), DiagnosticsType.SERVICE_START, sensorSource.describe()),
            )
            if (adoption.restored) {
                ledger.note(
                    DiagnosticsEvent(System.currentTimeMillis(), DiagnosticsType.RESTORED, "tracker state dropped"),
                )
            }
            registerReceivers()
            launch { sensorSource.readings.collect(::onReading) }
            launch { preferencesSource.data.distinctUntilChanged().collect(::onPreferences) }
            persist()
            liveSteps.setServiceRunning(true)
            widgets.notify(WidgetEvent.TRACKING_STATE)
            onScreenChanged(powerManager.isInteractive)
        }
    }

    /** When this installation of the app began: renewed by a reinstall or a restore, kept by an update. */
    private fun installedAtMillis(): Long = packageManager.getPackageInfo(packageName, 0).firstInstallTime

    private fun onReading(reading: SensorReading) {
        when (reading) {
            is SensorReading.Sample -> {
                val ledger = ledger ?: return
                if (ledger.record(reading.sample, snapshots.current())) scope.launch { persist() }
                publishLive()
                scheduleNotification()
                displayedToday()?.let { widgets.notify(WidgetEvent.STEPS, it) }
            }

            SensorReading.FlushCompleted -> {
                pendingFlush?.complete(Unit)
                pendingFlush = null
            }
        }
    }

    // --- Persistence (PLANNING.md §4.5) --------------------------------------------------------

    /** Writes the buffer in one transaction. Never cancelled halfway, so never half-counted. */
    private suspend fun persist() = withContext(NonCancellable) {
        persistMutex.withLock {
            val ledger = ledger ?: return@withLock
            val batch = ledger.drain()
            if (batch != null) {
                val day = today()
                inFlightToday = batch.increments.filter { it.localEpochDay == day }.sumOf { it.steps }
                try {
                    repository.persist(batch, System.currentTimeMillis())
                } catch (e: Exception) {
                    // Back in the buffer: the next write carries it again, with the live state.
                    Log.e(TAG, "Writing the step buffer failed", e)
                    ledger.restore(batch)
                    inFlightToday = 0
                    return@withLock
                }
            }
            refreshStoredTodayLocked()
            inFlightToday = 0
            publishLive()
        }
    }

    private suspend fun refreshStoredTodayLocked() {
        val day = today()
        storedToday = StoredDay(day, repository.stepsOn(day))
        storedVersion++
    }

    /**
     * Asks the sensor for its batched events and waits until they have all been accounted,
     * for at most [FLUSH_TIMEOUT_MS]. Returns false on a timeout or if the sensor refused.
     */
    private suspend fun flushSensor(): Boolean {
        // A flush already in flight is shared: its completion covers this request too.
        val done = pendingFlush ?: CompletableDeferred<Unit>().also { fresh ->
            pendingFlush = fresh
            if (!sensorSource.requestFlush()) {
                pendingFlush = null
                return false
            }
        }
        return withTimeoutOrNull(FLUSH_TIMEOUT_MS) { done.await() } != null
    }

    // --- Screen, shutdown, time (PLANNING.md §4.2, §4.3) --------------------------------------

    private fun onScreenChanged(on: Boolean) {
        interactive = on
        screenJob?.cancel()
        screenJob = scope.launch {
            if (on) {
                // What was batched while the screen was off, then live events.
                flushSensor()
                sensorSource.register(SCREEN_ON_LATENCY_US)
                startTicker()
                notifyNow()
                widgets.notify(WidgetEvent.SCREEN_ON)
            } else {
                widgets.notify(WidgetEvent.SCREEN_OFF)
                tickerJob?.cancel()
                notifyJob?.cancel()
                flushSensor()
                persist()
                sensorSource.register(SCREEN_OFF_LATENCY_US)
            }
        }
    }

    private fun onShutdown(pending: BroadcastReceiver.PendingResult) {
        scope.launch {
            try {
                val flushed = flushSensor()
                ledger?.note(
                    DiagnosticsEvent(
                        System.currentTimeMillis(),
                        DiagnosticsType.SHUTDOWN_FLUSH,
                        if (flushed) "flushed" else "flush timed out",
                    ),
                )
                persist()
            } finally {
                pending.finish()
            }
        }
    }

    private fun onTimeChanged(action: String) {
        scope.launch {
            ledger?.note(DiagnosticsEvent(System.currentTimeMillis(), DiagnosticsType.TIME_CHANGED, action))
            persist()
            if (interactive) notifyNow()
            // The date may have moved with the clock; the policy repaints only with the screen on.
            widgets.notify(WidgetEvent.DAY_CHANGED)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenChanged(false)

                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT ->
                    if (!interactive) {
                        onScreenChanged(true)
                    } else {
                        scope.launch {
                            flushSensor()
                            notifyNow()
                            widgets.notify(WidgetEvent.SCREEN_ON)
                        }
                    }
            }
        }
    }

    private val shutdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onShutdown(goAsync())
        }
    }

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onTimeChanged(intent.action.orEmpty())
        }
    }

    private fun registerReceivers() {
        if (receiversRegistered) return
        val screen = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        // Delivered only to receivers registered at runtime (PLANNING.md §4.1).
        val shutdown = IntentFilter().apply {
            addAction(Intent.ACTION_SHUTDOWN)
            addAction(ACTION_QUICKBOOT_POWEROFF)
        }
        val time = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        // Not exported: these are system broadcasts, which still reach a non-exported receiver.
        val flags = ContextCompat.RECEIVER_NOT_EXPORTED
        ContextCompat.registerReceiver(this, screenReceiver, screen, flags)
        ContextCompat.registerReceiver(this, shutdownReceiver, shutdown, flags)
        ContextCompat.registerReceiver(this, timeReceiver, time, flags)
        receiversRegistered = true
    }

    private fun unregisterReceivers() {
        if (!receiversRegistered) return
        unregisterReceiver(screenReceiver)
        unregisterReceiver(shutdownReceiver)
        unregisterReceiver(timeReceiver)
        receiversRegistered = false
    }

    // --- The notification, only while someone can see it (PLANNING.md §8) ---------------------

    private fun displayedToday(): Int? {
        val stored = storedToday ?: return null
        val day = today()
        if (stored.day != day) return null
        return stored.steps + inFlightToday + (ledger?.pendingStepsOn(day) ?: 0)
    }

    /**
     * Hands today's live count to the screens ([LiveSteps]). The pending minutes exclude the
     * batch being written: a screen adds them to the stored minutes, which will hold that
     * batch once the write lands, and must never count it twice.
     */
    private fun publishLive() {
        val steps = displayedToday()
        val day = today()
        liveSteps.publish(steps?.let { LiveToday(day, it, ledger?.pendingOn(day).orEmpty()) })
    }

    /** At most one update every [NOTIFY_INTERVAL_MS], trailing, and only with the screen on. */
    private fun scheduleNotification() {
        if (!interactive || notifyJob?.isActive == true) return
        val wait = NOTIFY_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastNotifyElapsed)
        notifyJob = scope.launch {
            if (wait > 0) delay(wait)
            notifyNow()
        }
    }

    private fun notifyNow() {
        lastNotifyElapsed = SystemClock.elapsedRealtime()
        // A newer update replaces one still reading: it would post older numbers.
        renderJob?.cancel()
        renderJob = scope.launch {
            val content = notificationContent()
            shownContent = content
            notifications.update(content)
        }
    }

    /** A new goal, profile or units: the numbers change, and are shown if someone can see them. */
    private fun onPreferences(preferences: UserPreferences) {
        this.preferences = preferences
        if (interactive) notifyNow()
    }

    /**
     * Today for the notification: the same [TodayOverview] as Today and the widgets, over the
     * stored minutes plus the buffered ones. Without the usual day: the notification tells the
     * way to the goal, which needs no history.
     */
    private suspend fun notificationContent(): NotificationContent {
        val steps = displayedToday()
        val preferences = preferences ?: return NotificationContent(steps)
        if (steps == null) return NotificationContent(null)
        val zone = ZoneId.systemDefault()
        val day = LocalDate.now(zone).toEpochDay()
        // A read that fails costs the expanded form of this one update, never the service.
        val stored = try {
            storedMinutes(day)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Reading today's minutes for the notification failed", e)
            return NotificationContent(steps)
        }
        val minutes = stored.withPending(ledger?.pendingOn(day).orEmpty())
            .map { DayMinute(minuteOfDay(it.epochMinute, it.localEpochDay, zone), it.steps) }
        val now = LocalTime.now(zone)
        val overview = TodayOverview.of(
            minutes = minutes,
            profile = preferences.profile,
            goalSteps = preferences.settings.dailyGoalSteps,
            nowMinute = (now.hour * MINUTES_PER_HOUR + now.minute).toDouble(),
            typical = null,
            liveSteps = steps,
        )
        return NotificationContent(steps, overview, preferences.settings.units)
    }

    private suspend fun storedMinutes(day: Long): List<MinuteSteps> {
        minutesCache?.takeIf { it.day == day && it.version == storedVersion }?.let { return it.minutes }
        // Taken before the read: a write that lands during it makes the next update read again.
        val version = storedVersion
        val minutes = repository.minutesOn(listOf(day))[day].orEmpty()
        minutesCache = StoredMinutes(day, version, minutes)
        return minutes
    }

    /** Screen on only: notices the day rolling over, so the notification starts again from 0. */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(MILLIS_PER_MINUTE - System.currentTimeMillis() % MILLIS_PER_MINUTE)
                if (storedToday?.day != today()) {
                    persistMutex.withLock { refreshStoredTodayLocked() }
                    publishLive()
                    notifyNow()
                    widgets.notify(WidgetEvent.DAY_CHANGED)
                }
            }
        }
    }

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private data class StoredDay(val day: Long, val steps: Int)

    private class StoredMinutes(val day: Long, val version: Long, val minutes: List<MinuteSteps>)

    private companion object {
        const val TAG = "StepTracking"

        /** Screen off: let the sensor hub batch for up to ten minutes (PLANNING.md §4.3). */
        const val SCREEN_OFF_LATENCY_US = 10 * 60 * 1_000_000

        /** Screen on: someone may be looking, deliver within a second. */
        const val SCREEN_ON_LATENCY_US = 1_000_000

        /** How long a flush may take before the shutdown gives up on it (PLANNING.md §4.2). */
        const val FLUSH_TIMEOUT_MS = 1_500L

        const val NOTIFY_INTERVAL_MS = 5_000L
        const val MILLIS_PER_MINUTE = 60_000L
        const val MINUTES_PER_HOUR = 60

        /** HTC and a few other vendors' fast power-off, which skips ACTION_SHUTDOWN. */
        const val ACTION_QUICKBOOT_POWEROFF = "android.intent.action.QUICKBOOT_POWEROFF"

        /** Outlives the service, so onDestroy can still write the last buffer. */
        val FinalWrites = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
