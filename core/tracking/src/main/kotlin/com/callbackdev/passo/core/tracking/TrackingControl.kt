package com.callbackdev.passo.core.tracking

import android.content.Context
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pause and resume, one way for every door that offers them: Settings' switch, Today's card and
 * the widget's "tap to resume" (PLANNING.md §4.2, §7).
 */
@Singleton
class TrackingControl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val tracking: TrackingRepository,
) {
    /**
     * Counting starts again from now. The baseline is forgotten first, so the steps the hardware
     * counted during the pause are not added in one go by the first sample (PLANNING.md §15).
     */
    suspend fun resume() {
        tracking.forgetBaseline()
        settings.updateSettings { it.copy(trackingEnabled = true) }
        StepTracking.start(context)
    }

    /** Stops the service, which writes its buffer as it goes. */
    suspend fun pause() {
        settings.updateSettings { it.copy(trackingEnabled = false) }
        StepTracking.stop(context)
    }

    companion object {
        /**
         * On the app's launch intent: the reader tapped a widget that said "Paused, tap to
         * resume". The app resumes from the activity, where starting the foreground service is
         * allowed, and the reader sees it happen.
         */
        const val EXTRA_RESUME: String = "com.callbackdev.passo.extra.RESUME_TRACKING"
    }
}
