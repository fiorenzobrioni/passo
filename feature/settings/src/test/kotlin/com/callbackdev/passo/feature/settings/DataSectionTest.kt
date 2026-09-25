package com.callbackdev.passo.feature.settings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.backup.DataContents
import com.callbackdev.passo.core.data.backup.ImportReport
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.backup.Backup
import com.callbackdev.passo.core.domain.backup.BackupDay
import com.callbackdev.passo.core.domain.backup.CsvTable
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.feature.settings.data.DataActions
import com.callbackdev.passo.feature.settings.data.DataFailure
import com.callbackdev.passo.feature.settings.data.DataOutcome
import com.callbackdev.passo.feature.settings.data.DataTags
import com.callbackdev.passo.feature.settings.data.DataTask
import com.callbackdev.passo.feature.settings.data.DataUiState
import com.callbackdev.passo.feature.settings.data.ImportPreview
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** The data rows of Settings (PLANNING.md §11 Phase 7): what they say, and what they ask first. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class DataSectionTest {
    @get:Rule val compose = createComposeRule()

    private val state = SettingsUiState(
        settings = UserSettings(units = UnitPreference.METRIC),
        profile = Profile(heightMeters = 1.76),
        version = "0.9.0",
    )
    private val contents = DataContents(days = 412, outings = 38)

    private fun show(data: DataUiState, actions: DataActions = DataActions()) {
        compose.setContent {
            PassoTheme {
                SettingsScreen(state, onBack = {}, actions = SettingsActions(), data = data, dataActions = actions)
            }
        }
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(DataTags.TABLE))
    }

    @Test
    fun `each row says what it carries before it is tapped`() {
        show(DataUiState(contents = contents))

        compose.onNodeWithText("Back up to a file").assertIsDisplayed()
        compose.onNodeWithText(
            "412 days and 38 outings, to the minute, with your profile and settings. Import it here or on another phone.",
        ).assertIsDisplayed()
        compose.onNodeWithText("From a file Passo wrote. Nothing on this phone is deleted.").assertIsDisplayed()
        compose.onNodeWithText("CSV: one row per day, per minute or per outing.").assertIsDisplayed()
        snapshot("settings_data")
    }

    @Test
    fun `while a file is written the rows say so and wait`() {
        show(DataUiState(contents = contents, task = DataTask.WRITING_BACKUP))

        compose.onNodeWithText("Writing the file…").assertIsDisplayed()
        compose.onNodeWithTag(DataTags.IMPORT).assertIsNotEnabled()
    }

    @Test
    fun `the spreadsheet export asks which table, each with what its rows are`() {
        show(DataUiState(contents = contents))

        compose.onNodeWithTag(DataTags.TABLE).performClick()
        compose.onNodeWithText("One row per minute with steps: the finest the history goes.").assertIsDisplayed()
        compose.onNodeWithText("distances in kilometres", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("${DataTags.TABLE_OPTION}-${CsvTable.OUTINGS.name}").performClick()
        snapshot("settings_data_table")
    }

    @Test
    fun `an import is shown before anything is written, and the settings can stay`() {
        var imported: Boolean? = null
        val first = LocalDate.of(2025, 3, 3).toEpochDay()
        val last = LocalDate.of(2026, 9, 25).toEpochDay()
        val backup = Backup(
            exportedAtMillis = LocalDate.of(2026, 9, 25).atTime(18, 30).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            zone = "Europe/Rome",
            appVersion = "0.9.0",
            profile = Profile(),
            settings = UserSettings(),
            days = listOf(first, last).map {
                BackupDay(DailySummary(it, 9_000, 0.0, 0.0, 0, 0, 8_000, true), emptyList())
            },
            plans = emptyList(),
            sessions = emptyList(),
        )
        show(
            DataUiState(contents = contents, preview = ImportPreview(backup, "passo-backup-2026-09-25.json")),
            DataActions(confirmImport = { imported = it }),
        )

        compose.onNodeWithText("Import this backup?").assertIsDisplayed()
        compose.onNodeWithText("Written on Sep 25, 2026 at 6:30 PM, by Passo 0.9.0.").assertIsDisplayed()
        compose.onNodeWithText("2 days of steps").assertIsDisplayed()
        compose.onNodeWithText("From Mar 3, 2025 to Sep 25, 2026").assertIsDisplayed()
        compose.onNodeWithText("No outings").assertIsDisplayed()
        compose.onNodeWithText("nothing here is deleted", substring = true).assertIsDisplayed()
        snapshot("settings_data_import")
        compose.onNodeWithTag(DataTags.PREFERENCES).performClick()
        compose.onNodeWithTag(DataTags.CONFIRM_IMPORT).performClick()
        assertThat(imported).isFalse()
    }

    @Test
    fun `an import says what it did, in one sentence of only what happened`() {
        var dismissed = false
        val report = ImportReport(410, 2, 0, addedOutings = 38, addedPlans = 3, preferences = true)
        show(
            DataUiState(contents = contents, outcome = DataOutcome.Imported(report)),
            DataActions(dismissOutcome = { dismissed = true }),
        )

        compose.onNodeWithText("Backup imported").assertIsDisplayed()
        compose.onNodeWithText(
            "410 days added, 2 days joined with this phone’s, 38 outings added, profile and settings taken from the file.",
        ).assertIsDisplayed()
        snapshot("settings_data_imported")
        compose.onNodeWithText("OK").performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun `a file that is not a backup is said, with what Passo can import`() {
        show(DataUiState(contents = contents, outcome = DataOutcome.Failed(DataFailure.NOT_PASSO)))

        compose.onNodeWithText("This is not a Passo backup").assertIsDisplayed()
        compose.onNodeWithText("A spreadsheet table cannot be imported", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a saved backup says where it went`() {
        show(
            DataUiState(
                contents = contents,
                outcome = DataOutcome.BackupWritten("passo-backup-2026-09-25.json", 412, 38),
            ),
        )

        compose.onNodeWithText("412 days and 38 outings in passo-backup-2026-09-25.json.").assertIsDisplayed()
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        runCatching {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
