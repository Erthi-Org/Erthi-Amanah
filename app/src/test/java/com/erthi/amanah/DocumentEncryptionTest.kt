package com.erthi.amanah

import com.erthi.amanah.data.security.DocumentEncryption
import com.erthi.amanah.data.security.DocumentTooLargeException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class DocumentEncryptionTest {
    private val key = SecretKeySpec(ByteArray(32), "AES")

    @Test
    fun readsWithLegacyFormatAndRejectsTampering() {
        val plain = ByteArray(230_000) { (it % 251).toByte() }
        val encrypted = ByteArrayOutputStream().also {
            DocumentEncryption.write(ByteArrayInputStream(plain), it, key)
        }.toByteArray()
        assertEquals("WASI_DOC", String(encrypted, 0, 8, Charsets.US_ASCII))
        assertArrayEquals(plain, decrypt(encrypted))
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(javax.crypto.AEADBadTagException::class.java) { decrypt(encrypted) }
    }

    @Test
    fun limitAppliesWithNoReportedSizeAndOnlyReadsOneExtraByte() {
        val limit = DocumentEncryption.MAX_BYTES
        val exact = SizedStream(limit)
        DocumentEncryption.write(exact, discard, key)
        assertEquals(limit, exact.readBytesCount)

        val tooLarge = SizedStream(limit + 50)
        assertThrows(DocumentTooLargeException::class.java) {
            DocumentEncryption.write(tooLarge, discard, key)
        }
        assertEquals(limit + 1, tooLarge.readBytesCount)
    }

    private fun decrypt(file: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, file.copyOfRange(8, 20)))
        return cipher.doFinal(file, 20, file.size - 20)
    }

    private val discard = object : OutputStream() {
        override fun write(b: Int) = Unit
        override fun write(b: ByteArray, off: Int, len: Int) = Unit
    }

    private class SizedStream(private val size: Long) : InputStream() {
        var readBytesCount = 0L
            private set

        override fun read(): Int = error("Expected buffered reads")

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (readBytesCount == size) return -1
            val count = minOf(len.toLong(), size - readBytesCount).toInt()
            readBytesCount += count
            return count
        }
    }
}
