package com.callbackdev.passo.core.domain.onboarding

import java.util.Locale

/**
 * The manufacturers whose battery managers are known to stop background apps (PLANNING.md §9
 * rule 7). Onboarding shows its battery tip only on these: on every other phone it would be a
 * warning about a problem that phone does not have.
 *
 * Samsung is not on the list: since One UI 6 (Android 14, Passo's minSdk) Samsung has committed,
 * with Google, to leaving alone the foreground services of apps that target Android 14 and
 * declare their type, as Passo's `health` service does (§15).
 */
object OemTips {
    private val makers = listOf(
        "xiaomi",
        "redmi",
        "poco",
        "huawei",
        "honor",
        "oneplus",
        "oppo",
        "realme",
        "vivo",
        "meizu",
        "asus",
        "nokia",
        "hmd global",
        "sony",
        "lenovo",
    )

    /** Whether [manufacturer] (`Build.MANUFACTURER`) is one whose phones need the tip. */
    fun needsTip(manufacturer: String): Boolean {
        val name = manufacturer.trim().lowercase(Locale.ROOT)
        return name.isNotEmpty() && makers.any { name.startsWith(it) }
    }
}
