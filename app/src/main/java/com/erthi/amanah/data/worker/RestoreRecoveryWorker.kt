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
 * Returns Result.retry() on any recoverable failure so WorkManager re-attempts on next launch.
 */
@HiltWorker
class RestoreRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val restoreJournalDao: RestoreJournalDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val journal = restoreJournalDao.getPendingJournal() ?: return Result.success()
        return try {
            val filesArr = JSONArray(journal.stagedJson)
            val incomplete = mutableListOf<String>()

            for (i in 0 until filesArr.length()) {
                val entry = filesArr.getJSONObject(i)
                val staged = File(entry.getString("staged"))
                val final = File(entry.getString("final"))

                when {
                    staged.exists() -> {
                        // Staged file present — complete the move
                        final.parentFile?.mkdirs()
                        val tmpDest = File(final.parentFile, "${final.name}.tmp")
                        try {
                            streamCopy(staged, tmpDest)
                            if (!tmpDest.renameTo(final)) {
                                // rename may fail cross-filesystem; tmpDest is already the data
                                if (final.exists()) final.delete()
                                if (!tmpDest.renameTo(final)) tmpDest.renameTo(final).also {
                                    if (!it) incomplete += final.name
                                }
                            }
                            if (final.exists() && final.length() == staged.length()) {
                                staged.delete()
                            } else {
                                incomplete += final.name
                            }
                        } catch (e: Exception) {
                            tmpDest.delete()
                            incomplete += final.name
                        }
                    }
                    final.exists() && final.length() > 0 -> {
                        // Staged already moved in a previous attempt — nothing to do
                    }
                    else -> {
                        // Neither staged nor destination exists — data is gone
                        incomplete += final.name
                    }
                }
            }

            // Clean up staging dir only after all moves are verified
            if (incomplete.isEmpty()) {
                runCatching { File(journal.stagingDirPath).deleteRecursively() }
                restoreJournalDao.deleteJournal(journal.sessionId)
                Result.success()
            } else {
                // Keep journal so the next retry can attempt the remaining moves
                Result.retry()
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
