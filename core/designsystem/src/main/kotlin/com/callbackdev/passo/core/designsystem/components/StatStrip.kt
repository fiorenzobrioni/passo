package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme

/**
 * One amount of a [StatStrip]: its value, the word under it, what TalkBack says for the two
 * ([spoken]; null where the strip is read as one) and its share of the row against the others.
 */
class StripStat(val value: AnnotatedString, val label: String, val spoken: String? = null, val weight: Float = 1f)

/**
 * Amounts side by side between two hairlines, a hairline between every two: each a value with
 * the word under it, centred on its share of the row. The one way the app sets a few amounts in
 * a row, on Today under the day's sentence and on an outing's card, so the two read as one thing
 * wherever they meet (owner, 11 Nov 2026: centred everywhere, as on Today).
 *
 * Where a share is narrower than [minShare] at the reader's text size, a number would break in
 * the middle: the amounts go one above the other, or [narrow] is shown in their place.
 *
 * @param valueStyle the values' style: the reading's on a page, a step smaller inside a card.
 */
@Composable
fun StatStrip(
    stats: List<StripStat>,
    minShare: Dp,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = PassoTheme.type.readingValue,
    narrow: (@Composable () -> Unit)? = null,
) {
    val rule = MaterialTheme.colorScheme.outlineVariant
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val across = maxWidth / stats.size / LocalDensity.current.fontScale >= minShare
        if (!across && narrow != null) {
            narrow()
        } else {
            Column {
                HorizontalDivider(color = rule)
                if (across) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        stats.forEachIndexed { i, stat ->
                            if (i > 0) VerticalDivider(color = rule)
                            Stat(stat, valueStyle, Modifier.weight(stat.weight).padding(horizontal = 4.dp))
                        }
                    }
                } else {
                    stats.forEachIndexed { i, stat ->
                        if (i > 0) HorizontalDivider(color = rule)
                        Stat(stat, valueStyle, Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                    }
                }
                HorizontalDivider(color = rule)
            }
        }
    }
}

@Composable
private fun Stat(stat: StripStat, valueStyle: TextStyle, modifier: Modifier) {
    // Centred on its share: amounts read one at a time gain nothing from a left edge, which
    // helps a column of numbers compared down.
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(vertical = 10.dp)
            .then(
                if (stat.spoken != null) {
                    Modifier.clearAndSetSemantics { contentDescription = stat.spoken }
                } else {
                    Modifier
                },
            ),
    ) {
        Text(stat.value, style = valueStyle, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(
            stat.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
