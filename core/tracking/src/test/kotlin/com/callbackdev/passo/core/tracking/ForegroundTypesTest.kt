package com.callbackdev.passo.core.tracking

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.content.pm.ServiceInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The service's types, when the system says yes and when it says no (PLANNING.md §11 Phase 15,
 * «The service»; docs/adr/0016-location.md, decision 4): a refused `location` type never stops
 * the count, and no start of the service takes the type away from an outing that holds it.
 */
@RunWith(AndroidJUnit4::class)
class ForegroundTypesTest {
    private val notification = Notification()
    private val passed = mutableListOf<Int>()
    private val told = mutableListOf<Pair<Boolean, Throwable?>>()

    /** What the system answers to each combination of types: null lets it through. */
    private var refusal: (Int) -> RuntimeException? = { null }

    private val types = ForegroundTypes(
        start = { _, asked ->
            passed += asked
            refusal(asked)?.let { throw it }
        },
        onLocationType = { added, why -> told += added to why },
    )

    private val health = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
    private val both = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION

    @Test
    fun `with no outing with GPS it is health alone, and nothing is said of location`() {
        assertThat(types.enter(notification, withLocation = false)).isTrue()

        assertThat(passed).containsExactly(health)
        assertThat(types.locationHeld).isFalse()
        assertThat(told).isEmpty()
    }

    @Test
    fun `granted - an outing with GPS adds the location type, told once`() {
        assertThat(types.enter(notification, withLocation = true)).isTrue()

        assertThat(passed).containsExactly(both)
        assertThat(types.locationHeld).isTrue()
        assertThat(told).containsExactly(true to null)
    }

    @Test
    fun `the app opened, or a button of the notification touched, mid-outing - the type stays`() {
        types.enter(notification, withLocation = true)

        // Every start of the service enters with the types it already holds.
        repeat(3) { assertThat(types.enter(notification, withLocation = types.locationHeld)).isTrue() }

        assertThat(passed).containsExactly(both, both, both, both)
        assertThat(types.locationHeld).isTrue()
        // Added once: the log's row counts additions, not looks at the app.
        assertThat(told).containsExactly(true to null)
    }

    @Test
    fun `the location type refused where the rule does not allow it - back to health, the count goes on`() {
        val denied = SecurityException("location is while-in-use")
        refusal = { asked -> denied.takeIf { asked == both } }

        assertThat(types.enter(notification, withLocation = true)).isTrue()

        assertThat(passed).containsExactly(both, health).inOrder()
        assertThat(types.locationHeld).isFalse()
        assertThat(told).containsExactly(false to denied)
    }

    @Test
    fun `refused in a way no page documents - caught all the same`() {
        val odd = IllegalArgumentException("a type on a running service")
        refusal = { asked -> odd.takeIf { asked == both } }

        assertThat(types.enter(notification, withLocation = true)).isTrue()

        assertThat(types.locationHeld).isFalse()
        assertThat(told).containsExactly(false to odd)
    }

    @Test
    fun `not allowed to start from the background with location - back to health`() {
        val late = ForegroundServiceStartNotAllowedException("from the background")
        refusal = { asked -> late.takeIf { asked == both } }

        assertThat(types.enter(notification, withLocation = true)).isTrue()

        assertThat(passed).containsExactly(both, health).inOrder()
        assertThat(types.locationHeld).isFalse()
    }

    @Test
    fun `held and then refused - the type is lost, and said so`() {
        types.enter(notification, withLocation = true)
        val denied = SecurityException("the permission was taken back")
        refusal = { asked -> denied.takeIf { asked == both } }

        assertThat(types.enter(notification, withLocation = true)).isTrue()

        assertThat(types.locationHeld).isFalse()
        assertThat(told).containsExactly(true to null, false to denied).inOrder()
    }

    @Test
    fun `paused or over - the type is dropped, back to health`() {
        types.enter(notification, withLocation = true)

        assertThat(types.enter(notification, withLocation = false)).isTrue()

        assertThat(passed).containsExactly(both, health).inOrder()
        assertThat(types.locationHeld).isFalse()
        // And taken again at the resume: a second addition, told as one.
        types.enter(notification, withLocation = true)
        assertThat(told).containsExactly(true to null, true to null)
    }

    @Test
    fun `a process the system restarted comes back without the type, and passes health alone`() {
        // A new process: nothing is held, whatever the one before it held.
        assertThat(types.locationHeld).isFalse()

        assertThat(types.enter(notification, withLocation = types.locationHeld)).isTrue()

        assertThat(passed).containsExactly(health)
    }

    @Test
    fun `only a refused health type stops the service`() {
        refusal = { SecurityException("no activity recognition") }

        assertThat(types.enter(notification, withLocation = true)).isFalse()
        assertThat(types.enter(notification, withLocation = false)).isFalse()
        assertThat(types.locationHeld).isFalse()
    }

    @Test
    fun `health not allowed from the background - the service cannot run`() {
        refusal = { ForegroundServiceStartNotAllowedException("from the background") }

        assertThat(types.enter(notification, withLocation = false)).isFalse()
    }
}
