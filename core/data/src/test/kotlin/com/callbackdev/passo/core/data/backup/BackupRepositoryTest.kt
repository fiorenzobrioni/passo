package com.callbackdev.passo.core.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.db.RouteDatabase
import com.callbackdev.passo.core.data.db.StepLengthSampleEntity
import com.callbackdev.passo.core.data.db.WayJourneyEntity
import com.callbackdev.passo.core.data.db.toEntity
import com.callbackdev.passo.core.data.db.toModel
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.rounds.RoundRepository
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.sessions.LiveSession
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.backup.BackupCodec
import com.callbackdev.passo.core.domain.backup.BackupRead
import com.callbackdev.passo.core.domain.backup.CsvTable
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMarks
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPause
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.model.TrackerState
import com.callbackdev.passo.core.model.UnitSystem
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourneyState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.ZoneOffset
import kotlin.math.cos
import kotlin.math.hypot

/**
 * The export and the import (PLANNING.md §11 Phase 7) between two phones, each a real Room
 * database and a real settings file: the phase's acceptance is the first test.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private val day = 20_720L
    private var today = day + 2

    /** One phone: its database, its settings, and the repositories over them. */
    private inner class Phone(name: String) {
        val database: PassoDatabase =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PassoDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val store = TestDataStore(File(folder.root, name).apply { mkdirs() })
        val preferences = UserPreferencesDataSource(store.dataStore)
        val tracking = TrackingRepository(database.trackingDao(), preferences) { today }
        val routeDatabase: RouteDatabase =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RouteDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val routes = RouteRepository(routeDatabase.routeDao())
        val live = LiveSession()
        val rounds = RoundRepository(database.roundDao(), database.sessionDao(), routes, preferences)
        val backups = BackupRepository(
            database.trackingDao(),
            database.sessionDao(),
            database.wayDao(),
            preferences,
            tracking,
            routes,
            live,
            database.roundDao(),
            rounds,
        )

        suspend fun walk(vararg minutes: MinuteSteps) {
            tracking.persist(
                LedgerBatch(increments = minutes.toList(), state = null, diagnostics = emptyList()),
                nowWallMillis = 0L,
            )
        }

        suspend fun summaries() = database.trackingDao().allSummaries()

        suspend fun minutes() = database.trackingDao().allMinutes()

        fun close() {
            database.close()
            store.scope.cancel()
        }
    }

    private lateinit var old: Phone
    private lateinit var new: Phone

    @Before
    fun setUp() {
        old = Phone("old")
        new = Phone("new")
    }

    @After
    fun tearDown() {
        old.close()
        new.close()
    }

    private fun minute(day: Long, minuteOfDay: Int, steps: Int) = MinuteSteps(day * 1_440 + minuteOfDay, day, steps)

    /** Three days on the old phone, a profile change in the middle, a goal, an outing and its plan. */
    private suspend fun history() {
        old.tracking.changeProfile { it.copy(heightMeters = 1.82, weightKg = 80.0) }
        old.tracking.changeSettings {
            it.copy(dailyGoalSteps = 9_500, theme = ThemeMode.DARK, onboardingCompleted = true)
        }
        today = day
        old.walk(minute(day, 600, 104), minute(day, 601, 98), minute(day, 900, 12))
        today = day + 1
        old.walk(minute(day + 1, 480, 120), minute(day + 1, 481, 133))
        old.tracking.changeProfile {
            it.copy(stepLengthMode = StepLengthMode.CALIBRATED, walkingStepLengthMeters = 0.78)
        }
        today = day + 2
        old.walk(minute(day + 2, 420, 90))
        val planId = old.database.sessionDao().appendPlan(
            SessionPlan(
                name = "Canal",
                goalKind = SessionGoalKind.DISTANCE,
                goalValue = 3_000,
                intensity = SessionIntensity.BRISK,
                milestones = setOf(SessionMilestone.HALF),
            ).toEntity(),
        )
        old.database.sessionDao().insertSession(
            com.callbackdev.passo.core.model.Session(
                planId = planId,
                name = "Canal",
                goalKind = SessionGoalKind.DISTANCE,
                goalValue = 3_000,
                intensity = SessionIntensity.BRISK,
                milestones = setOf(SessionMilestone.HALF),
                vibrate = true,
                localEpochDay = day + 1,
                startedAtMillis = (day + 1) * 86_400_000 + 8 * 3_600_000,
                state = SessionState.FINISHED,
                endedAtMillis = (day + 1) * 86_400_000 + 9 * 3_600_000,
                end = SessionEnd.GOAL,
                totals = SessionTotals(steps = 4_000, movingMillis = 2_400_000, distanceMeters = 3_000.0),
            ).toEntity(),
        )
        old.database.trackingDao().writeBatch(
            increments = emptyList(),
            state = com.callbackdev.passo.core.data.db.TrackerStateEntity(
                bootCount = 7,
                lastCounterValue = 55_000,
                lastSampleElapsedNanos = 1,
                lastSampleWallMillis = 1,
                updatedAtMillis = 1,
            ),
            diagnostics = emptyList(),
            profile = old.preferences.current().profile,
            goalSteps = 9_500,
            today = today,
            diagnosticsKept = 10,
        )
    }

    private suspend fun exported(withRoutes: Boolean = false): String = BackupCodec.encode(
        old.backups.backup(appVersion = "0.9.0", nowMillis = 1L, zone = ZoneOffset.UTC, withRoutes = withRoutes),
    )

    /** The history's one outing, measured with GPS: its climb, its legs, and its route on the old phone. */
    private suspend fun measuredHistory(): List<RoutePoint> {
        history()
        val outing = checkNotNull(old.database.sessionDao().allSessions().single().toModel())
        old.database.sessionDao().upsertSession(
            outing.copy(
                gps = true,
                stepsMeters = 2_900.0,
                measuredShare = 0.97,
                ascentMeters = 24.0,
                descentMeters = 19.5,
                legs = listOf(
                    OutingLeg(
                        0,
                        100.0,
                        71_000,
                        130.5,
                        endedAtMillis = outing.startedAtMillis + 72_000,
                        ascentMeters = 4.0,
                    ),
                    OutingLeg(1, 62.0, 44_000, 81.0, endedAtMillis = outing.startedAtMillis + 118_000),
                ),
            ),
        )
        val points = List(40) { index ->
            RoutePoint(
                epochSecond = outing.startedAtMillis / 1_000 + index * 2L,
                latitudeE6 = 45_464_200 + index * 19,
                longitudeE6 = 9_191_400 + index * 27,
                altitudeDm = 1_220 + index,
                accuracyMeters = 5,
                kind = if (index == 0) RoutePointKind.START else RoutePointKind.MEASURED,
            )
        }
        old.routes.append(outing.id, outing.startedAtMillis, points, AltitudeSource.BAROMETER)
        return points
    }

    private fun read(text: String) = (BackupCodec.decode(text) as BackupRead.Ok).backup

    @Test
    fun `export and import on a clean install reproduce the history exactly`() = runTest {
        history()

        val report = new.backups.import(read(exported()), withPreferences = true)

        assertThat(new.summaries()).isEqualTo(old.summaries())
        assertThat(new.minutes()).isEqualTo(old.minutes())
        assertThat(new.preferences.current().profile).isEqualTo(old.preferences.current().profile)
        val newSettings = new.preferences.current().settings
        assertThat(newSettings.copy(onboardingCompleted = true)).isEqualTo(old.preferences.current().settings)
        // The new phone's own first run is its own.
        assertThat(newSettings.onboardingCompleted).isFalse()
        val outing = new.database.sessionDao().allSessions().single().session
        val plan = new.database.sessionDao().plans().single()
        assertThat(outing.copy(id = 0, planId = null))
            .isEqualTo(old.database.sessionDao().allSessions().single().session.copy(id = 0, planId = null))
        assertThat(outing.planId).isEqualTo(plan.id)
        assertThat(report).isEqualTo(ImportReport(3, 0, 0, addedOutings = 1, addedPlans = 1, preferences = true))
        // Another phone's counter is never brought over (ADR 0007).
        assertThat(new.database.trackingDao().trackerState()).isNull()
    }

    @Test
    fun `what an outing with GPS said of the step travels with the reader's own file`() = runTest {
        history()
        val outing = old.database.sessionDao().allSessions().single().session
        old.database.sessionDao().upsertSession(
            checkNotNull(outing.toModel()).copy(gps = true, stepsMeters = 2_900.0, measuredShare = 0.97),
        )
        old.database.sessionDao().insertStepLengthSamples(
            listOf(
                StepLengthSampleEntity(sessionId = outing.id, cadence = 110, steps = 330, meters = 252.0),
                StepLengthSampleEntity(sessionId = outing.id, cadence = 128, steps = 256, meters = 216.5),
            ),
        )

        new.backups.import(read(exported()), withPreferences = true)

        val came = new.database.sessionDao().allSessions().single().session
        assertThat(came.gps).isTrue()
        assertThat(came.stepsMeters).isEqualTo(2_900.0)
        assertThat(came.measuredShare).isEqualTo(0.97)
        // Under the outing's id on this phone, which is not the other phone's.
        assertThat(new.database.sessionDao().stepLengthSamples(came.id).map { Triple(it.cadence, it.steps, it.meters) })
            .containsExactly(Triple(110, 330, 252.0), Triple(128, 256, 216.5)).inOrder()

        // The same file again adds no outing, and so no sample twice.
        new.backups.import(read(exported()), withPreferences = false)
        assertThat(new.database.sessionDao().allStepLengthSamples()).hasSize(2)
    }

    @Test
    fun `an outing's climb and its legs travel with the file - numbers, and no place`() = runTest {
        measuredHistory()

        val file = exported()
        new.backups.import(read(file), withPreferences = true)

        val came = checkNotNull(new.database.sessionDao().allSessions().single().toModel())
        assertThat(came.ascentMeters).isEqualTo(24.0)
        assertThat(came.descentMeters).isEqualTo(19.5)
        assertThat(came.legs.map { it.copy() }).isEqualTo(
            checkNotNull(old.database.sessionDao().allSessions().single().toModel()).legs,
        )
        // Not ticked, the routes stay where they are: the file holds no place.
        assertThat(file).doesNotContain("\"route\"")
        assertThat(new.routes.count.first()).isEqualTo(0)
    }

    /** Four walks round the old phone's park, the first three its round «Park»; the fourth said not the same. */
    private suspend fun roundHistory(): List<Long> {
        val ids = (0 until 4).map { walk ->
            val started = (day + walk) * 86_400_000L + 7 * 3_600_000L
            old.database.sessionDao().insertSession(
                com.callbackdev.passo.core.model.Session(
                    planId = null,
                    name = null,
                    goalKind = SessionGoalKind.OPEN,
                    goalValue = 0,
                    intensity = SessionIntensity.FREE,
                    milestones = emptySet(),
                    vibrate = true,
                    localEpochDay = day + walk,
                    startedAtMillis = started,
                    state = SessionState.FINISHED,
                    endedAtMillis = started + 2_400_000,
                    end = SessionEnd.STOPPED,
                    totals = SessionTotals(steps = 4_000, movingMillis = 2_400_000, distanceMeters = 3_200.0),
                    gps = true,
                    measuredShare = 1.0,
                ).toEntity(),
            )
        }
        val rounds = old.database.roundDao()
        val round = rounds.pair(ids[0], ids[1])
        rounds.join(round, ids[2])
        rounds.rename(round, "Park")
        rounds.insertApart(listOf(com.callbackdev.passo.core.data.db.RoundApartEntity(ids[2], ids[3])))
        return ids
    }

    @Test
    fun `a round travels with its outings, its name and the pairs said not the same, numbers and no place`() = runTest {
        roundHistory()
        val file = exported()
        assertThat(file).contains("\"rounds\"")
        new.backups.import(read(file), withPreferences = false)

        val came = new.database.sessionDao().allSessions().map { it.session }.sortedBy { it.startedAtMillis }
        val round = new.rounds.rounds.first().single()
        assertThat(round.name).isEqualTo("Park")
        assertThat(round.walks).containsExactly(came[0].id, came[1].id, came[2].id)
        assertThat(new.database.roundDao().apart().map { it.firstId to it.secondId })
            .containsExactly(came[2].id to came[3].id)
    }

    /**
     * A walk round the park on [phone], 3.2 km from the door [days] after the first day, measured, with
     * its route: a point every five metres, a few metres off the line as a receiver draws it. Its id.
     */
    private suspend fun loop(phone: Phone, days: Long, seed: Int): Long {
        val started = (day + days) * 86_400_000L + 7 * 3_600_000L
        val id = phone.database.sessionDao().insertSession(
            com.callbackdev.passo.core.model.Session(
                planId = null,
                name = null,
                goalKind = SessionGoalKind.OPEN,
                goalValue = 0,
                intensity = SessionIntensity.FREE,
                milestones = emptySet(),
                vibrate = true,
                localEpochDay = day + days,
                startedAtMillis = started,
                state = SessionState.FINISHED,
                endedAtMillis = started + 2_400_000,
                end = SessionEnd.STOPPED,
                totals = SessionTotals(steps = 4_000, movingMillis = 2_400_000, distanceMeters = 3_200.0),
                gps = true,
                measuredShare = 1.0,
            ).toEntity(),
        )
        val corners = listOf(0.0 to 0.0, 800.0 to 0.0, 800.0 to 800.0, 0.0 to 800.0, 0.0 to 0.0)
        val points = ArrayList<RoutePoint>()
        for (index in 1 until corners.size) {
            val (x0, y0) = corners[index - 1]
            val (x1, y1) = corners[index]
            val steps = (hypot(x1 - x0, y1 - y0) / 5).toInt()
            for (step in (if (index == 1) 0 else 1)..steps) {
                val t = step.toDouble() / steps
                val wander = ((step * 7 + seed * 3) % 9 - 4).toDouble()
                val east = x0 + (x1 - x0) * t + wander
                val north = y0 + (y1 - y0) * t - wander
                points += RoutePoint(
                    epochSecond = started / 1_000 + points.size * 4L,
                    latitudeE6 = ((45.4642 + north / 111_320.0) * 1e6).toInt(),
                    longitudeE6 = ((9.19 + east / (111_320.0 * cos(Math.toRadians(45.4642)))) * 1e6).toInt(),
                    altitudeDm = null,
                    accuracyMeters = 5,
                    kind = if (points.isEmpty()) RoutePointKind.START else RoutePointKind.MEASURED,
                )
            }
        }
        phone.routes.append(id, started, points)
        return id
    }

    @Test
    fun `a file with older walks of the phone's round, with their routes, joins that round, its name the phone's`() =
        runTest {
            // The old phone: three walks round the park in October, a round it named «Park».
            val old1 = loop(old, 0, seed = 1)
            old.rounds.find(loop(old, 1, seed = 2))
            old.rounds.find(loop(old, 2, seed = 3))
            old.database.roundDao().rename(checkNotNull(old.database.roundDao().roundOf(old1)), "Park")
            // The new phone, walked before the file came: the same park, three times, «Around the park».
            new.rounds.find(loop(new, 30, seed = 4))
            new.rounds.find(loop(new, 31, seed = 5))
            new.rounds.find(loop(new, 32, seed = 6))
            val phoneRound = new.rounds.rounds.first().single()
            new.database.roundDao().rename(phoneRound.id, "Around the park")

            new.backups.import(read(exported(withRoutes = true)), withPreferences = false)

            val round = new.rounds.rounds.first().single()
            assertThat(round.id).isEqualTo(phoneRound.id)
            assertThat(round.name).isEqualTo("Around the park")
            assertThat(round.walks).hasSize(6)
        }

    @Test
    fun `the same file again adds no round twice`() = runTest {
        roundHistory()
        val file = exported()
        new.backups.import(read(file), withPreferences = false)
        new.backups.import(read(file), withPreferences = false)
        assertThat(new.rounds.rounds.first()).hasSize(1)
        assertThat(new.database.roundDao().apart()).hasSize(1)
    }

    @Test
    fun `with the routes ticked, each route travels with its outing`() = runTest {
        val points = measuredHistory()

        val report = new.backups.import(read(exported(withRoutes = true)), withPreferences = true)

        val came = new.database.sessionDao().allSessions().single().session
        val route = checkNotNull(new.routes.route(came.id, came.startedAtMillis))
        assertThat(route.points).isEqualTo(points)
        assertThat(route.altitudes).isEqualTo(AltitudeSource.BAROMETER)
        assertThat(report.addedRoutes).isEqualTo(1)
    }

    @Test
    fun `the same file again adds no route twice, and takes none away`() = runTest {
        val points = measuredHistory()
        val file = exported(withRoutes = true)
        new.backups.import(read(file), withPreferences = true)

        val again = new.backups.import(read(file), withPreferences = false)
        // A file without the routes, after one with: the route the phone has stays.
        val without = new.backups.import(read(exported()), withPreferences = false)

        val came = new.database.sessionDao().allSessions().single().session
        assertThat(again.addedRoutes).isEqualTo(0)
        assertThat(without.addedRoutes).isEqualTo(0)
        assertThat(checkNotNull(new.routes.route(came.id, came.startedAtMillis)).points).isEqualTo(points)
    }

    @Test
    fun `a phone restored from the cloud copy gets its routes back from the reader's own file`() = runTest {
        val points = measuredHistory()
        // The cloud copy carried the outing and never its route.
        new.backups.import(read(exported()), withPreferences = true)
        val came = new.database.sessionDao().allSessions().single().session
        assertThat(new.routes.route(came.id, came.startedAtMillis)).isNull()

        val report = new.backups.import(read(exported(withRoutes = true)), withPreferences = false)

        // The outing was there already: no outing added, and its route laid on it.
        assertThat(report.addedOutings).isEqualTo(0)
        assertThat(report.addedRoutes).isEqualTo(1)
        assertThat(checkNotNull(new.routes.route(came.id, came.startedAtMillis)).points).isEqualTo(points)
    }

    @Test
    fun `the data rows count the routes kept`() = runTest {
        measuredHistory()

        assertThat(old.backups.contents.first().routes).isEqualTo(1)
        assertThat(new.backups.contents.first().routes).isEqualTo(0)
    }

    @Test
    fun `the route still being written is kept, and is not among those Delete all routes counts`() = runTest {
        val walked = measuredHistory()
        val finishedOnly = old.backups.contents.first()
        val plan = SessionPlans.NEW.copy(gps = true)
        val started = checkNotNull(SessionPlans.start(plan, 1_800_000_000_000, today, 0, 8_000, withGps = true))
        val live = started.copy(id = old.database.sessionDao().insertSession(started.toEntity()))
        old.routes.append(live.id, live.startedAtMillis, walked.take(10))

        val underWay = old.backups.contents.first()
        assertThat(underWay.routes).isEqualTo(2)
        assertThat(underWay.deletableRoutes).isEqualTo(1)
        assertThat(underWay.deletableBytes).isEqualTo(finishedOnly.deletableBytes)

        old.backups.deleteRoutes()
        val after = old.backups.contents.first()
        assertThat(after.routes).isEqualTo(1)
        assertThat(after.deletableRoutes).isEqualTo(0)
        assertThat(after.deletableBytes).isEqualTo(0)
        assertThat(old.routes.route(live.id, live.startedAtMillis)).isNotNull()
    }

    @Test
    fun `the route of an outing that can still be taken back is kept too`() = runTest {
        measuredHistory()
        val ended = checkNotNull(old.database.sessionDao().allSessions().single().toModel())
        // Ended by its goal a moment ago: the service holds it, «Keep going» would reopen it.
        old.live.publish(LiveSessionState(ended, cadence = null, canKeepGoing = true, alertsWhileScreenOff = true))

        assertThat(old.backups.contents.first().deletableRoutes).isEqualTo(0)
        old.backups.deleteRoutes()
        assertThat(old.routes.route(ended.id, ended.startedAtMillis)).isNotNull()

        // Over for good: the reader's to delete.
        old.live.publish(null)
        assertThat(old.backups.contents.first().deletableRoutes).isEqualTo(1)
        old.backups.deleteRoutes()
        assertThat(old.routes.route(ended.id, ended.startedAtMillis)).isNull()
    }

    @Test
    fun `an open outing travels with the file, its pauses with it, and comes in once`() = runTest {
        history()
        val outing = checkNotNull(old.database.sessionDao().allSessions().single().toModel())
        val start = outing.startedAtMillis
        val pauses = listOf(SessionPause(start + 60_000, start + 120_000))
        old.database.sessionDao().upsertSession(
            outing.copy(
                goalKind = SessionGoalKind.OPEN,
                goalValue = 0,
                reachedAtMillis = null,
                pauses = pauses,
                marks = SessionMarks.DISTANCE,
                markMiles = true,
                toldMarks = 2,
            ),
        )

        val file = read(exported())
        new.backups.import(file, withPreferences = true)
        val came = checkNotNull(new.database.sessionDao().allSessions().single().toModel())
        assertThat(came.open).isTrue()
        assertThat(came.pauses).isEqualTo(pauses)
        // Its marks as they were frozen (Phase 21.2): the mark, its unit, how many were told.
        assertThat(came.marks).isEqualTo(SessionMarks.DISTANCE)
        assertThat(came.markMiles).isTrue()
        assertThat(came.toldMarks).isEqualTo(2)

        // The same file again: the outing is already here, and so are its pauses, once.
        new.backups.import(file, withPreferences = true)
        assertThat(new.database.sessionDao().allSessions().single().pauses).hasSize(1)
    }

    @Test
    fun `importing the same file again changes nothing`() = runTest {
        history()
        val file = read(exported())
        new.backups.import(file, withPreferences = true)
        val summaries = new.summaries()
        val minutes = new.minutes()

        val again = new.backups.import(file, withPreferences = true)

        assertThat(new.summaries()).isEqualTo(summaries)
        assertThat(new.minutes()).isEqualTo(minutes)
        assertThat(new.database.sessionDao().allSessions()).hasSize(1)
        assertThat(new.database.sessionDao().plans()).hasSize(1)
        assertThat(again).isEqualTo(ImportReport(0, 0, 3, addedOutings = 0, addedPlans = 0, preferences = true))
    }

    @Test
    fun `a new phone that counted today keeps its steps, and the file's are added`() = runTest {
        history()
        new.walk(minute(day + 2, 420, 60), minute(day + 2, 700, 110))

        val report = new.backups.import(read(exported()), withPreferences = false)

        val todayRow = new.summaries().single { it.localEpochDay == day + 2 }
        assertThat(todayRow.steps).isEqualTo(90 + 110)
        assertThat(todayRow.finalized).isFalse()
        assertThat(report.addedDays).isEqualTo(2)
        assertThat(report.mergedDays).isEqualTo(1)
        // Without the preferences, this phone's profile and goal stand.
        assertThat(new.preferences.current().settings.dailyGoalSteps).isEqualTo(8_000)
        assertThat(todayRow.goalSteps).isEqualTo(8_000)
    }

    @Test
    fun `the data rows count what an export would carry`() = runTest {
        history()

        assertThat(old.backups.contents.first()).isEqualTo(DataContents(days = 3, outings = 1))
    }

    @Test
    fun `each spreadsheet table has a row per day, minute or outing`() = runTest {
        history()

        fun rows(csv: String) = csv.trimEnd().lines().size - 1
        assertThat(rows(old.backups.csv(CsvTable.DAYS, UnitSystem.METRIC, ZoneOffset.UTC))).isEqualTo(3)
        assertThat(rows(old.backups.csv(CsvTable.MINUTES, UnitSystem.METRIC, ZoneOffset.UTC))).isEqualTo(6)
        assertThat(rows(old.backups.csv(CsvTable.OUTINGS, UnitSystem.METRIC, ZoneOffset.UTC))).isEqualTo(1)
    }

    @Test
    fun `a way travels with the file, and comes in put down when this phone walks another`() = runTest {
        history()
        old.database.wayDao().insert(
            WayJourneyEntity(
                way = WayId.VIA_FRANCIGENA.name,
                startEpochDay = day,
                startedAtMillis = 5_000,
                state = WayJourneyState.ACTIVE.name,
                endedEpochDay = null,
                toldMeters = 1_200,
            ),
        )
        new.database.wayDao().insert(
            WayJourneyEntity(
                way = WayId.VIA_DEGLI_DEI.name,
                startEpochDay = day + 1,
                startedAtMillis = 9_000,
                state = WayJourneyState.ACTIVE.name,
                endedEpochDay = null,
                toldMeters = 0,
            ),
        )
        val file = exported()
        val report = new.backups.import(read(file), withPreferences = false)
        assertThat(report.addedWays).isEqualTo(1)
        val francigena = new.database.wayDao().journeys().single { it.way == WayId.VIA_FRANCIGENA.name }
        assertThat(francigena.state).isEqualTo(WayJourneyState.LEFT.name)
        assertThat(francigena.toldMeters).isEqualTo(1_200)
        assertThat(new.database.wayDao().activeAmong(listOf(WayId.VIA_DEGLI_DEI.name)).single().way)
            .isEqualTo(WayId.VIA_DEGLI_DEI.name)
        // The same file again brings nothing.
        assertThat(new.backups.import(read(file), withPreferences = false).addedWays).isEqualTo(0)
    }
}
