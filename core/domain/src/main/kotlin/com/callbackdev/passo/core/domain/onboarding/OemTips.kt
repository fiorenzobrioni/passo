package com.callbackdev.passo.core.domain.onboarding

import java.util.Locale

/**
 * The manufacturers whose battery managers are known to stop background apps, and the page
 * of dontkillmyapp.com that says how to exempt one on their phones (PLANNING.md §9 rule 7).
 * Onboarding shows its battery tip only on these: on every other phone it would be a warning
 * about a problem that phone does not have.
 */
object OemTips {
    private val slugs = linkedMapOf(
        "xiaomi" to "xiaomi",
        "redmi" to "xiaomi",
        "poco" to "xiaomi",
        "huawei" to "huawei",
        "honor" to "huawei",
        "oneplus" to "oneplus",
        "oppo" to "oppo",
        "realme" to "realme",
        "vivo" to "vivo",
        "samsung" to "samsung",
        "meizu" to "meizu",
        "asus" to "asus",
        "nokia" to "nokia",
        "hmd global" to "nokia",
        "sony" to "sony",
        "lenovo" to "lenovo",
    )

    /** The dontkillmyapp.com page for [manufacturer] (`Build.MANUFACTURER`), or null when it needs none. */
    fun slugFor(manufacturer: String): String? {
        val name = manufacturer.trim().lowercase(Locale.ROOT)
        return slugs.entries.firstOrNull { name.startsWith(it.key) }?.value
    }

    fun pageFor(slug: String): String = "https://dontkillmyapp.com/$slug"
}
