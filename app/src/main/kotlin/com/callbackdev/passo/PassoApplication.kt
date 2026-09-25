package com.callbackdev.passo

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import com.callbackdev.passo.widget.WidgetPreviews
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class PassoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
