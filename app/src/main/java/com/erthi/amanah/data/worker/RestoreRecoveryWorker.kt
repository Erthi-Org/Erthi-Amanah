package com.erthi.amanah.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.erthi.amanah.data.backup.RestoreFiles
import com.erthi.amanah.data.db.dao.RestoreJournalDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class RestoreRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val restoreJournalDao: RestoreJournalDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        RestoreFiles.mutex.withLock {
            val journal = restoreJournalDao.getPendingJournal() ?: return@withLock Result.success()
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            try {
                RestoreFiles.entries(journal.stagedJson).forEach(RestoreFiles::publish)
                restoreJournalDao.deleteJournal(journal.sessionId)
                File(journal.stagingDirPath).deleteRecursively()
                prefs.edit().remove(PREF_KEY_LOSS_AT).remove(PREF_KEY_LOSS_SESSION).apply()
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    // Preserve all staged copies and journal references for a later recovery.
                    prefs.edit().putLong(PREF_KEY_LOSS_AT, System.currentTimeMillis())
                        .putString(PREF_KEY_LOSS_SESSION, journal.sessionId).commit()
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val PREFS_NAME = "erthi_recovery_state"
        const val PREF_KEY_LOSS_AT = "data_loss_at_ms"
        const val PREF_KEY_LOSS_SESSION = "data_loss_session_id"
    }
}
