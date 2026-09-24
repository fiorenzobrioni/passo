package com.callbackdev.passo.core.domain.tracking

/**
 * The goal written into a day's summary until the goal setting exists (Phase 2/3), where the
 * user's own replaces it. Provisional: past days are not finalized before Phase 2, so the value
 * written now never becomes history.
 *
 * 8 000: where the mortality benefit of daily steps starts to level off in adults under 60
 * (8 000 to 10 000), and where it already has over 60 (6 000 to 8 000). Paluch et al., Lancet
 * Public Health 2022, a meta-analysis of 15 cohorts.
 */
const val DEFAULT_GOAL_STEPS: Int = 8_000
