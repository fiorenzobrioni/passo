package com.callbackdev.passo.core.domain.metrics

/**
 * Every constant the estimates are made of (PLANNING.md §6), in one place, each with where it
 * comes from. They are defaults to be validated in the field, and the screens call what they
 * produce estimates: distance, calories and active time are never presented as measurements.
 */
object MetricsConstants {
    // --- Step length ----------------------------------------------------------------------

    /**
     * Walking step length as a share of height, the rule of thumb pedometer makers and the
     * American College of Sports Medicine's walking programs have long used: 0.415 × height
     * for men, 0.413 for women. Studies of adults walking at a self-selected pace find step
     * length between about 0.41 and 0.45 of height, so the rule sits at the conservative end.
     * With no sex given, the men's ratio (PLANNING.md §6).
     */
    const val STEP_LENGTH_HEIGHT_RATIO: Double = 0.415
    const val STEP_LENGTH_HEIGHT_RATIO_FEMALE: Double = 0.413

    /**
     * The walking step length when the height is not known: 0.415 × 1.69 m, about the mean
     * height of European adults of both sexes (NCD-RisC, eLife 2016).
     */
    const val DEFAULT_WALKING_STEP_LENGTH_M: Double = 0.70

    /**
     * Running steps are longer: at the walk-to-run transition step length grows by roughly a
     * quarter to a third over a brisk walk. 1.3 until the reader sets their own (PLANNING.md §6).
     */
    const val RUNNING_STEP_LENGTH_FACTOR: Double = 1.3

    // --- Cadence --------------------------------------------------------------------------

    /**
     * A minute with at least this many steps counts as an active minute: sustained stepping
     * for a good part of it, rather than a few steps around the house (VISION.md glossary).
     */
    const val ACTIVE_MINUTE_THRESHOLD: Int = 40

    /**
     * 100 steps per minute: the cadence that marks moderate-intensity walking (about 3 METs)
     * in adults. Tudor-Locke et al., "Walking cadence (steps/min) and intensity in 21-40 year
     * olds: CADENCE-adults", Int J Behav Nutr Phys Act 2019.
     */
    const val BRISK_MINUTE_THRESHOLD: Int = 100

    /**
     * From 140 steps per minute a minute is taken as running: above the cadence of even a
     * very fast walk (about 130 spm marks vigorous walking in the same CADENCE-adults work).
     * Running minutes use the running step length and the running energy cost.
     */
    const val RUNNING_CADENCE: Int = 140

    /**
     * 130 steps per minute marks vigorous walking (about 6 METs) in the same CADENCE-adults
     * study. Used only to put the average cadence in words.
     */
    const val VIGOROUS_CADENCE: Int = 130

    // --- Guidelines -----------------------------------------------------------------------

    /**
     * The weekly moderate-intensity activity the WHO recommends to adults: 150 to 300
     * minutes (WHO guidelines on physical activity and sedentary behaviour, 2020). Since
     * 2020 every minute counts, not only bouts of ten, so brisk minutes (moderate-intensity
     * walking, [BRISK_MINUTE_THRESHOLD]) add up to it directly.
     */
    const val WHO_WEEKLY_MODERATE_MINUTES: Int = 150

    /** A day's share of [WHO_WEEKLY_MODERATE_MINUTES], rounded up: 150 / 7 = 21.4 → 22. */
    const val DAILY_BRISK_SHARE_MINUTES: Int = (WHO_WEEKLY_MODERATE_MINUTES + 6) / 7

    // --- Typical day (PLANNING.md §6.2) ---------------------------------------------------

    /** How many past weeks of the same weekday make the typical day. */
    const val TYPICAL_DAY_WEEKS: Int = 4

    /**
     * A day under this many steps is left out of the typical day: the phone stayed at home,
     * or tracking was paused, and averaging it in would drag "usual" towards a day that did
     * not happen.
     */
    const val TYPICAL_DAY_MIN_STEPS: Int = 500

    /** With fewer valid days than this there is no "usual" to speak of. */
    const val TYPICAL_DAY_MIN_VALID_DAYS: Int = 2

    /** The typical day's resolution: 15-minute slots, 96 of them. */
    const val TYPICAL_DAY_SLOT_MINUTES: Int = 15

    // --- Energy ---------------------------------------------------------------------------

    /**
     * The body weight used when the reader has not given theirs: the 70 kg reference adult
     * of physiology (ICRP Publication 23).
     */
    const val DEFAULT_WEIGHT_KG: Double = 70.0

    /**
     * Net energy of walking per kilogram per kilometre. The ACSM metabolic equation for
     * walking prices the horizontal component at 0.1 mL O2 per kg per metre; at about
     * 5 kcal per litre of oxygen that is 0.5 kcal/kg/km. "Net": resting metabolism is not
     * included, which is why the screens say "active calories".
     * ACSM's Guidelines for Exercise Testing and Prescription, metabolic equations.
     */
    const val WALK_KCAL_PER_KG_KM: Double = 0.5

    /**
     * The cost per kilometre of walking grows again above the most economical speed (about
     * 1.3 m/s), so it rises linearly from [WALK_KCAL_PER_KG_KM] at [BRISK_MINUTE_THRESHOLD]
     * to this value just below [RUNNING_CADENCE]. About 20% more at a very fast walk, in
     * line with the classic energy-speed curves (Ralston 1958; Browning et al. 2006).
     */
    const val FAST_WALK_KCAL_PER_KG_KM: Double = 0.6

    /**
     * Net energy of running per kilogram per kilometre: the ACSM running equation's
     * 0.2 mL O2 per kg per metre, about 1 kcal/kg/km, nearly independent of speed.
     */
    const val RUN_KCAL_PER_KG_KM: Double = 1.0
}
