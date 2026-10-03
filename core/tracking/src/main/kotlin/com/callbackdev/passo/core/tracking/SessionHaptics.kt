package com.callbackdev.passo.core.tracking

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import com.callbackdev.passo.core.model.SessionMilestone

/**
 * The outing's signals as vibrations (PLANNING.md §11 Phase 10), for a phone in a pocket or on
 * an arm: a count of short pulses for the quarters (one, two, three), one long one for the goal,
 * two long ones for an end brought by a long stillness; on a city walk, one short pulse a place;
 * on an interval walk (Phase 13), "faster" and "slower", unlike all of them. Read without
 * looking, and learned from the editor's "Try it".
 *
 * "Faster" is a quick run of four taps, a drum roll that hurries; "slower" one long pulse and a
 * short one, a breath let out. Neither is a count (the quarters are slow, even pulses), nor a
 * single long one (the goal), nor two long ones (the stillness).
 *
 * They are notification vibrations for Android: the phone's silent mode and its notification
 * vibration setting apply to them, as they do to any notification.
 */
object SessionHaptics {
    /** Plays [milestone]'s pattern. */
    fun play(context: Context, milestone: SessionMilestone) = vibrate(context, pattern(milestone))

    /** An interval walk's change: "faster" into a fast interval, "slower" out of it. */
    fun playInterval(context: Context, fast: Boolean) = vibrate(context, intervalPattern(fast))

    /** A city walk's place reached: one short pulse (a walk has no quarters to confuse it with). */
    fun playPlace(context: Context) = vibrate(context, placePattern())

    /** Plays the end by stillness: the outing is over, and can still be taken back. */
    fun playEndedStill(context: Context) = vibrate(context, endedStillPattern())

    private fun vibrate(context: Context, pattern: LongArray) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern, -1),
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

    /** "Faster" or "slower", off and on as [pattern]'s are. */
    fun intervalPattern(fast: Boolean): LongArray = if (fast) {
        longArrayOf(0, TAP_MS, TAP_GAP_MS, TAP_MS, TAP_GAP_MS, TAP_MS, TAP_GAP_MS, TAP_MS)
    } else {
        longArrayOf(0, EASE_LONG_MS, EASE_GAP_MS, EASE_SHORT_MS)
    }

    /** A walk's place, off and on as [pattern]'s are. */
    fun placePattern(): LongArray = pulses(1)

    /** The end by stillness, off and on as [pattern]'s are. */
    fun endedStillPattern(): LongArray = longArrayOf(0, STILL_MS, STILL_GAP_MS, STILL_MS)

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

    // Two long pulses: nothing like the quarters' counts, and not the goal's single one.
    private const val STILL_MS = 500L
    private const val STILL_GAP_MS = 300L

    // Faster: four taps, each a third of a quarter's pulse, twice as close: felt as one hurry.
    private const val TAP_MS = 70L
    private const val TAP_GAP_MS = 80L

    // Slower: long, then short, a long gap between: the second pulse fading out.
    private const val EASE_LONG_MS = 450L
    private const val EASE_GAP_MS = 250L
    private const val EASE_SHORT_MS = 120L
}
