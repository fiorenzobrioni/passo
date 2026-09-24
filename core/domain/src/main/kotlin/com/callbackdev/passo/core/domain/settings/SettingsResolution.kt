package com.callbackdev.passo.core.domain.settings

import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UnitSystem
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * The regions where distances are walked in miles: the United States, Liberia and Myanmar (CLDR's
 * "US" measurement system) and the United Kingdom (CLDR's "UK" system, mixed, but miles on
 * every road sign and in every walking conversation).
 */
private val IMPERIAL_REGIONS = setOf("US", "LR", "MM", "GB")

/** The units to show, with [UnitPreference.SYSTEM] resolved from the locale's [region]. */
fun UnitPreference.resolve(region: String?): UnitSystem = when (this) {
    UnitPreference.METRIC -> UnitSystem.METRIC

    UnitPreference.IMPERIAL -> UnitSystem.IMPERIAL

    UnitPreference.SYSTEM ->
        if (region?.uppercase(Locale.ROOT) in IMPERIAL_REGIONS) UnitSystem.IMPERIAL else UnitSystem.METRIC
}

/** The first day of the week: the reader's choice, or the one [locale] uses. */
fun firstDayOfWeek(chosen: DayOfWeek?, locale: Locale): DayOfWeek = chosen ?: WeekFields.of(locale).firstDayOfWeek
