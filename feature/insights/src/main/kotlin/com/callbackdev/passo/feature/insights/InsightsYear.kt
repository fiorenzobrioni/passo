package com.callbackdev.passo.feature.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.format.unorphaned
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin

/**
 * In season (1 December to 31 January), the year ending as Insights' first card (owner,
 * 1 Oct 2026; PLANNING.md §11 Phase 12). No notification: it waits here, and once on Today.
 */
@Composable
internal fun YearSeasonCard(year: Int, onOpen: (Int) -> Unit) {
    StatusCard(
        icon = PassoIcons.Calendar,
        title = stringResource(R.string.insights_year_season_title, year),
        body = stringResource(R.string.insights_year_season_body).unorphaned(),
        tone = StatusTone.NOTE,
        action = stringResource(R.string.insights_year_open),
        onAction = { onOpen(year) },
        modifier = Modifier.padding(
            horizontal = ScreenMargin,
        ).padding(bottom = 16.dp).testTag(InsightsTags.YEAR_SEASON),
    )
}

/**
 * Your year on foot, by hand at any time: the year so far, and every past year with 30 days or
 * more counted, newest first. A year is its row; what a year's pages are is said once, under the
 * group, where every row said it again.
 */
@Composable
internal fun YearDoors(years: List<Int>, currentYear: Int, onOpen: (Int) -> Unit) {
    Column {
        SettingsGroup(modifier = Modifier.testTag(InsightsTags.YEARS)) {
            years.forEachIndexed { index, year ->
                if (index > 0) GroupDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onOpen(year) }, role = Role.Button)
                        .heightIn(min = 56.dp)
                        .padding(horizontal = ScreenMargin, vertical = 8.dp),
                ) {
                    Text(
                        if (year == currentYear) {
                            stringResource(R.string.insights_year_so_far, year)
                        } else {
                            stringResource(R.string.insights_year_full, year)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        PassoIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.insights_year_body).unorphaned(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}
