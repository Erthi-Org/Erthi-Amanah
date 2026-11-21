package com.erthi.amanah.data.worker

import android.content.Context
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

                when {
                    staged.exists() -> {
                        // Staged file present — complete the move atomically via tmp.
                        final.parentFile?.mkdirs()
                        val tmpDest = File(final.parentFile, "${final.name}.tmp")
                        try {
                            streamCopy(staged, tmpDest)
                            if (tmpDest.length() != staged.length()) {
                                // Copy truncated — discard and retry next time.
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
                    final.exists() && final.length() > 0 -> {
                        // Already moved in a previous attempt — nothing to do.
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
                    // don't loop forever, and report failure so WorkManager surfaces it.
                    runCatching { stagingDir.deleteRecursively() }
                    restoreJournalDao.deleteJournal(journal.sessionId)
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

    private companion object {
        const val COPY_BUFFER_SIZE = 256 * 1024
    }
}
