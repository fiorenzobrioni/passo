package com.callbackdev.passo.core.tracking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.callbackdev.passo.core.domain.routes.GpsReadiness

/**
 * Whether an outing can be measured with GPS on this phone, right now, and the permissions that
 * question is about (docs/adr/0016-location.md, decisions 2 and 3). Satellites only, through the
 * platform's own `LocationManager`: no fused provider, which on some phones answers from lookups
 * that are requests to a network.
 */
object Gps {
    /**
     * Asked together, as Android has wanted since 12, and the reader may grant the approximate
     * one alone. Never in the first run: only when an outing with «Measure with GPS» first starts.
     */
    val PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    fun readiness(context: Context): GpsReadiness = when {
        !hasReceiver(context) -> GpsReadiness.NO_RECEIVER

        granted(context, Manifest.permission.ACCESS_FINE_LOCATION) -> if (enabled(context)) {
            GpsReadiness.READY
        } else {
            GpsReadiness.LOCATION_OFF
        }

        granted(context, Manifest.permission.ACCESS_COARSE_LOCATION) -> GpsReadiness.APPROXIMATE_ONLY

        else -> GpsReadiness.PERMISSION_NEEDED
    }

    /** Whether the phone has a satellite receiver at all: without one the switch is not offered. */
    fun hasReceiver(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) &&
            context.getSystemService(LocationManager::class.java)?.hasProvider(LocationManager.GPS_PROVIDER) == true

    /** The system's own switch for location, for a reader who turned it off. */
    fun locationSettingsIntent(): Intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    /**
     * The system's page for Passo, where the permission is changed once Android no longer shows
     * its request (after a second refusal): the one way back left then.
     */
    fun appSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    private fun enabled(context: Context): Boolean =
        context.getSystemService(LocationManager::class.java)?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
