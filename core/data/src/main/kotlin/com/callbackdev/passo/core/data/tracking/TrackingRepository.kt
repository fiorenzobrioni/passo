package com.callbackdev.passo.core.data.tracking

import com.callbackdev.passo.core.data.db.DiagnosticsEventEntity
import com.callbackdev.passo.core.data.db.MinuteStepsEntity
import com.callbackdev.passo.core.data.db.TrackerStateEntity
import com.callbackdev.passo.core.data.db.TrackingDao
import com.callbackdev.passo.core.domain.tracking.DEFAULT_GOAL_STEPS
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.domain.tracking.TrackingConstants
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.TrackerState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** The step data as the tracking engine and the screens see it. */
@Singleton
class TrackingRepository
@Inject
constructor(private val dao: TrackingDao) {
    suspend fun trackerState(): TrackerState? = dao.trackerState()?.let {
        TrackerState(
            bootCount = it.bootCount,
            lastCounterValue = it.lastCounterValue,
            lastSampleElapsedNanos = it.lastSampleElapsedNanos,
            lastSampleWallMillis = it.lastSampleWallMillis,
        )
    }

    /** Writes a batch from the ledger in one transaction (PLANNING.md §4.5). */
    suspend fun persist(batch: LedgerBatch, nowWallMillis: Long) {
        dao.writeBatch(
            increments = batch.increments.map { MinuteStepsEntity(it.epochMinute, it.localEpochDay, it.steps) },
            state = batch.state?.let {
                TrackerStateEntity(
                    bootCount = it.bootCount,
                    lastCounterValue = it.lastCounterValue,
                    lastSampleElapsedNanos = it.lastSampleElapsedNanos,
                    lastSampleWallMillis = it.lastSampleWallMillis,
                    updatedAtMillis = nowWallMillis,
                )
            },
            diagnostics = batch.diagnostics.map {
                DiagnosticsEventEntity(wallMillis = it.wallMillis, type = it.type.name, detail = it.detail)
            },
            goalSteps = DEFAULT_GOAL_STEPS,
            diagnosticsKept = TrackingConstants.DIAGNOSTICS_LOG_SIZE,
        )
    }

    suspend fun stepsOn(localEpochDay: Long): Int = dao.stepsOn(localEpochDay)

    fun observeStepsOn(localEpochDay: Long): Flow<Int> = dao.observeStepsOn(localEpochDay)

    /** The tracking log, oldest first. Unknown types (from a newer build) are skipped. */
    suspend fun diagnostics(): List<DiagnosticsEvent> = dao.diagnostics().mapNotNull { row ->
        DiagnosticsType.entries.firstOrNull {
            it.name == row.type
        }?.let { DiagnosticsEvent(row.wallMillis, it, row.detail) }
    }
}
