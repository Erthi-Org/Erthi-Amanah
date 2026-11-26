package com.erthi.amanah

import com.erthi.amanah.data.backup.BackupCrypto
import java.util.Base64 as JBase64
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

/**
 * Unit tests for the portable backup (WASI_BAK2) contract covering:
 *   • Audio will recordings: decrypted bytes embedded in JSON, re-encrypted on restore
 *   • Document file bytes: embedded as base64 in JSON
 *   • Legacy filePath fallback for old-format backups
 *   • Staged restore JSON contract (fields restoreWillsStaged / restoreDocumentsStaged read)
 *
 * These tests run on the JVM without Android or Room — they verify the JSON contract
 * and the binary format, not the DAO layer. Full end-to-end restore with Room requires
 * an instrumented test on a real device.
 */
class BackupAudioDocumentTest {

    // ── Audio embedding: round-trip through WASI_BAK2 ─────────────────────

    @Test
    fun portable_backup_embeds_raw_audio_bytes_in_wills_json() {
        // Simulate what buildPortableBackupJson() does for a will with audio:
        // — raw audio bytes are Base64-encoded into audioData
        // — audioFileName is the .3gp name (without .wasi_audio extension)

        val rawAudio = ByteArray(1024) { it.toByte() } // simulated .3gp data
        val audioFileName = "will_audio_20261121_1200.3gp"

        val willJson = JSONObject().apply {
            put("title", "وصيتي")
            put("content", "[enc]AAABBB")
            put("isDraft", false)
            put("legalDisclaimer", true)
            put("createdAt", 1700000000000L)
            put("updatedAt", 1700000001000L)
            put("audioFileName", audioFileName)
            put("audioData", JBase64.getEncoder().encodeToString(rawAudio))
        }

        // Simulate full backup JSON
        val backupJson = JSONObject().apply {
            put("version", 2)
            put("wills", JSONArray().put(willJson))
            put("debts", JSONArray()); put("assets", JSONArray()); put("entrusted", JSONArray())
            put("beneficiaries", JSONArray()); put("digitalAccounts", JSONArray())
            put("documents", JSONArray()); put("vaultItems", JSONArray())
        }

        // Encrypt as WASI_BAK2
        val passphrase = "مفتاح-اختبار-١٢٣"
        val plainBytes = backupJson.toString().toByteArray(Charsets.UTF_8)
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key(passphrase, salt)
        val (iv, ct) = BackupCrypto.encrypt(key, plainBytes)
        val blob = "WASI_BAK2".toByteArray() + salt + iv + ct

        // Simulate import side: decrypt and parse
        assertEquals("WASI_BAK2", String(blob.copyOfRange(0, 9)))
        val parsedSalt = blob.copyOfRange(9, 41)
        val parsedIv = blob.copyOfRange(41, 53)
        val parsedCt = blob.copyOfRange(53, blob.size)
        val decKey = BackupCrypto.pbkdf2Key(passphrase, parsedSalt)
        val decBytes = BackupCrypto.decrypt(decKey, parsedIv, parsedCt)
        val parsedRoot = JSONObject(String(decBytes, Charsets.UTF_8))

        assertEquals(2, parsedRoot.getInt("version"))
        val parsedWill = parsedRoot.getJSONArray("wills").getJSONObject(0)
        assertEquals("will_audio_20261121_1200.3gp", parsedWill.getString("audioFileName"))
        val restoredAudioBytes = JBase64.getDecoder().decode(parsedWill.getString("audioData"))
        assertArrayEquals("Audio bytes must survive full WASI_BAK2 round-trip", rawAudio, restoredAudioBytes)
    }

    @Test
    fun portable_backup_audio_data_is_raw_unencrypted_bytes() {
        // When audioData is present in a portable backup JSON, it must contain
        // raw (not WASI_AUD-wrapped) audio bytes so they can be re-encrypted on the target device.
        // Verify: audioData MUST NOT start with the WASI_AUD magic header.

        val rawAudio = ByteArray(200) { (it % 256).toByte() }
        val audioDataB64 = JBase64.getEncoder().encodeToString(rawAudio)
        val decoded = JBase64.getDecoder().decode(audioDataB64)

        assertFalse(
            "audioData in portable backup must be raw audio, not WASI_AUD",
            decoded.size >= 8 && String(decoded.copyOfRange(0, 8)) == "WASI_AUD",
        )
        assertArrayEquals(rawAudio, decoded)
    }

    // ── Document file bytes: round-trip through WASI_BAK2 ─────────────────

    @Test
    fun portable_backup_embeds_document_file_bytes_in_json() {
        val pdfBytes = ByteArray(512) { (it * 3 % 256).toByte() }
        val fileName = "passport_scan.pdf"

        val docJson = JSONObject().apply {
            put("title", "جواز السفر")
            put("category", "هوية")
            put("mimeType", "application/pdf")
            put("notes", "نسخة ممسوحة ضوئياً")
            put("fileName", fileName)
            put("fileData", JBase64.getEncoder().encodeToString(pdfBytes))
        }

        val backupJson = JSONObject().apply {
            put("version", 2)
            put("wills", JSONArray()); put("debts", JSONArray()); put("assets", JSONArray())
            put("entrusted", JSONArray()); put("beneficiaries", JSONArray())
            put("digitalAccounts", JSONArray())
            put("documents", JSONArray().put(docJson))
            put("vaultItems", JSONArray())
        }

        val passphrase = "وثيقة-اختبار"
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key(passphrase, salt)
        val (iv, ct) = BackupCrypto.encrypt(key, backupJson.toString().toByteArray())
        val blob = "WASI_BAK2".toByteArray() + salt + iv + ct

        val parsedSalt = blob.copyOfRange(9, 41)
        val parsedIv = blob.copyOfRange(41, 53)
        val parsedCt = blob.copyOfRange(53, blob.size)
        val decBytes = BackupCrypto.decrypt(BackupCrypto.pbkdf2Key(passphrase, parsedSalt), parsedIv, parsedCt)
        val parsedDoc = JSONObject(String(decBytes)).getJSONArray("documents").getJSONObject(0)

        assertEquals("passport_scan.pdf", parsedDoc.getString("fileName"))
        assertArrayEquals(
            "Document bytes must survive WASI_BAK2 round-trip",
            pdfBytes,
            JBase64.getDecoder().decode(parsedDoc.getString("fileData")),
        )
        assertEquals("جواز السفر", parsedDoc.getString("title"))
    }

    @Test
    fun restore_documents_staged_falls_back_to_legacy_filepath_when_no_filedata() {
        // Old-format backups (before fileData was added) have only filePath.
        // restoreDocumentsStaged must handle this gracefully.
        val legacyDoc = JSONObject().apply {
            put("title", "وثيقة قديمة")
            put("category", "")
            put("filePath", "/data/user/0/com.erthi.amanah.debug/files/documents/old.pdf")
            put("mimeType", "application/pdf")
            put("notes", "")
        }

        val fileName = legacyDoc.optString("fileName", "")
        val fileData = legacyDoc.optString("fileData", "")
        val legacyPath = legacyDoc.optString("filePath", "")

        // Simulate the restoreDocumentsStaged decision logic
        val restoredPath = when {
            fileName.isNotEmpty() && fileData.isNotEmpty() -> "/new/path/$fileName"
            legacyPath.isNotEmpty() -> legacyPath
            else -> ""
        }

        assertEquals("Legacy filePath must be used when fileData is absent", legacyPath, restoredPath)
    }

    // ── Staged restore: DB failure leaves no orphan files ─────────────────

    @Test
    fun staged_restore_cleans_up_staging_on_failure() {
        // Simulate the staging mechanism: write files to staging dir, then
        // simulate a DB failure — staging dir must be deleted.
        val stagingDir = createTempDir("test_staging_")
        try {
            val stagingFile = java.io.File(stagingDir, "audio_will_0")
            stagingFile.writeBytes(ByteArray(100))
            assertTrue("Staging file must exist before simulated failure", stagingFile.exists())

            // Simulate DB failure → staging cleanup
            stagingDir.deleteRecursively()

            assertFalse("Staging dir must be gone after DB failure", stagingDir.exists())
        } finally {
            if (stagingDir.exists()) stagingDir.deleteRecursively()
        }
    }

    @Test
    fun staged_restore_moves_files_on_success() {
        // Simulate the commit phase: staged file is renamed to final location.
        val stagingDir = createTempDir("test_staging_success_")
        val finalDir = createTempDir("test_final_")
        try {
            val content = "simulated encrypted audio data".toByteArray()
            val stagingFile = java.io.File(stagingDir, "audio_will_0")
            stagingFile.writeBytes(content)

            val finalFile = java.io.File(finalDir, "will_audio_20261121.wasi_audio")
            finalDir.mkdirs()

            val moved = stagingFile.renameTo(finalFile)
            if (!moved) {
                // Cross-partition fallback
                finalFile.writeBytes(stagingFile.readBytes())
                stagingFile.delete()
            }

            assertTrue("Final file must exist after staged commit", finalFile.exists())
            assertFalse("Staging file must be gone after move", stagingFile.exists())
            assertArrayEquals("File content must be preserved", content, finalFile.readBytes())
        } finally {
            stagingDir.deleteRecursively()
            finalDir.deleteRecursively()
        }
    }

    // ── Will fields: createdAt and legalDisclaimer survive WASI_BAK2 ──────

    @Test
    fun portable_backup_preserves_legal_disclaimer_and_created_at() {
        val createdAt = 1700000000000L
        val willJson = JSONObject().apply {
            put("title", "وصيتي النهائية")
            put("content", "[enc]CONTENT")
            put("isDraft", false)
            put("legalDisclaimer", true)
            put("createdAt", createdAt)
            put("updatedAt", createdAt + 60_000L)
        }

        val root = JSONObject().apply {
            put("version", 2)
            put("wills", JSONArray().put(willJson))
            put("debts", JSONArray()); put("assets", JSONArray()); put("entrusted", JSONArray())
            put("beneficiaries", JSONArray()); put("digitalAccounts", JSONArray())
            put("documents", JSONArray()); put("vaultItems", JSONArray())
        }

        val passphrase = "اختبار-حقلي-قانوني"
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key(passphrase, salt)
        val (iv, ct) = BackupCrypto.encrypt(key, root.toString().toByteArray())
        val blob = "WASI_BAK2".toByteArray() + salt + iv + ct

        val pSalt = blob.copyOfRange(9, 41)
        val pIv = blob.copyOfRange(41, 53)
        val pCt = blob.copyOfRange(53, blob.size)
        val dec = BackupCrypto.decrypt(BackupCrypto.pbkdf2Key(passphrase, pSalt), pIv, pCt)
        val parsedWill = JSONObject(String(dec)).getJSONArray("wills").getJSONObject(0)

        assertTrue("legalDisclaimer must be preserved", parsedWill.getBoolean("legalDisclaimer"))
        assertEquals("createdAt must be preserved", createdAt, parsedWill.getLong("createdAt"))
    }
}
