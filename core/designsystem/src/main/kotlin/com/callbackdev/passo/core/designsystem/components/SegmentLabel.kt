package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * A segmented button's label: one line at the reader's text size, and only as much smaller as
 * the segment needs when that size does not fit (Phase 7's large-text pass), never below the
 * size it has at the standard text setting. At twice the size, four segments across a small
 * phone read «We…» and «Mo…»; a word a little smaller is still a word. TalkBack reads it whole
 * either way.
 */
@Composable
fun SegmentLabel(text: String) {
    val size = LocalTextStyle.current.fontSize
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = if (size.isSpecified && scale > 1f) {
            TextAutoSize.StepBased(minFontSize = (size.value / scale).sp, maxFontSize = size)
        } else {
            null
        },
    )
}
