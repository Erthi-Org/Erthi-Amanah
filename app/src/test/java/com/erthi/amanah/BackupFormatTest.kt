package com.erthi.amanah

import com.erthi.amanah.data.backup.BackupCrypto
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

/**
 * Tests for WASI_BAK2 format layout and BackupCrypto production utilities.
 * Exercises BackupCrypto.pbkdf2Key(), BackupCrypto.encrypt(), BackupCrypto.decrypt()
 * — the actual production functions used by BackupViewModel.
 */
class BackupFormatTest {

    private val WASI_BAK_MAGIC = "WASI_BAK".toByteArray()
    private val WASI_BAK2_MAGIC = "WASI_BAK2".toByteArray()

    @Test
    fun wasi_bak2_magic_header_is_9_bytes() {
        assertEquals(9, WASI_BAK2_MAGIC.size)
    }

    @Test
    fun wasi_bak_magic_is_8_bytes() {
        assertEquals(8, WASI_BAK_MAGIC.size)
    }

    @Test
    fun wasi_bak2_format_salt_offset_is_9() {
        // magic(9) + salt(32) + iv(12) + ciphertext
        val saltStart = WASI_BAK2_MAGIC.size
        val saltEnd = saltStart + 32
        assertEquals(9, saltStart)
        assertEquals(41, saltEnd)
    }

    @Test
    fun wasi_bak2_format_iv_offset_is_41() {
        val ivStart = 9 + 32
        val ivEnd = ivStart + 12
        assertEquals(41, ivStart)
        assertEquals(53, ivEnd)
    }

    @Test
    fun wasi_bak2_format_ciphertext_starts_at_53() {
        val ctStart = 9 + 32 + 12
        assertEquals(53, ctStart)
    }

    // ── BackupCrypto.pbkdf2Key — production function ───────────────────────

    @Test
    fun production_pbkdf2_key_produces_256_bit_key() {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key: SecretKeySpec = BackupCrypto.pbkdf2Key("test-passphrase", salt)
        assertEquals("AES", key.algorithm)
        assertEquals(32, key.encoded.size)
    }

    @Test
    fun production_pbkdf2_different_salts_produce_different_keys() {
        val rng = SecureRandom()
        val salt1 = ByteArray(32).also { rng.nextBytes(it) }
        val salt2 = ByteArray(32).also { rng.nextBytes(it) }
        val key1 = BackupCrypto.pbkdf2Key("passphrase", salt1)
        val key2 = BackupCrypto.pbkdf2Key("passphrase", salt2)
        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }

    @Test
    fun production_pbkdf2_different_passphrases_produce_different_keys() {
        val salt = ByteArray(32) { it.toByte() }
        val key1 = BackupCrypto.pbkdf2Key("correct-horse", salt)
        val key2 = BackupCrypto.pbkdf2Key("wrong-passphrase", salt)
        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }

    // ── BackupCrypto.encrypt / decrypt round-trip ─────────────────────────

    @Test
    fun production_aes_gcm_encrypt_decrypt_roundtrip() {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key("my-secret", salt)
        val plaintext = """{"will":"Test will content"}""".toByteArray()
        val (iv, ciphertext) = BackupCrypto.encrypt(key, plaintext)
        val decrypted = BackupCrypto.decrypt(key, iv, ciphertext)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun production_encrypt_generates_12_byte_iv() {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key("passphrase", salt)
        val (iv, _) = BackupCrypto.encrypt(key, "data".toByteArray())
        assertEquals(12, iv.size)
    }

    @Test
    fun production_wrong_passphrase_causes_gcm_authentication_failure() {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encKey = BackupCrypto.pbkdf2Key("correct", salt)
        val decKey = BackupCrypto.pbkdf2Key("wrong", salt)
        val (iv, ciphertext) = BackupCrypto.encrypt(encKey, "sensitive data".toByteArray())
        try {
            BackupCrypto.decrypt(decKey, iv, ciphertext)
            fail("Expected AEADBadTagException for wrong passphrase")
        } catch (e: Exception) {
            // Expected — GCM authentication tag mismatch
        }
    }

    @Test
    fun portable_backup_binary_layout_roundtrip() {
        val passphrase = "الوصيتي-backup-2024"
        val plaintext = """{"version":2,"wills":[]}""".toByteArray()
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = BackupCrypto.pbkdf2Key(passphrase, salt)
        val (iv, ciphertext) = BackupCrypto.encrypt(key, plaintext)
        // Compose binary blob exactly as BackupViewModel does
        val magic = "WASI_BAK2".toByteArray()
        val blob = magic + salt + iv + ciphertext
        // Parse it back
        assertEquals("WASI_BAK2", String(blob.copyOfRange(0, 9)))
        val parsedSalt = blob.copyOfRange(9, 41)
        val parsedIv = blob.copyOfRange(41, 53)
        val parsedCt = blob.copyOfRange(53, blob.size)
        assertArrayEquals(salt, parsedSalt)
        assertArrayEquals(iv, parsedIv)
        val restoredKey = BackupCrypto.pbkdf2Key(passphrase, parsedSalt)
        val decrypted = BackupCrypto.decrypt(restoredKey, parsedIv, parsedCt)
        assertArrayEquals(plaintext, decrypted)
    }
}
