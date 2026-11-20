package com.callbackdev.passo.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The permission gate's rules (PLANNING.md §10, §11 Phase 15; docs/adr/0016-location.md
 * decision 2): the merged manifest requests exactly the list, and a forbidden permission is
 * told by name.
 */
class PermissionVerdictTest {
    private val release = "com.callbackdev.passo"

    private fun held(applicationId: String = release): List<String> =
        AllowedPermissions.map { it.replace(APPLICATION_ID_PLACEHOLDER, applicationId) }

    private fun verdict(requested: List<String>, applicationId: String = release) =
        permissionVerdict(requested, AllowedPermissions, ForbiddenPermissionPatterns, applicationId)

    @Test
    fun `exactly the list passes`() {
        assertTrue(verdict(held()).passes)
    }

    @Test
    fun `an unlisted permission fails, by name`() {
        val result = verdict(held() + "android.permission.CAMERA")

        assertFalse(result.passes)
        assertEquals(listOf("android.permission.CAMERA"), result.unlisted)
        assertEquals(emptyList<String>(), result.forbidden)
    }

    @Test
    fun `background location fails as forbidden, not as unlisted`() {
        val result = verdict(held() + "android.permission.ACCESS_BACKGROUND_LOCATION")

        assertFalse(result.passes)
        assertEquals(listOf("android.permission.ACCESS_BACKGROUND_LOCATION"), result.forbidden)
        assertEquals(emptyList<String>(), result.unlisted)
    }

    @Test
    fun `the network fails as forbidden`() {
        val result = verdict(held() + "android.permission.INTERNET")

        assertEquals(listOf("android.permission.INTERNET"), result.forbidden)
    }

    @Test
    fun `every forbidden name is told as forbidden`() {
        val names = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM",
            "android.permission.HIGH_SAMPLING_RATE_SENSORS",
            "android.permission.BODY_SENSORS",
            "android.permission.BODY_SENSORS_BACKGROUND",
        )

        assertEquals(names.sorted(), verdict(held() + names).forbidden)
    }

    @Test
    fun `fine and coarse location are on the list, and no other location permission is`() {
        val allowed = held()

        assertTrue("android.permission.ACCESS_FINE_LOCATION" in allowed)
        assertTrue("android.permission.ACCESS_COARSE_LOCATION" in allowed)
        assertTrue("android.permission.FOREGROUND_SERVICE_LOCATION" in allowed)
        assertEquals(
            listOf("android.permission.ACCESS_MEDIA_LOCATION"),
            verdict(allowed + "android.permission.ACCESS_MEDIA_LOCATION").unlisted,
        )
    }

    @Test
    fun `a listed permission nothing requests fails too`() {
        val result = verdict(held() - "android.permission.VIBRATE")

        assertFalse(result.passes)
        assertEquals(listOf("android.permission.VIBRATE"), result.missing)
    }

    @Test
    fun `the permission named after the application id is read per variant`() {
        val debug = "com.callbackdev.passo.debug"

        assertTrue(verdict(held(debug), debug).passes)
        // The release's own name in a debug manifest is another permission.
        val crossed = verdict(held(release), debug)
        assertEquals(listOf("$release.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"), crossed.unlisted)
        assertEquals(listOf("$debug.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"), crossed.missing)
    }

    @Test
    fun `a permission requested twice counts once`() {
        assertTrue(verdict(held() + held()).passes)
    }
}
