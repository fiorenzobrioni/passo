package com.callbackdev.passo.feature.sessions

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.SegmentLabel
import com.callbackdev.passo.core.designsystem.components.StillRouteView
import com.callbackdev.passo.core.designsystem.components.sessionIcon
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.datePattern
import com.callbackdev.passo.core.designsystem.format.durationWords
import com.callbackdev.passo.core.designsystem.format.movingMinutes
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.sessionOutcome
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.unorphaned
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.routes.DistanceKind
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.routes.distanceKind
import com.callbackdev.passo.core.domain.sessions.KeptOuting
import com.callbackdev.passo.core.domain.sessions.OutingMonth
import com.callbackdev.passo.core.domain.sessions.OutingsFilter
import com.callbackdev.passo.core.domain.sessions.OutingsSummary
import com.callbackdev.passo.core.domain.sessions.RouteShare
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.Session
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import com.callbackdev.passo.core.designsystem.R as DesignR

/**
 * «Your outings», with its state, its filter and the routes' pictures.
 *
 * @param withRoutes open on the outings that hold a route: Your data's routes row asks for it.
 */
@Composable
fun OutingsListRoute(
    withRoutes: Boolean,
    onBack: () -> Unit,
    onOpenOuting: (Long) -> Unit,
    viewModel: OutingsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The page's own, saved with it: the model is the activity's, and two pages open on it (one
    // under Today, one under Settings) each keep theirs.
    var filter by rememberSaveable { mutableStateOf(if (withRoutes) OutingsFilter.WITH_ROUTE else OutingsFilter.ALL) }
    OutingsListScreen(
        state = state,
        filter = filter,
        onFilter = { filter = it },
        onBack = onBack,
        onOpenOuting = onOpenOuting,
        pictures = viewModel.pictures,
    )
}

/**
 * «Your outings» (PLANNING.md §11 Phase 23, step 23.2): one sentence of how many and since when,
 * the filter where some hold a route, then the outings by month, newest first, each row the day's
 * list's (its mark or its route, its day and time, its name, how it went, its distance and time in
 * motion) and a door to its page.
 *
 * Every row is an item of its own, its month's ground drawn piece by piece, so the list composes
 * only the rows on screen: a route is read for those alone, from [pictures], when its row comes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutingsListScreen(
    state: OutingsListUiState?,
    filter: OutingsFilter,
    onFilter: (OutingsFilter) -> Unit,
    onBack: () -> Unit,
    onOpenOuting: (Long) -> Unit,
    modifier: Modifier = Modifier,
    pictures: RoutePictures = RoutePictures.None,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.outings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.outings_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the outings are read, a bare page: an empty list would say there are none.
        if (state != null) {
            OutingsList(state, filter, onFilter, onOpenOuting, pictures, Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable
private fun OutingsList(
    state: OutingsListUiState,
    filter: OutingsFilter,
    onFilter: (OutingsFilter) -> Unit,
    onOpenOuting: (Long) -> Unit,
    pictures: RoutePictures,
    modifier: Modifier,
) {
    val format = rememberMeasureFormatter(state.units)
    val outings = state.outings
    val summary = outings.summary
    val months = outings.months(filter)
    val reduced = reducedMotion()
    // The rows the filter takes away or brings back move on the page's springs, and fade.
    val placement: FiniteAnimationSpec<IntOffset>? = if (reduced) null else PassoMotion.spatial()
    LazyColumn(
        modifier = modifier.testTag(OutingsListTags.LIST),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
    ) {
        if (summary != null) {
            item(key = "summary") {
                Text(
                    text = summarySentence(summary, format).unorphaned(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
                        .testTag(OutingsListTags.SUMMARY),
                )
            }
        }
        // A choice only where there is one: some outing holds a route, or the page was opened on them.
        if (summary != null && (summary.withRoute > 0 || filter == OutingsFilter.WITH_ROUTE)) {
            item(key = "filter") { FilterRow(filter, onFilter) }
        }
        if (filter == OutingsFilter.WITH_ROUTE && months.isNotEmpty()) {
            item(key = "routes-note") {
                Text(
                    text = stringResource(R.string.outings_routes_note).unorphaned(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .animateItem(fadeInSpec = PassoMotion.effects(reduced), placementSpec = placement)
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }
        if (months.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(
                        if (summary == null) R.string.outings_empty else R.string.outings_empty_routes,
                    ).unorphaned(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag(OutingsListTags.EMPTY),
                )
            }
        }
        months.forEachIndexed { index, month ->
            month(month, first = index == 0, state.today, format, onOpenOuting, pictures, reduced, placement)
        }
    }
}

/** A month: its header, then its rows, on one rounded ground drawn row by row. */
private fun LazyListScope.month(
    month: OutingMonth,
    first: Boolean,
    today: LocalDate,
    format: MeasureFormatter,
    onOpen: (Long) -> Unit,
    pictures: RoutePictures,
    reduced: Boolean,
    placement: FiniteAnimationSpec<IntOffset>?,
) {
    item(key = "month-${month.month}", contentType = "month") {
        MonthHeader(
            month,
            today,
            first,
            format,
            Modifier.animateItem(
                fadeInSpec = PassoMotion.effects(reduced),
                placementSpec = placement,
                fadeOutSpec = PassoMotion.effects(reduced),
            ),
        )
    }
    val last = month.outings.lastIndex
    itemsIndexed(month.outings, key = { _, it -> "outing-${it.session.id}" }, contentType = { _, _ -> "outing" }) {
            index,
            outing,
        ->
        KeptOutingRow(
            outing = outing,
            format = format,
            top = index == 0,
            bottom = index == last,
            onOpen = onOpen,
            pictures = pictures,
            modifier = Modifier.animateItem(
                fadeInSpec = PassoMotion.effects(reduced),
                placementSpec = placement,
                fadeOutSpec = PassoMotion.effects(reduced),
            ),
        )
    }
}

/** All, or only the outings that still hold a route. */
@Composable
private fun FilterRow(filter: OutingsFilter, onFilter: (OutingsFilter) -> Unit) {
    val options = OutingsFilter.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin, vertical = 4.dp)
            .testTag(OutingsListTags.FILTER),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == filter,
                onClick = { onFilter(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                contentPadding = PaddingValues(horizontal = 4.dp),
                // The words alone, as History's scales: at twice the text size they need the room.
                icon = {},
                label = {
                    SegmentLabel(
                        stringResource(
                            when (option) {
                                OutingsFilter.ALL -> R.string.outings_filter_all
                                OutingsFilter.WITH_ROUTE -> R.string.outings_filter_routes
                            },
                        ),
                    )
                },
                modifier = Modifier.testTag("${OutingsListTags.FILTER}-${option.name}"),
            )
        }
    }
}

/**
 * «November», or «March 2025» in a year before this one, and what it holds: «12 outings · 6 h
 * 40 min in motion». One heading for TalkBack, which can jump from month to month.
 */
@Composable
private fun MonthHeader(
    month: OutingMonth,
    today: LocalDate,
    first: Boolean,
    format: MeasureFormatter,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    val date = month.month.atDay(1)
    val name = datePattern(date, if (month.month.year == today.year) "MMMM" else "MMMMyyyy")
    val minutes = (month.movingMillis / MILLIS_PER_MINUTE).toInt()
    val count = month.outings.size
    val counted = format.steps(count)
    val held = pluralStringResource(R.plurals.outings_month, count, counted, shortDuration(minutes, format))
    val spoken = "$name. " + res.getQuantityString(R.plurals.outings_month, count, counted, res.durationWords(minutes))
    FlowRow(
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = if (first) 12.dp else 24.dp, bottom = 8.dp)
            .testTag("${OutingsListTags.MONTH}-${month.month}")
            .clearAndSetSemantics {
                heading()
                contentDescription = spoken
            },
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = held,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One outing, as the day's list draws it, with its day: its route small at its start where it has
 * one (or its mark), the day and the time it started, its name, how it went, then its distance,
 * «measured» where it is, and its time in motion. One node for TalkBack, a door to its page.
 *
 * @param top the first of its month, and [bottom] the last: where its month's ground is rounded.
 */
@Composable
private fun KeptOutingRow(
    outing: KeptOuting,
    format: MeasureFormatter,
    top: Boolean,
    bottom: Boolean,
    onOpen: (Long) -> Unit,
    pictures: RoutePictures,
    modifier: Modifier = Modifier,
) {
    val session = outing.session
    val res = LocalResources.current
    val name = res.sessionName(session)
    val date = LocalDate.ofEpochDay(session.localEpochDay)
    val day = datePattern(date, "MMMEd")
    val time = clockTime(startMinute(session))
    // An open outing's outcome is its time in motion, which its figures already say.
    val outcome = if (session.open) null else res.sessionOutcome(session, format)
    val distance = format.distance(session.totals.distanceMeters).text()
    val measured = session.distanceKind() == DistanceKind.MEASURED
    // A figure is never broken inside itself («2.43 / km»), the words after it may be, at a large
    // text size: «47 min / in motion», not «47 min in motio / n».
    val distanceShown = if (measured) {
        stringResource(DesignR.string.session_row_measured, distance.unbroken())
    } else {
        stringResource(DesignR.string.walk_estimates, distance).unbroken()
    }
    val moving = stringResource(R.string.outings_row_moving, shortDuration(session.movingMinutes(), format).unbroken())
    val spoken = listOfNotNull(
        // After the name, mid-sentence: «mercoledì 18 novembre», not capitalised where Italian does not.
        "$name, " +
            stringResource(R.string.outings_row_spoken_when, datePattern(date, LONG_DATE, capitalized = false), time),
        outcome,
        listOf(
            if (measured) {
                stringResource(DesignR.string.session_row_measured, distance)
            } else {
                stringResource(R.string.outings_row_spoken_estimated, distance)
            },
            stringResource(R.string.outings_row_moving, res.durationWords(session.movingMinutes())),
        ).joinToString(", "),
        if (outing.hasRoute) stringResource(R.string.outings_row_spoken_route).removeSuffix(".") else null,
    ).joinToString(". ")
    val open = stringResource(DesignR.string.session_card_open)
    val corner = GroupShape.topStart
    val ground = RoundedCornerShape(
        topStart = if (top) corner else ZeroCorner,
        topEnd = if (top) corner else ZeroCorner,
        bottomStart = if (bottom) corner else ZeroCorner,
        bottomEnd = if (bottom) corner else ZeroCorner,
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = ground,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin),
    ) {
        Column {
            if (!top) GroupDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    // Before the semantics it is cleared with, which would clear it too.
                    .testTag("${OutingsListTags.ROW}-${session.id}")
                    .clickable { onOpen(session.id) }
                    .clearAndSetSemantics {
                        contentDescription = spoken
                        role = Role.Button
                        onClick(label = open) {
                            onOpen(session.id)
                            true
                        }
                    }
                    .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
            ) {
                OutingTile(outing, pictures)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // The time goes under the day when the two do not fit one line (large text).
                    FlowRow(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        itemVerticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = day.unbroken(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(
                            text = time.unbroken(),
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (outcome != null) Outcome(outcome, reached = session.reached)
                    FiguresLine(distanceShown, moving, measured)
                }
                Icon(
                    PassoIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(CHEVRON_SIZE),
                )
            }
        }
    }
}

/** «Goal reached», in the goal's colour after its check; «72% of the goal» in the quiet ink. */
@Composable
private fun Outcome(text: String, reached: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (reached) {
            Icon(
                PassoIcons.Check,
                contentDescription = null,
                tint = PassoTheme.colors.goal,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (reached) PassoTheme.colors.goal else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The distance and the time in motion, on one line with a dot between them where the two fit, or
 * one under the other where they do not: never a dot left at a line's end, nor a figure broken
 * in two. A distance measured by satellite has the satellites' mark before it.
 */
@Composable
private fun FiguresLine(distance: String, moving: String, measured: Boolean) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Layout(
        content = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (measured) {
                    Icon(
                        PassoIcons.Gps,
                        contentDescription = null,
                        tint = ink,
                        // On the text's first line: where the distance wraps, the mark stays with it.
                        modifier = Modifier.padding(top = 3.dp).size(14.dp),
                    )
                }
                Text(distance, style = style, color = ink)
            }
            Text(FIGURE_DOT, style = style, color = ink, modifier = Modifier.padding(horizontal = 7.dp))
            Text(moving, style = style, color = ink)
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val first = measurables[0].measure(loose)
        val gap = measurables[1].measure(loose)
        val second = measurables[2].measure(loose)
        val oneLine = first.width + gap.width + second.width <= constraints.maxWidth
        val width = if (oneLine) first.width + gap.width + second.width else maxOf(first.width, second.width)
        val height = if (oneLine) maxOf(first.height, gap.height, second.height) else first.height + second.height
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height) {
            first.placeRelative(0, 0)
            if (oneLine) {
                gap.placeRelative(first.width, 0)
                second.placeRelative(first.width + gap.width, 0)
            } else {
                second.placeRelative(0, first.height)
            }
        }
    }
}

/** What a row's tile shows of its route: read on the row's first frame, or still being read. */
private sealed interface Picture {
    data object Reading : Picture

    data class Read(val shape: RouteShape?) : Picture
}

/**
 * The row's tile: its route on plain ground, as its page draws it but still and small, where it has
 * one; its mark otherwise. The route is read when the row is composed, not before: a picture kept
 * from an earlier frame is drawn at once, one read now fades in over its ground.
 */
@Composable
private fun OutingTile(outing: KeptOuting, pictures: RoutePictures) {
    val key: RouteKey? = outing.route
    if (key == null) {
        MarkTile(outing.session)
        return
    }
    val kept = remember(key) { pictures.peek(key) }
    val picture by produceState<Picture>(kept?.let { Picture.Read(it) } ?: Picture.Reading, key) {
        // Set for every key, a kept one included: the state outlives a key that changed (a route
        // that grew while its row was on screen), and must not keep the line before.
        value = Picture.Read(kept ?: pictures.of(key))
    }
    val shape = (picture as? Picture.Read)?.shape
    // Gone between the list's read and the row's (deleted meanwhile): its mark, as any other.
    if (picture is Picture.Read && shape == null) {
        MarkTile(outing.session)
        return
    }
    val alpha by animateFloatAsState(
        targetValue = if (shape != null) 1f else 0f,
        animationSpec = if (kept != null) snap() else PassoMotion.effects(reducedMotion()),
        label = "route",
    )
    Box(
        modifier = Modifier
            .size(TILE_SIZE)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (shape != null) {
            StillRouteView(
                shape = shape,
                // The row's own sentence says it has a route: one node, read once.
                contentDescription = "",
                ratio = 1f,
                clip = MaterialTheme.shapes.large,
                modifier = Modifier.size(TILE_SIZE).alpha(alpha),
            )
        }
    }
}

/** An outing without a route: its mark, on the ground a route would have. */
@Composable
private fun MarkTile(session: Session) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TILE_SIZE)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Icon(
            imageVector = sessionIcon(session.intensity, session.goalKind),
            contentDescription = null,
            tint = if (session.reached) PassoTheme.colors.goal else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(MARK_SIZE),
        )
    }
}

/**
 * «48 outings since March; 21 with their route.»: how many, since when (with its year before this
 * one), and how many hold a route.
 */
@Composable
private fun summarySentence(summary: OutingsSummary, format: MeasureFormatter): String {
    val res = LocalResources.current
    if (summary.count == 1) {
        return stringResource(
            if (summary.routes == RouteShare.ALL) R.string.outings_summary_one_route else R.string.outings_summary_one,
        )
    }
    val since = sinceMonth(summary.since, summary.sinceThisYear)
    val count = summary.count
    val counted = format.steps(count)
    return when (summary.routes) {
        RouteShare.NONE -> res.getQuantityString(R.plurals.outings_summary, count, counted, since)

        RouteShare.ALL -> res.getQuantityString(R.plurals.outings_summary_all, count, counted, since)

        RouteShare.SOME -> res.getQuantityString(
            R.plurals.outings_summary_some,
            count,
            counted,
            since,
            res.getQuantityString(R.plurals.outings_with_route, summary.withRoute, format.steps(summary.withRoute)),
        )
    }
}

/** «March», «March 2025»: in the sentence, so not capitalised where the language does not. */
@Composable
private fun sinceMonth(month: YearMonth, thisYear: Boolean): String =
    datePattern(month.atDay(1), if (thisYear) "MMMM" else "MMMMyyyy", capitalized = false)

/**
 * A time in motion as a row or a header shows it, one way whatever its length: «47 min», «2 h»,
 * «6 h 40 min». Said in words to TalkBack ([durationWords]).
 */
@Composable
private fun shortDuration(minutes: Int, format: MeasureFormatter): String = when {
    minutes < MINUTES_PER_HOUR -> format.minutes(minutes).text()

    minutes % MINUTES_PER_HOUR == 0 -> stringResource(DesignR.string.duration_hours, minutes / MINUTES_PER_HOUR)

    else -> stringResource(
        DesignR.string.duration_hours_minutes,
        minutes / MINUTES_PER_HOUR,
        minutes % MINUTES_PER_HOUR,
    )
}

/** The minute of its day an outing started at, in the reader's zone. */
private fun startMinute(session: Session): Int =
    minuteOfDay(session.startedAtMillis / MILLIS_PER_MINUTE, session.localEpochDay, ZoneId.systemDefault())

/** A figure that never breaks inside itself: a line too long breaks between two, after the dot. */
private fun String.unbroken(): String = replace(' ', ' ').replace(' ', ' ')

private const val FIGURE_DOT = "·"

/** «Wednesday, November 18», as the app's long dates are. */
private const val LONG_DATE = "EEEEdMMMM"
private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60
private val TILE_SIZE = 56.dp
private val MARK_SIZE = 24.dp
private val CHEVRON_SIZE = 24.dp
private val ZeroCorner = CornerSize(0.dp)

/** Hooks for the UI tests. */
object OutingsListTags {
    const val LIST = "outings_list"
    const val SUMMARY = "outings_summary"
    const val FILTER = "outings_filter"
    const val MONTH = "outings_month"
    const val ROW = "outings_row"
    const val EMPTY = "outings_empty"
}
