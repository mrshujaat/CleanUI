package com.example.mediabrowser.poison

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mediabrowser.data.local.dao.InteractionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodic cleanup so the interaction history never grows unbounded. Deletes rows
 * older than 30 days. The derived affinity scores are unaffected — they're already
 * aggregated and time-decayed, so pruning raw history is purely housekeeping.
 */
@HiltWorker
class PoisonCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val interactionDao: InteractionDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val cutoff = System.currentTimeMillis() - THIRTY_DAYS_MS
            interactionDao.deleteOlderThan(cutoff)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "poison_cleanup"
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }
}