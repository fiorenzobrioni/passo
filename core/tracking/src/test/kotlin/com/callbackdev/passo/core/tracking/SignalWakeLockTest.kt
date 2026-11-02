package com.callbackdev.passo.core.tracking

import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.sessions.SignalWake
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowPowerManager

@RunWith(AndroidJUnit4::class)
class SignalWakeLockTest {
    private val powerManager = ApplicationProvider.getApplicationContext<Context>().getSystemService(
        PowerManager::class.java,
    )
    private val log = mutableListOf<String>()
    private val lock = SignalWakeLock(powerManager) { log += it }

    @Test
    fun `held once however often asked, let go once, and its release logged`() {
        lock.hold(true)
        lock.hold(true)
        assertThat(lock.held).isTrue()
        assertThat(ShadowPowerManager.getLatestWakeLock()?.isHeld).isTrue()

        lock.hold(false)
        lock.hold(false)
        assertThat(lock.held).isFalse()
        assertThat(ShadowPowerManager.getLatestWakeLock()?.isHeld).isFalse()
        assertThat(log).hasSize(1)
        assertThat(log.single()).startsWith("released after ")
    }

    @Test
    fun `let go by its timeout, it is logged as such, and taken anew only if asked`() {
        lock.hold(true)
        // What the platform does at the timeout: the lock goes without a call from here.
        ShadowPowerManager.getLatestWakeLock()?.release()

        lock.hold(false)
        assertThat(log).containsExactly("timed out after ${SignalWake.TIMEOUT_MILLIS / 1_000}s")

        lock.hold(true)
        assertThat(lock.held).isTrue()
        lock.hold(false)
        assertThat(log).hasSize(2)
        assertThat(log.last()).startsWith("released after ")
    }

    @Test
    fun `never asked, nothing is held and nothing is logged`() {
        lock.hold(false)
        assertThat(lock.held).isFalse()
        assertThat(log).isEmpty()
    }
}
