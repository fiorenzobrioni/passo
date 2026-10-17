package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer

/**
 * A [Text] that may hold a hyphenated place name (Saint-Jean-Pied-de-Port, Pont-Saint-Martin).
 * Android does not break a line at a hyphen here: a name wider than the line is cut inside one
 * of its words («Saint-Jean-Pied-de-Po / rt»). Only then may it break after a hyphen; a name
 * that fits keeps its hyphens whole, so the text breaks exactly as a plain [Text] would.
 */
@Composable
fun PlaceText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        val shown = remember(text, style, width) {
            val tooWide = '-' in text &&
                text.split(' ', ' ').any { word ->
                    '-' in word && measurer.measure(word, style).size.width > width
                }
            if (tooWide) text.replace("-", "-​") else text
        }
        Text(shown, color = color, style = style)
    }
}
