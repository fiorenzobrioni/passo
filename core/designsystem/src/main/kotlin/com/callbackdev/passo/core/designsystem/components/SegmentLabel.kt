package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * A segmented button's label: one line at the reader's text size, and only as much smaller as
 * the segment needs when that size does not fit ([FittedLabel]). At twice the size, four segments
 * across a small phone read «We…» and «Mo…», and a long word in a narrow one («Settimana», four
 * across 320dp) is cut the same way; a word a little smaller is still a word. TalkBack reads it
 * whole either way.
 */
@Composable
fun SegmentLabel(text: String) = FittedLabel(text)

/**
 * A label kept on one line: at the reader's text size where it fits, and only as much smaller as
 * its room needs where it does not (Phase 7's large-text pass). At a large text size never below
 * the size it has at the standard setting, and at the standard one a step smaller at most.
 */
@Composable
fun FittedLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val size = style.fontSize
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = when {
            !size.isSpecified -> null

            scale > 1f -> TextAutoSize.StepBased(minFontSize = (size.value / scale).sp, maxFontSize = size)

            // The sizes tried are counted up from the smallest: a whole number of steps below
            // the label's own size, or a label that fits would be set a hair smaller than it is.
            else -> TextAutoSize.StepBased(
                minFontSize = (size.value - NARROW_STEPS * NARROW_STEP).sp,
                maxFontSize = size,
                stepSize = NARROW_STEP.sp,
            )
        },
    )
}

/** At the standard text size a label gives up 2sp at most, half a point at a time: 12sp from 14sp. */
private const val NARROW_STEP = 0.5f
private const val NARROW_STEPS = 4
