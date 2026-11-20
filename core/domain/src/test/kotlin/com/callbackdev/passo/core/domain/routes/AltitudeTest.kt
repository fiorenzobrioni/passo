package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.AltitudeSource
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sin

/**
 * The altitude of a route's points, blended from the pressure and the satellites, and the climb
 * counted on it (PLANNING.md §11 Phase 16, «The climb»).
 */
class AltitudeTest {
    /** The pressure at [meters] in the standard atmosphere, less [frontHpa] of weather. */
    private fun pressure(meters: Double, frontHpa: Double = 0.0): Float =
        (1013.25 * (1.0 - meters / 44_330.0).pow(5.255) - frontHpa).toFloat()

    /** A receiver's altitude: the truth, with a wander of [noise] metres over a few minutes. */
    private fun satellites(truth: Double, second: Int, noise: Double = 8.0): Double =
        truth + noise * sin(second / 47.0) + noise / 2 * sin(second / 11.0 + 1.0)

    /** A walk of [seconds], a point every two: its true altitude, and the weather over it. */
    private fun walk(
        seconds: Int,
        barometer: Boolean = true,
        frontHpa: (Int) -> Double = { 0.0 },
        truth: (Int) -> Double,
    ): Pair<List<Altitude.Height>, Altitude> {
        val altitude = Altitude()
        val blended = (0..seconds step 2).mapNotNull { second ->
            altitude.next(
                atMillis = second * 1_000L,
                pressureHpa = if (barometer) pressure(truth(second), frontHpa(second)) else null,
                satelliteMeters = satellites(truth(second), second),
            )
        }
        return blended to altitude
    }

    private fun climbed(heights: List<Altitude.Height>, threshold: Double): Climb.Step {
        val climb = Climb(threshold)
        return heights.map { climb.next(it.relative) }.fold(Climb.Step.NONE) { sum, step ->
            Climb.Step(sum.up + step.up, sum.down + step.down)
        }
    }

    @Test
    fun `the pressure's altitude is the standard atmosphere's`() {
        assertThat(Altitude.pressureAltitude(1013.25f)).isWithin(0.01).of(0.0)
        // About 8.3 m a hectopascal near sea level.
        assertThat(Altitude.pressureAltitude(1012.25f)).isWithin(0.2).of(8.3)
        assertThat(Altitude.pressureAltitude(pressure(240.0))).isWithin(0.5).of(240.0)
    }

    @Test
    fun `a flat walk under a restless receiver climbs nothing`() {
        val (heights, altitude) = walk(seconds = 3_600) { 120.0 }

        assertThat(altitude.source).isEqualTo(AltitudeSource.BAROMETER)
        val total = climbed(heights, Climb.BAROMETER_THRESHOLD_METERS)
        assertThat(total.up).isEqualTo(0.0)
        assertThat(total.down).isEqualTo(0.0)
        // And the level is the satellites', their wander averaged away.
        assertThat(heights.last().meters).isWithin(4.0).of(120.0)
    }

    @Test
    fun `a flat walk under a passing weather front - no climb invented`() {
        // Three hectopascals fall in two hours: 25 m of "climb" to a barometer alone.
        val (heights, _) = walk(seconds = 7_200, frontHpa = { it / 7_200.0 * 3.0 }) { 120.0 }

        val total = climbed(heights, Climb.BAROMETER_THRESHOLD_METERS)
        assertThat(total.up).isLessThan(Climb.BAROMETER_THRESHOLD_METERS + 2.0)
        assertThat(total.down).isEqualTo(0.0)
        // The blend follows the satellites over the long term: never far from the true level.
        assertThat(heights.maxOf { it.meters } - 120.0).isLessThan(8.0)
    }

    @Test
    fun `a hill is in the pressure at once - 240 m up and down again`() {
        // Up 240 m in forty minutes, down in forty.
        val (heights, _) = walk(seconds = 4_800) { second ->
            100.0 + if (second <= 2_400) second / 10.0 else (4_800 - second) / 10.0
        }

        val total = climbed(heights, Climb.BAROMETER_THRESHOLD_METERS)
        assertThat(total.up).isWithin(12.0).of(240.0)
        assertThat(total.down).isWithin(12.0).of(240.0)
        assertThat(heights.maxOf { it.meters }).isWithin(10.0).of(340.0)
    }

    @Test
    fun `a climb in the very first minutes is counted too`() {
        // A flight of steps from the door: 20 m up in the first two minutes, then level.
        val (heights, _) = walk(seconds = 1_200) { second -> 100.0 + minOf(second, 120) / 6.0 }

        val total = climbed(heights, Climb.BAROMETER_THRESHOLD_METERS)
        assertThat(total.up).isWithin(4.0).of(20.0)
        assertThat(total.down).isEqualTo(0.0)
    }

    @Test
    fun `a phone with no barometer - satellites alone, the higher threshold, the hill still found`() {
        val (heights, altitude) = walk(seconds = 4_800, barometer = false) { second ->
            100.0 + if (second <= 2_400) second / 10.0 else (4_800 - second) / 10.0
        }

        assertThat(altitude.source).isEqualTo(AltitudeSource.SATELLITES)
        val total = climbed(heights, Climb.SATELLITE_THRESHOLD_METERS)
        assertThat(total.up).isWithin(30.0).of(240.0)
        assertThat(total.down).isWithin(30.0).of(240.0)
    }

    @Test
    fun `no barometer, a flat walk - the satellites' wander stays near the threshold`() {
        val (heights, _) = walk(seconds = 3_600, barometer = false) { 120.0 }

        val total = climbed(heights, Climb.SATELLITE_THRESHOLD_METERS)
        assertThat(total.up + total.down).isLessThan(2 * Climb.SATELLITE_THRESHOLD_METERS + 1.0)
    }

    @Test
    fun `no height before the satellites have given the level`() {
        val altitude = Altitude()

        assertThat(altitude.next(0, pressure(100.0), satelliteMeters = null)).isNull()
        assertThat(altitude.next(2_000, pressure(101.0), satelliteMeters = 104.0)?.meters).isEqualTo(104.0)
        // From there the pressure carries it, with or without a satellite altitude.
        assertThat(altitude.next(4_000, pressure(103.0), satelliteMeters = null)?.meters).isWithin(0.2).of(106.0)
    }

    @Test
    fun `a gap in the pressure readings adds no jump when they come back`() {
        val altitude = Altitude()
        altitude.next(0, pressure(100.0), 100.0)
        altitude.next(2_000, pressure(100.0), 100.0)
        // No reading near this point; then readings again, 30 m higher on the sensor's scale
        // (a front, or the phone moved from a pocket to a car): not a climb of the reader's.
        val gap = altitude.next(4_000, null, 100.0)
        val back = altitude.next(6_000, pressure(130.0), 100.0)

        assertThat(gap?.meters).isWithin(0.5).of(100.0)
        assertThat(back?.meters).isWithin(0.5).of(100.0)
        assertThat(back?.relative).isWithin(0.5).of(100.0)
    }

    @Test
    fun `the climb is counted past its threshold, and from where it was last counted`() {
        val climb = Climb(3.0)

        assertThat(climb.next(100.0).isNone).isTrue()
        assertThat(climb.next(102.9).isNone).isTrue()
        assertThat(climb.next(103.5)).isEqualTo(Climb.Step(up = 3.5, down = 0.0))
        assertThat(climb.next(101.0).isNone).isTrue()
        assertThat(climb.next(100.0)).isEqualTo(Climb.Step(up = 0.0, down = 3.5))
    }

    @Test
    fun `a line cut - a pause, a ride - is not a climb`() {
        val climb = Climb(3.0)
        climb.next(100.0)
        climb.cut()

        // A funicular took the reader 80 m up during the pause.
        assertThat(climb.next(180.0).isNone).isTrue()
        assertThat(climb.next(184.0)).isEqualTo(Climb.Step(up = 4.0, down = 0.0))
    }

    @Test
    fun `the threshold follows the source`() {
        assertThat(Climb.thresholdFor(AltitudeSource.BAROMETER)).isEqualTo(3.0)
        assertThat(Climb.thresholdFor(AltitudeSource.SATELLITES)).isEqualTo(10.0)
    }
}
