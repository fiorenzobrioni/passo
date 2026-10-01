package com.callbackdev.passo.feature.ways

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.WayStartChoice
import com.callbackdev.passo.core.domain.ways.WayStarts
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Starting a way (PLANNING.md §11 Phase 11): from which day. Each choice says where it would
 * place the reader, from the days already walked: "you would already be past Siena" is the
 * moment the feature is made for. Only the choices that mean different days are offered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartDialog(
    state: WaysUiState,
    way: Way,
    onStart: (WayStartChoice, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = state.today
    val first = state.firstCounted
    val choices = buildList {
        val seen = HashSet<LocalDate>()
        for (choice in listOf(WayStartChoice.TODAY, WayStartChoice.THIS_YEAR, WayStartChoice.FIRST_DAY)) {
            if (seen.add(WayStarts.dayOf(choice, today, first, null))) add(choice)
        }
        if (first != null && first.isBefore(today)) add(WayStartChoice.CHOSEN)
    }
    var selected by rememberSaveable { mutableStateOf(WayStartChoice.TODAY) }
    var chosenDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val chosen = chosenDay?.let(LocalDate::ofEpochDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(WaysTags.START_DIALOG),
        title = { Text(stringResource(R.string.way_start_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.way_start_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.selectableGroup()) {
                    choices.forEach { choice ->
                        val day = WayStarts.dayOf(choice, today, first, chosen)
                        val label = when (choice) {
                            WayStartChoice.TODAY -> stringResource(R.string.way_start_today)

                            WayStartChoice.THIS_YEAR -> stringResource(R.string.way_start_year)

                            WayStartChoice.FIRST_DAY -> stringResource(R.string.way_start_first)

                            WayStartChoice.CHOSEN -> chosen?.let {
                                stringResource(R.string.way_start_chosen_day, dateText(it, today))
                            } ?: stringResource(R.string.way_start_chosen)
                        }
                        ChoiceRow(
                            label = label,
                            preview = if (choice == WayStartChoice.CHOSEN && chosen == null) {
                                null
                            } else {
                                preview(state, way, day)
                            },
                            selected = selected == choice,
                            onSelect = {
                                selected = choice
                                if (choice == WayStartChoice.CHOSEN) picking = true
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onStart(selected, chosen) },
                enabled = selected != WayStartChoice.CHOSEN || chosen != null,
            ) { Text(stringResource(R.string.way_start_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.way_cancel)) } },
    )

    if (picking && first != null) {
        val bounds = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcDay(utcTimeMillis).let { !it.isBefore(first) && !it.isAfter(today) }

            override fun isSelectableYear(year: Int): Boolean = year in first.year..today.year
        }
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = (chosen ?: first).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = bounds,
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { chosenDay = utcDay(it).toEpochDay() }
                    picking = false
                }) { Text(stringResource(R.string.way_start_pick)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.way_cancel)) } },
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun ChoiceRow(label: String, preview: String?, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (preview != null) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Where a start on [day] would place the reader, from the days already walked. */
@Composable
private fun preview(state: WaysUiState, way: Way, day: LocalDate): String {
    val pending = WayJourney(0, way.id, day.toEpochDay(), 0, WayJourneyState.ACTIVE, null, 0)
    val progress = WayProgress.of(way, pending, state.distances, state.today.toEpochDay())
    val format = rememberMeasureFormatter(state.units)
    val last = stringResource(placeNameRes(progress.last.stop.key))
    return when {
        progress.finished -> stringResource(R.string.way_start_arrived, last)
        progress.reached.size == 1 -> stringResource(R.string.way_start_at_start, last)
        else -> stringResource(R.string.way_start_past, last, format.distance(progress.walkedMeters).text())
    }
}

/** The date picker speaks UTC midnights. */
private fun utcDay(utcMillis: Long): LocalDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
