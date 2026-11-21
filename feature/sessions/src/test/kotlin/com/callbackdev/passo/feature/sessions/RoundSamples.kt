package com.callbackdev.passo.feature.sessions

import com.callbackdev.passo.core.domain.rounds.KeptRound
import com.callbackdev.passo.core.domain.rounds.RoundTimes
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.model.Round
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.UnitPreference
import java.time.LocalDate

/**
 * Usual rounds for the pages' tests and the README's picture: the brisk hour through Milan of
 * [OutingSamples], walked again and again since September, each time a little quicker of late.
 */
internal object RoundSamples {
    private const val WEEK = 7 * 86_400_000L

    /** The walks before the sample outing: weeks apart, their lengths and times a walk's own. */
    private val before = listOf(
        Triple(9, 4_310.0, 55.0),
        Triple(8, 4_240.0, 54.0),
        Triple(7, 4_290.0, 54.5),
        Triple(5, 4_220.0, 53.0),
        Triple(3, 4_260.0, 50.5),
        Triple(2, 4_250.0, 50.0),
        Triple(1, 4_300.0, 49.5),
    )

    /** The round's walks, oldest first, the sample outing the last of [count] (6: the 6th time). */
    fun walks(count: Int = before.size + 1, outing: Session = OutingSamples.measured): List<Session> {
        val earlier = before.takeLast(count - 1).mapIndexed { index, (weeks, meters, minutes) ->
            outing.copy(
                id = 100L + index,
                startedAtMillis = outing.startedAtMillis - weeks * WEEK,
                localEpochDay = outing.localEpochDay - weeks * 7,
                totals = outing.totals.copy(distanceMeters = meters, movingMillis = (minutes * 60_000).toLong()),
            )
        }
        return earlier + outing
    }

    fun round(
        walks: List<Session> = walks(),
        name: String? = null,
        route: RouteKey? = RouteKey(walks.last().id, walks.last().startedAtMillis, 1_400),
    ): KeptRound = KeptRound(
        round = Round(3, name, walks.map { it.id }.toSet()),
        walks = walks,
        usual = checkNotNull(RoundTimes.usual(walks)),
        route = route,
        running = walks.all { it.intensity == SessionIntensity.RUN },
    )

    /** The sample outing's page, as the [count]th walk of its round. */
    fun outingState(count: Int = 8, over: Boolean = true): OutingUiState {
        val walks = walks(count)
        val round = round(walks)
        val outing = walks.last()
        return OutingSamples.state(outing).copy(
            over = over,
            round = OutingRound(round, RoundTimes.comparison(outing, walks)),
        )
    }

    /** «Your rounds»: the city walk, and the park's loop by the river, walked three times. */
    fun list(): RoundsUiState {
        val city = round(name = "Navigli")
        val park = round(
            walks = walks(3).mapIndexed { index, it ->
                it.copy(
                    id = 200L + index,
                    startedAtMillis = it.startedAtMillis - 3 * WEEK,
                    localEpochDay = it.localEpochDay - 21,
                    totals = it.totals.copy(
                        distanceMeters = 3_180.0 + index * 20,
                        movingMillis =
                        2_340_000L - index * 30_000,
                    ),
                )
            },
            route = null,
        ).let { it.copy(round = it.round.copy(id = 4)) }
        return RoundsUiState(listOf(city, park), UnitPreference.METRIC, LocalDate.of(2026, 11, 21))
    }

    /** A round's page, with its map. */
    fun page(name: String? = null, map: Boolean = true): RoundUiState = RoundUiState(
        round = round(name = name),
        map = if (map) RouteShape.of(OutingSamples.points()) else null,
        units = UnitPreference.METRIC,
        today = LocalDate.of(2026, 11, 21),
    )
}
