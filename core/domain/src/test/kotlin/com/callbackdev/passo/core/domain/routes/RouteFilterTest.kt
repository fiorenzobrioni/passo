package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.domain.routes.RouteFilter.Verdict
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Which fixes a route is built from (PLANNING.md §11 Phase 15; docs/adr/0016-location.md decision 5). */
class RouteFilterTest {
    private val start = Fix(atMillis = 0, latitude = 45.0, longitude = 9.0, accuracyMeters = 5f)

    /** A fix [meters] north of [start], [seconds] later. */
    private fun fix(meters: Double, seconds: Int, accuracy: Float? = 5f, mock: Boolean = false) = Fix(
        atMillis = seconds * 1_000L,
        latitude = 45.0 + meters / METERS_PER_DEGREE,
        longitude = 9.0,
        accuracyMeters = accuracy,
        mock = mock,
    )

    private fun judge(fix: Fix, previous: Fix? = start, cadence: Int? = 110, allowMock: Boolean = false) =
        RouteFilter.judge(fix, previous, cadence, allowMock)

    @Test
    fun `the first fix needs only to be accurate`() {
        assertThat(judge(start, previous = null)).isEqualTo(Verdict.ACCEPTED)
    }

    @Test
    fun `accepted at 25 m of accuracy, refused beyond`() {
        assertThat(judge(fix(1.4, 1, accuracy = 25f))).isEqualTo(Verdict.ACCEPTED)
        assertThat(judge(fix(1.4, 1, accuracy = 25.1f))).isEqualTo(Verdict.INACCURATE)
    }

    @Test
    fun `a fix that gives no accuracy is not trusted`() {
        assertThat(judge(fix(1.4, 1, accuracy = null))).isEqualTo(Verdict.INACCURATE)
        assertThat(judge(fix(1.4, 1, accuracy = Float.NaN))).isEqualTo(Verdict.INACCURATE)
    }

    @Test
    fun `its time must move forward`() {
        assertThat(judge(fix(1.4, 0))).isEqualTo(Verdict.NOT_LATER)
        assertThat(judge(fix(1.4, -3))).isEqualTo(Verdict.NOT_LATER)
    }

    @Test
    fun `walking, no faster than three and a half metres a second`() {
        assertThat(judge(fix(3.4, 1))).isEqualTo(Verdict.ACCEPTED)
        assertThat(judge(fix(3.6, 1))).isEqualTo(Verdict.TOO_FAST)
    }

    @Test
    fun `running, up to eight`() {
        val running = MetricsConstants.RUNNING_CADENCE

        assertThat(judge(fix(7.9, 1), cadence = running)).isEqualTo(Verdict.ACCEPTED)
        assertThat(judge(fix(8.2, 1), cadence = running)).isEqualTo(Verdict.TOO_FAST)
        // Just below the running cadence the walking speed still holds.
        assertThat(judge(fix(7.9, 1), cadence = running - 1)).isEqualTo(Verdict.TOO_FAST)
    }

    @Test
    fun `before there is a cadence, the stricter speed holds`() {
        assertThat(judge(fix(5.0, 1), cadence = null)).isEqualTo(Verdict.TOO_FAST)
    }

    @Test
    fun `a fix 60 m off between two good ones is refused, and the line goes from good to good`() {
        val jump = fix(60.0, 1)
        val good = fix(2.8, 2)

        assertThat(judge(jump)).isEqualTo(Verdict.TOO_FAST)
        // The next one is judged against the last accepted, not against the jump.
        assertThat(judge(good, previous = start)).isEqualTo(Verdict.ACCEPTED)
    }

    @Test
    fun `a mock fix in a release build is refused`() {
        assertThat(judge(fix(1.4, 1, mock = true), allowMock = false)).isEqualTo(Verdict.MOCK)
    }

    @Test
    fun `a debug build takes a mock fix, held to the same rules`() {
        assertThat(judge(fix(1.4, 1, mock = true), allowMock = true)).isEqualTo(Verdict.ACCEPTED)
        assertThat(judge(fix(60.0, 1, mock = true), allowMock = true)).isEqualTo(Verdict.TOO_FAST)
    }

    @Test
    fun `the haversine distance reads a degree of latitude as about 111 km`() {
        assertThat(Geodesy.meters(45.0, 9.0, 46.0, 9.0)).isWithin(1.0).of(METERS_PER_DEGREE)
        // A degree of longitude is shorter away from the equator: by the cosine of the latitude.
        assertThat(Geodesy.meters(60.0, 9.0, 60.0, 10.0)).isWithin(100.0).of(METERS_PER_DEGREE / 2)
        assertThat(Geodesy.meters(45.0, 9.0, 45.0, 9.0)).isEqualTo(0.0)
    }

    private companion object {
        /** A degree of latitude on the mean sphere the haversine formula uses. */
        const val METERS_PER_DEGREE = 111_195.08
    }
}
