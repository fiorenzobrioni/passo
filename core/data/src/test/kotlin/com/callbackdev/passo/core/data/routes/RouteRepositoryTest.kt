package com.callbackdev.passo.core.data.routes

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.db.RouteDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.RouteBox
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.StepLengthSample
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
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The routes' own database (PLANNING.md §11 Phase 15; docs/adr/0016-location.md, decisions 8 and
 * 9), on real Room databases: what is written is read back, a route goes before its outing, no
 * place outlives its outing, and Android's cloud backup is never told of the file.
 */
@RunWith(AndroidJUnit4::class)
class RouteRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var database: PassoDatabase
    private lateinit var routeDatabase: RouteDatabase
    private lateinit var store: TestDataStore
    private lateinit var routes: RouteRepository
    private lateinit var sessions: SessionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, PassoDatabase::class.java).allowMainThreadQueries().build()
        routeDatabase =
            Room.inMemoryDatabaseBuilder(context, RouteDatabase::class.java).allowMainThreadQueries().build()
        store = TestDataStore(folder.root)
        routes = RouteRepository(routeDatabase.routeDao())
        sessions = SessionRepository(database.sessionDao(), UserPreferencesDataSource(store.dataStore), routes)
    }

    @After
    fun tearDown() {
        database.close()
        routeDatabase.close()
        store.scope.cancel()
    }

    /** [count] points going north-east from Milan's cathedral, one every two seconds, from [from]. */
    private fun points(count: Int, from: Int = 0): List<RoutePoint> = List(count) { offset ->
        val index = from + offset
        RoutePoint(
            epochSecond = START_SECOND + index * 2L,
            latitudeE6 = 45_464_200 + index * 19,
            longitudeE6 = 9_191_400 + index * 27,
            altitudeDm = 1_220,
            accuracyMeters = 5,
            kind = if (index == 0) RoutePointKind.START else RoutePointKind.MEASURED,
        )
    }

    private suspend fun outing(startedAtMillis: Long = START_SECOND * 1_000): Session {
        val plan = SessionPlans.NEW.copy(gps = true)
        return sessions.insert(
            checkNotNull(SessionPlans.start(plan, startedAtMillis, 20_720, 0, 8_000, withGps = true)),
        )
    }

    @Test
    fun `chunks appended as the outing goes are read back as one route, in order`() = runTest {
        val outing = outing()

        routes.append(outing.id, outing.startedAtMillis, points(40))
        routes.append(outing.id, outing.startedAtMillis, points(25, from = 40))
        routes.append(outing.id, outing.startedAtMillis, points(35, from = 65))

        val route = checkNotNull(routes.route(outing.id, outing.startedAtMillis))
        assertThat(route.points).isEqualTo(points(100))
        assertThat(route.sessionStartedAtMillis).isEqualTo(outing.startedAtMillis)
        assertThat(route.box).isEqualTo(RouteBox.of(points(100)))
    }

    @Test
    fun `the route's own row keeps the count and the box as chunks arrive`() = runTest {
        val outing = outing()

        routes.append(outing.id, outing.startedAtMillis, points(40))
        routes.append(outing.id, outing.startedAtMillis, points(60, from = 40))

        val row = checkNotNull(routeDatabase.routeDao().route(outing.id))
        val box = checkNotNull(RouteBox.of(points(100)))
        assertThat(row.pointCount).isEqualTo(100)
        assertThat(RouteBox(row.southE6, row.westE6, row.northE6, row.eastE6)).isEqualTo(box)
    }

    @Test
    fun `a route says where its heights came from`() = runTest {
        val outing = outing()

        routes.append(outing.id, outing.startedAtMillis, points(40))
        assertThat(checkNotNull(routes.route(outing.id, outing.startedAtMillis)).altitudes).isNull()
        routes.append(outing.id, outing.startedAtMillis, points(25, from = 40), AltitudeSource.BAROMETER)
        assertThat(checkNotNull(routes.route(outing.id, outing.startedAtMillis)).altitudes)
            .isEqualTo(AltitudeSource.BAROMETER)
        // A chunk that says nothing leaves what was said.
        routes.append(outing.id, outing.startedAtMillis, points(10, from = 65))
        assertThat(checkNotNull(routes.route(outing.id, outing.startedAtMillis)).altitudes)
            .isEqualTo(AltitudeSource.BAROMETER)
    }

    @Test
    fun `the routes are counted and weighed, and all go at once, the outings staying`() = runTest {
        val first = outing()
        val second = outing(startedAtMillis = START_SECOND * 1_000 + 3_600_000)
        routes.append(first.id, first.startedAtMillis, points(100))
        routes.append(second.id, second.startedAtMillis, points(50))

        assertThat(routes.count.first()).isEqualTo(2)
        assertThat(routes.routeIds.first()).containsExactly(first.id, second.id)
        // About six bytes a point.
        assertThat(routes.bytes()).isIn(300L..1_500L)

        routes.deleteAll()

        assertThat(routes.count.first()).isEqualTo(0)
        assertThat(routes.bytes()).isEqualTo(0)
        assertThat(routes.route(first.id, first.startedAtMillis)).isNull()
        assertThat(sessions.session(first.id)).isNotNull()
        assertThat(sessions.session(second.id)).isNotNull()
    }

    @Test
    fun `no points, no chunk and no route`() = runTest {
        val outing = outing()

        routes.append(outing.id, outing.startedAtMillis, emptyList())

        assertThat(routes.route(outing.id, outing.startedAtMillis)).isNull()
        assertThat(routeDatabase.routeDao().routes()).isEmpty()
    }

    @Test
    fun `an outing without GPS has no route`() = runTest {
        assertThat(routes.route(sessionId = 7, sessionStartedAtMillis = 1)).isNull()
    }

    @Test
    fun `a route is its outing's by the outing's start, not by the id alone`() = runTest {
        val outing = outing()
        routes.append(outing.id, outing.startedAtMillis, points(40))

        // The same id, another outing: a database put back from elsewhere beside this file.
        assertThat(routes.route(outing.id, outing.startedAtMillis + 1)).isNull()

        // And that other outing's own points do not join the stranger's.
        routes.append(outing.id, outing.startedAtMillis + 1, points(10))
        val route = checkNotNull(routes.route(outing.id, outing.startedAtMillis + 1))
        assertThat(route.points).isEqualTo(points(10))
    }

    @Test
    fun `deleting a route leaves the outing and its numbers`() = runTest {
        val outing = outing()
        routes.append(outing.id, outing.startedAtMillis, points(40))

        routes.delete(outing.id)

        assertThat(routes.route(outing.id, outing.startedAtMillis)).isNull()
        assertThat(routeDatabase.routeDao().chunks(outing.id)).isEmpty()
        assertThat(sessions.session(outing.id)).isNotNull()
    }

    @Test
    fun `deleting an outing deletes its route, and what it said of the step`() = runTest {
        val outing = outing()
        routes.append(outing.id, outing.startedAtMillis, points(40))
        sessions.addStepLengthSamples(outing.id, listOf(StepLengthSample(110, 330, 252.0)))

        sessions.delete(outing.id)

        assertThat(sessions.session(outing.id)).isNull()
        assertThat(routeDatabase.routeDao().routes()).isEmpty()
        assertThat(routeDatabase.routeDao().chunks(outing.id)).isEmpty()
        assertThat(sessions.stepLengthSamples(outing.id)).isEmpty()
    }

    @Test
    fun `a crash between a route's deletion and its outing's leaves an outing without a route`() = runTest {
        val outing = outing()
        routes.append(outing.id, outing.startedAtMillis, points(40))

        // The route goes first (SessionRepository.delete); the process dies before the second step.
        routes.delete(outing.id)

        // A state the app already draws: the outing, its numbers, no route, and nothing to sweep.
        assertThat(sessions.session(outing.id)).isNotNull()
        assertThat(sessions.deleteOrphanRoutes()).isEqualTo(0)
    }

    @Test
    fun `a route whose outing is gone is deleted when the service starts`() = runTest {
        val kept = outing()
        routes.append(kept.id, kept.startedAtMillis, points(40))
        // Places with no outing: a `passo.db` put back from a backup beside a newer `routes.db`.
        routes.append(sessionId = 99, sessionStartedAtMillis = 5, points = points(10))
        routes.append(sessionId = kept.id + 1, sessionStartedAtMillis = 6, points = points(10))

        assertThat(sessions.deleteOrphanRoutes()).isEqualTo(2)

        assertThat(routeDatabase.routeDao().routes().map { it.sessionId }).containsExactly(kept.id)
        assertThat(routes.route(kept.id, kept.startedAtMillis)?.points).hasSize(40)
        assertThat(sessions.deleteOrphanRoutes()).isEqualTo(0)
    }

    @Test
    fun `an outing's samples are numbers kept with it, in the order they came`() = runTest {
        val outing = outing()

        sessions.addStepLengthSamples(outing.id, listOf(StepLengthSample(110, 330, 252.0)))
        sessions.addStepLengthSamples(outing.id, emptyList())
        sessions.addStepLengthSamples(outing.id, listOf(StepLengthSample(128, 256, 216.5)))

        assertThat(sessions.stepLengthSamples(outing.id)).containsExactly(
            StepLengthSample(110, 330, 252.0),
            StepLengthSample(128, 256, 216.5),
        ).inOrder()
    }

    @Test
    fun `an outing keeps what it measured, what the steps said, and its cap`() = runTest {
        val outing = outing()
        val walked = outing.copy(
            totals = outing.totals.copy(steps = 6_800, distanceMeters = 5_200.0),
            stepsMeters = 5_050.0,
            measuredShare = 0.96,
            capAskedForMillis = outing.capAtMillis,
        )

        sessions.save(walked)

        val read = checkNotNull(sessions.session(outing.id))
        assertThat(read.gps).isTrue()
        assertThat(read.stepsMeters).isEqualTo(5_050.0)
        assertThat(read.measuredShare).isEqualTo(0.96)
        assertThat(read.capAtMillis).isEqualTo(outing.capAtMillis)
        assertThat(read.capAskedForMillis).isEqualTo(outing.capAtMillis)
    }

    @Test
    fun `a plan keeps whether it measures with GPS`() = runTest {
        val id = sessions.savePlan(SessionPlans.NEW.copy(gps = true))

        assertThat(sessions.plan(id)?.gps).isTrue()
        assertThat(SessionPlans.NEW.gps).isFalse()
    }

    @Test
    fun `Android's cloud backup is never told of the routes, and the phone-to-phone move is`() {
        val rules = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(backupRules()).documentElement

        fun paths(section: String): List<String> {
            val node = rules.getElementsByTagName(section).item(0) as Element
            val includes = node.getElementsByTagName("include")
            return (0 until includes.length).map { (includes.item(it) as Element).getAttribute("path") }
        }

        // An allowlist: what is not named does not travel.
        assertThat(paths("cloud-backup")).containsExactly(
            "passo.db",
            "passo.db-wal",
            "datastore/settings.preferences_pb",
        )
        assertThat(paths("cloud-backup").none { RouteDatabase.NAME in it }).isTrue()
        assertThat(paths("device-transfer")).containsAtLeast(RouteDatabase.NAME, "${RouteDatabase.NAME}-wal")
        assertThat(paths("device-transfer")).containsAtLeast(PassoDatabase.NAME, "${PassoDatabase.NAME}-wal")
        assertThat(rules.getElementsByTagName("exclude").length).isEqualTo(0)
    }

    /** The app's backup rules, found from this module's folder, where the unit tests run. */
    private fun backupRules(): File {
        val relative = "app/src/main/res/xml/data_extraction_rules.xml"
        val start = File("").absoluteFile
        return generateSequence(start) { it.parentFile }.map { File(it, relative) }.first { it.isFile }
    }

    private companion object {
        const val START_SECOND = 1_795_000_000L
    }
}
