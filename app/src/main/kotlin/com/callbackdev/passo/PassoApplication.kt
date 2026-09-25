package com.callbackdev.passo

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import com.callbackdev.passo.core.tracking.GoalNotifier
import com.callbackdev.passo.widget.WidgetPreviews
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PassoApplication : Application() {
    @Inject lateinit var goals: GoalNotifier

    override fun onCreate() {
        super.onCreate()
        // Every start of the process arms the goal alarms again: a reboot clears them, and a
        // boot or an update starts the process (PLANNING.md §8).
        goals.start()
        // The widget picker's generated previews, once per version (a no-op below Android 15).
        val version = runCatching {
            PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        }
            .getOrDefault(0L)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching { WidgetPreviews.publishIfNeeded(this@PassoApplication, version) }
        }
    }
}
