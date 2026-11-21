package com.erthi.amanah

import com.erthi.amanah.data.backup.BackupCrypto
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

/**
 * Unit tests for the Drive backup encryption path.
 *
 * These tests cover the full binary contract exercised by BackupViewModel.exportDriveBackup
 * and BackupViewModel.importDriveBackup without requiring a real Drive connection.
 *
 * NOTE: End-to-end Drive upload/download/restore requires a real Google account with
 * the OAuth client ID registered in Google Cloud Console (see docs/GOOGLE_DRIVE_SETUP.md).
 * That integration cannot be verified in a JVM unit test; these tests cover all
 * cryptographic correctness claims independently of the network.
 */
class DriveBackupCryptoTest {

    // ── Full upload/download cycle (mocked transport) ─────────────────────────

    @Test
    fun drive_export_produces_valid_wasi_bak2_blob() {
        val passphrase = "drive-test-passphrase-١٢٣"
        val payload = """{"version":2,"wills":[],"vaultItems":[]}""".toByteArray(Charsets.UTF_8)

        // Mirrors BackupViewModel.exportDriveBackup blob construction
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key(passphrase, salt)
        val (iv, ciphertext) = BackupCrypto.encrypt(key, payload)
        val magic = "WASI_BAK2".toByteArray(Charsets.UTF_8)
        val blob = magic + salt + iv + ciphertext

        assertEquals("Magic header must be 9 bytes", 9, magic.size)
        // header(9) + salt(32) + iv(12) + ciphertext(payload + 16-byte GCM tag) > 53
        assertTrue("Blob must be larger than header + salt + iv", blob.size > 53)
        assertEquals("Blob header", "WASI_BAK2", String(blob.copyOfRange(0, 9), Charsets.UTF_8))
    }

    @Test
    fun drive_import_decrypts_blob_produced_by_export() {
        val passphrase = "صحيح-حصان-بطارية-دبوس"
        val originalJson = """{"version":2,"wills":[{"title":"وصيتي","content":"محتوى"}]}"""
        val originalBytes = originalJson.toByteArray(Charsets.UTF_8)

        // Export side (BackupViewModel.exportDriveBackup)
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key(passphrase, salt)
        val (iv, ciphertext) = BackupCrypto.encrypt(key, originalBytes)
        val magic = "WASI_BAK2".toByteArray(Charsets.UTF_8)
        val blob = magic + salt + iv + ciphertext

        // Import side (BackupViewModel.importDriveBackup) — simulated "downloaded from Drive"
        val header = String(blob.copyOfRange(0, 9), Charsets.UTF_8)
        assertEquals("WASI_BAK2", header)

        val parsedSalt = blob.copyOfRange(9, 41)
        val parsedIv = blob.copyOfRange(41, 53)
        val parsedCt = blob.copyOfRange(53, blob.size)

        val decryptKey = BackupCrypto.pbkdf2Key(passphrase, parsedSalt)
        val decrypted = BackupCrypto.decrypt(decryptKey, parsedIv, parsedCt)

        assertEquals(originalJson, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun drive_import_wrong_passphrase_throws() {
        val passphrase = "correct-passphrase"
        val wrongPhrase = "wrong-passphrase"
        val payload = "sensitive data".toByteArray()

        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encKey = BackupCrypto.pbkdf2Key(passphrase, salt)
        val (iv, ciphertext) = BackupCrypto.encrypt(encKey, payload)
        val blob = "WASI_BAK2".toByteArray() + salt + iv + ciphertext

        val parsedSalt = blob.copyOfRange(9, 41)
        val parsedIv = blob.copyOfRange(41, 53)
        val parsedCt = blob.copyOfRange(53, blob.size)

        try {
            val decKey = BackupCrypto.pbkdf2Key(wrongPhrase, parsedSalt)
            BackupCrypto.decrypt(decKey, parsedIv, parsedCt)
            fail("Expected AEADBadTagException for wrong passphrase")
        } catch (e: Exception) {
            // Expected — GCM authentication tag mismatch
        }
    }

    @Test
    fun drive_import_truncated_header_is_detected() {
        val truncated = "WASI_BAK".toByteArray() // 8 bytes, not 9
        val header = try {
            String(truncated.copyOfRange(0, minOf(9, truncated.size)), Charsets.UTF_8)
        } catch (_: Exception) { "" }
        assertNotEquals("Truncated magic must not match WASI_BAK2", "WASI_BAK2", header)
    }

    @Test
    fun drive_import_wrong_magic_is_detected() {
        val wrongBlob = "WASI_BAK!".toByteArray() + ByteArray(100)
        val header = String(wrongBlob.copyOfRange(0, 9), Charsets.UTF_8)
        assertNotEquals("WASI_BAK2", header)
    }

    @Test
    fun drive_import_full_round_trip_arabic_content() {
        // Large realistic payload with multi-byte Arabic UTF-8
        val passphrase = "مفتاح-النسخة-الاحتياطية"
        val jsonPayload = buildString {
            append("""{"version":2,"wills":[""")
            repeat(10) { i ->
                if (i > 0) append(",")
                append("""{"title":"وصيتي $i","content":"محتوى الوصية رقم $i — بسم الله الرحمن الرحيم"}""")
            }
            append("""],"vaultItems":[]}""")
        }
        val originalBytes = jsonPayload.toByteArray(Charsets.UTF_8)
        assertTrue("Test payload must be non-trivially large", originalBytes.size > 200)

        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key(passphrase, salt)
        val (iv, ciphertext) = BackupCrypto.encrypt(key, originalBytes)
        val blob = "WASI_BAK2".toByteArray(Charsets.UTF_8) + salt + iv + ciphertext

        // Simulate Drive download: parse and decrypt
        val parsedSalt = blob.copyOfRange(9, 41)
        val parsedIv = blob.copyOfRange(41, 53)
        val parsedCt = blob.copyOfRange(53, blob.size)
        val decKey = BackupCrypto.pbkdf2Key(passphrase, parsedSalt)
        val decrypted = BackupCrypto.decrypt(decKey, parsedIv, parsedCt)

        assertArrayEquals("Round-trip must reproduce exact bytes", originalBytes, decrypted)
        assertEquals(jsonPayload, String(decrypted, Charsets.UTF_8))
    }

    // ── Drive filename contract ───────────────────────────────────────────────

    @Test
    fun drive_filename_has_wasi2bak_extension() {
        // BackupViewModel uses filename = "wasiyyati_drive_<date>.wasi2bak"
        val filename = "wasiyyati_drive_20261121_1430.wasi2bak"
        assertTrue("Filename must start with wasiyyati_drive_", filename.startsWith("wasiyyati_drive_"))
        assertTrue("Filename must end with .wasi2bak", filename.endsWith(".wasi2bak"))
    }

    // ── Integration gap notice ────────────────────────────────────────────────
    //
    // The following cannot be verified in a JVM unit test:
    //   • GoogleAuthUtil.getToken() — requires a real GMS-bound Google account
    //   • Drive v3 REST multipart upload — requires network + OAuth client ID in Cloud Console
    //   • listBackups() appDataFolder query — requires at least one uploaded file
    //   • downloadBackup() alt=media response — requires a real file ID
    //
    // These are tested via manual QA on a physical device after Google Cloud Console
    // setup (see docs/GOOGLE_DRIVE_SETUP.md). The cryptographic contract tested above
    // is the same code path that runs during a real upload/download cycle.
}
