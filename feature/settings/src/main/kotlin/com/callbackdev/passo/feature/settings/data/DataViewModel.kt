package com.callbackdev.passo.feature.settings.data

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.backup.BackupRepository
import com.callbackdev.passo.core.data.backup.DataContents
import com.callbackdev.passo.core.data.backup.ImportReport
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.widget.WidgetUpdates
import com.callbackdev.passo.core.domain.backup.Backup
import com.callbackdev.passo.core.domain.backup.BackupCodec
import com.callbackdev.passo.core.domain.backup.BackupRead
import com.callbackdev.passo.core.domain.backup.CsvExport
import com.callbackdev.passo.core.domain.backup.CsvTable
import com.callbackdev.passo.core.domain.settings.resolve
import com.callbackdev.passo.core.domain.widget.WidgetEvent
import com.callbackdev.passo.core.tracking.TrackerLink
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** What the data rows are doing right now; each disables the rows until it is done. */
enum class DataTask {
    WRITING_BACKUP,
    WRITING_TABLE,
    READING,
    IMPORTING,
}

/** Why a file could not be written or taken. */
enum class DataFailure {
    /** The file could not be written: the place refused it, or ran out of room. */
    WRITE,

    /** The file could not be opened or read. */
    READ,

    /** Not a file Passo wrote. */
    NOT_PASSO,

    /** A backup from a newer Passo: this one needs an update to read it. */
    TOO_NEW,

    /** A Passo backup that cannot be read to its end. */
    DAMAGED,
}

/** What the last export or import came to, said on a card until the reader puts it away. */
sealed interface DataOutcome {
    data class BackupWritten(val file: String?, val days: Int, val outings: Int) : DataOutcome

    data class TableWritten(val table: CsvTable, val file: String?, val rows: Int) : DataOutcome

    data class Imported(val report: ImportReport) : DataOutcome

    data class Failed(val reason: DataFailure) : DataOutcome
}

/** A backup read and checked, waiting for the reader to say yes. */
data class ImportPreview(val backup: Backup, val file: String?)

data class DataUiState(
    val contents: DataContents = DataContents(0, 0),
    val task: DataTask? = null,
    val outcome: DataOutcome? = null,
    val preview: ImportPreview? = null,
)

/**
 * The data rows of Settings (PLANNING.md §11 Phase 7, ADR 0011): a backup to a file, a table
 * for a spreadsheet, a backup taken back. Files come from the Storage Access Framework, so
 * Passo needs no storage permission and sends nothing anywhere: the reader picks where the file
 * goes, and what to open. An import is read and checked first, and shown, before anything is
 * written.
 */
@HiltViewModel
class DataViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val backups: BackupRepository,
    private val settings: SettingsRepository,
    private val tracker: TrackerLink,
    private val widgets: WidgetUpdates,
) : ViewModel() {
    private val task = MutableStateFlow<DataTask?>(null)
    private val outcome = MutableStateFlow<DataOutcome?>(null)
    private val preview = MutableStateFlow<ImportPreview?>(null)

    val state: StateFlow<DataUiState> = combine(backups.contents, task, outcome, preview, ::DataUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DataUiState())

    /** The names the system's file picker offers, dated today. */
    fun backupFileName(): String = BackupCodec.fileName(LocalDate.now())

    fun tableFileName(table: CsvTable): String = CsvExport.fileName(table, LocalDate.now())

    /** Writes the whole backup to [uri], a file the reader just created. */
    fun writeBackup(uri: Uri) = run(DataTask.WRITING_BACKUP) {
        // With the screen off the sensor hub holds up to ten minutes of steps: take them first,
        // so the file has the walk just finished.
        tracker.catchUp()
        val backup = backups.backup(appVersion = appVersion(), nowMillis = System.currentTimeMillis(), zone = zone())
        val written = write(uri, BackupCodec.encode(backup))
        if (written) {
            DataOutcome.BackupWritten(displayName(uri), backup.days.size, backup.sessions.size)
        } else {
            DataOutcome.Failed(DataFailure.WRITE)
        }
    }

    /** Writes one spreadsheet table to [uri]. */
    fun writeTable(table: CsvTable, uri: Uri) = run(DataTask.WRITING_TABLE) {
        tracker.catchUp()
        val units = settings.settings.first().units.resolve(Resources.getSystem().configuration.locales[0]?.country)
        val csv = backups.csv(table, units, zone())
        if (write(uri, csv)) {
            DataOutcome.TableWritten(table, displayName(uri), rows = csv.count { it == '\n' } - 1)
        } else {
            DataOutcome.Failed(DataFailure.WRITE)
        }
    }

    /** Reads [uri] and, if it is a backup this version can take, shows what it holds. */
    fun readBackup(uri: Uri) = run(DataTask.READING) {
        val text = read(uri) ?: return@run DataOutcome.Failed(DataFailure.READ)
        when (val read = BackupCodec.decode(text)) {
            is BackupRead.Ok -> {
                preview.value = ImportPreview(read.backup, displayName(uri))
                null
            }

            BackupRead.NotPasso -> DataOutcome.Failed(DataFailure.NOT_PASSO)

            is BackupRead.TooNew -> DataOutcome.Failed(DataFailure.TOO_NEW)

            BackupRead.Damaged -> DataOutcome.Failed(DataFailure.DAMAGED)
        }
    }

    /** The reader said yes: the backup comes in, with its profile and settings if asked. */
    fun confirmImport(withPreferences: Boolean) {
        val pending = preview.value ?: return
        preview.value = null
        run(DataTask.IMPORTING) {
            // What the service still holds is written first, so its minutes meet the file's in
            // the database rather than being added on top of them afterwards.
            tracker.catchUp()
            val report = backups.import(pending.backup, withPreferences)
            widgets.notify(WidgetEvent.SETTINGS)
            DataOutcome.Imported(report)
        }
    }

    fun cancelImport() {
        preview.value = null
    }

    fun dismissOutcome() {
        outcome.value = null
    }

    /** One task at a time; its outcome replaces the last one. */
    private fun run(kind: DataTask, work: suspend () -> DataOutcome?) {
        if (task.value != null) return
        task.value = kind
        outcome.value = null
        // Off the main thread: a year of minutes is a few megabytes to encode, merge or tabulate.
        // And to its end, even if Settings is left meanwhile: an import stopped between the
        // settings and the days, or a file half written, would be worse than a finished one.
        viewModelScope.launch(Dispatchers.Default) {
            try {
                outcome.value = withContext(NonCancellable) { work() }
            } finally {
                task.value = null
            }
        }
    }

    private suspend fun write(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // "wt": a file picked again is replaced, not written over its old end.
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } !=
                null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                // Far past any real history (a year is a few megabytes): a bigger file is not a
                // backup, and is not read whole into memory to find that out.
                val bytes = input.readNBytes(MAX_BACKUP_BYTES + 1)
                if (bytes.size > MAX_BACKUP_BYTES) "" else bytes.toString(Charsets.UTF_8)
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    /** The file's name as the reader's file app shows it, for the card that says where it went. */
    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    private fun zone(): ZoneId = ZoneId.systemDefault()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_BACKUP_BYTES = 64 * 1024 * 1024
    }
}
