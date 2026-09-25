package com.callbackdev.passo.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.WidgetCardColor
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WidgetLookStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val store by lazy {
        WidgetLookStore(PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "look.preferences_pb") })
    }

    @After
    fun close() = scope.cancel()

    @Test
    fun `a card nobody configured wears Chiaro's defaults`() = runTest {
        val look = store.lookFor(7)
        assertThat(look).isEqualTo(WidgetLook())
        assertThat(look.background).isEqualTo(WidgetBackground.COLOR)
        assertThat(look.cardColor).isEqualTo(WidgetCardColor.BLUE)
        assertThat(look.opacityPct).isEqualTo(100)
    }

    @Test
    fun `each card keeps its own look`() = runTest {
        val clay = WidgetLook(cardColor = WidgetCardColor.CLAY, opacityPct = 40, showHours = false)
        val light =
            WidgetLook(
                background = WidgetBackground.LIGHT,
                arrangement = WidgetArrangement.RING_END,
                showSentence = false,
            )
        store.set(1, clay)
        store.set(2, light)
        assertThat(store.lookFor(1)).isEqualTo(clay)
        assertThat(store.lookFor(2)).isEqualTo(light)
    }

    @Test
    fun `a removed card leaves nothing behind`() = runTest {
        store.set(3, WidgetLook(showDetails = false))
        store.forget(intArrayOf(3))
        assertThat(store.lookFor(3)).isEqualTo(WidgetLook())
    }

    @Test
    fun `an opacity out of range is clamped`() = runTest {
        store.set(4, WidgetLook(opacityPct = 140))
        assertThat(store.lookFor(4).opacityPct).isEqualTo(100)
    }
}
