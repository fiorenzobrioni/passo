package com.callbackdev.passo.debug

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Process
import com.callbackdev.passo.R
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Test builds only (debug, and the debug-signed release; app/build.gradle.kts): catches an
 * uncaught exception and shows its stack trace in [CrashReportActivity], in a process of its own,
 * instead of the system's bare "Passo keeps stopping". Installed by a provider so a crash in the
 * Application or in Hilt's graph is caught too. Not a crash reporter: nothing is sent anywhere;
 * the reader shares the text if they choose. In the minified build the trace is R8's: retrace it
 * with the run's mapping.txt.
 *
 * Passo mostly runs with nothing on screen (the counting service, the receivers, the widgets),
 * and Android starts no activity from the background: there the report is a notification that
 * opens the page, and the crash then goes on to the system as usual, which records it. The
 * report is also kept in a file, so the page has it whichever way it is opened.
 */
class CrashCatcher : ContentProvider() {
    override fun onCreate(): Boolean {
        val context = context ?: return false
        // Under Robolectric a caught crash would end the test JVM, not show a page.
        if (Build.FINGERPRINT == "robolectric") return true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val report = runCatching { report(context, thread, error) }.getOrNull()
            if (report != null) runCatching { reportFile(context).writeText(report) }
            val shown = report != null && inForeground() && runCatching { show(context, report) }.isSuccess
            if (shown) {
                Process.killProcess(Process.myPid())
                exitProcess(EXIT_CODE)
            } else {
                if (report != null) runCatching { notify(context, report) }
                previous?.uncaughtException(thread, error)
            }
        }
        return true
    }

    /** An activity of Passo's is on screen: the page can open over it. */
    private fun inForeground(): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun show(context: Context, report: String) {
        context.startActivity(pageIntent(context, report).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    private fun notify(context: Context, report: String) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.debug_crash_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            pageIntent(context, report),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = context.getString(R.string.debug_crash_notification)
        manager.notify(
            NOTIFICATION_ID,
            android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.callbackdev.passo.core.tracking.R.drawable.ic_stat_steps)
                .setContentTitle(context.getString(R.string.debug_crash_title))
                .setContentText(text)
                .setStyle(android.app.Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun pageIntent(context: Context, report: String): Intent = Intent(context, CrashReportActivity::class.java)
        .putExtra(CrashReportActivity.EXTRA_REPORT, report)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun report(context: Context, thread: Thread, error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        // The binder caps an extra's size; the top of a trace is what names the cause.
        return buildString {
            appendLine("Passo $version (${context.packageName})")
            appendLine(
                "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            )
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(trace.take(MAX_REPORT))
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0

    companion object {
        private const val EXIT_CODE = 10
        private const val MAX_REPORT = 60_000
        private const val CHANNEL_ID = "debug_crash"
        private const val NOTIFICATION_ID = 0xC0DE

        /** Where the last report is kept: outside the backup, which carries the database and the settings only. */
        fun reportFile(context: Context): File = File(context.noBackupFilesDir, "crash-report.txt")
    }
}
