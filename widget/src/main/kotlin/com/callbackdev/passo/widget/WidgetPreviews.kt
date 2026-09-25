package com.callbackdev.passo.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.collection.intSetOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.callbackdev.passo.widget.glance.GlanceWidgetReceiver
import com.callbackdev.passo.widget.words.WordsWidgetReceiver
import kotlinx.coroutines.flow.first

/**
 * The picker's generated previews (Android 15+): the real cards, drawn from [WidgetSamples] by
 * `providePreview`, published once per app version. The platform rate-limits the call, and a
 * preview only changes when the app does, so the version it was published for is remembered;
 * a refused call is simply tried again at the next start. Below Android 15 the static
 * `previewLayout` is what the picker shows.
 */
object WidgetPreviews {
    suspend fun publishIfNeeded(context: Context, appVersion: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val store = context.widgetLookDataStore
        if (store.data.first()[PublishedFor] == appVersion) return
        val manager = GlanceAppWidgetManager(context)
        val categories = intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
        val results = listOf(GlanceWidgetReceiver::class, WordsWidgetReceiver::class).map { receiver ->
            runCatching { manager.setWidgetPreviews(receiver, categories) }.getOrNull()
        }
        if (results.all { it == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }) {
            store.edit { it[PublishedFor] = appVersion }
        }
    }

    private val PublishedFor = longPreferencesKey("previews_published_for")
}
