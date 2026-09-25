package com.callbackdev.passo.widget

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.widget.WidgetUpdates
import com.callbackdev.passo.core.domain.widget.WidgetDecision
import com.callbackdev.passo.core.domain.widget.WidgetEvent
import com.callbackdev.passo.core.domain.widget.WidgetUpdatePolicy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The widgets' side of [WidgetUpdates] (PLANNING.md §7): the tracking service and the app report
 * what happened, [WidgetUpdatePolicy] decides, and this carries the decision out. Push-based and
 * with no clock of its own: the one timer it ever holds is the trailing edge of the one-a-minute
 * throttle, and only while the screen is on (a screen-off cancels it).
 *
 * It also listens to the settings and the profile, which change only with someone in the app:
 * a new goal, other units, another palette or a new weight repaint the cards at once.
 *
 * Everything runs on the main thread, so the policy needs no lock.
 */
@Singleton
class WidgetUpdateCoordinator
@Inject
constructor(
    @ApplicationContext private val context: Context,
    settings: SettingsRepository,
) : WidgetUpdates {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val policy = WidgetUpdatePolicy(
        interactive = context.getSystemService(PowerManager::class.java)?.isInteractive ?: true,
    )
    private var latestSteps = 0
    private var goalSteps = Int.MAX_VALUE
    private var trailing: Job? = null

    init {
        scope.launch {
            var first = true
            combine(settings.settings, settings.profile) { s, p -> s to p }
                .distinctUntilChanged()
                .collect { (s, _) ->
                    goalSteps = s.dailyGoalSteps
                    if (first) first = false else handle(WidgetEvent.SETTINGS)
                }
        }
    }

    override fun notify(event: WidgetEvent, steps: Int) {
        scope.launch {
            if (event == WidgetEvent.STEPS) latestSteps = steps
            handle(event)
        }
    }

    private fun handle(event: WidgetEvent) {
        when (val decision = policy.decide(event, SystemClock.elapsedRealtime(), latestSteps, goalSteps)) {
            WidgetDecision.Now -> push()

            is WidgetDecision.Later -> if (trailing?.isActive != true) {
                trailing = scope.launch {
                    delay(decision.delayMillis)
                    trailing = null
                    handle(WidgetEvent.STEPS)
                }
            }

            WidgetDecision.Skip -> if (event == WidgetEvent.SCREEN_OFF) {
                trailing?.cancel()
                trailing = null
            }
        }
    }

    private fun push() {
        trailing?.cancel()
        trailing = null
        policy.pushed(SystemClock.elapsedRealtime(), latestSteps)
        scope.launch(Dispatchers.Default) { runCatching { PassoWidgets.updateAll(context) } }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetModule {
    @Binds
    abstract fun widgetUpdates(coordinator: WidgetUpdateCoordinator): WidgetUpdates
}
