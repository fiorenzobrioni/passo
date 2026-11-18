package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.AltitudeSource
import kotlin.math.exp
import kotlin.math.pow

/**
 * The altitude of a route's points (PLANNING.md §11 Phase 16, «The climb»), blended from the
 * two things a phone knows about height, each good at what the other is bad at:
 *
 * - **The pressure sensor** sees a metre climbed within a second, and knows nothing of sea
 *   level: the weather moves it by tens of metres in an afternoon.
 * - **The satellites' altitude** is right over the long term and wanders by ten metres or more
 *   from one minute to the next.
 *
 * So the barometer gives the changes and the satellites the level: a point's altitude is what
 * the pressure says, less what the weather is known to add to it, and that is learnt from the
 * satellites a little at a time ([BAROMETER_SETTLE_MILLIS]). A weather front that lowers the
 * pressure for two hours of a flat walk is then a level that stays level, not a hill; a real
 * hill is in the pressure at once, and in the satellites too.
 *
 * On a phone with no pressure sensor the satellites are alone: smoothed
 * ([SATELLITE_SETTLE_MILLIS]), and their climb is counted only past a higher threshold
 * ([Climb]).
 *
 * Pure, and fed in the order of time; not thread-safe.
 */
class Altitude {
    private var level: Double? = null
    private var relative: Double? = null

    // What the pressure's altitude stands above the satellites' by, as far as it is known: the
    // day's weather. Null while no pressure reading is in hand.
    private var drift: Double? = null
    private var lastAtMillis: Long? = null
    private var firstAtMillis: Long? = null
    private var corrections = 0

    /** Where the altitudes so far came from; null before the first. */
    var source: AltitudeSource? = null
        private set

    /**
     * The height of a point taken at [atMillis].
     *
     * @param pressureHpa the air pressure then, in hectopascals; null with no sensor, or when
     *   no reading is near enough in time.
     * @param satelliteMeters the fix's own altitude above mean sea level; null when it had none.
     * @return null until a satellite altitude has given the level: the pressure alone knows
     *   changes, not heights.
     */
    fun next(atMillis: Long, pressureHpa: Float?, satelliteMeters: Double?): Height? {
        val elapsed = (atMillis - (lastAtMillis ?: atMillis)).coerceAtLeast(0)
        lastAtMillis = atMillis
        val pressure = pressureHpa?.takeIf { it > 0f }?.let(::pressureAltitude)
        val before = level
        val weather = drift
        var current = before
        var driftChange = 0.0
        val warming = atMillis - (firstAtMillis ?: atMillis) < WARM_UP_MILLIS
        when {
            // The satellites alone: this point's, smoothed.
            pressure == null -> {
                drift = null
                if (satelliteMeters != null) {
                    if (source == null) source = AltitudeSource.SATELLITES
                    val gain = 1.0 - exp(-elapsed / SATELLITE_SETTLE_MILLIS)
                    current = if (current == null) satelliteMeters else current + (satelliteMeters - current) * gain
                }
            }

            // The first height: the satellites give the level, the pressure will carry it.
            current == null -> if (satelliteMeters != null) {
                source = AltitudeSource.BAROMETER
                drift = pressure - satelliteMeters
                current = satelliteMeters
            }

            // The pressure is back after a gap: taken up from where the level stands, so that
            // whatever it did meanwhile (the weather, a pocket) is not a climb.
            weather == null -> {
                source = AltitudeSource.BAROMETER
                drift = pressure - current
            }

            else -> {
                var now = weather
                if (satelliteMeters != null) {
                    corrections++
                    // The first figures are averaged as they come, so that one poor first fix
                    // is not the level the whole walk is then measured from.
                    val averaging = if (warming) 1.0 / corrections else 0.0
                    val gain = maxOf(1.0 - exp(-elapsed / BAROMETER_SETTLE_MILLIS), averaging)
                    now = weather + (pressure - satelliteMeters - weather) * gain
                    driftChange = now - weather
                    drift = now
                }
                current = pressure - now
            }
        }
        if (current == null) return null
        if (firstAtMillis == null) firstAtMillis = atMillis
        level = current
        // What the climb is counted on: the same changes, but for the level settling in its
        // first minutes, which is the satellites finding the height, not the reader climbing.
        val change = current - (before ?: current)
        val climbed = (relative ?: current) + if (pressure != null && warming) change + driftChange else change
        relative = climbed
        return Height(meters = current, relative = climbed)
    }

    /**
     * A point's height.
     *
     * @property meters above mean sea level: what the point keeps, and the profile draws.
     * @property relative the series the climb is counted on: [meters], less the settling of the
     *   level in a walk's first minutes.
     */
    data class Height(val meters: Double, val relative: Double)

    companion object {
        /**
         * With a barometer, the weather is learnt from the satellites over about ten minutes:
         * long enough that their minute-to-minute wander (ten metres and more) is averaged
         * away, short enough that a front (a few hectopascals an hour at the worst, some 25 m)
         * is never more than a few metres ahead. To be validated on the device.
         */
        const val BAROMETER_SETTLE_MILLIS: Double = 10 * 60_000.0

        /** Without one, the satellites are smoothed over a minute: slower would flatten a real hill. */
        const val SATELLITE_SETTLE_MILLIS: Double = 60_000.0

        /**
         * For its first five minutes the level is still being found: what the satellites say
         * is averaged as it comes, and that settling is kept out of the climb.
         */
        const val WARM_UP_MILLIS: Long = 5 * 60_000L

        private const val SEA_LEVEL_HPA = 1013.25
        private const val BAROMETRIC_SCALE_METERS = 44_330.0
        private const val BAROMETRIC_EXPONENT = 1.0 / 5.255

        /**
         * The altitude a pressure stands for in the standard atmosphere (the international
         * barometric formula, the one `SensorManager.getAltitude` uses). Only its differences
         * are used here, which hold whatever the day's pressure at sea level.
         */
        fun pressureAltitude(hpa: Float): Double =
            BAROMETRIC_SCALE_METERS * (1.0 - (hpa / SEA_LEVEL_HPA).pow(BAROMETRIC_EXPONENT))
    }
}

/**
 * How much an outing climbed and came down (PLANNING.md §11 Phase 16): metres up and metres
 * down, counted only past a threshold that noise does not cross. A walk on the flat under a
 * restless receiver climbs nothing; a hill of fifty metres is fifty, give or take the threshold.
 *
 * It keeps the altitude it last counted from: a rise is added once it stands [thresholdMeters]
 * above that, a fall once it stands as much below. Fed in the order of time; not thread-safe.
 *
 * @param thresholdMeters [BAROMETER_THRESHOLD_METERS] with a pressure sensor,
 *   [SATELLITE_THRESHOLD_METERS] without.
 */
class Climb(private val thresholdMeters: Double) {
    private var from: Double? = null

    /** What [altitudeMeters] adds: metres up or metres down, and most often neither. */
    fun next(altitudeMeters: Double): Step {
        val reference = from
        if (reference == null) {
            from = altitudeMeters
            return Step.NONE
        }
        val change = altitudeMeters - reference
        if (change >= thresholdMeters) {
            from = altitudeMeters
            return Step(up = change, down = 0.0)
        }
        if (change <= -thresholdMeters) {
            from = altitudeMeters
            return Step(up = 0.0, down = -change)
        }
        return Step.NONE
    }

    /** A line was cut (a pause, a ride): what comes next is counted from its own first point. */
    fun cut() {
        from = null
    }

    data class Step(val up: Double, val down: Double) {
        val isNone: Boolean get() = up == 0.0 && down == 0.0

        companion object {
            val NONE = Step(0.0, 0.0)
        }
    }

    companion object {
        /**
         * With a barometer: three metres, a floor of a building. The sensor's own noise is a
         * few decimetres; what three metres leaves out is the door slammed and the gust.
         */
        const val BAROMETER_THRESHOLD_METERS: Double = 3.0

        /** With the satellites alone, whose altitude wanders by several metres: ten. */
        const val SATELLITE_THRESHOLD_METERS: Double = 10.0

        fun thresholdFor(source: AltitudeSource): Double = when (source) {
            AltitudeSource.BAROMETER -> BAROMETER_THRESHOLD_METERS
            AltitudeSource.SATELLITES -> SATELLITE_THRESHOLD_METERS
        }
    }
}
