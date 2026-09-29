package com.callbackdev.passo.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A chart's gutter (the numbers right of the plot) and axis (the labels under it): 44 and 20dp at
 * the usual text size, and as wide and as tall as the reader's text needs above it (Phase 7's
 * large-text pass): at twice the size, 44dp no longer holds "8,000" and 20dp no longer holds an
 * hour, which then ran into the caption under the chart.
 *
 * @param widest the widest number the gutter shows.
 */
@Composable
internal fun chartMargins(measurer: TextMeasurer, style: TextStyle, widest: String): Pair<Dp, Dp> {
    val density = LocalDensity.current
    return remember(measurer, style, widest, density) {
        // At the standard text size, or a smaller one, the margins the charts were drawn with.
        if (density.fontScale <= 1f) return@remember GUTTER to AXIS
        val size = measurer.measure(widest, style).size
        with(density) {
            // The label stands 6dp right of the plot, as it always has.
            maxOf(GUTTER, size.width.toDp() + 6.dp) to maxOf(AXIS, size.height.toDp() + 4.dp)
        }
    }
}

private val GUTTER = 44.dp
private val AXIS = 20.dp

/** The least room between two labels under an axis; a label with less is left out. */
internal const val LABEL_GAP_DP = 4
