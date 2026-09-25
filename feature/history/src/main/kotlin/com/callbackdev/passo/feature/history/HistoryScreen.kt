package com.callbackdev.passo.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.history.Period
import com.callbackdev.passo.core.domain.history.PeriodOverview
import com.callbackdev.passo.core.domain.history.PeriodPages
import com.callbackdev.passo.core.domain.history.PeriodScale
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A place in History another screen can open: Insights' best week, for one. */
@Immutable
data class HistoryTarget(val scale: PeriodScale, val date: LocalDate)

/** History, with its state from [HistoryViewModel]. */
@Composable
fun HistoryRoute(
    onOpenSettings: () -> Unit,
    target: HistoryTarget?,
    onTargetShown: () -> Unit,
    bottomPadding: Dp,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        dayDetail = { date ->
            val flow = remember(date) { viewModel.day(date) }
            flow.collectAsStateWithLifecycle(initialValue = null).value
        },
        onOpenSettings = onOpenSettings,
        target = target,
        onTargetShown = onTargetShown,
        bottomPadding = bottomPadding,
    )
}

/**
 * History (PLANNING.md §11 Phase 5): a day, a week, a month or a year at a time, paged with a
 * swipe or the arrows, from the first day Passo counted to today and never past either. The
 * scale is a segmented choice; the period's name sits between the arrows. A bar opens the
 * period under it, so the charts are also the way down.
 *
 * @param state null until the first read: nothing is drawn rather than an empty history.
 * @param dayDetail one day in detail, null while it is being read.
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState?,
    dayDetail: @Composable (LocalDate) -> DayDetail?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    target: HistoryTarget? = null,
    onTargetShown: () -> Unit = {},
    bottomPadding: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
) {
    var scale by rememberSaveable { mutableStateOf(PeriodScale.WEEK) }
    // The day the reader is looking at: it survives a change of scale, so a week opens on
    // the month that holds it and a day on that day.
    var anchorDay by rememberSaveable { mutableLongStateOf(Long.MIN_VALUE) }
    LaunchedEffect(target) {
        if (target != null) {
            scale = target.scale
            anchorDay = target.date.toEpochDay()
            onTargetShown()
        }
    }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            if (state != null) {
                val today = state.today
                val anchor = if (anchorDay == Long.MIN_VALUE) today else LocalDate.ofEpochDay(anchorDay)
                val first = state.firstDay?.takeIf { !it.isAfter(today) }
                val pages = remember(scale, first, today, state.firstDayOfWeek) {
                    PeriodPages(scale, first, today, state.firstDayOfWeek)
                }
                val count = pages.count

                key(scale, state.firstDayOfWeek) {
                    val pager = rememberPagerState(initialPage = pages.pageOf(anchor)) { count }
                    val scope = rememberCoroutineScope()
                    // The anchor follows the pages: on a new period, its most recent day.
                    val currentAnchor by rememberUpdatedState(anchor)
                    val currentPages by rememberUpdatedState(pages)
                    LaunchedEffect(pager) {
                        snapshotFlow { pager.settledPage }.collect { page ->
                            val period = currentPages.periodAt(page)
                            if (currentAnchor !in period) anchorDay = minOf(period.end, today).toEpochDay()
                        }
                    }
                    // A request from elsewhere (a bar, Insights) moves the pager to it.
                    LaunchedEffect(anchorDay) {
                        val page = pages.pageOf(anchor)
                        if (page != pager.currentPage) pager.scrollToPage(page)
                    }

                    Header(
                        showLatest = pager.currentPage != count - 1,
                        onLatest = { scope.launch { pager.animateScrollToPage(count - 1) } },
                        onOpenSettings = onOpenSettings,
                    )
                    ScaleChoice(scale) { chosen ->
                        anchorDay = anchor.toEpochDay()
                        scale = chosen
                    }
                    if (first == null) {
                        StatusCard(
                            icon = PassoIcons.Calendar,
                            title = stringResource(R.string.history_empty_title),
                            body = stringResource(R.string.history_empty_body),
                            tone = StatusTone.NOTE,
                            modifier = Modifier.padding(ScreenMargin).testTag(HistoryTags.EMPTY),
                        )
                    } else {
                        Navigator(pager, count, pages.periodAt(pager.currentPage), today)
                        Pages(
                            state = state,
                            pager = pager,
                            pages = pages,
                            dayDetail = dayDetail,
                            bottomPadding = bottomPadding,
                            onOpen = { newScale, date ->
                                anchorDay = date.toEpochDay()
                                scale = newScale
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(showLatest: Boolean, onLatest: () -> Unit, onOpenSettings: () -> Unit) {
    val reduced = reducedMotion()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        AnimatedVisibility(
            showLatest,
            enter = fadeIn(PassoMotion.effects(reduced)),
            exit = fadeOut(PassoMotion.effects(reduced)),
        ) {
            TextButton(onClick = onLatest, modifier = Modifier.testTag(HistoryTags.LATEST)) {
                Text(stringResource(R.string.history_back_to_now))
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(PassoIcons.Settings, contentDescription = stringResource(R.string.history_settings))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaleChoice(scale: PeriodScale, onChoose: (PeriodScale) -> Unit) {
    val scales = PeriodScale.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = ScreenMargin,
            vertical = 8.dp,
        ).testTag(HistoryTags.SCALES),
    ) {
        scales.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == scale,
                onClick = { onChoose(option) },
                shape = SegmentedButtonDefaults.itemShape(index, scales.size),
                // No check mark: four words fit a phone's width only without it.
                icon = {},
                label = {
                    Text(
                        text = stringResource(
                            when (option) {
                                PeriodScale.DAY -> R.string.history_scale_day
                                PeriodScale.WEEK -> R.string.history_scale_week
                                PeriodScale.MONTH -> R.string.history_scale_month
                                PeriodScale.YEAR -> R.string.history_scale_year
                            },
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

/** The period's name between the arrows. It is announced when it changes. */
@Composable
private fun Navigator(pager: PagerState, count: Int, period: Period, today: LocalDate) {
    val scope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).testTag(HistoryTags.NAVIGATOR),
    ) {
        IconButton(
            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            enabled = pager.currentPage > 0,
        ) {
            Icon(PassoIcons.ChevronLeft, contentDescription = stringResource(R.string.history_earlier))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = periodTitle(period, today),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            periodSubtitle(period, today)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
        IconButton(
            onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            enabled = pager.currentPage < count - 1,
        ) {
            Icon(PassoIcons.ChevronRight, contentDescription = stringResource(R.string.history_later))
        }
    }
}

@Composable
private fun Pages(
    state: HistoryUiState,
    pager: PagerState,
    pages: PeriodPages,
    dayDetail: @Composable (LocalDate) -> DayDetail?,
    bottomPadding: Dp,
    onOpen: (PeriodScale, LocalDate) -> Unit,
) {
    val format = rememberMeasureFormatter(state.units)
    HorizontalPager(
        state = pager,
        key = { pages.periodAt(it).start.toEpochDay() },
        modifier = Modifier.fillMaxSize().testTag(HistoryTags.PAGER),
    ) { page ->
        val period = pages.periodAt(page)
        Box(Modifier.fillMaxSize()) {
            if (period.scale == PeriodScale.DAY) {
                DayPage(dayDetail(period.start), state.minWalkMinutes, format, bottomPadding)
            } else {
                val overview = remember(period, state.days, state.today, state.firstDay, state.goalSteps) {
                    PeriodOverview.of(period, state.days, state.today, state.firstDay, state.goalSteps)
                }
                PeriodPage(overview, state.today, state.firstDayOfWeek, format, onOpen, bottomPadding)
            }
        }
    }
}

/** Hooks for the UI tests. */
object HistoryTags {
    const val SCALES = "history_scales"
    const val NAVIGATOR = "history_navigator"
    const val LATEST = "history_latest"
    const val PAGER = "history_pager"
    const val PAGE = "history_page"
    const val CHART = "history_chart"
    const val CALENDAR = "history_calendar"
    const val METRICS = "history_metrics"
    const val WALKS = "history_walks"
    const val EMPTY = "history_empty"
}
