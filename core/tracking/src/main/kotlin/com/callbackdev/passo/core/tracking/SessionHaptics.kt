package com.callbackdev.passo.core.tracking

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import com.callbackdev.passo.core.model.SessionMilestone

/**
 * The outing's signals as vibrations (PLANNING.md §11 Phase 10), for a phone in a pocket or on
 * an arm: a count of short pulses for the quarters (one, two, three), one long one for the goal.
 * Read without looking, and learned from the editor's "Try it".
 *
 * They are notification vibrations for Android: the phone's silent mode and its notification
 * vibration setting apply to them, as they do to any notification.
 */
object SessionHaptics {
    /** Plays [milestone]'s pattern. */
    fun play(context: Context, milestone: SessionMilestone) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern(milestone), -1),
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION),
        )
    }

    /** Whether this phone can vibrate at all: without it the editor does not offer the switch. */
    fun available(context: Context): Boolean =
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.hasVibrator() == true

    /**
     * Off, on, off, on…, in milliseconds. A short pulse is long enough to be felt through
     * clothes and short enough to be counted; the gap lets two of them read as two.
     */
    fun pattern(milestone: SessionMilestone): LongArray = when (milestone) {
        SessionMilestone.QUARTER -> pulses(1)
        SessionMilestone.HALF -> pulses(2)
        SessionMilestone.THREE_QUARTERS -> pulses(3)
        SessionMilestone.GOAL -> longArrayOf(0, LONG_MS)
    }

    private fun pulses(count: Int): LongArray = LongArray(count * 2) { i ->
        if (i % 2 ==
            0
        ) {
            (if (i == 0) 0 else GAP_MS)
        } else {
            SHORT_MS
        }
    }

    private const val SHORT_MS = 180L
    private const val GAP_MS = 220L
    private const val LONG_MS = 900L
}
