package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * An outing with GPS a hundred metres at a time, and its kilometres or miles cut from them
 * (PLANNING.md §11 Phase 16, «Splits»).
 */
class SplitsTest {
    /** A reader at [speed] metres a second and 110 steps a minute, settled every ten seconds. */
    private fun walk(seconds: Int, speed: Double = 1.4, from: List<OutingLeg> = emptyList(), startAt: Long = 0) =
        (0 until seconds / 10).fold(from) { legs, batch ->
            val fromMillis = startAt + batch * 10_000L
            Legs.lay(
                legs,
                LegPiece(
                    meters = speed * 10,
                    steps = 110 / 6.0,
                    movingMillis = 10_000,
                    fromMillis = fromMillis,
                    toMillis = fromMillis + 10_000,
                ),
            )
        }

    @Test
    fun `the legs are a hundred metres each, and the last is the one under way`() {
        // 840 m in ten minutes.
        val legs = walk(600)

        assertThat(legs).hasSize(9)
        assertThat(legs.dropLast(1).map { it.meters }.distinct().single()).isWithin(1e-6).of(100.0)
        assertThat(legs.last().meters).isWithin(1e-6).of(40.0)
        assertThat(legs.map { it.index }).containsExactlyElementsIn(0..8).inOrder()
    }

    @Test
    fun `nothing is lost in the cutting - the legs add up to what was walked`() {
        val legs = walk(600)

        assertThat(legs.sumOf { it.meters }).isWithin(1e-6).of(840.0)
        assertThat(legs.sumOf { it.steps }).isWithin(1e-6).of(1_100.0)
        // The time is shared in whole milliseconds: a few lost in ten minutes.
        assertThat(legs.sumOf { it.movingMillis }.toDouble()).isWithin(100.0).of(600_000.0)
    }

    @Test
    fun `a leg ends when its last metre was walked`() {
        val legs = walk(600)

        // The first hundred metres take 71.4 s at 1.4 m/s.
        assertThat(legs[0].endedAtMillis.toDouble()).isWithin(50.0).of(71_428.0)
        assertThat(legs[1].endedAtMillis.toDouble()).isWithin(50.0).of(142_857.0)
        assertThat(legs.last().endedAtMillis).isEqualTo(600_000)
    }

    @Test
    fun `one long batch is laid over as many legs as it covers`() {
        // A sensor with no FIFO: 350 m handed over at once.
        val legs = Legs.lay(emptyList(), LegPiece(350.0, 460.0, 250_000, fromMillis = 0, toMillis = 250_000))

        assertThat(legs.map { it.meters }).containsExactly(100.0, 100.0, 100.0, 50.0).inOrder()
        assertThat(legs[0].steps).isWithin(1e-6).of(460.0 * 100 / 350)
        assertThat(legs[0].endedAtMillis.toDouble()).isWithin(2.0).of(250_000.0 * 100 / 350)
    }

    @Test
    fun `steps that went nowhere belong to the leg under way`() {
        val legs = walk(60)
        val marching = Legs.lay(legs, LegPiece(0.0, steps = 40.0, movingMillis = 20_000, 60_000, 80_000))

        assertThat(marching).hasSize(1)
        assertThat(marching[0].meters).isWithin(1e-6).of(84.0)
        assertThat(marching[0].steps).isWithin(1e-6).of(110.0 + 40.0)
        assertThat(marching[0].movingMillis).isEqualTo(80_000)
    }

    @Test
    fun `a new leg is numbered after the last, never by how many there are`() {
        // Legs read back with some missing: the next must not take an index already stored.
        val read = listOf(OutingLeg(index = 7, meters = 100.0, endedAtMillis = 70_000))

        val laid = walk(100, from = read, startAt = 70_000)

        assertThat(laid.map { it.index }).containsExactly(7, 8, 9).inOrder()
    }

    @Test
    fun `a leg too short to say its pace lends the one before it`() {
        val legs = listOf(
            OutingLeg(0, 100.0, 40_000, 100.0, endedAtMillis = 40_000),
            OutingLeg(1, 2.0, 0, 0.0, endedAtMillis = 41_000),
        )

        assertThat(Legs.cadenceAt(legs, 41_000)).isEqualTo(150)
        assertThat(Legs.cadenceAt(legs, 20_000)).isEqualTo(150)
        assertThat(Legs.cadenceAt(emptyList(), 0)).isNull()
    }

    @Test
    fun `an empty piece changes nothing`() {
        val legs = walk(60)

        assertThat(Legs.lay(legs, LegPiece(0.0, 0.0, 0, 60_000, 70_000))).isSameInstanceAs(legs)
    }

    @Test
    fun `a climb goes to the leg that was being walked then`() {
        val legs = walk(600)
        // Two minutes in: the second leg (71 s to 143 s).
        val climbed = Legs.climb(legs, atMillis = 120_000, upMeters = 4.0, downMeters = 0.0)

        assertThat(climbed[1].ascentMeters).isEqualTo(4.0)
        assertThat(climbed.sumOf { it.ascentMeters }).isEqualTo(4.0)
        // After the last leg's end (the altitudes come a moment late): the last leg.
        val late = Legs.climb(legs, atMillis = 700_000, upMeters = 0.0, downMeters = 3.0)
        assertThat(late.last().descentMeters).isEqualTo(3.0)
    }

    @Test
    fun `a kilometre a split, the last one saying its length`() {
        // 2.52 km in half an hour.
        val splits = Splits.of(walk(1_800), Splits.unitMeters(UnitSystem.METRIC))

        assertThat(splits).hasSize(3)
        assertThat(splits[0].meters).isWithin(1e-6).of(1_000.0)
        assertThat(splits[0].whole).isTrue()
        assertThat(splits[2].meters).isWithin(1e-3).of(520.0)
        assertThat(splits[2].whole).isFalse()
        // 1.4 m/s is 11:54 a kilometre.
        assertThat(splits[0].secondsPerKm).isWithin(1.0).of(714.3)
        assertThat(splits[2].secondsPerKm).isWithin(1.0).of(714.3)
        assertThat(splits[0].cadence).isEqualTo(110)
        assertThat(splits.sumOf { it.meters }).isWithin(1e-3).of(2_520.0)
    }

    @Test
    fun `a mile a split, for a reader who walks in miles`() {
        val splits = Splits.of(walk(1_800), Splits.unitMeters(UnitSystem.IMPERIAL))

        assertThat(splits).hasSize(2)
        assertThat(splits[0].meters).isWithin(1e-6).of(UnitConversions.METERS_PER_MILE)
        assertThat(splits[0].whole).isTrue()
        assertThat(splits[1].whole).isFalse()
        // The same pace, whichever the unit it is cut in.
        assertThat(splits[0].secondsPerKm).isWithin(1.0).of(714.3)
    }

    @Test
    fun `a split ends where its last metre was walked - the bead's place on the route`() {
        val splits = Splits.of(walk(1_800), 1_000.0)

        assertThat(splits[0].endedAtMillis.toDouble()).isWithin(200.0).of(714_286.0)
        assertThat(splits[1].endedAtMillis.toDouble()).isWithin(200.0).of(1_428_571.0)
        assertThat(splits[2].endedAtMillis).isEqualTo(1_800_000)
    }

    @Test
    fun `each split has its own pace and its own climb`() {
        // A kilometre at a stroll, uphill, then one brisk and level.
        val slow = walk(1_000, speed = 1.0)
        val uphill = Legs.climb(slow, atMillis = 400_000, upMeters = 35.0, downMeters = 0.0)
        val legs = walk(500, speed = 2.0, from = uphill, startAt = 1_000_000)

        val splits = Splits.of(legs, 1_000.0)

        assertThat(splits).hasSize(2)
        assertThat(splits[0].secondsPerKm).isWithin(1.0).of(1_000.0)
        assertThat(splits[1].secondsPerKm).isWithin(1.0).of(500.0)
        assertThat(splits[0].ascentMeters).isEqualTo(35.0)
        assertThat(splits[1].ascentMeters).isEqualTo(0.0)
        assertThat(Splits.fastest(splits)).isEqualTo(splits[1])
    }

    @Test
    fun `an outing shorter than a kilometre - one partial split, and no fastest`() {
        val splits = Splits.of(walk(300), 1_000.0)

        assertThat(splits).hasSize(1)
        assertThat(splits.single().whole).isFalse()
        assertThat(splits.single().meters).isWithin(1e-6).of(420.0)
        assertThat(Splits.fastest(splits)).isNull()
    }

    @Test
    fun `one kilometre and a bit - one whole split has nothing to be the fastest of`() {
        val splits = Splits.of(walk(900), 1_000.0)

        assertThat(splits.map { it.whole }).containsExactly(true, false).inOrder()
        assertThat(Splits.fastest(splits)).isNull()
    }

    @Test
    fun `a few steps past the mark are the split before's, not a split of ten metres`() {
        // 2,016 m: sixteen metres past the second kilometre.
        val splits = Splits.of(walk(1_440), 1_000.0)

        assertThat(splits).hasSize(2)
        assertThat(splits[1].meters).isWithin(1e-3).of(1_016.0)
        assertThat(splits[1].whole).isTrue()
    }

    @Test
    fun `a short last split has its length, and no pace made of noise`() {
        // 1,050 m.
        val splits = Splits.of(walk(750), 1_000.0)

        assertThat(splits).hasSize(2)
        assertThat(splits[1].meters).isWithin(1e-3).of(50.0)
        assertThat(splits[1].secondsPerKm).isNull()
    }

    @Test
    fun `no legs, no splits`() {
        assertThat(Splits.of(emptyList(), 1_000.0)).isEmpty()
        assertThat(Splits.fastest(emptyList())).isNull()
    }
}
