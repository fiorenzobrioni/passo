package com.callbackdev.passo.core.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.designsystem.format.planDescription
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.domain.sessions.SessionConstants
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.UnitPreference

/**
 * The outings on the launcher's long press (PLANNING.md §11 Phase 10): the ones started most
 * recently, so the walk of every evening is one touch from the home screen. A shortcut opens the
 * app and starts its outing there, where the reader sees it begin. Costs nothing: the launcher
 * keeps the list, and it is written only when a plan is started, saved or deleted.
 */
object SessionShortcuts {
    fun update(context: Context, plansByUse: List<SessionPlan>, units: UnitPreference) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val format = context.measureFormatter(units)
        val res = context.resources
        val shortcuts = plansByUse.take(SessionConstants.SHORTCUTS).mapIndexed { rank, plan ->
            ShortcutInfoCompat.Builder(context, "$ID_PREFIX${plan.id}")
                .setShortLabel(res.sessionName(plan))
                .setLongLabel(res.planDescription(plan, format))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_outing))
                .setIntent(Intent(launch).putExtra(SessionControl.EXTRA_START_PLAN, plan.id))
                .setRank(rank)
                .build()
        }
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (e: IllegalStateException) {
            // The launcher's rate limit: the next change writes them again.
        }
    }

    private const val ID_PREFIX = "outing-"
}
