package com.erthi.amanah

import com.erthi.amanah.data.security.CryptoManager
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the EncryptedData round-trip used by WillViewModel to store
 * will content encrypted in the Room database.
 *
 * Full AES/GCM operations require the Android Keystore (hardware/emulator),
 * so these tests cover the serialisation layer that the ViewModel relies on.
 */
class WillEncryptionTest {

    private val encPrefix = "[enc]"

    @Test
    fun encryptedData_toByteArray_roundTrip_preservesIvAndCiphertext() {
        val iv = ByteArray(12) { (it * 7).toByte() }
        val ciphertext = ByteArray(48) { (it + 50).toByte() }

        val encData = CryptoManager.EncryptedData(iv, ciphertext)
        val bytes = encData.toByteArray()
        val restored = CryptoManager.EncryptedData.fromByteArray(bytes)

        assertArrayEquals("IV must survive round-trip", iv, restored.iv)
        assertArrayEquals("Ciphertext must survive round-trip", ciphertext, restored.ciphertext)
    }

    @Test
    fun encryptedData_fromByteArray_tooShort_throwsIllegalArgument() {
        try {
            CryptoManager.EncryptedData.fromByteArray(ByteArray(11)) // < IV_LENGTH (12)
            fail("Expected IllegalArgumentException for byte array shorter than IV")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun encryptedData_fromByteArray_exactlyIvLength_throwsIllegalArgument() {
        try {
            CryptoManager.EncryptedData.fromByteArray(ByteArray(12)) // exactly IV_LENGTH, no ciphertext
            fail("Expected IllegalArgumentException for byte array with no ciphertext")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun encPrefix_marker_isDetected() {
        val fakeEncContent = encPrefix + "SGVsbG8="
        assertTrue("enc prefix should be detected", fakeEncContent.startsWith(encPrefix))

        val plainContent = "بسم الله الرحمن الرحيم"
        assertFalse("Plain content should not have enc prefix", plainContent.startsWith(encPrefix))
    }

    @Test
    fun encryptedData_equals_worksCorrectly() {
        val iv = ByteArray(12) { it.toByte() }
        val ct = ByteArray(16) { it.toByte() }
        val a = CryptoManager.EncryptedData(iv, ct)
        val b = CryptoManager.EncryptedData(iv.copyOf(), ct.copyOf())
        assertEquals("Two EncryptedData with same content must be equal", a, b)
    }

    @Test
    fun willContent_utf8_encodeDecode_roundTrip() {
        val arabic = "بسم الله الرحمن الرحيم — وصيتي لأبنائي"
        val bytes = arabic.toByteArray(Charsets.UTF_8)
        val decoded = String(bytes, Charsets.UTF_8)
        assertEquals("Arabic content must survive UTF-8 encode/decode", arabic, decoded)
    }
}
