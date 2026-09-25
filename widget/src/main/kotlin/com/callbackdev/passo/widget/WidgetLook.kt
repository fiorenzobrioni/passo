package com.callbackdev.passo.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.callbackdev.passo.core.designsystem.theme.WidgetCardColor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.widgetLookDataStore by preferencesDataStore(name = "widget_look")

/**
 * What a card is painted on: a plain card in the app's light or dark surface, or whichever the
 * phone is in, or one of the six colours of [WidgetCardColor]. Chiaro's list without its sky
 * (Passo has none to draw); [COLOR] is one value, and which colour is a second question.
 */
enum class WidgetBackground { LIGHT, DARK, SYSTEM, COLOR }

/** Which way round the «At a glance» one-row card is laid: the ring leading or closing the row. */
enum class WidgetArrangement { RING_START, RING_END }

/**
 * One widget's look and content, chosen from the launcher's reconfigure flow and kept per
 * widget, so the same card can sit on a home screen twice, dressed twice. The defaults are
 * Chiaro's (a solid blue card since its 21 Sep 2026 review), so a Passo card placed beside a
 * Chiaro one matches it before anybody configures anything.
 *
 * @property showSentence the day's sentence (ahead of or behind a usual day, what is left, when
 *   the goal was met). Whether there is room is the grant's decision; this can only take it away.
 * @property showGoal «In words»: the share of the goal walked, with the goal.
 * @property showMetrics «In words»: distance and active calories, estimated, on one line.
 * @property showDetails «In words», tall cards: the day in figures, one quantity per line.
 * @property showHours «At a glance», wide and tall cards: the day hour by hour, in 24 bars.
 * @property cardColor kept while another background is picked, so coming back finds it.
 */
data class WidgetLook(
    val background: WidgetBackground = WidgetBackground.COLOR,
    val opacityPct: Int = DEFAULT_OPACITY,
    val cardColor: WidgetCardColor = WidgetCardColor.BLUE,
    val showSentence: Boolean = true,
    val showGoal: Boolean = true,
    val showMetrics: Boolean = true,
    val showDetails: Boolean = true,
    val showHours: Boolean = true,
    val arrangement: WidgetArrangement = WidgetArrangement.RING_START,
) {
    companion object {
        /** Solid: below [INK_TRUST_FLOOR_PCT] the ink has to ask the wallpaper, a different conversation. */
        const val DEFAULT_OPACITY: Int = 100
    }
}

/**
 * The looks, keyed by appWidgetId. Presentation only, so it lives with the widgets and not in
 * `:core:data`; a removed widget takes its look with it ([forget]).
 */
@Singleton
class WidgetLookStore internal constructor(private val dataStore: DataStore<Preferences>) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.widgetLookDataStore)

    suspend fun lookFor(appWidgetId: Int): WidgetLook {
        val prefs = dataStore.data.first()
        val default = WidgetLook()
        return WidgetLook(
            background = prefs[backgroundKey(appWidgetId)]
                ?.let { name -> WidgetBackground.entries.firstOrNull { it.name == name } }
                ?: default.background,
            opacityPct = (prefs[opacityKey(appWidgetId)] ?: default.opacityPct).coerceIn(0, 100),
            cardColor = prefs[colorKey(appWidgetId)]
                ?.let { name -> WidgetCardColor.entries.firstOrNull { it.name == name } }
                ?: default.cardColor,
            showSentence = prefs[sentenceKey(appWidgetId)] ?: default.showSentence,
            showGoal = prefs[goalKey(appWidgetId)] ?: default.showGoal,
            showMetrics = prefs[metricsKey(appWidgetId)] ?: default.showMetrics,
            showDetails = prefs[detailsKey(appWidgetId)] ?: default.showDetails,
            showHours = prefs[hoursKey(appWidgetId)] ?: default.showHours,
            arrangement = prefs[arrangementKey(appWidgetId)]
                ?.let { name -> WidgetArrangement.entries.firstOrNull { it.name == name } }
                ?: default.arrangement,
        )
    }

    suspend fun set(appWidgetId: Int, look: WidgetLook) {
        dataStore.edit { prefs ->
            prefs[backgroundKey(appWidgetId)] = look.background.name
            prefs[opacityKey(appWidgetId)] = look.opacityPct.coerceIn(0, 100)
            prefs[colorKey(appWidgetId)] = look.cardColor.name
            prefs[sentenceKey(appWidgetId)] = look.showSentence
            prefs[goalKey(appWidgetId)] = look.showGoal
            prefs[metricsKey(appWidgetId)] = look.showMetrics
            prefs[detailsKey(appWidgetId)] = look.showDetails
            prefs[hoursKey(appWidgetId)] = look.showHours
            prefs[arrangementKey(appWidgetId)] = look.arrangement.name
        }
    }

    /** From the receivers' `onDeleted`: a removed widget leaves nothing behind. */
    suspend fun forget(appWidgetIds: IntArray) {
        dataStore.edit { prefs ->
            appWidgetIds.forEach { id ->
                keysOf(id).forEach { prefs.remove(it) }
            }
        }
    }

    private fun keysOf(id: Int): List<Preferences.Key<*>> = listOf(
        backgroundKey(id), opacityKey(id), colorKey(id), sentenceKey(id), goalKey(id),
        metricsKey(id), detailsKey(id), hoursKey(id), arrangementKey(id),
    )

    private fun backgroundKey(id: Int) = stringPreferencesKey("bg_$id")
    private fun opacityKey(id: Int) = intPreferencesKey("opacity_$id")
    private fun colorKey(id: Int) = stringPreferencesKey("card_color_$id")
    private fun sentenceKey(id: Int) = booleanPreferencesKey("sentence_$id")
    private fun goalKey(id: Int) = booleanPreferencesKey("goal_$id")
    private fun metricsKey(id: Int) = booleanPreferencesKey("metrics_$id")
    private fun detailsKey(id: Int) = booleanPreferencesKey("details_$id")
    private fun hoursKey(id: Int) = booleanPreferencesKey("hours_$id")
    private fun arrangementKey(id: Int) = stringPreferencesKey("arrangement_$id")
}
