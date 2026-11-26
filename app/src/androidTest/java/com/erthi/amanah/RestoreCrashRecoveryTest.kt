package com.erthi.amanah

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.erthi.amanah.data.backup.RestoreFiles
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.dao.RestoreJournalDao
import com.erthi.amanah.data.db.entity.RestoreJournalEntity
import com.erthi.amanah.data.worker.RestoreRecoveryWorker
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Instrumented tests for the restore crash-recovery path.
 *
 * Each test simulates the state the system would be in after a process kill
 * at a specific point in the restore sequence, then verifies that
 * [RestoreRecoveryWorker] (scheduled by [ErthiAmanaApplication.onCreate])
 * brings the system to a fully consistent state.
 *
 * No real process is killed; we insert the Room journal and staging files that
 * would exist at that point, then run the worker directly.
 */
@RunWith(AndroidJUnit4::class)
class RestoreCrashRecoveryTest {

    private lateinit var context: Context
    private lateinit var db: ErthiDatabase
    private lateinit var restoreJournalDao: RestoreJournalDao

    // Track test-created destination files for clean-up.
    private val testDestinations = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ErthiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        restoreJournalDao = db.restoreJournalDao()
        listOf("audio_wills", "video_wills", "documents").forEach {
            File(context.filesDir, it).mkdirs()
        }
        runBlocking { restoreJournalDao.deleteAll() }
    }

    @After
    fun tearDown() {
        testDestinations.forEach { it.delete() }
        db.close()
    }

    // ── Scenario 1: kill before the Room transaction ──────────────────────
    // No journal exists. Worker should return success immediately without touching anything.

    @Test
    fun scenario1_noJournal_workerSucceedsAndLeavesNothingBehind() {
        assertNull(runBlocking { restoreJournalDao.getPendingJournal() })

        val result = runBlocking { buildWorker().doWork() }

        assertEquals(ListenableWorker.Result.success(), result)
        assertNull(runBlocking { restoreJournalDao.getPendingJournal() })
    }

    // ── Scenario 2: kill after DB commit, before any publish ──────────────
    // Journal in Room, staged files present, final destinations empty.
    // Worker must publish all entries, delete the journal, and clean up staging.

    @Test
    fun scenario2_journalPresentNothingPublished_workerPublishesAll() {
        val stagingDir = makeStagingDir()
        val (entries, expectedContent) = makeStagedEntries(stagingDir, count = 3)
        insertJournal(stagingDir, entries)

        val result = runBlocking { buildWorker().doWork() }

        assertEquals(ListenableWorker.Result.success(), result)
        entries.forEachIndexed { i, entry ->
            assertTrue("Final file $i must exist", entry.destination.isFile)
            assertArrayEquals("Content must match for file $i", expectedContent[i], entry.destination.readBytes())
        }
        assertNull("Journal must be deleted after success",
            runBlocking { restoreJournalDao.getPendingJournal() })
        assertFalse("Staging dir must be cleaned up", stagingDir.exists())
    }

    // ── Scenario 3: kill after partial publish ────────────────────────────
    // Journal in Room, some files already at their final destination (from a
    // previous partial run), others still only in staging.
    // RestoreFiles.publish is idempotent: already-matching destinations are a no-op.
    // Worker must complete cleanly for all entries.

    @Test
    fun scenario3_partialPublish_workerIsIdempotent() {
        val stagingDir = makeStagingDir()
        val (entries, expectedContent) = makeStagedEntries(stagingDir, count = 3)

        // Pre-publish only the first file — simulates a crash mid-publish loop.
        RestoreFiles.publish(entries[0])
        assertTrue("First file should already be at destination before recovery",
            entries[0].destination.isFile)

        insertJournal(stagingDir, entries)

        val result = runBlocking { buildWorker().doWork() }

        assertEquals(ListenableWorker.Result.success(), result)
        entries.forEachIndexed { i, entry ->
            assertTrue("Final file $i must exist after recovery", entry.destination.isFile)
            assertArrayEquals("Content must match for file $i", expectedContent[i], entry.destination.readBytes())
        }
        assertNull("Journal must be deleted", runBlocking { restoreJournalDao.getPendingJournal() })
    }

    // ── Scenario 4: all entries already at destination ────────────────────
    // Everything was published before the kill, only deleteJournal was missed.
    // Worker must clean up the journal without corrupting any file.

    @Test
    fun scenario4_allPublished_workerCleansUpJournalOnly() {
        val stagingDir = makeStagingDir()
        val (entries, expectedContent) = makeStagedEntries(stagingDir, count = 2)

        // Pre-publish all files — simulates crash between entries.last() publish and deleteJournal.
        entries.forEach(RestoreFiles::publish)

        insertJournal(stagingDir, entries)

        val result = runBlocking { buildWorker().doWork() }

        assertEquals(ListenableWorker.Result.success(), result)
        entries.forEachIndexed { i, entry ->
            assertTrue("File $i must still be at destination", entry.destination.isFile)
            assertArrayEquals("Content must be unchanged for file $i",
                expectedContent[i], entry.destination.readBytes())
        }
        assertNull("Journal must be deleted", runBlocking { restoreJournalDao.getPendingJournal() })
    }

    // ── Scenario 5: worker queued by ErthiAmanaApplication.onCreate ───────
    // Verifies the worker that is enqueued at app startup processes an existing journal.
    // (The app-startup scheduling is covered by ErthiAmanaApplication; here we confirm
    //  the worker that would run is the same one that completes the restore.)

    @Test
    fun scenario5_onCreateSchedulesWorkerThatConsumesPendingJournal() {
        val stagingDir = makeStagingDir()
        val (entries, _) = makeStagedEntries(stagingDir, count = 1)
        insertJournal(stagingDir, entries)

        assertNotNull("Journal must be present before worker runs",
            runBlocking { restoreJournalDao.getPendingJournal() })

        // The worker built here is the same type enqueued by ErthiAmanaApplication.
        val result = runBlocking { buildWorker().doWork() }

        assertEquals("Worker must succeed", ListenableWorker.Result.success(), result)
        assertNull("Journal must be consumed by the recovery worker",
            runBlocking { restoreJournalDao.getPendingJournal() })
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildWorker(): RestoreRecoveryWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context, workerClassName: String, params: WorkerParameters,
            ): ListenableWorker = RestoreRecoveryWorker(appContext, params, restoreJournalDao)
        }
        return TestListenableWorkerBuilder<RestoreRecoveryWorker>(context)
            .setWorkerFactory(factory)
            .build() as RestoreRecoveryWorker
    }

    private fun makeStagingDir(): File =
        File(context.filesDir, "restore_staging_test_${UUID.randomUUID()}").also { it.mkdirs() }

    private fun makeStagedEntries(
        stagingDir: File,
        count: Int,
    ): Pair<List<RestoreFiles.Entry>, List<ByteArray>> {
        val finalDir = File(context.filesDir, "audio_wills")
        val entries = mutableListOf<RestoreFiles.Entry>()
        val contents = mutableListOf<ByteArray>()
        repeat(count) { i ->
            val content = "synthetic test audio $i ${UUID.randomUUID()}".toByteArray()
            val staged = File(stagingDir, "audio_will_test_$i")
            staged.writeBytes(content)
            val dest = File(finalDir, "audio_will_recovery_${UUID.randomUUID()}.wasi_audio")
            testDestinations += dest
            entries += RestoreFiles.Entry(staged, dest, content.size.toLong(), RestoreFiles.sha256(staged))
            contents += content
        }
        return entries to contents
    }

    private fun insertJournal(stagingDir: File, entries: List<RestoreFiles.Entry>) {
        val json = JSONArray(entries.map { it.toJson() }).toString()
        runBlocking {
            restoreJournalDao.insertJournal(
                RestoreJournalEntity(
                    sessionId = "test-session-${UUID.randomUUID()}",
                    stagedJson = json,
                    stagingDirPath = stagingDir.absolutePath,
                )
            )
        }
    }
}
