package com.erthi.amanah

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.erthi.amanah.data.backup.DriveBackupManager
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.entity.WillEntity
import com.erthi.amanah.data.security.CryptoManager
import com.erthi.amanah.data.security.VaultCrypto
import com.erthi.amanah.ui.viewmodel.BackupState
import com.erthi.amanah.ui.viewmodel.BackupViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Instrumented test for P0-3: when a device-bound (BAK4) backup is opened on a
 * different device (or any AES-GCM authentication failure), the user must see a
 * clear Arabic message rather than a raw Java exception string, and the existing
 * data in Room must be untouched.
 *
 * "Data unchanged" structural invariant: [BackupViewModel.importBackup] calls
 * [CryptoManager.decrypt] on the archive metadata before it ever touches Room.
 * If decrypt throws, [restoreAllStagedWithJournal] is never called, so no table
 * is cleared and no journal is written.  This test confirms both the user-facing
 * message AND the invariant by asserting the synthetic will survives the failed restore.
 */
@RunWith(AndroidJUnit4::class)
class CrossDeviceRestoreMessageTest {

    private lateinit var context: Context
    private lateinit var db: ErthiDatabase
    private lateinit var viewModel: BackupViewModel
    private lateinit var tempArchive: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ErthiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val crypto = CryptoManager()
        val drive = DriveBackupManager(context)
        viewModel = BackupViewModel(
            context = context,
            db = db,
            willDao = db.willDao(),
            debtDao = db.debtDao(),
            assetDao = db.assetDao(),
            entrustedDao = db.entrustedDao(),
            beneficiaryDao = db.beneficiaryDao(),
            digitalAccountDao = db.digitalAccountDao(),
            documentDao = db.documentDao(),
            vaultItemDao = db.vaultItemDao(),
            crypto = crypto,
            vaultCrypto = VaultCrypto(crypto, db, db.vaultItemDao()),
            driveManager = drive,
            restoreJournalDao = db.restoreJournalDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
        tempArchive.delete()
    }

    @Test
    fun crossDeviceRestore_emitsClearArabicMessage_andDbDataIsUnchanged() {
        // Seed a synthetic will so we can confirm it survives the failed restore.
        val syntheticTitle = "وصية اختبار P0-3"
        runBlocking {
            db.willDao().insertWill(
                WillEntity(title = syntheticTitle, content = "[enc]AAAAAAAAAAAAAAAA")
            )
        }

        // Craft a BAK4 archive with valid magic but garbage AES-GCM ciphertext.
        // CryptoManager.decrypt will throw AEADBadTagException, which previously
        // leaked `localizedMessage` ("Tag mismatch!" or similar) to the UI.
        tempArchive = buildFakeBak4Archive()
        val fakeUri = Uri.fromFile(tempArchive)

        // Observe state from a background coroutine to avoid blocking the main looper.
        val scope = CoroutineScope(Dispatchers.IO)

        viewModel.importBackup(fakeUri)

        val errorState: BackupState.Error = runBlocking {
            withTimeout(10_000) {
                viewModel.state.first { it is BackupState.Error || it is BackupState.Success }
            } as? BackupState.Error ?: error("Expected Error state, got Success")
        }

        // Message must be the fixed user-friendly constant, not a raw Java exception string.
        assertEquals(
            "Error message must match the fixed Arabic constant",
            BackupViewModel.ERROR_CROSS_DEVICE_RESTORE,
            errorState.message,
        )
        assertFalse(
            "Error message must not contain raw exception class name",
            errorState.message.contains("AEADBadTagException", ignoreCase = true),
        )
        assertFalse(
            "Error message must not contain raw localizedMessage patterns",
            errorState.message.contains("Tag mismatch", ignoreCase = true),
        )

        // Data-unchanged invariant: the synthetic will must still be in Room.
        val wills = runBlocking { db.willDao().getAllWills().first() }
        assertEquals("Room must contain exactly the seeded will", 1, wills.size)
        assertEquals("Will title must be unchanged", syntheticTitle, wills[0].title)

        // No restore journal must have been written (decrypt failed before commit).
        assertNull(
            "No journal must exist after a pre-commit failure",
            runBlocking { db.restoreJournalDao().getPendingJournal() }
        )

        scope.cancel()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns a File containing a minimal BAK4 archive:
     *   "WASI_BAK4" (9 bytes) + IV (12 zero bytes) + ctLen (32 as Int) + ciphertext (32 zero bytes)
     *
     * The 32-byte zero ciphertext will fail AES-GCM authentication tag verification,
     * triggering the cross-device error path in BackupViewModel.
     */
    private fun buildFakeBak4Archive(): File {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.write("WASI_BAK4".toByteArray(Charsets.UTF_8))
            out.write(ByteArray(12))  // fake IV
            out.writeInt(32)           // ciphertext length
            out.write(ByteArray(32))  // garbage ciphertext — GCM tag will not match
        }
        val file = File(context.cacheDir, "fake_bak4_cross_device_test.bin")
        file.writeBytes(bos.toByteArray())
        return file
    }
}
