package com.callbackdev.passo.core.domain.ways

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PolylineTest {
    @Test
    fun `decodes Google's reference polyline`() {
        // From Google's documentation of the format: (38.5, -120.2), (40.7, -120.95), (43.252, -126.453).
        val path = Polyline.decodePath("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertThat(path.latitudes.toList()).containsExactly(38.5, 40.7, 43.252).inOrder()
        assertThat(path.longitudes.toList()).containsExactly(-120.2, -120.95, -126.453).inOrder()
    }

    @Test
    fun `decodes three values a point, each with its scale`() {
        // tools/build_ways.py's encode([(1.0, 2.0, 0), (1.5, 2.5, 120)], (1e5, 1e5, 1)).
        val (latitudes, longitudes, meters) = Polyline.decode("_ibE_seK?_t`B_t`BoF", 3, Polyline.LINE)
        assertThat(latitudes.toList()).containsExactly(1.0, 1.5).inOrder()
        assertThat(longitudes.toList()).containsExactly(2.0, 2.5).inOrder()
        assertThat(meters.toList()).containsExactly(0.0, 120.0).inOrder()
    }

    @Test(expected = IllegalStateException::class)
    fun `a polyline cut in the middle of a point is refused`() {
        Polyline.decode("_ibE_seK", 3, Polyline.LINE)
    }
}
