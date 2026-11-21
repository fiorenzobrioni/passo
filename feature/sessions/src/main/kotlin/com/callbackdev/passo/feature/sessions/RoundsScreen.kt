package com.callbackdev.passo.feature.sessions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.RoundChartPoint
import com.callbackdev.passo.core.designsystem.components.RoundTimesChart
import com.callbackdev.passo.core.designsystem.components.RouteMapView
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.StillRouteView
import com.callbackdev.passo.core.designsystem.components.duration
import com.callbackdev.passo.core.designsystem.format.datePattern
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.longDate
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.shortDate
import com.callbackdev.passo.core.designsystem.format.shortDateWithYear
import com.callbackdev.passo.core.designsystem.format.unorphaned
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.rounds.KeptRound
import com.callbackdev.passo.core.domain.rounds.RoundTime
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.model.RouteKey
import java.time.LocalDate

/** «Your rounds», with its state and its rows' small maps. */
@Composable
fun RoundsRoute(onBack: () -> Unit, onOpenRound: (Long) -> Unit, viewModel: RoundsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RoundsScreen(state = state, onBack = onBack, onOpenRound = onOpenRound, pictures = viewModel.pictures)
}

/**
 * «Your rounds» (PLANNING.md §11 Phase 20): one sentence of how many, then each round on one ground,
 * the latest walked first: its small map from its latest walk's route (still, on plain ground, as a
 * list draws a route), its name, its length and how many times it was walked, and its usual time;
 * each a door to its page. Then how a round is found and compared, once. Never a record board: one
 * reader and their own rounds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundsScreen(
    state: RoundsUiState?,
    onBack: () -> Unit,
    onOpenRound: (Long) -> Unit,
    modifier: Modifier = Modifier,
    pictures: RoutePictures = RoutePictures.None,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rounds_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.rounds_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the rounds are read, a bare page: an empty list would say there are none.
        if (state != null) {
            val format = rememberMeasureFormatter(state.units)
            val resources = LocalResources.current
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag(RoundsTags.LIST),
                contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") {
                    Text(
                        text = if (state.rounds.isEmpty()) {
                            stringResource(R.string.rounds_empty)
                        } else {
                            resources.getQuantityString(
                                R.plurals.rounds_summary,
                                state.rounds.size,
                                format.steps(state.rounds.size),
                            )
                        }.unorphaned(),
                        style = if (state.rounds.isEmpty()) {
                            MaterialTheme.typography.bodyLarge
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        color = if (state.rounds.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp)
                            .testTag(RoundsTags.SUMMARY),
                    )
                }
                if (state.rounds.isNotEmpty()) {
                    item(key = "rounds") {
                        SettingsGroup {
                            state.rounds.forEachIndexed { index, round ->
                                if (index > 0) GroupDivider()
                                RoundRow(round, format, pictures, onOpenRound)
                            }
                        }
                    }
                }
                item(key = "note") {
                    Text(
                        text = stringResource(R.string.rounds_note).unorphaned(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** One round: its small map, its name, «3.20 km · 9 walks», «Usually 38 min in motion». One node. */
@Composable
private fun RoundRow(round: KeptRound, format: MeasureFormatter, pictures: RoutePictures, onOpen: (Long) -> Unit) {
    val resources = LocalResources.current
    val name = resources.roundName(round, format)
    val length = resources.format(format.distance(round.usual.meters))
    val walks = resources.roundWalks(round.walks.size, format)
    val usual = duration(minutesOf(round.usual.movingMillis))
    // A round named by its length does not say its length twice.
    val named = round.round.name != null
    val detail = if (named) stringResource(R.string.rounds_row_detail, length, walks) else walks
    val spoken =
        stringResource(R.string.rounds_row_spoken, name, detail, resources.roundMinutes(round.usual.movingMillis))
    val open = stringResource(R.string.rounds_open)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${RoundsTags.ROW}-${round.id}")
            .clickable { onOpen(round.id) }
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick(label = open) {
                    onOpen(round.id)
                    true
                }
            }
            .padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
    ) {
        RoundTile(round.route, pictures)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.rounds_row_usual, usual),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A round's small map: its latest walk's route, still, read when the row is composed, faded in over
 * its ground; the round's mark where no walk of it keeps a route.
 */
@Composable
private fun RoundTile(key: RouteKey?, pictures: RoutePictures) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TILE_SIZE)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (key == null) {
            Icon(PassoIcons.Round, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            return@Box
        }
        val kept = remember(key) { pictures.peek(key) }
        // Null while it is read; then the shape, or none (deleted meanwhile), which wears the mark.
        val read by produceState<Result<RouteShape?>?>(kept?.let { Result.success(it) }, key) {
            value = Result.success(kept ?: pictures.of(key))
        }
        val shape = read?.getOrNull()
        if (read != null && shape == null) {
            Icon(PassoIcons.Round, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            return@Box
        }
        val alpha by animateFloatAsState(
            targetValue = if (shape != null) 1f else 0f,
            animationSpec = if (kept != null) snap() else PassoMotion.effects(reducedMotion()),
            label = "round",
        )
        shape?.let {
            StillRouteView(
                shape = it,
                contentDescription = "",
                ratio = 1f,
                clip = MaterialTheme.shapes.large,
                modifier = Modifier.size(TILE_SIZE).alpha(alpha),
            )
        }
    }
}

/** A round's page with its state, its renaming and its forgetting. */
@Composable
fun RoundRoute(
    roundId: Long,
    onBack: () -> Unit,
    onOpenOuting: (Long) -> Unit,
    viewModel: RoundViewModel = hiltViewModel(),
) {
    LaunchedEffect(roundId) { viewModel.open(roundId) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    val state by viewModel.state.collectAsStateWithLifecycle()
    RoundScreen(
        state = state,
        onBack = onBack,
        onOpenOuting = onOpenOuting,
        onRename = viewModel::rename,
        // The page goes back by itself once the round is gone (below): one back, never two.
        onForget = viewModel::forget,
        onSame = viewModel::same,
        onAnother = viewModel::another,
    )
}

/**
 * A usual round's page (PLANNING.md §11 Phase 20): its sentence first («6 walks since March; the
 * last 3 faster than usual»), its usual, its map with the gestures of an outing's (from its latest
 * walk's route), its times over the weeks with the usual as a line, then each walk with its day and
 * its time brought to the usual length, a door to the walk's own page. Rename in the bar; «Forget
 * this round» at the foot, asked first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundScreen(
    state: RoundUiState?,
    onBack: () -> Unit,
    onOpenOuting: (Long) -> Unit,
    onRename: (String) -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
    onSame: (KeptRound) -> Unit = {},
    onAnother: (KeptRound) -> Unit = {},
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val resources = LocalResources.current
    val round = state?.round
    // No longer one of the reader's rounds (forgotten from another page, or left with two walks):
    // nothing true to show, so back.
    LaunchedEffect(state != null && round == null) { if (state != null && round == null) onBack() }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var forgetting by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    // A long name on one line, as a bar's title is: the page says the rest.
                    if (state != null && round != null) {
                        Text(
                            resources.roundName(round, rememberMeasureFormatter(state.units)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.round_back))
                    }
                },
                actions = {
                    if (round != null) {
                        TextButton(onClick = { renaming = true }, modifier = Modifier.testTag(RoundsTags.RENAME)) {
                            Text(stringResource(R.string.round_rename))
                        }
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        if (state != null && round != null) {
            val format = rememberMeasureFormatter(state.units)
            RoundPage(
                round,
                state.map,
                state.today,
                format,
                onOpenOuting,
                onForget = { forgetting = true },
                Modifier.fillMaxSize().padding(padding),
                same = state.same,
                onSame = onSame,
                onAnother = onAnother,
            )
            if (renaming) {
                RenameDialog(
                    current = round.round.name.orEmpty(),
                    onSave = {
                        renaming = false
                        onRename(it)
                    },
                    onDismiss = { renaming = false },
                )
            }
            if (forgetting) {
                AlertDialog(
                    onDismissRequest = { forgetting = false },
                    title = { Text(stringResource(R.string.round_forget_title)) },
                    text = { Text(stringResource(R.string.round_forget_text)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                forgetting = false
                                onForget()
                            },
                            modifier = Modifier.testTag(RoundsTags.FORGET_CONFIRM),
                        ) { Text(stringResource(R.string.round_forget_confirm)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { forgetting = false }) { Text(stringResource(R.string.round_cancel)) }
                    },
                )
            }
        }
    }
}

@Composable
private fun RoundPage(
    round: KeptRound,
    map: RouteShape?,
    today: LocalDate,
    format: MeasureFormatter,
    onOpenOuting: (Long) -> Unit,
    onForget: () -> Unit,
    modifier: Modifier,
    same: KeptRound? = null,
    onSame: (KeptRound) -> Unit = {},
    onAnother: (KeptRound) -> Unit = {},
) {
    val resources = LocalResources.current
    val trend = resources.roundTrendSentence(round.trend, today, format)
    val length = resources.format(format.distance(round.usual.meters))
    val usual = duration(minutesOf(round.usual.movingMillis))
    LazyColumn(
        modifier = modifier.testTag(RoundsTags.PAGE),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "sentence") {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Text(
                    trend,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }.testTag(RoundsTags.SENTENCE),
                )
                Text(
                    stringResource(R.string.round_usual, resources.roundMinutes(round.usual.movingMillis), length),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // The round it may be, asked once: Passo cannot know it, the reader can.
        if (same != null) {
            item(key = "same") { SameRoundQuestion(same, format, onSame, onAnother) }
        }
        item(key = "map") {
            Box(Modifier.padding(horizontal = ScreenMargin)) {
                if (map != null) {
                    RouteMapView(
                        shape = map,
                        beads = emptyList(),
                        units = format.units,
                        contentDescription = mapSpoken(map, length, resources),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        stringResource(R.string.round_no_map),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp).testTag(RoundsTags.NO_MAP),
                    )
                }
            }
        }
        item(key = "times") {
            RoundGroup {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.round_times),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    val times = round.times
                    val slowest = times.maxOf { it.millis }
                    val fastest = times.minOf { it.millis }
                    val first = LocalDate.ofEpochDay(times.first().localEpochDay)
                    val last = LocalDate.ofEpochDay(times.last().localEpochDay)
                    RoundTimesChart(
                        points = times.map { RoundChartPoint(it.localEpochDay, it.millis) },
                        usualMillis = round.usual.movingMillis,
                        slowest = duration(minutesOf(slowest)),
                        fastest = duration(minutesOf(fastest)),
                        firstDay = dayLabel(first, today),
                        lastDay = dayLabel(last, today),
                        description = trend,
                    )
                    Text(
                        stringResource(R.string.round_times_note, length, usual),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item(key = "walks-title") {
            Text(
                stringResource(R.string.round_walks),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).semantics { heading() },
            )
        }
        item(key = "walks") {
            SettingsGroup {
                round.times.asReversed().forEachIndexed { index, time ->
                    if (index > 0) GroupDivider()
                    WalkTimeRow(time, today, format, onOpenOuting)
                }
            }
        }
        item(key = "forget") {
            TextButton(
                onClick = onForget,
                modifier = Modifier
                    .padding(horizontal = ScreenMargin)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(RoundsTags.FORGET),
            ) { Text(stringResource(R.string.round_forget), color = MaterialTheme.colorScheme.error) }
        }
    }
}

/**
 * One walk of the round: its day, its time brought to the usual length, the distance it walked. One
 * node, a door. The day short, as the list of outings writes it, and the time under it where the two
 * do not fit one line (a large text size): never a word broken in two.
 */
@Composable
private fun WalkTimeRow(time: RoundTime, today: LocalDate, format: MeasureFormatter, onOpen: (Long) -> Unit) {
    val resources = LocalResources.current
    val date = LocalDate.ofEpochDay(time.localEpochDay)
    val day = longDate(date)
    val shown = datePattern(date, if (date.year == today.year) "MMMEd" else "yMMMEd")
    val brought = duration(minutesOf(time.millis))
    val walked = stringResource(R.string.round_walk_meters, resources.format(format.distance(time.meters)))
    val spoken = stringResource(
        R.string.round_walk_spoken,
        day,
        resources.roundMinutes(time.millis),
        resources.format(format.distance(time.meters)),
    )
    val open = stringResource(com.callbackdev.passo.core.designsystem.R.string.session_card_open)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${RoundsTags.WALK}-${time.sessionId}")
            .clickable { onOpen(time.sessionId) }
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick(label = open) {
                    onOpen(time.sessionId)
                    true
                }
            }
            .padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(shown, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
                Text(brought, style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"))
            }
            Text(
                walked,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The reader's own name for the round, or none, which names it by its length. The field takes the
 * focus with the name selected, as a rename does: typing replaces it, a touch keeps it.
 */
@Composable
private fun RenameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(current, selection = TextRange(0, current.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.round_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = if (it.text.length <= MAX_NAME) it else text },
                    label = { Text(stringResource(R.string.round_rename_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSave(text.text) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag(RoundsTags.NAME_FIELD),
                )
                Text(
                    stringResource(R.string.round_rename_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.text) }, modifier = Modifier.testTag(RoundsTags.NAME_SAVE)) {
                Text(stringResource(R.string.round_rename_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.round_cancel)) } },
    )
}

/**
 * Whether this page's round and [other] are one (PLANNING.md §15, «Phase 20: two rounds made one»): one of them
 * lost every walk's route, so Passo could not know the other's walks were it. Asked, never guessed:
 * «The same» makes them one on this page, «Another round» keeps them two and is not asked again.
 */
@Composable
private fun SameRoundQuestion(
    other: KeptRound,
    format: MeasureFormatter,
    onSame: (KeptRound) -> Unit,
    onAnother: (KeptRound) -> Unit,
) {
    val resources = LocalResources.current
    val name = resources.roundName(other, format)
    // This page's round has a line, the other none: its walks lost their routes. Or the other way.
    val lost = other.route == null
    RoundGroup {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(RoundsTags.SAME)) {
            Text(
                stringResource(R.string.round_same_title, name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(if (lost) R.string.round_same_text_lost else R.string.round_same_text_since, name)
                    .unorphaned(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // As the app's other questions are answered: the answer that keeps things as they are first.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    onClick = { onAnother(other) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(RoundsTags.SAME_NO),
                ) { Text(stringResource(R.string.round_same_no)) }
                FilledTonalButton(
                    onClick = { onSame(other) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(RoundsTags.SAME_YES),
                ) { Text(stringResource(R.string.round_same_yes)) }
            }
        }
    }
}

/** A part of the page on its own rounded ground, as wide as the page's groups. */
@Composable
private fun RoundGroup(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

/** «25 Sep», or «25 Sep 2025» for a day of a year before this one. */
@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String =
    if (date.year == today.year) shortDate(date) else shortDateWithYear(date)

/** «Your round: 3.20 km, from your door and back.», or from one point of the compass to another. */
private fun mapSpoken(shape: RouteShape, length: String, resources: android.content.res.Resources): String {
    val heading = shape.heading ?: return resources.getString(R.string.round_map_spoken, length)
    return resources.getString(
        R.string.round_map_spoken_line,
        length,
        resources.getString(heading.opposite.word),
        resources.getString(heading.word),
    )
}

private fun minutesOf(millis: Long): Int =
    ((millis + MILLIS_PER_MINUTE / 2) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)

private const val MILLIS_PER_MINUTE = 60_000L

/** A name the reader types: a line's worth. */
private const val MAX_NAME = 40
private val TILE_SIZE = 56.dp

/** Hooks for the UI tests. */
object RoundsTags {
    const val LIST = "rounds_list"
    const val SUMMARY = "rounds_summary"
    const val ROW = "rounds_row"
    const val PAGE = "round_page"
    const val SENTENCE = "round_sentence"
    const val NO_MAP = "round_no_map"
    const val WALK = "round_walk"
    const val RENAME = "round_rename"
    const val NAME_FIELD = "round_name_field"
    const val NAME_SAVE = "round_name_save"
    const val FORGET = "round_forget"
    const val FORGET_CONFIRM = "round_forget_confirm"
    const val SAME = "round_same"
    const val SAME_YES = "round_same_yes"
    const val SAME_NO = "round_same_no"
}
