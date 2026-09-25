package com.callbackdev.passo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.callbackdev.passo.widget.glance.GlanceWidget
import com.callbackdev.passo.widget.glance.GlanceWidgetReceiver
import com.callbackdev.passo.widget.words.WordsWidget
import com.callbackdev.passo.widget.words.WordsWidgetReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The two cards, as the settings screen and the previews need to tell them apart. */
enum class WidgetKind {
    /** «At a glance»: the ring, the number, the sentence, and the day hour by hour. */
    GLANCE,

    /** «In words»: the number and the day in type, Chiaro's «In parole». */
    WORDS,
}

/**
 * The widgets as one household: who is placed, and how to repaint them. Repaints go by the
 * system's own mapping of ids to providers, never by Glance's class bookkeeping (Chiaro saw every
 * widget repainted with the last-placed one's content that way), and always through
 * [WidgetRefresh] first, because `update()` alone wakes a live session without reloading it.
 */
object PassoWidgets {
    private val household: List<Pair<Class<out GlanceAppWidgetReceiver>, () -> GlanceAppWidget>> = listOf(
        GlanceWidgetReceiver::class.java to { GlanceWidget() },
        WordsWidgetReceiver::class.java to { WordsWidget() },
    )

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return household.any { (receiver, _) -> manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty() }
    }

    /** Which card [appWidgetId] is; null for an id the host has not bound yet. */
    fun kindOf(context: Context, appWidgetId: Int): WidgetKind? =
        when (AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.className) {
            GlanceWidgetReceiver::class.java.name -> WidgetKind.GLANCE
            WordsWidgetReceiver::class.java.name -> WidgetKind.WORDS
            else -> null
        }

    /** Every placed card, with a fresh model. */
    suspend fun updateAll(context: Context) = update(context) { true }

    /** One card, with a fresh model: its settings screen's «apply now». */
    suspend fun updateOne(context: Context, appWidgetId: Int) = update(context) { it == appWidgetId }

    private suspend fun update(context: Context, which: (Int) -> Boolean) {
        WidgetRefresh.invalidate()
        val manager = AppWidgetManager.getInstance(context)
        val glance = GlanceAppWidgetManager(context)
        household.forEach { (receiver, widget) ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).filter(which).forEach { id ->
                runCatching { widget().update(context, glance.getGlanceIdBy(id)) }
            }
        }
    }
}

/** What Glance's classes, which Hilt does not build, need from the graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun loader(): WidgetModelLoader

    fun looks(): WidgetLookStore
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/** A removed card takes its look with it. */
abstract class PassoWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext
        val pending = goAsync()
        Cleanup.launch {
            try {
                runCatching { app.widgetEntryPoint().looks().forget(appWidgetIds) }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val Cleanup = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
