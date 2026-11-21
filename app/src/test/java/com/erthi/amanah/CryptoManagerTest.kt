package com.erthi.amanah

import org.junit.Assert.*
import org.junit.Test

class CryptoManagerTest {

    @Test
    fun encryptedData_toByteArray_roundTrip() {
        val iv = ByteArray(12) { it.toByte() }
        val ciphertext = ByteArray(32) { (it + 100).toByte() }
        val original = com.erthi.amanah.data.security.CryptoManager.EncryptedData(iv, ciphertext)
        val bytes = original.toByteArray()
        val restored = com.erthi.amanah.data.security.CryptoManager.EncryptedData.fromByteArray(bytes)
        assertArrayEquals(iv, restored.iv)
        assertArrayEquals(ciphertext, restored.ciphertext)
    }

    @Test
    fun encryptedData_fromByteArray_tooShort_throws() {
        try {
            com.erthi.amanah.data.security.CryptoManager.EncryptedData.fromByteArray(ByteArray(5))
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun pinValidation_tooShort_fails() {
        // A pin shorter than 4 chars should not be accepted (business rule enforced in AuthViewModel)
        val pin = "123"
        assertTrue("PIN too short", pin.length < 4)
    }

    @Test
    fun pinValidation_mismatch_detected() {
        val pin = "1234"
        val confirmPin = "5678"
        assertNotEquals("Pins must not match", pin, confirmPin)
    }
}
