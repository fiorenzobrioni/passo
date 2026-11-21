package com.callbackdev.passo.feature.sessions

import android.content.res.Resources
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.ClimbProfile
import com.callbackdev.passo.core.designsystem.components.DayShareLine
import com.callbackdev.passo.core.designsystem.components.IntervalBars
import com.callbackdev.passo.core.designsystem.components.MetricTile
import com.callbackdev.passo.core.designsystem.components.MinuteStrip
import com.callbackdev.passo.core.designsystem.components.RouteBead
import com.callbackdev.passo.core.designsystem.components.RouteMapView
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SessionLiveButtons
import com.callbackdev.passo.core.designsystem.components.SessionNotices
import com.callbackdev.passo.core.designsystem.components.SplitList
import com.callbackdev.passo.core.designsystem.components.SplitRow
import com.callbackdev.passo.core.designsystem.components.StatStrip
import com.callbackdev.passo.core.designsystem.components.StripStat
import com.callbackdev.passo.core.designsystem.components.TilePair
import com.callbackdev.passo.core.designsystem.components.duration
import com.callbackdev.passo.core.designsystem.components.minuteStripBars
import com.callbackdev.passo.core.designsystem.format.annotated
import com.callbackdev.passo.core.designsystem.format.clockRange
import com.callbackdev.passo.core.designsystem.format.dayShareText
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.longDate
import com.callbackdev.passo.core.designsystem.format.minuteStripSpoken
import com.callbackdev.passo.core.designsystem.format.paceSpoken
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.sessionGoalDescription
import com.callbackdev.passo.core.designsystem.format.sessionHeadline
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.sessionZone
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.unitOf
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.share.ShareImage
import com.callbackdev.passo.core.designsystem.share.SharedFiles
import com.callbackdev.passo.core.designsystem.share.writeDocument
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.padding
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.routes.Compass
import com.callbackdev.passo.core.domain.routes.DistanceKind
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.routes.Split
import com.callbackdev.passo.core.domain.routes.Splits
import com.callbackdev.passo.core.domain.routes.distanceKind
import com.callbackdev.passo.core.domain.routes.pace
import com.callbackdev.passo.core.domain.routes.stepsShare
import com.callbackdev.passo.core.domain.sessions.OutingMinutes
import com.callbackdev.passo.core.domain.sessions.OutingShape
import com.callbackdev.passo.core.domain.sessions.cadenceFloor
import com.callbackdev.passo.core.domain.sessions.pausedMillis
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.UnitSystem
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/**
 * What an outing's page can ask for, as functions: the screen is a plain composable a test can draw.
 *
 * @property live what the card of an outing under way can ask for, which its page asks too while
 *   it counts (Phase 21): Stop, Pause, Resume, «Keep going» to the question before the cap, and
 *   the line about the satellites' way out. No «See the outing»: this is the outing.
 */
class OutingActions(
    val shareImage: () -> Unit = {},
    val shareGpx: () -> Unit = {},
    val saveGpx: () -> Unit = {},
    val deleteRoute: () -> Unit = {},
    val openStepCard: () -> Unit = {},
    val live: SessionCardActions = SessionCardActions(),
)

/** An outing's page with its state, and the sharing and saving of its route. */
@Composable
fun OutingRoute(
    sessionId: Long,
    onBack: () -> Unit,
    onOpenStepCard: () -> Unit = {},
    viewModel: OutingViewModel = hiltViewModel(),
) {
    LaunchedEffect(sessionId) { viewModel.open(sessionId) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state?.takeIf { it.session.id == sessionId }
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val messages = remember { SnackbarHostState() }
    val layer = rememberGraphicsLayer()
    val ground = MaterialTheme.colorScheme.surface.toArgb()
    val ink = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val signature = stringResource(R.string.outing_image_signature)
    val chooser = stringResource(R.string.outing_share_chooser)
    val saved = stringResource(R.string.outing_saved)
    val saveFailed = stringResource(R.string.outing_save_failed)
    val name = current?.let { resources.sessionName(it.session) }.orEmpty()
    val file = current?.let { fileName(it.session) }.orEmpty()
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(GPX_MIME)) { uri ->
        if (uri != null) {
            scope.launch {
                val gpx = viewModel.gpx(name)
                val ok = gpx != null && SharedFiles.save(context, uri) { it.writeDocument(gpx) }
                // Said on the page, where it can be read: a toast is gone before it is.
                messages.showSnackbar(if (ok) saved else saveFailed)
            }
        }
    }
    OutingScreen(
        state = current,
        onBack = onBack,
        messages = messages,
        shareLayer = layer,
        actions = OutingActions(
            shareImage = {
                scope.launch {
                    val image = ShareImage.compose(layer.toImageBitmap().asAndroidBitmap(), ground, ink, signature)
                    SharedFiles.cache(context, "$file.png") { ShareImage.write(image, it) }
                        ?.let { SharedFiles.share(context, it, ShareImage.MIME_TYPE, chooser) }
                }
            },
            shareGpx = {
                scope.launch {
                    val gpx = viewModel.gpx(name) ?: return@launch
                    SharedFiles.cache(context, "$file.gpx") { it.writeDocument(gpx) }
                        ?.let { SharedFiles.share(context, it, GPX_MIME, chooser) }
                }
            },
            saveGpx = { runCatching { save.launch("$file.gpx") } },
            deleteRoute = viewModel::deleteRoute,
            openStepCard = onOpenStepCard,
            live = SessionCardActions(
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
                onKeepGoing = viewModel::keepGoing,
                onGps = viewModel::gps,
            ),
        ),
    )
}

/** «passo-2026-11-18-1802»: the outing's day and the minute it began, in the reader's own time. */
internal fun fileName(session: Session, zone: ZoneId = ZoneId.systemDefault()): String {
    val start = Instant.ofEpochMilli(session.startedAtMillis).atZone(zone)
    return "passo-%04d-%02d-%02d-%02d%02d".format(
        Locale.ROOT,
        start.year,
        start.monthValue,
        start.dayOfMonth,
        start.hour,
        start.minute,
    )
}

/**
 * An outing's own page (PLANNING.md §11 Phase 16), for every outing, with GPS or without. In
 * order: the sentence (what it was and how it went against its goal) and when it was; for one
 * with a route, the map; its numbers, each with the line that says what it means and which is
 * a measure and which an estimate; an interval outing's intervals; then, with GPS, its
 * kilometres one by one, the shape of its climb, and what its route can do (share, save,
 * delete). Without GPS, the same page without those parts, and no empty box in their place.
 * Every outing walked since its pauses are kept shows the shape of the walk after the map, or
 * where the map would be: its minutes, and its share of the day (Phase 21.3).
 *
 * @param shareLayer where the image to share is recorded: the route's card, drawn out of sight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutingScreen(
    state: OutingUiState?,
    onBack: () -> Unit,
    actions: OutingActions,
    modifier: Modifier = Modifier,
    messages: SnackbarHostState = remember { SnackbarHostState() },
    shareLayer: GraphicsLayer? = null,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val resources = LocalResources.current
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        snackbarHost = { SnackbarHost(messages) },
        topBar = {
            TopAppBar(
                title = { Text(state?.let { resources.sessionName(it.session) }.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.outing_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
        bottomBar = {
            // While it counts the page is the outing's card made large (Phase 21): its two
            // buttons stay at hand at the page's foot, however far down the reader has scrolled,
            // so a reader who opened it at the park stops it at the car without going back.
            val session = state?.session
            if (session != null && session.live) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    SessionLiveButtons(
                        session = session,
                        actions = actions.live,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(pageGutter(sideInsets = false).contentPadding())
                            .padding(horizontal = ScreenMargin, vertical = 12.dp)
                            .testTag(OutingTags.LIVE_BUTTONS),
                    )
                }
            }
        },
    ) { padding ->
        // Until the outing is read, a bare page: nothing true can be drawn of it yet.
        if (state != null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                val format = rememberMeasureFormatter(state.units)
                // What is shared is an outing that is over: its card, its whole map, its numbers.
                if (shareLayer != null && state.route != null && state.over) {
                    ShareCard(state, state.route, format, shareLayer)
                }
                OutingPage(state, format, actions)
            }
        }
    }
}

@Composable
private fun OutingPage(state: OutingUiState, format: MeasureFormatter, actions: OutingActions) {
    val session = state.session
    val route = state.route
    val resources = LocalResources.current
    val splits = remember(session.legs, format.units) { Splits.of(session.legs, Splits.unitMeters(format.units)) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(OutingTags.PAGE),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "sentence") { Sentence(session, format) }
        // What the outing under way asks of the reader, as its card asks it: the receiver's line
        // with its way out, «Still walking?» before its cap.
        if (session.live) {
            item(key = "notices") {
                val now by produceState(System.currentTimeMillis(), session) { value = System.currentTimeMillis() }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                ) {
                    SessionNotices(session, state.measure?.line, now, actions.live)
                }
            }
        }
        if (route != null) {
            item(key = "map") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                ) {
                    RouteMapView(
                        shape = route.shape,
                        beads = beads(route.shape, splits, format, resources),
                        units = format.units,
                        contentDescription = routeSpoken(route.shape, session, format, resources),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // A dash is an estimate drawn among measures: said wherever there is one.
                    // Why the ground is plain is the guide's to say, once, not every page's.
                    if (route.shape.hasLaid) Note(stringResource(R.string.outing_map_dashes))
                }
            }
        } else if (session.gps && (session.measuredShare ?: 0.0) <= 0.0) {
            // Asked to measure, and no fix: said, where the map would have been. While the
            // outing counts, the satellites may still answer, and the route comes in its place.
            item(key = "no-fix") {
                Note(
                    stringResource(
                        when (session.state) {
                            SessionState.ACTIVE -> R.string.outing_no_fix_yet
                            SessionState.PAUSED -> R.string.outing_no_fix_so_far
                            SessionState.FINISHED -> R.string.outing_no_fix
                        },
                    ),
                    Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        // Its minutes, where its pauses are known: an outing from before would draw a pause as walked.
        val shape = state.shape
        val minutes = shape?.minutes
        if (shape != null && minutes != null) {
            item(key = "shape") { ShapeSection(session, minutes, shape, format) }
        }
        item(key = "numbers") { Numbers(session, route?.altitudes, format) }
        // What this outing measured of the step, once it is over: from its own samples, not from
        // the curve, and absent without one (PLANNING.md §11 Phase 17).
        val step = state.stepAtPace
        if (state.over && session.gps && step != null) {
            item(key = "step") { StepLine(step, state.stepReady, format, actions.openStepCard) }
        }
        if (session.intervals != null && session.splits.any { it.fast }) {
            item(key = "intervals") {
                Group { IntervalBars(session, MaterialTheme.colorScheme.primary) }
            }
        }
        if (splits.isNotEmpty()) {
            item(key = "splits") { SplitsSection(splits, format) }
        }
        val profile = route?.shape?.profile.orEmpty()
        if (profile.size >= MIN_PROFILE_POINTS && session.ascentMeters != null) {
            item(key = "profile") { ProfileSection(profile, session.totals.distanceMeters, format) }
        }
        // What a route can do (share, save, delete) is for an outing over for good: while it
        // counts, or can still be taken back, the line may grow, and its page is to follow it.
        if (route != null && state.over) {
            item(key = "route") { RouteActions(actions, onDelete = { deleting = true }) }
        }
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.outing_delete_route_title)) },
            text = { Text(stringResource(R.string.outing_delete_route_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = false
                        actions.deleteRoute()
                    },
                    modifier = Modifier.testTag(OutingTags.DELETE_CONFIRM),
                ) { Text(stringResource(R.string.outing_delete_route_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.outing_delete_route_cancel)) }
            },
        )
    }
}

/** One sentence before any number: what it came to, then when it was and what it set out to do. */
@Composable
private fun Sentence(session: Session, format: MeasureFormatter) {
    val resources = LocalResources.current
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(session.startedAtMillis).atZone(zone)
    val end = Instant.ofEpochMilli(session.endedAtMillis ?: session.lastStepAtMillis).atZone(zone)
    val lasted = end.toInstant().toEpochMilli() - session.startedAtMillis
    val minutes = (lasted / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
    val date = longDate(start.toLocalDate())
    val range = clockRange(start.hour * MINUTES_PER_HOUR + start.minute, end.hour * MINUTES_PER_HOUR + end.minute)
    // How long it lasted is kept whole: «51 minutes», never «51» at a line's end.
    val lasting = duration(minutes).replace(' ', NO_BREAK)
    // The time paused, where the pauses were kept (Phase 21): «53 minutes, 8 minutes paused».
    val pausedMinutes = (
        session.pausedMillis(
            session.endedAtMillis ?: session.lastEventAtMillis,
        ) / MILLIS_PER_MINUTE
        ).toInt()
    val whenLine = if (pausedMinutes > 0) {
        stringResource(
            R.string.outing_when_paused,
            date,
            range,
            lasting,
            duration(pausedMinutes).replace(' ', NO_BREAK),
        )
    } else {
        stringResource(R.string.outing_when, date, range, lasting)
    }
    // What it set out to do. An open outing set out to do nothing in particular: said only where it
    // had a pace to keep («No goal, at a brisk pace»), which its numbers are read against.
    val aim = when {
        !session.open -> stringResource(R.string.outing_goal, resources.sessionGoalDescription(session, format))
        session.intensity.cadenceFloor != null -> resources.sessionGoalDescription(session, format)
        else -> null
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Text(
            text = resources.sessionHeadline(session, format),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag(OutingTags.SENTENCE).semantics { heading() },
        )
        Text(whenLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        aim?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private class Tile(val icon: ImageVector, val label: String, val value: AnnotatedString, val meaning: String)

/** The outing's numbers, two by two, each with the line that says what it means. */
@Composable
private fun Numbers(session: Session, altitudes: AltitudeSource?, format: MeasureFormatter) {
    val resources = LocalResources.current
    val unit = SpanStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize)
    val tiles = ArrayList<Tile>()
    val distance = format.distance(session.totals.distanceMeters)
    val stepsSaid = session.stepsMeters?.let { format.distance(it).text() }.orEmpty()
    tiles += Tile(
        PassoIcons.Distance,
        stringResource(R.string.outing_distance),
        distance.annotated(unit),
        when (session.distanceKind()) {
            DistanceKind.MEASURED -> stringResource(R.string.outing_distance_measured, stepsSaid)

            // No fix at all: the steps' estimate, said as one. «100% of it from your steps» and
            // «your steps alone said» the same figure would say it twice, under the note that
            // already says why.
            DistanceKind.PARTLY_MEASURED if (session.measuredShare ?: 0.0) <= 0.0 ->
                stringResource(R.string.outing_distance_estimated)

            DistanceKind.PARTLY_MEASURED ->
                stringResource(R.string.outing_distance_partly, format.percent(session.stepsShare() ?: 1.0), stepsSaid)

            DistanceKind.ESTIMATED -> stringResource(R.string.outing_distance_estimated)
        },
    )
    val movingMinutes = (session.totals.movingMillis / MILLIS_PER_MINUTE).toInt()
    tiles += Tile(
        PassoIcons.Clock,
        stringResource(R.string.outing_moving),
        AnnotatedString(duration(movingMinutes)),
        stringResource(R.string.outing_moving_meaning),
    )
    session.pace()?.let {
        tiles += Tile(
            PassoIcons.Gps,
            stringResource(R.string.outing_pace),
            format.pace(it).annotated(unit),
            stringResource(R.string.outing_pace_meaning),
        )
    }
    tiles += Tile(
        PassoIcons.Steps,
        stringResource(R.string.outing_steps),
        AnnotatedString(format.steps(session.totals.steps)),
        stringResource(R.string.outing_steps_meaning),
    )
    if (session.totals.movingMillis >= MILLIS_PER_MINUTE) {
        val cadence = (session.totals.steps * MILLIS_PER_MINUTE.toDouble() / session.totals.movingMillis).roundToInt()
        tiles += Tile(
            PassoIcons.Pulse,
            stringResource(R.string.outing_cadence),
            format.cadence(cadence).annotated(unit),
            stringResource(R.string.outing_cadence_meaning),
        )
    }
    val ascent = session.ascentMeters
    tiles += Tile(
        PassoIcons.Flame,
        stringResource(R.string.outing_energy),
        format.energy(session.totals.activeKcal).annotated(unit),
        stringResource(
            if (ascent != null && ascent > 0) R.string.outing_energy_meaning_climb else R.string.outing_energy_meaning,
        ),
    )
    if (ascent != null) {
        val down = resources.format(format.elevation(session.descentMeters ?: 0.0))
        tiles += Tile(
            PassoIcons.TrendUp,
            stringResource(R.string.outing_climb),
            AnnotatedString(stringResource(R.string.outing_climb_value, resources.format(format.elevation(ascent)))),
            stringResource(
                if (altitudes == AltitudeSource.SATELLITES) {
                    R.string.outing_climb_meaning_satellites
                } else {
                    R.string.outing_climb_meaning
                },
                down,
            ),
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin).testTag(OutingTags.NUMBERS),
    ) {
        tiles.chunked(2).forEach { pair ->
            TilePair {
                pair.forEach { tile ->
                    MetricTile(
                        icon = tile.icon,
                        label = tile.label,
                        value = tile.value,
                        meaning = tile.meaning,
                        spoken = stringResource(R.string.outing_tile_spoken, tile.label, tile.value.text, tile.meaning),
                        fitLabel = true,
                    )
                }
            }
        }
        resources.sessionZone(session, format)?.let { Note(it, Modifier.padding(horizontal = 4.dp)) }
    }
}

/**
 * The shape of the walk (Phase 21.3): its minutes as a strip, taller than on its card, with what a
 * bar is, what a gap is and what the line is where there is one, then its share of its day's goal.
 */
@Composable
private fun ShapeSection(session: Session, minutes: OutingMinutes, shape: OutingShape, format: MeasureFormatter) {
    val resources = LocalResources.current
    val share = resources.dayShareText(session, shape, format)
    Group {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag(OutingTags.SHAPE)) {
            Title(
                stringResource(
                    if (session.intensity == SessionIntensity.RUN) R.string.outing_shape_run else R.string.outing_shape,
                ),
            )
            BoxWithConstraints {
                // What a bar holds is the strip's to decide, at its width: said under it as it is.
                val group = minutes.groupFor(minuteStripBars(maxWidth))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MinuteStrip(
                        minutes = minutes,
                        description = resources.minuteStripSpoken(session, minutes),
                        height = SHAPE_HEIGHT,
                    )
                    val legend = listOfNotNull(
                        resources.getQuantityString(R.plurals.outing_shape_legend, group, group, minutes.floor),
                        stringResource(R.string.outing_shape_gaps).takeIf { minutes.pauses > 0 },
                        stringResource(R.string.outing_shape_goal_line).takeIf { minutes.goalAt != null },
                    ).joinToString(" ")
                    Note(legend)
                }
            }
            share?.let { DayShareLine(it, shape.dayShare(session.totals.steps) ?: 0.0) }
        }
    }
}

@Composable
private fun SplitsSection(splits: List<Split>, format: MeasureFormatter) {
    val resources = LocalResources.current
    val metric = format.units == UnitSystem.METRIC
    val fastest = Splits.fastest(splits)
    val best = fastest?.secondsPerKm
    val rows = splits.map { split ->
        val pace = split.secondsPerKm
        val name = if (split.whole) (split.index + 1).toString() else resources.format(format.distance(split.meters))
        val detail = listOfNotNull(
            split.cadence?.let { resources.format(format.cadence(it)) },
            split.ascentMeters.takeIf { it >= MIN_SPLIT_CLIMB }
                ?.let { stringResource(R.string.outing_split_up, resources.format(format.elevation(it))) },
            split.descentMeters.takeIf { it >= MIN_SPLIT_CLIMB }
                ?.let { stringResource(R.string.outing_split_down, resources.format(format.elevation(it))) },
        ).joinToString(" · ").takeIf { it.isNotEmpty() }
        val said = listOfNotNull(
            pace?.let { resources.paceSpoken(it, format) } ?: stringResource(R.string.outing_split_no_pace),
            detail,
            stringResource(R.string.outing_split_spoken_fastest).takeIf { split == fastest },
        ).joinToString(". ")
        SplitRow(
            name = name,
            pace = pace?.let { resources.format(format.pace(it)) },
            detail = detail,
            // Against the fastest: twice as slow is half as long. Where none is marked, the
            // quickest of what there is.
            share = pace?.let { ((best ?: splits.mapNotNull { s -> s.secondsPerKm }.min()) / it).toFloat() } ?: 0f,
            fastest = split == fastest,
            spoken = when {
                !split.whole -> stringResource(R.string.outing_split_spoken_last, name, said)
                metric -> stringResource(R.string.outing_split_spoken_km, name, said)
                else -> stringResource(R.string.outing_split_spoken_mile, name, said)
            },
        )
    }
    Group {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Title(stringResource(if (metric) R.string.outing_splits_km else R.string.outing_splits_mile))
            SplitList(rows, stringResource(R.string.outing_split_fastest))
            Note(stringResource(R.string.outing_splits_note))
        }
    }
}

@Composable
private fun ProfileSection(profile: List<Pair<Double, Double>>, distanceMeters: Double, format: MeasureFormatter) {
    val resources = LocalResources.current
    val low = resources.format(format.elevation(profile.minOf { it.second }))
    val high = resources.format(format.elevation(profile.maxOf { it.second }))
    Group {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Title(stringResource(R.string.outing_profile))
            ClimbProfile(
                profile = profile,
                lowest = low,
                highest = high,
                // The outing's own distance: the shape spans the walk, whatever its line's own length.
                length = resources.format(format.distance(distanceMeters)),
                description = stringResource(
                    R.string.outing_profile_spoken,
                    resources.format(format.elevation(profile.first().second)),
                    resources.format(format.elevation(profile.last().second)),
                    low,
                    high,
                ),
            )
        }
    }
}

/** What the route can do: share, save, delete. The GPX's choice says what the file holds, once, here. */
@Composable
private fun RouteActions(actions: OutingActions, onDelete: () -> Unit) {
    Group {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Title(stringResource(R.string.outing_route))
            FilledTonalButton(
                onClick = actions.shareImage,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(OutingTags.SHARE_IMAGE),
            ) { Text(stringResource(R.string.outing_share_image)) }
            Note(stringResource(R.string.outing_share_image_note))
            OutlinedButton(
                onClick = actions.shareGpx,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(OutingTags.SHARE_GPX),
            ) { Text(stringResource(R.string.outing_share_gpx)) }
            OutlinedButton(
                onClick = actions.saveGpx,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(OutingTags.SAVE_GPX),
            ) { Text(stringResource(R.string.outing_save_gpx)) }
            Note(stringResource(R.string.outing_gpx_note))
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(OutingTags.DELETE_ROUTE),
            ) { Text(stringResource(R.string.outing_delete_route), color = MaterialTheme.colorScheme.error) }
        }
    }
}

/**
 * The route as an image to share: its name, the sentence, the map as it lies whole, and three
 * numbers, in the card's dress. Laid out at a width of its own and recorded into [layer], never
 * drawn on the page: what the reader sends is this, and nothing of the page around it.
 */
@Composable
private fun ShareCard(state: OutingUiState, route: DrawnRoute, format: MeasureFormatter, layer: GraphicsLayer) {
    val session = state.session
    val resources = LocalResources.current
    val unit = SpanStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            // Measured at its own size and given no room: it is there to be recorded, not seen.
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixedWidth(SHARE_WIDTH.dp.roundToPx()))
                layout(0, 0) { placeable.place(0, 0) }
            }
            .drawWithContent { layer.record { this@drawWithContent.drawContent() } }
            .clearAndSetSemantics { },
    ) {
        Column(Modifier.width(SHARE_WIDTH.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(resources.sessionName(session), style = MaterialTheme.typography.titleMedium)
            Text(resources.sessionHeadline(session, format), style = MaterialTheme.typography.titleLarge)
            RouteMapView(
                shape = route.shape,
                beads = emptyList(),
                units = format.units,
                contentDescription = "",
                modifier = Modifier.fillMaxWidth(),
                still = true,
            )
            val minutes = (session.totals.movingMillis / MILLIS_PER_MINUTE).toInt()
            val pace = session.pace()?.let(format::pace)
            StatStrip(
                stats = listOfNotNull(
                    StripStat(
                        format.distance(session.totals.distanceMeters).annotated(unit),
                        stringResource(R.string.outing_distance),
                    ),
                    StripStat(AnnotatedString(duration(minutes)), stringResource(R.string.outing_moving)),
                    pace?.let { StripStat(AnnotatedString(it.number), resources.unitOf(it)) },
                ),
                minShare = 0.dp,
                valueStyle = PassoTheme.type.readingValue.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                ),
            )
        }
    }
}

/** A part of the page on its own rounded ground, as wide as the page's groups. */
@Composable
private fun Group(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

/**
 * «Your step at this pace», and on the outing whose samples made the learnt step ready, while it
 * waits, the way to it: the moment the reader has just measured, where a card in Settings might
 * never be seen. Never a notification.
 */
@Composable
private fun StepLine(meters: Double, ready: Boolean, format: MeasureFormatter, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = ScreenMargin)) {
        Note(
            stringResource(R.string.outing_step_at_pace, format.stepLength(meters).text()),
            Modifier.padding(horizontal = 4.dp).testTag(OutingTags.STEP),
        )
        if (ready) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(GroupShape)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .testTag(OutingTags.STEP_READY),
            ) {
                Icon(
                    PassoIcons.Gps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.outing_step_ready),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun Note(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** A bead at the end of each whole kilometre, named with its number and its pace. */
private fun beads(shape: RouteShape, splits: List<Split>, format: MeasureFormatter, resources: Resources) = splits
    .filter { it.whole }
    .map { split ->
        val mark = resources.format(format.distance((split.index + 1) * Splits.unitMeters(format.units)))
        val pace = split.secondsPerKm?.let { resources.format(format.pace(it)) }
        RouteBead(
            at = shape.at(split.endedAtMillis),
            label = if (pace == null) mark else resources.getString(R.string.outing_bead, mark, pace),
        )
    }

/** «Your route: 5.20 km, from north to south, 3 stops.» */
private fun routeSpoken(shape: RouteShape, session: Session, format: MeasureFormatter, resources: Resources): String {
    val distance = resources.format(format.distance(session.totals.distanceMeters))
    val heading = shape.heading
    val way = if (heading == null) {
        resources.getString(R.string.outing_route_loop)
    } else {
        resources.getString(
            R.string.outing_route_heading,
            resources.getString(heading.opposite.word),
            resources.getString(heading.word),
        )
    }
    return if (shape.stops == 0) {
        resources.getString(R.string.outing_route_spoken_no_stops, distance, way)
    } else {
        resources.getQuantityString(R.plurals.outing_route_spoken, shape.stops, distance, way, shape.stops)
    }
}

private val Compass.word: Int
    get() = when (this) {
        Compass.NORTH -> R.string.compass_north
        Compass.NORTH_EAST -> R.string.compass_north_east
        Compass.EAST -> R.string.compass_east
        Compass.SOUTH_EAST -> R.string.compass_south_east
        Compass.SOUTH -> R.string.compass_south
        Compass.SOUTH_WEST -> R.string.compass_south_west
        Compass.WEST -> R.string.compass_west
        Compass.NORTH_WEST -> R.string.compass_north_west
    }

private const val GPX_MIME = "application/gpx+xml"
private const val NO_BREAK = '\u00A0'
private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60
private const val SHARE_WIDTH = 360

/** The strip on the page: taller than on a card, where it is the walk's whole section. */
private val SHAPE_HEIGHT = 72.dp

/** Under two known altitudes a profile is a point, not a shape. */
private const val MIN_PROFILE_POINTS = 2

/** A split's climb is said from a metre up: under it, it is the threshold's dust. */
private const val MIN_SPLIT_CLIMB = 1.0

/** Hooks for the UI tests. */
object OutingTags {
    const val PAGE = "outing_page"
    const val LIVE_BUTTONS = "outing_live_buttons"
    const val SENTENCE = "outing_sentence"
    const val NUMBERS = "outing_numbers"
    const val SHAPE = "outing_shape"
    const val SHARE_IMAGE = "outing_share_image"
    const val SHARE_GPX = "outing_share_gpx"
    const val SAVE_GPX = "outing_save_gpx"
    const val DELETE_ROUTE = "outing_delete_route"
    const val DELETE_CONFIRM = "outing_delete_confirm"
    const val STEP = "outing_step"
    const val STEP_READY = "outing_step_ready"
}
