package com.callbackdev.passo.core.model

/** A way, walked over months with the days' distance, or a walk through a city, in outings. */
enum class WayKind {
    WAY,
    WALK,
}

/**
 * The continents the city walks are grouped by, in the order the Ways page lists them
 * (docs/adr/0015-city-walks.md, decision 12). Never stored: a walk's continent is its [WayId]'s.
 * One is added with its first city, never before: the page has no empty group.
 */
enum class Continent {
    EUROPE,
    AMERICAS,
    ASIA_OCEANIA,
    AFRICA,
}

/**
 * The ways and the city walks (PLANNING.md §11 Phase 11), in the order the Ways page lists them:
 * the ways shortest first, then the walks. Stored by name: the order may change, a name may not.
 *
 * @property city for a walk, its city's key: the walks of one city are listed together.
 * @property continent for a walk, the continent its city is listed under.
 */
enum class WayId(val kind: WayKind, val city: String? = null, val continent: Continent? = null) {
    VIA_DEGLI_DEI(WayKind.WAY),
    CAMINO_PORTUGUES(WayKind.WAY),
    VIA_DI_FRANCESCO(WayKind.WAY),
    CAMINO_FRANCES(WayKind.WAY),
    VIA_FRANCIGENA(WayKind.WAY),
    MILAN_DUOMO_NAVIGLI(WayKind.WALK, city = "milan", continent = Continent.EUROPE),
    ROME_COLOSSEUM_VATICAN(WayKind.WALK, city = "rome", continent = Continent.EUROPE),
    PARIS_VOSGES_EIFFEL(WayKind.WALK, city = "paris", continent = Continent.EUROPE),
    LONDON_PALACE_TOWER(WayKind.WALK, city = "london", continent = Continent.EUROPE),
    MADRID_DEBOD_RETIRO(WayKind.WALK, city = "madrid", continent = Continent.EUROPE),
    BERLIN_WALL_VICTORY(WayKind.WALK, city = "berlin", continent = Continent.EUROPE),
    VIENNA_BELVEDERE_PRATER(WayKind.WALK, city = "vienna", continent = Continent.EUROPE),
    PORTO_SE_PILAR(WayKind.WALK, city = "porto", continent = Continent.EUROPE),
    AMSTERDAM_CENTRAAL_WESTERKERK(WayKind.WALK, city = "amsterdam", continent = Continent.EUROPE),
    PRAGUE_CASTLE_WENCESLAS(WayKind.WALK, city = "prague", continent = Continent.EUROPE),
    LIMA_SAN_MARTIN_RESERVA(WayKind.WALK, city = "lima", continent = Continent.AMERICAS),
    CUSCO_ARMAS_QORIKANCHA(WayKind.WALK, city = "cusco", continent = Continent.AMERICAS),
    NEW_YORK_PARK_BRIDGE(WayKind.WALK, city = "new_york", continent = Continent.AMERICAS),
    RIO_CENTRO_SUGARLOAF(WayKind.WALK, city = "rio", continent = Continent.AMERICAS),
    MEXICO_CITY_ZOCALO_CHAPULTEPEC(WayKind.WALK, city = "mexico_city", continent = Continent.AMERICAS),
    BUENOS_AIRES_MAYO_RECOLETA(WayKind.WALK, city = "buenos_aires", continent = Continent.AMERICAS),
    SAN_FRANCISCO_FERRY_PALACE(WayKind.WALK, city = "san_francisco", continent = Continent.AMERICAS),
    QUEBEC_PARLEMENT_BASSE_VILLE(WayKind.WALK, city = "quebec", continent = Continent.AMERICAS),
    HAVANA_CAPITOLIO_PAULA(WayKind.WALK, city = "havana", continent = Continent.AMERICAS),
    CARTAGENA_RELOJ_SAN_FELIPE(WayKind.WALK, city = "cartagena", continent = Continent.AMERICAS),
    TOKYO_SENSOJI_PALACE(WayKind.WALK, city = "tokyo", continent = Continent.ASIA_OCEANIA),
    SYDNEY_LUNA_PARK_GARDEN(WayKind.WALK, city = "sydney", continent = Continent.ASIA_OCEANIA),
    SEOUL_GWANGHWAMUN_NAMSAN(WayKind.WALK, city = "seoul", continent = Continent.ASIA_OCEANIA),
    BEIJING_TIANANMEN_YONGHE(WayKind.WALK, city = "beijing", continent = Continent.ASIA_OCEANIA),
    HONG_KONG_VICTORIA_WESTERN(WayKind.WALK, city = "hong_kong", continent = Continent.ASIA_OCEANIA),
    SINGAPORE_CHINATOWN_GARDENS(WayKind.WALK, city = "singapore", continent = Continent.ASIA_OCEANIA),
    BANGKOK_SWING_ARUN(WayKind.WALK, city = "bangkok", continent = Continent.ASIA_OCEANIA),
    KYOTO_KIYOMIZU_NISHIKI(WayKind.WALK, city = "kyoto", continent = Continent.ASIA_OCEANIA),
    HANOI_VAN_MIEU_LONG_BIEN(WayKind.WALK, city = "hanoi", continent = Continent.ASIA_OCEANIA),
    MELBOURNE_FLINDERS_EXHIBITION(WayKind.WALK, city = "melbourne", continent = Continent.ASIA_OCEANIA),
    CAIRO_MUSEUM_CITADEL(WayKind.WALK, city = "cairo", continent = Continent.AFRICA),
    CAPE_TOWN_LIGHTHOUSE_BO_KAAP(WayKind.WALK, city = "cape_town", continent = Continent.AFRICA),
    MARRAKECH_MAJORELLE_SI_SAID(WayKind.WALK, city = "marrakech", continent = Continent.AFRICA),
    TUNIS_CLOCK_BELVEDERE(WayKind.WALK, city = "tunis", continent = Continent.AFRICA),
    ALEXANDRIA_SHOQAFA_QAITBAY(WayKind.WALK, city = "alexandria", continent = Continent.AFRICA),
    DAKAR_MUSEUM_UNIVERSITY(WayKind.WALK, city = "dakar", continent = Continent.AFRICA),
    ADDIS_ABABA_MESKEL_TAITU(WayKind.WALK, city = "addis_ababa", continent = Continent.AFRICA),
    FEZ_PALACE_ANDALUSIANS(WayKind.WALK, city = "fez", continent = Continent.AFRICA),
}

/** Where a way the reader started stands. Stored by name. */
enum class WayJourneyState {
    /** Under way: the days since [WayJourney.startEpochDay] move the reader along it. */
    ACTIVE,

    /** Walked to its end, on [WayJourney.endedEpochDay]: the days after it no longer count. */
    FINISHED,

    /** Put down by the reader on [WayJourney.endedEpochDay], where it was then. */
    LEFT,
}

/**
 * A way the reader started (`way_journey`). It keeps no distance of its own: what was walked
 * is the sum of the days' own estimates from [startEpochDay], so a way can never disagree with
 * History (PLANNING.md §11 Phase 11).
 *
 * @property toldMeters how far along the way the stages have been told: a stage at or before
 *   it is never notified again. At a start in the past it is set to where the reader already
 *   stands, so the stages behind them are stamped, not announced.
 */
data class WayJourney(
    val id: Long,
    val way: WayId,
    val startEpochDay: Long,
    val startedAtMillis: Long,
    val state: WayJourneyState,
    val endedEpochDay: Long?,
    val toldMeters: Int,
)
