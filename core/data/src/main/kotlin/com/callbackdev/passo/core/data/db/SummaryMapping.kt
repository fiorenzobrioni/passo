package com.callbackdev.passo.core.data.db

import com.callbackdev.passo.core.model.DailySummary

internal fun DailySummaryEntity.toModel() = DailySummary(
    localEpochDay = localEpochDay,
    steps = steps,
    distanceMeters = distanceMeters,
    activeKcal = activeKcal,
    activeMinutes = activeMinutes,
    briskMinutes = briskMinutes,
    goalSteps = goalSteps,
    finalized = finalized,
)

internal fun DailySummary.toEntity() = DailySummaryEntity(
    localEpochDay = localEpochDay,
    steps = steps,
    distanceMeters = distanceMeters,
    activeKcal = activeKcal,
    activeMinutes = activeMinutes,
    briskMinutes = briskMinutes,
    goalSteps = goalSteps,
    finalized = finalized,
)
