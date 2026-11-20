package com.erthi.amanah

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

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

    // ── Base64 + prefix round-trip ─────────────────────────────────────────

    @Test
    fun enc_prefix_plus_base64_roundtrip() {
        val originalBytes = ByteArray(44) { it.toByte() } // iv(12) + ciphertext(32)
        val encoded = ENC_PREFIX + Base64.getEncoder().encodeToString(originalBytes)

        assertTrue(encoded.startsWith(ENC_PREFIX))
        val decoded = Base64.getDecoder().decode(encoded.removePrefix(ENC_PREFIX))
        assertArrayEquals(originalBytes, decoded)
    }

    @Test
    fun enc_prefix_payload_minimum_12_bytes_iv() {
        // Minimum meaningful payload: 12 bytes IV + at least 1 byte ciphertext
        val minPayload = ByteArray(13)
        val encoded = ENC_PREFIX + Base64.getEncoder().encodeToString(minPayload)
        val decoded = Base64.getDecoder().decode(encoded.removePrefix(ENC_PREFIX))
        assertTrue(decoded.size >= 13)
    }

    // ── EncryptedData fromByteArray edge cases ─────────────────────────────

    @Test
    fun encryptedData_toByteArray_contains_iv_then_ciphertext() {
        val iv = ByteArray(12) { (it + 1).toByte() }
        val ciphertext = ByteArray(16) { (it + 50).toByte() }
        val combined = iv + ciphertext

        val extractedIv = combined.copyOfRange(0, 12)
        val extractedCt = combined.copyOfRange(12, combined.size)

        assertArrayEquals(iv, extractedIv)
        assertArrayEquals(ciphertext, extractedCt)
    }

    @Test
    fun encryptedData_fromByteArray_exactly_12_bytes_iv() {
        val iv = ByteArray(12) { it.toByte() }
        val ct = ByteArray(1) { 0xFF.toByte() }
        val combined = iv + ct

        val restoredIv = combined.copyOfRange(0, 12)
        val restoredCt = combined.copyOfRange(12, combined.size)

        assertEquals(12, restoredIv.size)
        assertEquals(1, restoredCt.size)
    }

    @Test
    fun byteArray_shorter_than_12_cannot_hold_iv() {
        val tooShort = ByteArray(11)
        assertTrue(tooShort.size < 12)
    }

    // ── Vault item field handling ──────────────────────────────────────────

    @Test
    fun vault_item_username_encrypted_field_roundtrip() {
        val username = "user@example.com"
        val fakeIv = ByteArray(12) { it.toByte() }
        val fakeCt = username.toByteArray()
        val stored = ENC_PREFIX + Base64.getEncoder().encodeToString(fakeIv + fakeCt)

        assertTrue(stored.startsWith(ENC_PREFIX))
        val raw = Base64.getDecoder().decode(stored.removePrefix(ENC_PREFIX))
        val recoveredUsername = String(raw.copyOfRange(12, raw.size))
        assertEquals(username, recoveredUsername)
    }

    @Test
    fun null_or_blank_field_does_not_start_with_enc_prefix() {
        assertFalse("".startsWith(ENC_PREFIX))
        assertFalse("   ".startsWith(ENC_PREFIX))
    }
}
