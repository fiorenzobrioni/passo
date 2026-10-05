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
import com.callbackdev.passo.core.data.sessions.LiveSession
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.LiveToday
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.withPending
import com.callbackdev.passo.core.data.ways.WayRepository
import com.callbackdev.passo.core.data.widget.WidgetUpdates
import com.callbackdev.passo.core.designsystem.format.intervalWord
import com.callbackdev.passo.core.domain.goals.GoalReached
import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.IntervalSchedule
import com.callbackdev.passo.core.domain.sessions.SessionAnnouncement
import com.callbackdev.passo.core.domain.sessions.SessionConstants
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.sessions.SessionSignal
import com.callbackdev.passo.core.domain.sessions.SessionTracker
import com.callbackdev.passo.core.domain.sessions.SignalWake
import com.callbackdev.passo.core.domain.sessions.intervalAt
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.tracking.StepLedger
import com.callbackdev.passo.core.domain.ways.WalkDays
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.domain.widget.WidgetEvent
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayKind
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
 *
 * An outing (PLANNING.md §11 Phase 10) is measured here too, from the same samples: a
 * [SessionTracker] fed every accounted delta, written with the step batches in their
 * transaction. While one is under way, and only then, the sensor is the wake-up counter with a
 * [SESSION_LATENCY_US] latency, so its signals reach a phone in a pocket on time
 * (docs/adr/0009-sessions.md); paused, over, or with no outing, the registration is the usual
 * one. Commands (start, pause, resume, stop, keep going) arrive as intents ([SessionControl]).
 * An outing on a city walk (Phase 11) is one more: its places are told as its distance passes
 * them, by the same samples, with no timer of its own.
 *
 * An interval outing (Phase 13, docs/adr/0013-interval-walks.md) tells each change of interval
 * on time with the screen off: the wake-up counter at [SESSION_LATENCY_US], and at
 * [INTERVAL_NEAR_LATENCY_US] in the last [SessionConstants.INTERVAL_WINDOW_MILLIS] of motion
 * before a change, back after it. A change is told once the batch of samples that crossed it is
 * all in (the latest only, if it crossed several), with its delay in the diagnostics log. With the
 * screen on, a one-second ticker moves the notification's countdown, and stops with the screen.
 *
 * On a phone whose step counter cannot wake it, an outing that tells its signals keeps the
 * processor awake while it counts ([SignalWakeLock], [SignalWake]): the steps then arrive as they
 * are taken, and every signal with them. The one wake lock of Passo's own besides the shutdown
 * flush's; let go at a pause, the end, or the service's.
 */
@AndroidEntryPoint
class StepTrackingService : Service() {
    @Inject lateinit var repository: TrackingRepository

    @Inject lateinit var liveSteps: LiveSteps

    @Inject lateinit var widgets: WidgetUpdates

    @Inject lateinit var preferencesSource: UserPreferencesDataSource

    @Inject lateinit var goals: GoalNotifier

    @Inject lateinit var link: TrackerLink

    @Inject lateinit var sessions: SessionRepository

    @Inject lateinit var liveSession: LiveSession

    @Inject lateinit var ways: WayRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val persistMutex = Mutex()

    private lateinit var snapshots: SystemSnapshots
    private lateinit var notifications: TrackingNotifications
    private lateinit var sessionNotifications: SessionNotifications
    private lateinit var speech: SessionSpeech
    private lateinit var sensorSource: StepSensorSource
    private lateinit var powerManager: PowerManager
    private lateinit var signalWake: SignalWakeLock

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

    // The last day whose goal was seen reached, as stored: checked on every sample without a
    // read. The store has the last word (GoalNotifier claims the day there).
    private var goalToldDay: Long? = null

    // The outing under way or paused; after its goal, kept a while for "Keep going". Written
    // with the next batch when it changed.
    private var tracker: SessionTracker? = null
    private var sessionDirty = false

    // An interval outing's change found in a batch of samples, told once the batch is all in; and
    // the screen-on countdown of its notification.
    private var pendingChange: SessionSignal.IntervalChange? = null

    // How late each change of the interval outing under way was told, for its one log row.
    private val intervalDelays = mutableListOf<Long>()
    private var changeJob: Job? = null
    private var countdownJob: Job? = null

    // Session commands arrive with the start intents, and wait until tracking has started.
    private val commands = Channel<Intent>(Channel.UNLIMITED)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        snapshots = SystemSnapshots(contentResolver)
        notifications = TrackingNotifications(this).also { it.ensureChannel() }
        sessionNotifications = SessionNotifications(this).also { it.ensureChannel() }
        speech = SessionSpeech(this).also { voice ->
            voice.onCallEnded = { hidden -> intervalNowSentence() ?: hidden }
        }
        sensorSource = StepSensorSource(
            sensorManager = requireNotNull(getSystemService(SensorManager::class.java)) { "No SensorManager" },
            handler = Handler(Looper.getMainLooper()),
        )
        powerManager = requireNotNull(getSystemService(PowerManager::class.java)) { "No PowerManager" }
        signalWake = SignalWakeLock(powerManager) { detail ->
            ledger?.note(DiagnosticsEvent(System.currentTimeMillis(), DiagnosticsType.SIGNAL_WAKE, detail))
        }
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
        if (intent?.action in SESSION_ACTIONS) commands.trySend(requireNotNull(intent))
        if (!started) {
            started = true
            startTracking()
        } else if (interactive) {
            // Opened from the app, so someone is looking: the numbers catch up at once, and a
            // forgotten outing is closed where it ended.
            checkSession()
            notifyNow()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        link.attach(null)
        liveSteps.publish(null)
        liveSteps.setServiceRunning(false)
        liveSession.publish(null)
        speech.release()
        signalWake.hold(false)
        unregisterReceivers()
        sensorSource.close()
        // Whatever is still buffered is written on a scope that outlives this one. If the
        // process dies first, the stored state is older than the counter and the next sample
        // recomputes the delta: nothing is lost, only the minutes may shift.
        // A stopped service cannot repaint at the next screen-on, so the widgets hear it now,
        // after the last write, and show the count it stopped at.
        val batch = ledger?.drain()
        val outing = tracker?.takeIf { it.session.live }
        FinalWrites.launch {
            persistMutex.withLock {
                if (batch != null) {
                    repository.persist(batch, System.currentTimeMillis(), outing?.session)
                } else if (outing != null) {
                    sessions.save(outing.session)
                }
                // Counting paused by the reader: an outing cannot go on without its steps. A
                // service the system stopped keeps it, and the next start picks it up.
                if (outing != null && !preferencesSource.current().settings.trackingEnabled) {
                    outing.stop(System.currentTimeMillis())?.let { finished ->
                        if (finished.kept) sessions.save(finished.session) else sessions.delete(finished.session.id)
                    }
                }
            }
            widgets.notify(WidgetEvent.TRACKING_STATE)
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun startTracking() {
        scope.launch {
            // A paused count is never started by a side door (an old notification's button, a
            // shortcut): the steps of the pause would be added at the first sample.
            if (!preferencesSource.current().settings.trackingEnabled) {
                stopSelf()
                return@launch
            }
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
            goalToldDay = preferencesSource.goalNoticeDay()
            // An outing the system interrupted (the process died): it goes on from where it was
            // written, and the steps meanwhile reach it with the next sample.
            sessions.liveSession()?.let {
                tracker = newTracker(it)
                if (it.voice != SessionVoice.OFF) speech.prepare()
            }
            registerReceivers()
            // A reminder about to read the count asks for the sensor's batch first.
            link.attach { withContext(Dispatchers.Main.immediate) { flushSensor() } }
            launch { sensorSource.readings.collect(::onReading) }
            launch { preferencesSource.data.distinctUntilChanged().collect(::onPreferences) }
            launch { for (command in commands) onSessionCommand(command) }
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
                ledger.lastAccounted?.let { onSessionSteps(it.steps, it.atWallMillis) }
                publishLive()
                scheduleNotification()
                displayedToday()?.let {
                    widgets.notify(WidgetEvent.STEPS, it)
                    checkGoal(it)
                }
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
            // The outing as these steps leave it, in their transaction.
            val outing = tracker?.session?.takeIf { sessionDirty }
            sessionDirty = false
            if (batch != null) {
                val day = today()
                inFlightToday = batch.increments.filter { it.localEpochDay == day }.sumOf { it.steps }
                try {
                    repository.persist(batch, System.currentTimeMillis(), outing)
                } catch (e: Exception) {
                    // Back in the buffer: the next write carries it again, with the live state.
                    Log.e(TAG, "Writing the step buffer failed", e)
                    ledger.restore(batch)
                    if (outing != null) sessionDirty = true
                    inFlightToday = 0
                    return@withLock
                }
            } else if (outing != null) {
                try {
                    sessions.save(outing)
                } catch (e: Exception) {
                    Log.e(TAG, "Writing the outing failed", e)
                    sessionDirty = true
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
                checkSession()
                applyRegistration()
                startTicker()
                notifyNow()
                widgets.notify(WidgetEvent.SCREEN_ON)
            } else {
                widgets.notify(WidgetEvent.SCREEN_OFF)
                tickerJob?.cancel()
                countdownJob?.cancel()
                notifyJob?.cancel()
                flushSensor()
                persist()
                applyRegistration()
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
                            checkSession()
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

    /**
     * "Goal reached", once a day (PLANNING.md §8): noticed here, on the samples the service
     * receives anyway, so it costs no wake of its own. With the screen off it can come a few
     * minutes after the step that made it, as late as the sensor's batch.
     */
    private fun checkGoal(steps: Int) {
        val goal = preferences?.settings?.dailyGoalSteps ?: return
        val day = today()
        if (!GoalReached.isNews(day, steps, goal, goalToldDay)) return
        goalToldDay = day
        goals.goalReached(day)
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
        val outing = tracker?.takeIf { it.session.live }
        if (outing != null) {
            val units = preferences?.settings?.units ?: UnitPreference.SYSTEM
            val now = System.currentTimeMillis()
            val notice = SessionNotice(outing.session, outing.cadenceAt(now), units, now)
            return NotificationContent(steps, units = units, session = notice)
        }
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

    // --- Outings (PLANNING.md §11 Phase 10) ----------------------------------------------------

    /**
     * The usual registration, or the outing's: while one is counting, the wake-up counter (if
     * the phone has one) reporting within [SESSION_LATENCY_US], so a signal is felt on time
     * with the screen off. Paused, over, or none: exactly the registration of every other
     * moment (docs/adr/0002-sensor-reporting.md).
     */
    private fun applyRegistration(reconsiderWake: Boolean = true) {
        val measuring = tracker?.session?.state == SessionState.ACTIVE
        // A phone its counter cannot wake, an outing with signals: the processor is kept awake
        // while it counts, and the steps come as they are taken (docs/adr/0013-interval-walks.md).
        if (reconsiderWake) {
            signalWake.hold(
                SignalWake.needed(
                    session = tracker?.session,
                    wakeUpCounter = sensorSource.wakeUpSensor != null,
                    signalsAllowed = sessionNotifications.signalsAllowed(),
                ),
            )
        }
        // An interval outing near a change: reports every two seconds, so it is felt on time. Kept
        // awake, the same: a counter with a FIFO would otherwise hold the steps for half a minute.
        val nearChange = tracker?.untilIntervalSignal()?.let { it <= SessionConstants.INTERVAL_WINDOW_MILLIS } == true
        val latency = when {
            interactive -> SCREEN_ON_LATENCY_US
            measuring && (nearChange || signalWake.held) -> INTERVAL_NEAR_LATENCY_US
            measuring -> SESSION_LATENCY_US
            else -> SCREEN_OFF_LATENCY_US
        }
        sensorSource.register(latency, wakeUp = measuring)
        startCountdown()
    }

    /**
     * Screen on, an interval outing counting: the notification's countdown moves once a second
     * (§9.4 allows a ticker while the screen is on). It ends by itself with the screen, a pause or
     * the outing's last interval.
     */
    private fun startCountdown() {
        if (!interactive || countdownJob?.isActive == true || tracker?.untilIntervalSignal() == null) return
        countdownJob = scope.launch {
            while (isActive && interactive && tracker?.untilIntervalSignal() != null) {
                delay(COUNTDOWN_TICK_MS - System.currentTimeMillis() % COUNTDOWN_TICK_MS)
                notifyNow()
            }
        }
    }

    /**
     * Once the batch of samples in hand is all accounted (a job dispatched after it, not run
     * inside it): the change it crossed is told, the latest only, and the sensor's latency follows
     * what is left to the next one.
     */
    private fun afterIntervalBatch() {
        if (changeJob?.isActive == true) return
        changeJob = scope.launch(Dispatchers.Main) {
            pendingChange?.let(::tellIntervalChange)
            pendingChange = null
            if (!interactive) applyRegistration(reconsiderWake = false)
        }
    }

    /**
     * An interval outing's delays, in one log row when it ends: how many changes were told, how
     * late the median and the latest one were (negative: told ahead of the step that crossed it),
     * and the goal's, if it was reached. One row an outing, not one a change: the log is a ring
     * of the latest rows, and its rare rows (a reboot, a reset) must not be pushed out by walks.
     * The goal's step is the last of its batch's time in motion, so the moment it fell is that
     * much before the batch's end.
     */
    private fun logIntervalSummary(session: Session) {
        val schedule = IntervalSchedule.of(session) ?: return
        val now = System.currentTimeMillis()
        val goalLate = session.reachedAtMillis?.takeIf { session.end == SessionEnd.GOAL }?.let { reachedAt ->
            now - (reachedAt - (session.totals.movingMillis - schedule.totalMillis).coerceAtLeast(0))
        }
        val delays = intervalDelays.sorted()
        intervalDelays.clear()
        if (delays.isEmpty() && goalLate == null) return
        val detail = buildString {
            append("changes=${delays.size}")
            if (delays.isNotEmpty()) append(" lateMedian=${delays[delays.size / 2]}ms lateMax=${delays.last()}ms")
            goalLate?.let { append(" goalLate=${it}ms") }
            append(" wakeUp=${sensorSource.wakeUpSensor != null}")
        }
        ledger?.note(DiagnosticsEvent(now, DiagnosticsType.INTERVAL_CHANGE, detail))
    }

    /**
     * After a call that hid a sentence of an interval outing still counting: the interval it is
     * in now, with what is left of it («Slow, 2 minutes left»), not a change that is minutes old.
     * Null for any other outing, whose sentence is said as it was.
     */
    private fun intervalNowSentence(): String? {
        val session = tracker?.session?.takeIf { it.state == SessionState.ACTIVE } ?: return null
        val position = session.intervalAt(System.currentTimeMillis()) ?: return null
        val minutes = ((position.leftMillis + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
        return getString(
            R.string.spoken_interval_now,
            resources.intervalWord(position.fast),
            resources.getQuantityString(R.plurals.spoken_minutes, minutes, minutes),
        )
    }

    /** A change of interval: "faster" or "slower", the voice if the outing speaks, and its delay logged. */
    private fun tellIntervalChange(change: SessionSignal.IntervalChange) {
        val session = tracker?.session?.takeIf { it.live } ?: return
        val now = System.currentTimeMillis()
        val position = change.position
        intervalDelays += now - change.changedAtMillis
        val allowed = sessionNotifications.signalsAllowed()
        if (session.vibrate && allowed) SessionHaptics.playInterval(this, position.fast)
        if (session.voice != SessionVoice.OFF && allowed) speak(SessionAnnouncement.intervalChanged(position), session)
        // Once, even with the screen off: whoever looks next sees the interval they are in.
        notifyNow()
        publishSession()
    }

    private fun onSessionSteps(steps: Int, atWallMillis: Long) {
        val tracker = tracker ?: return
        if (steps <= 0) return
        onSessionSignals(tracker.onSteps(atWallMillis, steps))
        sessionDirty = true
        dropExpiredKeepGoing()
        publishSession()
        if (this.tracker?.untilIntervalSignal() != null || pendingChange != null) afterIntervalBatch()
    }

    /** Closes an outing left still, paused or open for too long; drops a "Keep going" gone stale. */
    private fun checkSession() {
        val tracker = tracker ?: return
        onSessionSignals(tracker.check(System.currentTimeMillis()))
        dropExpiredKeepGoing()
        publishSession()
    }

    private fun onSessionSignals(signals: List<SessionSignal>) {
        for (signal in signals) {
            when (signal) {
                is SessionSignal.Milestone -> {
                    val current = tracker ?: continue
                    val session = current.session
                    val allowed = sessionNotifications.signalsAllowed()
                    if (session.vibrate && allowed) SessionHaptics.play(this, signal.milestone)
                    if (session.voice != SessionVoice.OFF && allowed) {
                        val cadence = current.cadenceAt(System.currentTimeMillis())
                        // At the goal: whether this outing's steps also took the day across its own.
                        val dayGoal = signal.milestone == SessionMilestone.GOAL &&
                            SessionAnnouncement.broughtDayGoal(
                                todaySteps = displayedToday() ?: 0,
                                sessionSteps = session.totals.steps,
                                dailyGoalSteps = preferences?.settings?.dailyGoalSteps ?: Int.MAX_VALUE,
                            )
                        speak(SessionAnnouncement.milestone(session, signal.milestone, cadence, dayGoal), session)
                    }
                    // Once, even with the screen off: whoever looks next sees where it stands.
                    if (signal.milestone != SessionMilestone.GOAL) notifyNow()
                }

                is SessionSignal.Places -> {
                    val current = tracker ?: continue
                    val session = current.session
                    val allowed = sessionNotifications.signalsAllowed()
                    // At the walk's last place the goal's long pulse says it: one signal, not two.
                    val atEnd = signals.any { it is SessionSignal.Milestone && it.milestone == SessionMilestone.GOAL }
                    if (session.vibrate && allowed && !atEnd) SessionHaptics.playPlace(this)
                    if (session.voice != SessionVoice.OFF && allowed) {
                        speak(SessionAnnouncement.placesReached(session, signal.places), session)
                    }
                    if (!atEnd) notifyNow()
                }

                // Told once the batch is in: a later change in the same batch replaces it.
                is SessionSignal.IntervalChange -> pendingChange = signal

                is SessionSignal.Finished -> onSessionFinished(signal)
            }
        }
    }

    private fun onSessionFinished(finished: SessionSignal.Finished) {
        val session = finished.session
        // The goal is the last interval's end: its long pulse, never a change on top of it.
        pendingChange = null
        logIntervalSummary(session)
        // Kept a while after its goal or a long stillness, for "Keep going" and "Resume";
        // otherwise it is over here. The voice is let go once its last sentence is said.
        val reopenable = finished.kept && tracker?.canKeepGoing(System.currentTimeMillis()) == true
        if (!reopenable) tracker = null
        if (reopenable && session.end == SessionEnd.IDLE) tellEndedStill(session)
        speech.release()
        sessionDirty = false
        scope.launch {
            persistMutex.withLock {
                try {
                    if (finished.kept) sessions.save(session) else sessions.delete(session.id)
                    // A city walk walked to its last place is done: the next outing on it
                    // begins it again.
                    val walk = session.walk
                    if (walk != null && session.end == SessionEnd.GOAL) ways.finishWalk(walk, today())
                } catch (e: Exception) {
                    Log.e(TAG, "Writing the end of an outing failed", e)
                }
            }
        }
        if (session.end == SessionEnd.GOAL || reopenable) {
            val units = preferences?.settings?.units ?: UnitPreference.SYSTEM
            sessionNotifications.post(sessionNotifications.ended(session, units, canReopen = reopenable))
        }
        applyRegistration()
        notifyNow()
        publishSession()
        widgets.notify(WidgetEvent.TRACKING_STATE)
    }

    /**
     * The end of an outing that stood still too long, told as the reader walks on (or looks):
     * without it the reader who stopped for a chat learns only later, from the screen, that the
     * walk they went on with was not counted. Once, as a signal on the way is.
     */
    private fun tellEndedStill(session: Session) {
        if (!sessionNotifications.signalsAllowed()) return
        if (session.vibrate) SessionHaptics.playEndedStill(this)
        if (session.voice != SessionVoice.OFF) speak(SessionAnnouncement.endedStill(session), session)
    }

    /**
     * Past its "Keep going" window, or put away by the reader ([now]), an ended outing is over
     * for good: the button goes too.
     */
    private fun dropExpiredKeepGoing(now: Boolean = false) {
        val current = tracker ?: return
        val session = current.session
        if (session.state != SessionState.FINISHED) return
        if (!now && current.canKeepGoing(System.currentTimeMillis())) return
        tracker = null
        publishSession()
        if (sessionNotifications.endShowing()) {
            val units = preferences?.settings?.units ?: UnitPreference.SYSTEM
            sessionNotifications.post(sessionNotifications.ended(session, units, canReopen = false))
        }
    }

    private fun publishSession() {
        val current = tracker
        val now = System.currentTimeMillis()
        liveSession.publish(
            current?.let {
                LiveSessionState(
                    session = it.session,
                    cadence = it.cadenceAt(now),
                    canKeepGoing = it.canKeepGoing(now),
                    alertsWhileScreenOff = sensorSource.wakeUpSensor != null,
                )
            },
        )
    }

    private suspend fun onSessionCommand(command: Intent) {
        try {
            when (command.action) {
                SessionControl.ACTION_START -> startSession(command.getLongExtra(SessionControl.EXTRA_PLAN_ID, 0L))

                SessionControl.ACTION_START_REST_OF_DAY -> startSession(null)

                SessionControl.ACTION_START_WALK -> {
                    val name = command.getStringExtra(SessionControl.EXTRA_WALK)
                    val walk = WayId.entries.firstOrNull { it.name == name && it.kind == WayKind.WALK }
                    if (walk != null) startWalk(walk, command.getBooleanExtra(SessionControl.EXTRA_AGAIN, false))
                }

                SessionControl.ACTION_PAUSE -> changeSession { tracker -> tracker.pause(System.currentTimeMillis()) }

                SessionControl.ACTION_RESUME -> changeSession { tracker -> tracker.resume(System.currentTimeMillis()) }

                SessionControl.ACTION_KEEP_GOING -> {
                    var crossed: List<SessionSignal> = emptyList()
                    val reopened = changeSession { tracker ->
                        tracker.keepGoing(System.currentTimeMillis())?.also { crossed = it } != null
                    }
                    if (reopened) {
                        sessionNotifications.cancelEnd()
                        if (tracker?.session?.voice?.let { it != SessionVoice.OFF } == true) speech.prepare()
                        // The steps since the end may have crossed a milestone, a change, or the goal.
                        onSessionSignals(crossed)
                        afterIntervalBatch()
                    }
                }

                SessionControl.ACTION_DISMISS -> dropExpiredKeepGoing(now = true)

                SessionControl.ACTION_STOP -> {
                    // The steps up to the touch belong to the outing.
                    flushSensor()
                    tracker?.stop(System.currentTimeMillis())?.let(::onSessionFinished)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "An outing's command failed: ${command.action}", e)
        }
    }

    /**
     * Pause, resume, keep going: after the sensor's batch, so the steps up to the touch land on
     * the right side of it. Returns whether it changed anything.
     */
    private suspend fun changeSession(change: (SessionTracker) -> Boolean): Boolean {
        flushSensor()
        val tracker = tracker ?: return false
        if (!change(tracker)) return false
        sessionDirty = true
        persist()
        applyRegistration()
        notifyNow()
        publishSession()
        widgets.notify(WidgetEvent.TRACKING_STATE)
        return true
    }

    /**
     * Starts the outing of [planId], or the rest of the day when it is null. One at a time: with
     * one under way, the command only brings its notification up to date.
     */
    private suspend fun startSession(planId: Long?) {
        val current = preferencesSource.current()
        if (!current.settings.trackingEnabled) return
        if (tracker?.session?.live == true) {
            notifyNow()
            return
        }
        // A new outing ends the last one's "Keep going".
        tracker = null
        sessionNotifications.cancelEnd()
        // Everything walked until now is the day's, not the outing's.
        flushSensor()
        persist()
        val plan = if (planId == null) {
            sessions.plans.first().firstOrNull { it.goalKind == SessionGoalKind.REST_OF_DAY }
                ?: SessionPlans.REST_OF_DAY
        } else {
            sessions.plan(planId) ?: return
        }
        val now = System.currentTimeMillis()
        val day = today()
        val todaySteps = displayedToday() ?: repository.stepsOn(day)
        val session = SessionPlans.start(plan, now, day, todaySteps, current.settings.dailyGoalSteps) ?: return
        val saved = persistMutex.withLock { sessions.insert(session) }
        if (plan.id != 0L) sessions.markPlanUsed(plan.id, now)
        tracker = newTracker(saved, current.profile)
        if (saved.voice != SessionVoice.OFF && sessionNotifications.signalsAllowed()) {
            speak(SessionAnnouncement.started(saved), saved)
        }
        applyRegistration()
        notifyNow()
        publishSession()
        widgets.notify(WidgetEvent.TRACKING_STATE)
        SessionShortcuts.update(this, sessions.plansByUse(), current.settings.units)
    }

    /**
     * Starts an outing on the city walk [walk]: from where the walk's journey stands (the sum of
     * its outings), or, [again] or with nothing left of it, from its first place on a new one.
     * One outing at a time, as [startSession].
     */
    private suspend fun startWalk(walk: WayId, again: Boolean) {
        val current = preferencesSource.current()
        if (!current.settings.trackingEnabled) return
        if (tracker?.session?.live == true) {
            notifyNow()
            return
        }
        tracker = null
        sessionNotifications.cancelEnd()
        flushSensor()
        persist()
        val now = System.currentTimeMillis()
        val day = today()
        val way = Ways.of(walk)
        val journey = ways.walkJourney(walk, again, LocalDate.ofEpochDay(day), now)
        var from = WalkDays.of(journey, sessions.walkSessionsNow()).values.sum().toInt()
        if (way.lengthMeters - from < SessionPlans.MIN_WALK_LEFT_METERS) {
            // Walked to its end by outings that stopped a few steps short: done, and begun anew.
            ways.finishWalk(walk, day)
            ways.walkJourney(walk, again = false, LocalDate.ofEpochDay(day), now)
            from = 0
        }
        val session = SessionPlans.startWalk(way, from, current.settings.walkVoice, now, day) ?: return
        val saved = persistMutex.withLock { sessions.insert(session) }
        tracker = newTracker(saved, current.profile)
        if (saved.voice != SessionVoice.OFF && sessionNotifications.signalsAllowed()) {
            SessionAnnouncement.walkStarted(saved)?.let { speak(it, saved) }
        }
        applyRegistration()
        notifyNow()
        publishSession()
        widgets.notify(WidgetEvent.TRACKING_STATE)
    }

    /** One sentence of [session]'s, through the route its voice allows (headphones, ringer). */
    private fun speak(announcement: SessionAnnouncement, session: Session) {
        val units = preferences?.settings?.units ?: UnitPreference.SYSTEM
        speech.say(spoken(announcement, session, units), session.voice)
    }

    private suspend fun newTracker(session: Session, profile: Profile? = null): SessionTracker {
        val measuredWith = profile ?: preferencesSource.current().profile
        return SessionTracker(session, StepLengths.of(measuredWith), MetricsCalculator.weightKg(measuredWith))
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

        /**
         * An outing with the screen off: the wake-up counter reports within half a minute, so a
         * milestone is felt at most that late, for about two brief wakes a minute while walking
         * and none while still (docs/adr/0009-sessions.md).
         */
        const val SESSION_LATENCY_US = 30 * 1_000_000

        /**
         * An interval outing in the last [SessionConstants.INTERVAL_WINDOW_MILLIS] of motion before
         * a change (docs/adr/0013-interval-walks.md, option A): reports every two seconds, so the
         * change is felt within a few seconds; back to [SESSION_LATENCY_US] once it is told.
         */
        const val INTERVAL_NEAR_LATENCY_US = (SessionConstants.INTERVAL_NEAR_LATENCY_MILLIS * 1_000).toInt()

        /** The screen-on countdown of an interval outing moves once a second. */
        const val COUNTDOWN_TICK_MS = 1_000L

        val SESSION_ACTIONS = setOf(
            SessionControl.ACTION_START,
            SessionControl.ACTION_START_REST_OF_DAY,
            SessionControl.ACTION_START_WALK,
            SessionControl.ACTION_PAUSE,
            SessionControl.ACTION_RESUME,
            SessionControl.ACTION_STOP,
            SessionControl.ACTION_KEEP_GOING,
            SessionControl.ACTION_DISMISS,
        )

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
