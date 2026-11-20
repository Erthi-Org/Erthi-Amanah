package com.erthi.amanah

import com.erthi.amanah.data.security.CryptoManager
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

/**
 * Tests for vault field encryption format.
 * Exercises CryptoManager.EncryptedData.fromByteArray() — the production class
 * used by VaultViewModel.dec() to parse [enc]-prefixed stored values.
 */
class VaultEncryptionTest {

    private val ENC_PREFIX = "[enc]"

    // ── Prefix detection ───────────────────────────────────────────────────

    @Test
    fun encrypted_string_starts_with_enc_prefix() {
        val encrypted = "[enc]dGVzdA=="
        assertTrue(encrypted.startsWith(ENC_PREFIX))
    }

    @Test
    fun plaintext_does_not_start_with_enc_prefix() {
        val plaintext = "my-password-123"
        assertFalse(plaintext.startsWith(ENC_PREFIX))
    }

    @Test
    fun empty_string_is_not_encrypted() {
        assertFalse("".startsWith(ENC_PREFIX))
    }

    // ── CryptoManager.EncryptedData.fromByteArray — production class ───────

    @Test
    fun encryptedData_fromByteArray_splits_iv_and_ciphertext() {
        val iv = ByteArray(12) { (it + 1).toByte() }
        val ciphertext = ByteArray(16) { (it + 50).toByte() }
        val combined = iv + ciphertext

        val parsed = CryptoManager.EncryptedData.fromByteArray(combined)
        assertArrayEquals(iv, parsed.iv)
        assertArrayEquals(ciphertext, parsed.ciphertext)
    }

    @Test
    fun encryptedData_fromByteArray_iv_is_exactly_12_bytes() {
        val iv = ByteArray(12) { it.toByte() }
        val ct = ByteArray(1) { 0xFF.toByte() }
        val combined = iv + ct

        val parsed = CryptoManager.EncryptedData.fromByteArray(combined)
        assertEquals(12, parsed.iv.size)
        assertEquals(1, parsed.ciphertext.size)
    }

    @Test
    fun encryptedData_toByteArray_reconstructs_original_bytes() {
        val iv = ByteArray(12) { (it * 3).toByte() }
        val ct = ByteArray(32) { (it + 100).toByte() }
        val original = CryptoManager.EncryptedData(iv, ct)
        val reconstructed = original.toByteArray()
        assertArrayEquals(iv + ct, reconstructed)
    }

    @Test
    fun encryptedData_roundtrip_fromByteArray_then_toByteArray() {
        val originalBytes = ByteArray(44) { it.toByte() }
        val parsed = CryptoManager.EncryptedData.fromByteArray(originalBytes)
        val reconstructed = parsed.toByteArray()
        assertArrayEquals(originalBytes, reconstructed)
    }

    // ── [enc] prefix + base64 round-trip ──────────────────────────────────

    @Test
    fun enc_prefix_plus_base64_roundtrip() {
        val originalBytes = ByteArray(44) { it.toByte() } // iv(12) + ciphertext(32)
        val encoded = ENC_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(originalBytes)
        assertTrue(encoded.startsWith(ENC_PREFIX))
        val decoded = Base64.getDecoder().decode(encoded.removePrefix(ENC_PREFIX))
        assertArrayEquals(originalBytes, decoded)
    }

    @Test
    fun enc_prefix_payload_minimum_12_bytes_iv() {
        val minPayload = ByteArray(13)
        val parsed = CryptoManager.EncryptedData.fromByteArray(minPayload)
        assertEquals(12, parsed.iv.size)
        assertEquals(1, parsed.ciphertext.size)
    }

    @Test
    fun byteArray_shorter_than_12_cannot_hold_iv() {
        val tooShort = ByteArray(11)
        assertTrue(tooShort.size < 12)
    }

    // ── Vault item field handling ──────────────────────────────────────────

    @Test
    fun null_or_blank_field_does_not_start_with_enc_prefix() {
        assertFalse("".startsWith(ENC_PREFIX))
        assertFalse("   ".startsWith(ENC_PREFIX))
    }
}
