package com.callbackdev.passo.core.tracking

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.UnitPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Today's steps in Quick Settings (PLANNING.md §8). It reads only between
 * [onStartListening] and [onStopListening], which is while the panel is open: a closed panel
 * costs nothing. While open it follows the live count, as Today does, so it moves under the
 * reader's thumb. A tap opens Today; a paused tile's tap opens it asking to resume, as the
 * widget's does ([TrackingControl.EXTRA_RESUME]).
 */
@AndroidEntryPoint
class StepsTileService : TileService() {
    @Inject lateinit var preferences: UserPreferencesDataSource

    @Inject lateinit var tracking: TrackingRepository

    @Inject lateinit var liveSteps: LiveSteps

    private var listening: CoroutineScope? = null
    private var shown: CountingState? = null

    override fun onStartListening() {
        super.onStartListening()
        listening?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        listening = scope
        val day = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        scope.launch {
            combine(
                preferences.data,
                liveSteps.today,
                liveSteps.serviceRunning,
                tracking.observeStepsOn(day),
            ) { prefs, live, running, stored ->
                val settings = prefs.settings
                val state = CountingState.of(
                    hasSensor = StepTracking.hasStepCounter(this@StepsTileService),
                    onboarded = settings.onboardingCompleted,
                    hasPermission = StepTracking.hasActivityRecognition(this@StepsTileService),
                    enabled = settings.trackingEnabled,
                    serviceRunning = running,
                )
                val steps = live?.takeIf { it.localEpochDay == day }?.steps ?: stored
                TileFace.of(this@StepsTileService, state, steps, settings.dailyGoalSteps, settings.units)
            }
                // A tile that cannot read says nothing new, rather than taking the panel down.
                .catch { }
                .collect(::show)
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val resume = shown == CountingState.PAUSED
        val open = {
            openAppIntent(if (resume) REQUEST_RESUME else REQUEST_OPEN) {
                if (resume) putExtra(TrackingControl.EXTRA_RESUME, true)
            }?.let { startActivityAndCollapse(it) }
        }
        if (isLocked) unlockAndRun { open() } else open()
    }

    private fun show(face: TileFace) {
        val tile = qsTile ?: return
        shown = face.counting
        tile.state = face.state
        tile.label = face.label
        tile.subtitle = face.subtitle
        tile.stateDescription = face.subtitle
        tile.updateTile()
    }

    private companion object {
        const val REQUEST_OPEN = 10
        const val REQUEST_RESUME = 11
    }
}

/** What the tile shows: the count with the goal while counting, or why it is not counting. */
internal data class TileFace(val counting: CountingState, val state: Int, val label: String, val subtitle: String) {
    companion object {
        fun of(context: Context, counting: CountingState, steps: Int, goalSteps: Int, units: UnitPreference): TileFace {
            val format = context.measureFormatter(units)
            val res = context.resources
            val count = res.getQuantityString(R.plurals.tile_steps, steps, format.steps(steps))
            val name = res.getString(R.string.tile_label)
            return when (counting) {
                CountingState.COUNTING -> TileFace(
                    counting,
                    Tile.STATE_ACTIVE,
                    count,
                    if (steps >= goalSteps) {
                        res.getString(R.string.tile_goal_reached)
                    } else {
                        res.getString(R.string.tile_progress, format.percent(steps.toDouble() / goalSteps))
                    },
                )

                // The count so far still holds; the line says it is not moving.
                CountingState.PAUSED -> TileFace(
                    counting,
                    Tile.STATE_INACTIVE,
                    count,
                    res.getString(R.string.tile_paused),
                )

                CountingState.STOPPED ->
                    TileFace(counting, Tile.STATE_INACTIVE, count, res.getString(R.string.tile_stopped))

                CountingState.PERMISSION_NEEDED ->
                    TileFace(counting, Tile.STATE_INACTIVE, name, res.getString(R.string.tile_permission))

                CountingState.NOT_SET_UP ->
                    TileFace(counting, Tile.STATE_INACTIVE, name, res.getString(R.string.tile_not_set_up))

                CountingState.NO_SENSOR ->
                    TileFace(counting, Tile.STATE_UNAVAILABLE, name, res.getString(R.string.tile_no_sensor))
            }
        }
    }
}

/** What came of asking the system to add the tile. */
enum class TileRequestResult {
    ADDED,
    ALREADY_ADDED,
    NOT_ADDED,

    /** The system could not ask (not in the foreground, a request already open). */
    FAILED,
}

/** The tile as Settings offers it: one system prompt to add it, with no trip to the panel's editor. */
object StepsTile {
    fun requestAdd(context: Context, onResult: (TileRequestResult) -> Unit) {
        val manager = context.getSystemService(StatusBarManager::class.java)
        if (manager == null) {
            onResult(TileRequestResult.FAILED)
            return
        }
        manager.requestAddTileService(
            ComponentName(context, StepsTileService::class.java),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_stat_steps),
            context.mainExecutor,
        ) { code ->
            onResult(
                when (code) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> TileRequestResult.ADDED
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> TileRequestResult.ALREADY_ADDED
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> TileRequestResult.NOT_ADDED
                    else -> TileRequestResult.FAILED
                },
            )
        }
    }
}
