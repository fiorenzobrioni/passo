package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.time.TodaySource
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.ways.WayRepository
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * The way's arrival, end to end on a real database: told, then finished, since the notifier is
 * the only place a way is marked finished (PLANNING.md §11 Phase 11).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS")
class WayNotifierTest {
    @get:Rule val folder = TemporaryFolder()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.now(ZoneId.systemDefault())
    private val way = Ways.of(WayId.VIA_DEGLI_DEI)
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: PassoDatabase
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var ways: WayRepository

    private val posted get() = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(app, PassoDatabase::class.java).allowMainThreadQueries().build()
        preferences = UserPreferencesDataSource(
            PreferenceDataStoreFactory.create(scope = storeScope) { File(folder.root, "settings.preferences_pb") },
        )
        ways =
            WayRepository(
                database.wayDao(),
                TrackingRepository(database.trackingDao(), preferences, TodaySource.System),
            )
    }

    @After
    fun tearDown() {
        database.close()
        storeScope.cancel()
    }

    private fun notifier() = WayNotifier(app, ways, preferences)

    private suspend fun started(): WayJourney {
        checkNotNull(ways.start(way.id, today, today, System.currentTimeMillis()))
        return checkNotNull(ways.active.first())
    }

    private suspend fun stored(): WayJourney = ways.journeys.first().single()

    private val arrival get() = mapOf(today.toEpochDay() to way.lengthMeters.toDouble())

    @Test
    fun `the arrival is told, then the way is finished on its day`() = runTest {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val journey = started()

        notifier().check(journey, arrival)

        assertThat(posted.single().extras.getString("android.title")).isEqualTo("Arrived: Florence")
        assertThat(stored().state).isEqualTo(WayJourneyState.FINISHED)
        assertThat(stored().endedEpochDay).isEqualTo(today.toEpochDay())
        assertThat(ways.active.first()).isNull()
    }

    @Test
    fun `a stage short of the end is told and the way stays under way`() = runTest {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val journey = started()
        val stage = way.stages[1]

        notifier().check(journey, mapOf(today.toEpochDay() to stage.distanceMeters + 100.0))

        assertThat(posted).hasSize(1)
        assertThat(stored().state).isEqualTo(WayJourneyState.ACTIVE)
    }

    @Test
    fun `without the notification permission the way is finished all the same`() = runTest {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val journey = started()

        notifier().check(journey, arrival)

        assertThat(posted).isEmpty()
        assertThat(stored().state).isEqualTo(WayJourneyState.FINISHED)
    }

    @Test
    fun `an arrival already told by an earlier process is not told again, and still finishes the way`() = runTest {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val journey = started()
        ways.claimTold(journey.id, way.lengthMeters)

        notifier().check(journey, arrival)

        assertThat(posted).isEmpty()
        assertThat(stored().state).isEqualTo(WayJourneyState.FINISHED)
    }
}
