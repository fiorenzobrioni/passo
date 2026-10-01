package com.callbackdev.passo.core.tracking

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** A stage reached and the end, as the ways' notifications say them (PLANNING.md §11 Phase 11). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS")
class WayNotificationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = WayNotifications(context)
    private val way = Ways.of(WayId.VIA_FRANCIGENA)
    private val start = 20_500L

    private fun Notification.title() = NotificationCompat.getContentTitle(this).toString()

    private fun Notification.text() = NotificationCompat.getContentText(this).toString()

    private fun Notification.bigText() = extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()

    private fun progress(meters: Double, state: WayJourneyState = WayJourneyState.ACTIVE): WayProgress {
        val journey = WayJourney(1, way.id, start, 0, state, null, 0)
        return WayProgress.of(way, journey, mapOf(start to meters), start)
    }

    @Test
    fun `the channel is its own, at the default importance`() {
        notifications.ensureChannel()
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel("ways")
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
        assertThat(channel.name.toString()).isEqualTo("Ways")
    }

    @Test
    fun `a stage says where, how far, what is next, and its line`() {
        val siena = way.stops.single { it.key == "siena" }
        val notification = notifications.stage(siena, progress(siena.distanceMeters + 300.0), UnitPreference.METRIC)
        assertThat(notification.title()).isEqualTo("Stage reached: Siena")
        assertThat(notification.text()).startsWith("Via Francigena: 741 km of 1,020 km. Next stage, Ponte d’Arbia")
        assertThat(notification.bigText()).contains("Piazza del Campo")
    }

    @Test
    fun `the end says it was walked, and in how many days`() {
        val rome = way.stops.last()
        val notification = notifications.stage(rome, progress(way.lengthMeters.toDouble()), UnitPreference.METRIC)
        assertThat(notification.title()).isEqualTo("Arrived: Rome")
        assertThat(notification.text()).isEqualTo("Via Francigena: 1,020 km walked in 1 day.")
    }
}
