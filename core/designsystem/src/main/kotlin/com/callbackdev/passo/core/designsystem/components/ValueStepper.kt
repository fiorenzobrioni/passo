package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoTheme

/**
 * A number with a range, picked, never typed (Chiaro's rule for a value with a range): the value
 * large, a step down and a step up on either side for precision, and a slider under it for
 * distance. The caller owns the unit, the step and the rounding; [text] is the value as shown.
 */
@Composable
fun ValueStepper(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    text: String,
    onValueChange: (Float) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseLabel: String,
    increaseLabel: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilledTonalIconButton(onClick = onDecrease, enabled = value > range.start, colors = buttonColors()) {
                Icon(PassoIcons.Minus, contentDescription = decreaseLabel)
            }
            Text(
                text = text,
                style = PassoTheme.type.heroNumber.copy(fontSize = 40.sp, lineHeight = 48.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(onClick = onIncrease, enabled = value < range.endInclusive, colors = buttonColors()) {
                Icon(PassoIcons.Plus, contentDescription = increaseLabel)
            }
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = onValueChange,
            valueRange = range,
            // The accent for the value, a neutral for the rest: Material's default paints the rest
            // in the secondary container, which in the vivid dress is amber beside a blue thumb.
            colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            modifier = Modifier.semantics {
                stateDescription = text
                if (caption != null) contentDescription = caption
            },
        )
    }
}

@Composable
private fun buttonColors() = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)
