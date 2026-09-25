package com.callbackdev.passo.feature.settings.data

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.components.ValueRow
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.shortDateWithYear
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.backup.CsvTable
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.model.UnitSystem
import com.callbackdev.passo.feature.settings.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What the data rows can ask for. */
class DataActions(
    val writeBackup: (Uri) -> Unit = {},
    val writeTable: (CsvTable, Uri) -> Unit = { _, _ -> },
    val readBackup: (Uri) -> Unit = {},
    val confirmImport: (Boolean) -> Unit = {},
    val cancelImport: () -> Unit = {},
    val dismissOutcome: () -> Unit = {},
    val backupFileName: () -> String = { "passo-backup.json" },
    val tableFileName: (CsvTable) -> String = { "passo-${it.name.lowercase()}.csv" },
)

/**
 * The file pickers and the choice of table, held by the screen rather than by the rows: the rows
 * live in a lazy list and leave the composition when scrolled away, and a picker's answer or a
 * dialog must not leave with them.
 */
@Stable
internal class DataFiles(
    val saveBackup: () -> Unit,
    val openBackup: () -> Unit,
    val chooseTable: () -> Unit,
    internal val tableDialog: MutableState<Boolean>,
    internal val table: MutableState<CsvTable>,
    internal val saveTable: () -> Unit,
)

@Composable
internal fun rememberDataFiles(actions: DataActions): DataFiles {
    val tableDialog = rememberSaveable { mutableStateOf(false) }
    val table = rememberSaveable { mutableStateOf(CsvTable.DAYS) }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        if (uri != null) actions.writeBackup(uri)
    }
    val createTable = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV)) { uri ->
        if (uri != null) actions.writeTable(table.value, uri)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.readBackup(uri)
    }
    return remember(actions) {
        DataFiles(
            saveBackup = { runCatching { createBackup.launch(actions.backupFileName()) } },
            // Some file apps call a .json file text, or nothing at all: all three are offered.
            openBackup = { runCatching { openBackup.launch(arrayOf(JSON, "text/plain", "application/octet-stream")) } },
            chooseTable = { tableDialog.value = true },
            tableDialog = tableDialog,
            table = table,
            saveTable = { runCatching { createTable.launch(actions.tableFileName(table.value)) } },
        )
    }
}

/**
 * Your data (PLANNING.md §11 Phase 7, ADR 0011): the backup, the import and the spreadsheet
 * tables, each a row that says what it carries before it is tapped. The system's file picker
 * chooses where a file goes or comes from, so no storage permission is asked and Passo sends
 * nothing anywhere. What an export or import came to is a card over the rows, which stays until
 * it is put away; an import is shown, with what it will do, before anything is written.
 */
@Composable
internal fun DataGroup(state: DataUiState, format: MeasureFormatter, files: DataFiles, onDismissOutcome: () -> Unit) {
    val idle = state.task == null
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.outcome?.let { OutcomeCard(it, format, onDismissOutcome) }
        SettingsGroup {
            ValueRow(
                label = stringResource(R.string.data_backup),
                value = when (state.task) {
                    DataTask.WRITING_BACKUP -> stringResource(R.string.data_busy_writing)
                    else -> backupNote(state, format)
                },
                icon = PassoIcons.Export,
                enabled = idle,
                onClick = files.saveBackup,
                modifier = Modifier.testTag(DataTags.BACKUP),
            )
            GroupDivider()
            ValueRow(
                label = stringResource(R.string.data_import),
                value = when (state.task) {
                    DataTask.READING -> stringResource(R.string.data_busy_reading)
                    DataTask.IMPORTING -> stringResource(R.string.data_busy_importing)
                    else -> stringResource(R.string.data_import_note)
                },
                icon = PassoIcons.Import,
                enabled = idle,
                onClick = files.openBackup,
                modifier = Modifier.testTag(DataTags.IMPORT),
            )
            GroupDivider()
            ValueRow(
                label = stringResource(R.string.data_table),
                value = if (state.task == DataTask.WRITING_TABLE) {
                    stringResource(R.string.data_busy_writing)
                } else {
                    stringResource(R.string.data_table_note)
                },
                icon = PassoIcons.Table,
                enabled = idle,
                onClick = files.chooseTable,
                modifier = Modifier.testTag(DataTags.TABLE),
            )
        }
    }
}

/** The data rows' dialogs, drawn by the screen whatever the list is showing. */
@Composable
internal fun DataDialogs(state: DataUiState, format: MeasureFormatter, files: DataFiles, actions: DataActions) {
    if (files.tableDialog.value) {
        TableDialog(
            selected = files.table.value,
            units = format.units,
            onSelect = { files.table.value = it },
            onExport = {
                files.tableDialog.value = false
                files.saveTable()
            },
            onDismiss = { files.tableDialog.value = false },
        )
    }
    state.preview?.let { preview ->
        ImportDialog(preview, format, onImport = actions.confirmImport, onDismiss = actions.cancelImport)
    }
}

@Composable
private fun backupNote(state: DataUiState, format: MeasureFormatter): String {
    val contents = state.contents
    if (contents.days == 0 && contents.outings == 0) return stringResource(R.string.data_backup_note_empty)
    return stringResource(
        R.string.data_backup_note,
        pluralStringResource(R.plurals.data_days, contents.days, format.steps(contents.days)),
        pluralStringResource(R.plurals.data_outings, contents.outings, format.steps(contents.outings)),
    )
}

/** Which table, each with what its rows are; the file is named for it. */
@Composable
private fun TableDialog(
    selected: CsvTable,
    units: UnitSystem,
    onSelect: (CsvTable) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        Triple(CsvTable.DAYS, R.string.data_table_days, R.string.data_table_days_note),
        Triple(CsvTable.MINUTES, R.string.data_table_minutes, R.string.data_table_minutes_note),
        Triple(CsvTable.OUTINGS, R.string.data_table_outings, R.string.data_table_outings_note),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.data_table_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                Text(
                    stringResource(
                        R.string.data_table_explanation,
                        stringResource(
                            if (units ==
                                UnitSystem.IMPERIAL
                            ) {
                                R.string.data_units_mi
                            } else {
                                R.string.data_units_km
                            },
                        ),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                options.forEach { (option, label, note) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, onClick = {
                                onSelect(option)
                            }, role = Role.RadioButton)
                            .padding(vertical = 8.dp)
                            .testTag("${DataTags.TABLE_OPTION}-${option.name}"),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Column(
                            modifier = Modifier.padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(note),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onExport, modifier = Modifier.testTag(DataTags.TABLE_EXPORT)) {
                Text(stringResource(R.string.data_table_export))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

/**
 * What the file holds and what importing it does, before anything is written: when and by
 * which Passo it was made, its days and outings, the merge in one sentence, and whether the
 * profile and settings come too.
 */
@Composable
private fun ImportDialog(
    preview: ImportPreview,
    format: MeasureFormatter,
    onImport: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val backup = preview.backup
    var withPreferences by rememberSaveable { mutableStateOf(true) }
    val written = Instant.ofEpochMilli(backup.exportedAtMillis).atZone(ZoneId.systemDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.data_preview_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // At 200% type, on a small phone, the dialog scrolls rather than hiding its button.
                modifier = Modifier.verticalScroll(rememberScrollState()).testTag(DataTags.PREVIEW),
            ) {
                val date = shortDateWithYear(written.toLocalDate())
                val time = clockTime(written.hour * 60 + written.minute)
                Text(
                    if (backup.appVersion.isNotEmpty()) {
                        stringResource(R.string.data_preview_written, date, time, backup.appVersion)
                    } else {
                        stringResource(R.string.data_preview_written_unversioned, date, time)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val first = backup.firstDay
                    val last = backup.lastDay
                    if (first == null || last == null) {
                        Text(stringResource(R.string.data_preview_no_days), style = MaterialTheme.typography.titleSmall)
                    } else {
                        Text(
                            pluralStringResource(
                                R.plurals.data_preview_days,
                                backup.days.size,
                                format.steps(backup.days.size),
                            ),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(
                                R.string.data_preview_range,
                                shortDateWithYear(LocalDate.ofEpochDay(first)),
                                shortDateWithYear(LocalDate.ofEpochDay(last)),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        if (backup.sessions.isEmpty()) {
                            stringResource(R.string.data_preview_no_outings)
                        } else {
                            pluralStringResource(
                                R.plurals.data_outings,
                                backup.sessions.size,
                                format.steps(backup.sessions.size),
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(stringResource(R.string.data_preview_merge), style = MaterialTheme.typography.bodyMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = withPreferences, onValueChange = {
                            withPreferences = it
                        }, role = Role.Checkbox)
                        .testTag(DataTags.PREFERENCES),
                ) {
                    Checkbox(checked = withPreferences, onCheckedChange = null)
                    Column(
                        modifier = Modifier.padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            stringResource(R.string.data_preview_preferences),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.data_preview_preferences_note),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(withPreferences) }, modifier = Modifier.testTag(DataTags.CONFIRM_IMPORT)) {
                Text(stringResource(R.string.data_preview_import))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

/** What the last export or import came to, until it is put away. */
@Composable
private fun OutcomeCard(outcome: DataOutcome, format: MeasureFormatter, onDismiss: () -> Unit) {
    val (title, body) = outcomeText(outcome, format)
    StatusCard(
        icon = if (outcome is DataOutcome.Failed) PassoIcons.Warning else PassoIcons.Check,
        title = title,
        body = body,
        tone = if (outcome is DataOutcome.Failed) StatusTone.PROBLEM else StatusTone.NOTE,
        dismiss = stringResource(R.string.data_ok),
        onDismiss = onDismiss,
        modifier = Modifier.padding(horizontal = ScreenMargin).testTag(DataTags.OUTCOME),
    )
}

@Composable
private fun outcomeText(outcome: DataOutcome, format: MeasureFormatter): Pair<String, String> {
    val res = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    return when (outcome) {
        is DataOutcome.BackupWritten -> {
            val days = pluralStringResource(R.plurals.data_days, outcome.days, format.steps(outcome.days))
            val outings = pluralStringResource(R.plurals.data_outings, outcome.outings, format.steps(outcome.outings))
            stringResource(R.string.data_saved_title) to if (outcome.file != null) {
                stringResource(R.string.data_saved_body, days, outings, outcome.file)
            } else {
                stringResource(R.string.data_saved_body_unnamed, days, outings)
            }
        }

        is DataOutcome.TableWritten -> stringResource(R.string.data_table_saved_title) to if (outcome.file != null) {
            pluralStringResource(
                R.plurals.data_table_saved_body,
                outcome.rows,
                format.steps(outcome.rows),
                outcome.file,
            )
        } else {
            pluralStringResource(R.plurals.data_table_saved_body_unnamed, outcome.rows, format.steps(outcome.rows))
        }

        is DataOutcome.Imported -> {
            // A list of what happened, as one sentence: only the parts that did.
            val report = outcome.report
            val parts = buildList {
                if (report.addedDays > 0) {
                    add(
                        res.getQuantityString(
                            R.plurals.data_imported_added,
                            report.addedDays,
                            format.steps(report.addedDays),
                        ),
                    )
                }
                if (report.mergedDays > 0) {
                    add(
                        res.getQuantityString(
                            R.plurals.data_imported_merged,
                            report.mergedDays,
                            format.steps(report.mergedDays),
                        ),
                    )
                }
                if (report.unchangedDays > 0) {
                    add(
                        res.getQuantityString(
                            R.plurals.data_imported_same,
                            report.unchangedDays,
                            format.steps(report.unchangedDays),
                        ),
                    )
                }
                if (report.addedOutings > 0) {
                    add(
                        res.getQuantityString(
                            R.plurals.data_imported_outings,
                            report.addedOutings,
                            format.steps(report.addedOutings),
                        ),
                    )
                }
                if (report.preferences) add(res.getString(R.string.data_imported_preferences))
            }
            val nothingNew = report.addedDays == 0 && report.mergedDays == 0 && report.addedOutings == 0 &&
                !report.preferences
            stringResource(R.string.data_imported_title) to if (nothingNew) {
                stringResource(R.string.data_imported_nothing)
            } else {
                parts.joinToString(", ").replaceFirstChar { it.titlecase(locale) } + "."
            }
        }

        is DataOutcome.Failed -> when (outcome.reason) {
            DataFailure.WRITE ->
                stringResource(R.string.data_failed_write_title) to stringResource(R.string.data_failed_write_body)

            DataFailure.READ ->
                stringResource(R.string.data_failed_read_title) to stringResource(R.string.data_failed_read_body)

            DataFailure.NOT_PASSO ->
                stringResource(R.string.data_failed_not_passo_title) to
                    stringResource(R.string.data_failed_not_passo_body)

            DataFailure.TOO_NEW ->
                stringResource(R.string.data_failed_too_new_title) to stringResource(R.string.data_failed_too_new_body)

            DataFailure.DAMAGED ->
                stringResource(R.string.data_failed_damaged_title) to stringResource(R.string.data_failed_damaged_body)
        }
    }
}

private const val JSON = "application/json"
private const val CSV = "text/csv"

/** Hooks for the UI tests. */
object DataTags {
    const val BACKUP = "data_backup"
    const val IMPORT = "data_import"
    const val TABLE = "data_table"
    const val TABLE_OPTION = "data_table_option"
    const val TABLE_EXPORT = "data_table_export"
    const val OUTCOME = "data_outcome"
    const val PREVIEW = "data_preview"
    const val PREFERENCES = "data_preferences"
    const val CONFIRM_IMPORT = "data_confirm_import"
}
