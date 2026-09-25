package com.callbackdev.passo.core.designsystem.format

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.settings.resolve
import com.callbackdev.passo.core.model.Measure
import com.callbackdev.passo.core.model.MeasureUnit
import com.callbackdev.passo.core.model.UnitPreference

/**
 * The formatter for the app's language (numbers read the way the text around them does) and
 * the reader's units. "System" units follow the phone's region, not the app's language: an
 * Italian phone with Passo in English still walks in kilometres.
 */
@Composable
fun rememberMeasureFormatter(units: UnitPreference): MeasureFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale, units) { MeasureFormatter(locale, units.resolve(systemRegion())) }
}

/** The same formatter outside Compose (the widget, the notification). */
fun Context.measureFormatter(units: UnitPreference): MeasureFormatter =
    MeasureFormatter(resources.configuration.locales[0], units.resolve(systemRegion()))

/** A measure with its unit, e.g. "2.40 km". */
@Composable
fun Measure.text(): String = stringResource(unit.formatRes, number)

/** Measures side by side, e.g. "5 ft 9 in". */
@Composable
fun List<Measure>.text(): String {
    // A plain loop: joinToString's lambda is not inline, so it cannot call a composable.
    val parts = ArrayList<String>(size)
    for (measure in this) parts += measure.text()
    return parts.joinToString(" ")
}

/**
 * A measure with its unit set apart in [unitStyle]: the number is the reading, the unit a quiet
 * suffix («4,21 km» with the km small), wherever the unit's resource puts it.
 */
@Composable
fun Measure.annotated(unitStyle: SpanStyle): AnnotatedString {
    val full = text()
    val start = full.indexOf(number)
    return buildAnnotatedString {
        append(full)
        if (start < 0) return@buildAnnotatedString
        if (start > 0) addStyle(unitStyle, 0, start)
        if (start + number.length < full.length) addStyle(unitStyle, start + number.length, full.length)
    }
}

fun Resources.format(measure: Measure): String = getString(measure.unit.formatRes, measure.number)

fun Resources.format(measures: List<Measure>): String = measures.joinToString(" ") { format(it) }

private fun systemRegion(): String? = Resources.getSystem().configuration.locales[0]?.country

@get:StringRes
private val MeasureUnit.formatRes: Int
    get() = when (this) {
        MeasureUnit.KILOMETER -> R.string.measure_kilometers
        MeasureUnit.MILE -> R.string.measure_miles
        MeasureUnit.KILOCALORIE -> R.string.measure_kilocalories
        MeasureUnit.KILOGRAM -> R.string.measure_kilograms
        MeasureUnit.POUND -> R.string.measure_pounds
        MeasureUnit.CENTIMETER -> R.string.measure_centimeters
        MeasureUnit.FOOT -> R.string.measure_feet
        MeasureUnit.INCH -> R.string.measure_inches
        MeasureUnit.STEPS_PER_MINUTE -> R.string.measure_steps_per_minute
        MeasureUnit.MINUTE -> R.string.measure_minutes
    }
