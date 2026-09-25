package com.callbackdev.passo.core.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Starts tracking again after an app update, which stops the old process (PLANNING.md §4.2). */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) StepTracking.startFromBroadcast(this, context)
    }
}
