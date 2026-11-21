package com.erthi.amanah.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.erthi.amanah.data.db.dao.RestoreJournalDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONArray

/**
 * Runs at app startup via WorkManager. If a previous restore was interrupted after the DB
 * transaction committed (journal present in Room), completes the pending file moves idempotently.
 */
@HiltWorker
class RestoreRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val restoreJournalDao: RestoreJournalDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val journal = restoreJournalDao.getPendingJournal() ?: return Result.success()
        try {
            val filesArr = JSONArray(journal.stagedJson)
            for (i in 0 until filesArr.length()) {
                val entry = filesArr.getJSONObject(i)
                val staged = java.io.File(entry.getString("staged"))
                val final = java.io.File(entry.getString("final"))
                if (!staged.exists()) continue       // already moved on a prior attempt
                if (final.exists()) { staged.delete(); continue }
                final.parentFile?.mkdirs()
                if (!staged.renameTo(final)) {
                    runCatching { final.writeBytes(staged.readBytes()); staged.delete() }
                }
            }
            java.io.File(journal.stagingDirPath).deleteRecursively()
            restoreJournalDao.deleteJournal(journal.sessionId)
        } catch (_: Exception) {
            // Don't delete the journal on failure — retry on next launch
            return Result.retry()
        }
        return Result.success()
    }
}
