package com.callbackdev.passo.core.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Starts tracking after a boot, without the app being opened (PLANNING.md §4.2). The steps
 * taken since power-on are not lost meanwhile: the counter holds them until the first sample.
 * A pause the reader chose outlives the reboot: the setting is read before starting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) StepTracking.startFromBroadcast(this, context)
    }
}
