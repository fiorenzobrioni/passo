package com.callbackdev.passo.core.tracking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A plan's shortcut wears the mark its row wears on the Outings page: the interval walk was set
 * apart from the other walks there, and its shortcut kept the way to a flag until 9 Nov 2026.
 */
@RunWith(AndroidJUnit4::class)
class SessionShortcutsTest {

    @Test
    fun `every plan's shortcut wears its own mark`() {
        SessionIntensity.entries.forEach { intensity ->
            SessionGoalKind.entries.forEach { goal ->
                val expected = when {
                    goal == SessionGoalKind.INTERVALS -> R.drawable.ic_shortcut_intervals
                    intensity == SessionIntensity.RUN -> R.drawable.ic_shortcut_run
                    else -> R.drawable.ic_shortcut_outing
                }
                assertEquals("$intensity, $goal", expected, SessionShortcuts.shortcutIcon(intensity, goal))
            }
        }
    }
}
