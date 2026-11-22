package com.erthi.amanah.data.security

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey

internal class DocumentTooLargeException : IOException()

internal object DocumentEncryption {
    const val MAX_BYTES = 50L * 1024 * 1024
    private val magic = "WASI_DOC".toByteArray(Charsets.US_ASCII)

    /** Preserves the existing magic + 12-byte IV + ciphertext + GCM tag format. */
    fun write(
        input: InputStream,
        output: OutputStream,
        key: SecretKey,
        checkActive: () -> Unit = {},
    ) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        check(cipher.iv.size == 12)
        output.write(magic)
        output.write(cipher.iv)
        val buffer = ByteArray(64 * 1024)
        var remaining = MAX_BYTES
        try {
            while (true) {
                checkActive()
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining + 1).toInt())
                if (count == -1) break
                if (count == 0) throw IOException("Document provider returned an empty read")
                if (count > remaining) throw DocumentTooLargeException()
                remaining -= count
                cipher.update(buffer, 0, count)?.let { output.write(it) }
            }
            checkActive()
            output.write(cipher.doFinal())
        } finally {
            buffer.fill(0)
        }
    }
}
