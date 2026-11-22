package com.erthi.amanah.data.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.erthi.amanah.data.db.dao.RestoreJournalDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Runs at app startup via WorkManager. If a previous restore was interrupted after the DB
 * transaction committed (journal present in Room), completes the pending file moves idempotently.
 *
 * Failure classification:
 *   - IO error during copy/rename  → retriable (Result.retry): file data still in staged location
 *   - staged missing + dest missing → unrecoverable data loss (Result.failure): nothing to recover
 *   - staged missing + dest present → already moved (success for that file)
 */
@HiltWorker
class RestoreRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val restoreJournalDao: RestoreJournalDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val journal = restoreJournalDao.getPendingJournal() ?: return Result.success()

        // Clean up stale .tmp files from previous failed attempts before retrying.
        val stagingDir = File(journal.stagingDirPath)
        stagingDir.listFiles { f -> f.name.endsWith(".tmp") }?.forEach { it.delete() }

        return try {
            val filesArr = JSONArray(journal.stagedJson)
            var hasRetriable = false
            var hasUnrecoverable = false

            for (i in 0 until filesArr.length()) {
                val entry = filesArr.getJSONObject(i)
                val staged = File(entry.getString("staged"))
                val final = File(entry.getString("final"))
                // expectedSize added in journal v2; 0 means legacy journal (skip size check).
                val expectedSize = entry.optLong("expectedSize", 0L)

                when {
                    staged.exists() -> {
                        // Staged file present — complete the move atomically via tmp.
                        final.parentFile?.mkdirs()
                        val tmpDest = File(final.parentFile, "${final.name}.tmp")
                        try {
                            streamCopy(staged, tmpDest)
                            val copiedOk = if (expectedSize > 0) {
                                tmpDest.length() == expectedSize
                            } else {
                                tmpDest.length() == staged.length()
                            }
                            if (!copiedOk) {
                                tmpDest.delete()
                                hasRetriable = true
                                continue
                            }
                            // Single atomic rename attempt; if it fails (cross-fs), leave tmp
                            // and retry next launch — do NOT delete staged yet.
                            if (tmpDest.renameTo(final)) {
                                staged.delete()
                            } else {
                                tmpDest.delete()
                                hasRetriable = true
                            }
                        } catch (_: Exception) {
                            tmpDest.delete()
                            hasRetriable = true
                        }
                    }
                    final.exists() -> {
                        // Destination present — verify size matches expectation before accepting.
                        val sizeOk = expectedSize <= 0 || final.length() == expectedSize
                        if (!sizeOk) {
                            // Destination exists but size is wrong — data may be corrupt; retry.
                            hasRetriable = true
                        }
                        // Otherwise already correctly moved in a previous attempt.
                    }
                    else -> {
                        // Neither staged nor destination exists — data is gone, cannot recover.
                        hasUnrecoverable = true
                    }
                }
            }

            when {
                hasUnrecoverable -> {
                    // Data is permanently lost for at least one file; clear the journal so we
                    // don't loop forever, persist a durable failure marker for the UI to surface,
                    // and report failure so WorkManager also records it.
                    runCatching { stagingDir.deleteRecursively() }
                    restoreJournalDao.deleteJournal(journal.sessionId)
                    prefs().edit()
                        .putLong(PREF_KEY_LOSS_AT, System.currentTimeMillis())
                        .putString(PREF_KEY_LOSS_SESSION, journal.sessionId)
                        .apply()
                    Result.failure()
                }
                hasRetriable -> Result.retry()
                else -> {
                    runCatching { stagingDir.deleteRecursively() }
                    restoreJournalDao.deleteJournal(journal.sessionId)
                    Result.success()
                }
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun streamCopy(src: File, dst: File) {
        val buf = ByteArray(COPY_BUFFER_SIZE)
        FileInputStream(src).use { ins ->
            FileOutputStream(dst).use { out ->
                var n: Int
                while (ins.read(buf).also { n = it } != -1) {
                    out.write(buf, 0, n)
                }
                out.flush()
            }
        }
    }

    private fun prefs(): SharedPreferences =
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val COPY_BUFFER_SIZE = 256 * 1024
        const val PREFS_NAME = "erthi_recovery_state"
        const val PREF_KEY_LOSS_AT = "data_loss_at_ms"
        const val PREF_KEY_LOSS_SESSION = "data_loss_session_id"
    }
}
