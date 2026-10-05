package com.callbackdev.passo.feature.ways

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.CityMark
import com.callbackdev.passo.core.designsystem.components.ContinentCity
import com.callbackdev.passo.core.designsystem.components.ContinentMapView
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.ways.wayNameRes
import com.callbackdev.passo.core.domain.ways.Continents
import com.callbackdev.passo.core.model.Continent
import com.callbackdev.passo.core.model.WayId

/** A continent's cities, with the Ways' state from [WaysViewModel] (one for every Ways page). */
@Composable
fun ContinentRoute(
    continent: Continent,
    onBack: () -> Unit,
    onOpenWay: (WayId, Long?) -> Unit,
    viewModel: WaysViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ContinentScreen(state, continent, onBack, onOpenWay)
}

/**
 * A continent's page (docs/adr/0015-city-walks.md, decision 12): its map, with each city where
 * it is and where its walk stands, then the cities, each walk as the Ways page listed it before
 * the cities were grouped. Each opens its walk's page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinentScreen(
    state: WaysUiState?,
    continent: Continent,
    onBack: () -> Unit,
    onOpenWay: (WayId, Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(continentNameRes(continent))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.ways_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the outings are read, a bare page: a map without them would say nothing is walked.
        if (state != null) ContinentList(state, continent, onOpenWay, Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun ContinentList(
    state: WaysUiState,
    continent: Continent,
    onOpenWay: (WayId, Long?) -> Unit,
    modifier: Modifier,
) {
    val format = rememberMeasureFormatter(state.units)
    val walks = state.walksIn(continent)
    LazyColumn(
        modifier = modifier.testTag(WaysTags.CONTINENT_PAGE),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "map") {
            ContinentMapView(
                map = Continents.map(continent),
                cities = continentCities(walks),
                contentDescription = continentMapSpoken(continent, walks),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenMargin)
                    .testTag(WaysTags.CONTINENT_MAP),
            )
        }
        item(key = "cities") {
            SettingsGroup(modifier = Modifier.testTag(WaysTags.CITIES)) {
                walks.forEachIndexed { index, walk ->
                    if (index > 0) GroupDivider()
                    WalkRow(walk, state, format) { onOpenWay(walk.way.id, null) }
                }
            }
        }
        item(key = "footer") { Footer() }
    }
}

/** A continent's name, as the Ways page's row and the continent's page title it. */
@StringRes
internal fun continentNameRes(continent: Continent): Int = when (continent) {
    Continent.EUROPE -> R.string.ways_continent_europe
    Continent.AMERICAS -> R.string.ways_continent_americas
}

/** The cities of [walks] as their continent's map draws them: where each walk begins. */
@Composable
internal fun continentCities(walks: List<WalkView>): List<ContinentCity> {
    val names = walks.map { stringResource(wayNameRes(it.way.id)) }
    val marks = walks.map { it.mark }
    return remember(walks.map { it.way.id }, names, marks) {
        walks.mapIndexed { i, walk ->
            val start = walk.way.stops.first()
            ContinentCity(start.latitude, start.longitude, names[i], marks[i])
        }
    }
}

/** The map in one sentence, for TalkBack: «Europe on the map: 10 cities, 1 under way, 2 walked.» */
@Composable
private fun continentMapSpoken(continent: Continent, walks: List<WalkView>): String {
    val underWay = walks.count { it.mark == CityMark.UNDER_WAY }
    val walked = walks.count { it.mark == CityMark.WALKED }
    val parts = listOfNotNull(
        pluralStringResource(R.plurals.ways_continent_cities, walks.size, walks.size),
        if (underWay > 0) pluralStringResource(R.plurals.ways_continent_under_way, underWay, underWay) else null,
        if (walked > 0) pluralStringResource(R.plurals.ways_continent_walked, walked, walked) else null,
    )
    return stringResource(
        R.string.ways_continent_map,
        stringResource(continentNameRes(continent)),
        parts.joinToString(),
    )
}
