package com.callbackdev.passo.core.tracking

import android.content.Context
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.domain.routes.GpsAnswer
import com.callbackdev.passo.core.domain.routes.GpsStart
import com.callbackdev.passo.core.domain.routes.GpsStartStep
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An outing that waits for the reader's word on the location permission before it starts.
 *
 * @property step what is to be shown: Passo's own screen and then the system's request, or the
 *   request alone.
 * @property shown the app has put it in front of the reader: a screen drawn again (a rotation)
 *   must not ask a second time.
 */
data class WaitingOuting(val planId: Long, val step: GpsStartStep, val shown: Boolean = false)

/**
 * The one door every start of a plan's outing goes through (PLANNING.md §11 Phase 15, «One door
 * for every start»; docs/adr/0016-location.md, decision 2). An outing starts from the Outings
 * page and from a launcher's shortcut, modules that do not know each other: both ask here, so a
 * plan with «Measure with GPS» on never skips Passo's own screen, and none asks twice.
 *
 * It holds no screen. When the reader's word is needed it says so in [waiting], and the app,
 * which owns the activity the system's request needs, shows what is to be shown and reports the
 * answer back. Either way the outing then starts: with GPS if the phone is ready for it, from
 * the steps if not, which the tracking service decides at the start, for every door alike.
 */
@Singleton
class OutingStarter
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val sessions: SessionRepository,
    private val preferences: UserPreferencesDataSource,
) {
    private val state = MutableStateFlow<WaitingOuting?>(null)

    /** The outing waiting for the reader's answer, if one is. */
    val waiting: StateFlow<WaitingOuting?> = state.asStateFlow()

    /**
     * Starts the outing of plan [planId], or, when its permission must be asked first, leaves it
     * in [waiting]. Returns false only when the start could not be sent at all.
     */
    suspend fun start(planId: Long): Boolean {
        val plan = sessions.plan(planId)
        val step = GpsStart.step(
            planAsksGps = plan?.gps == true,
            readiness = Gps.readiness(context),
            answer = preferences.gpsAnswer(),
        )
        if (step == GpsStartStep.START) return SessionControl.start(context, planId)
        state.value = WaitingOuting(planId, step)
        return true
    }

    /** The app has put the question in front of the reader. */
    fun shown() {
        state.update { it?.copy(shown = true) }
    }

    /** The system's request was answered: remembered, and the outing starts. */
    suspend fun answered(preciseGranted: Boolean) {
        finish(GpsStart.answer(preciseGranted))
    }

    /**
     * «Not now» on Passo's own screen: a refusal like the system's, remembered, so it is not
     * asked again by itself. The outing starts, from the steps.
     */
    suspend fun declined() {
        finish(GpsAnswer.REFUSED)
    }

    /** The reader went back without answering: nothing is remembered, and nothing starts. */
    fun cancelled() {
        state.value = null
    }

    private suspend fun finish(answer: GpsAnswer) {
        val outing = state.value ?: return
        state.value = null
        preferences.setGpsAnswer(answer)
        SessionControl.start(context, outing.planId)
    }
}
