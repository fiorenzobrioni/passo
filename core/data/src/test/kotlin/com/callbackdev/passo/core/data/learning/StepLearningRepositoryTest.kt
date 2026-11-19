package com.callbackdev.passo.core.data.learning

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.db.RouteDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.StepLengthMode
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
import kotlin.math.roundToInt

/** The step that learns on a real database (PLANNING.md §11 Phase 17). */
@RunWith(AndroidJUnit4::class)
class StepLearningRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var database: PassoDatabase
    private lateinit var routeDatabase: RouteDatabase
    private lateinit var store: TestDataStore
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var tracking: TrackingRepository
    private lateinit var sessions: SessionRepository
    private lateinit var repository: StepLearningRepository

    private val day = 20_720L
    private var today = day

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PassoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        routeDatabase =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), RouteDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        store = TestDataStore(folder.root)
        preferences = UserPreferencesDataSource(store.dataStore)
        tracking = TrackingRepository(database.trackingDao(), preferences) { today }
        sessions = SessionRepository(database.sessionDao(), preferences, RouteRepository(routeDatabase.routeDao()))
        repository =
            StepLearningRepository(database.sessionDao(), database.trackingDao(), preferences, tracking) { today }
    }

    @After
    fun tearDown() {
        database.close()
        routeDatabase.close()
        store.scope.cancel()
    }

    /** A day of walking at 105 steps a minute, the usual pace, on [onDay]. */
    private suspend fun walk(onDay: Long, minutes: Int = 60) {
        val first = onDay * 1_440
        tracking.persist(
            LedgerBatch(
                increments = (0 until minutes).map {
                    MinuteSteps(first + it, onDay, 105)
                },
                state = null,
                diagnostics = emptyList(),
            ),
            nowWallMillis = 0,
        )
    }

    /** An outing with GPS over, with what its clean stretches said of the step. */
    private suspend fun outing(startedAtMillis: Long, step: Double, meters: Double = 300.0): Long {
        val session = sessions.insert(
            Session(
                planId = null,
                name = null,
                goalKind = SessionGoalKind.TIME,
                goalValue = 30,
                intensity = SessionIntensity.FREE,
                milestones = emptySet(),
                vibrate = false,
                localEpochDay = day,
                startedAtMillis = startedAtMillis,
                state = SessionState.FINISHED,
                endedAtMillis = startedAtMillis + 1_800_000,
                gps = true,
            ),
        )
        sessions.addStepLengthSamples(session.id, listOf(StepLengthSample(105, (meters / step).roundToInt(), meters)))
        return session.id
    }

    @Test
    fun `the outing that made the curve ready carries the line, and no other`() = runTest {
        walk(day)
        val first = outing(1_000, 0.80)
        assertThat(repository.learning.first().proposed).isFalse()
        val second = outing(2_000, 0.80)
        outing(3_000, 0.80)

        val learning = repository.learning.first()
        assertThat(learning.proposed).isTrue()
        assertThat(learning.readyOutingId).isEqualTo(second)
        assertThat(learning.readyOutingId).isNotEqualTo(first)
        assertThat(repository.stepOf(second).first()).isWithin(1e-3).of(0.80)
    }

    @Test
    fun `an outing with GPS and no sample has no step of its own`() = runTest {
        val session = sessions.insert(
            Session(
                planId = null,
                name = null,
                goalKind = SessionGoalKind.TIME,
                goalValue = 30,
                intensity = SessionIntensity.FREE,
                milestones = emptySet(),
                vibrate = false,
                localEpochDay = day,
                startedAtMillis = 9_000,
                gps = true,
            ),
        )

        assertThat(repository.stepOf(session.id).first()).isNull()
    }

    @Test
    fun `use it, today and the days after follow, past days keep theirs`() = runTest {
        walk(day - 1)
        walk(day)
        outing(1_000, 0.80)
        outing(2_000, 0.80)
        val yesterday = database.trackingDao().summary(day - 1)?.distanceMeters
        val curve = requireNotNull(repository.learning.first().offer).curve

        repository.use(curve)

        assertThat(preferences.current().profile.stepLengthMode).isEqualTo(StepLengthMode.LEARNED)
        // Yesterday was frozen with the step it was walked with before the profile changed.
        assertThat(database.trackingDao().summary(day - 1)?.distanceMeters).isEqualTo(yesterday)
        assertThat(database.trackingDao().summary(day - 1)?.finalized).isTrue()
        assertThat(database.trackingDao().summary(day)?.distanceMeters).isWithin(1e-6).of(60 * 105 * 0.80)
        // The curve in use is the reference now: nothing is proposed, and nothing is put away.
        assertThat(repository.learning.first().proposed).isFalse()
        assertThat(preferences.stepPutAway.first()).isNull()
    }

    @Test
    fun `apply to past days too reaches the days before with the learnt step`() = runTest {
        walk(day - 1)
        outing(1_000, 0.80)
        outing(2_000, 0.80)
        repository.use(requireNotNull(repository.learning.first().offer).curve)

        tracking.applyProfileToPastDays()

        assertThat(database.trackingDao().summary(day - 1)?.distanceMeters).isWithin(1e-6).of(60 * 105 * 0.80)
    }

    @Test
    fun `not now puts the card away, and the row's offer stays`() = runTest {
        walk(day)
        outing(1_000, 0.80)
        outing(2_000, 0.80)
        val offer = requireNotNull(repository.learning.first().offer)

        repository.notNow(offer.curve)

        val after = repository.learning.first()
        assertThat(after.proposed).isFalse()
        assertThat(after.offer).isNotNull()
    }

    @Test
    fun `go back returns to the step typed, and puts the curve gone back from away`() = runTest {
        walk(day)
        tracking.changeProfile { Profile(stepLengthMode = StepLengthMode.MANUAL, walkingStepLengthMeters = 0.74) }
        outing(1_000, 0.80)
        outing(2_000, 0.80)
        val curve = requireNotNull(repository.learning.first().offer).curve
        repository.use(curve)

        repository.goBack()

        assertThat(preferences.current().profile)
            .isEqualTo(Profile(stepLengthMode = StepLengthMode.MANUAL, walkingStepLengthMeters = 0.74))
        assertThat(preferences.stepPutAway.first()?.points).isEqualTo(curve.points)
        assertThat(repository.learning.first().proposed).isFalse()
    }

    @Test
    fun `a typed length while a curve counts is in use, and the curve is put away`() = runTest {
        walk(day)
        outing(1_000, 0.80)
        outing(2_000, 0.80)
        assertThat(repository.learning.first().proposed).isTrue()

        repository.chooseLength { it.copy(stepLengthMode = StepLengthMode.MANUAL, walkingStepLengthMeters = 0.76) }

        assertThat(StepLengths.of(preferences.current().profile).walkingMeters).isEqualTo(0.76)
        val after = repository.learning.first()
        assertThat(after.proposed).isFalse()
        // The calibration page's row stays.
        assertThat(after.offer).isNotNull()
    }

    @Test
    fun `an unchanged length puts nothing away`() = runTest {
        walk(day)
        outing(1_000, 0.80)
        outing(2_000, 0.80)

        repository.chooseLength { it }

        assertThat(preferences.stepPutAway.first()).isNull()
        assertThat(repository.learning.first().proposed).isTrue()
    }

    @Test
    fun `an outing deleted takes its samples, the curve in use is not recomputed`() = runTest {
        walk(day)
        val first = outing(1_000, 0.80)
        outing(2_000, 0.80)
        repository.use(requireNotNull(repository.learning.first().offer).curve)
        val inUse = preferences.current().profile

        sessions.delete(first)

        assertThat(repository.learning.first().offer).isNull()
        assertThat(preferences.current().profile).isEqualTo(inUse)
    }

    @Test
    fun `the learnt step and the curve put away survive in the settings file`() = runTest {
        walk(day)
        outing(1_000, 0.80)
        outing(2_000, 0.80)
        val curve = requireNotNull(repository.learning.first().offer).curve
        repository.use(curve)
        repository.notNow(curve)

        val reread = UserPreferencesDataSource(store.dataStore)
        assertThat(reread.current().profile.learnedStep?.points).isEqualTo(curve.points)
        assertThat(reread.stepPutAway.first()?.points).isEqualTo(curve.points)
    }
}
