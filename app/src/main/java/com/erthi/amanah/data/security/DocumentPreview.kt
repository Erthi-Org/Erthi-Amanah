package com.erthi.amanah.data.security

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.DataInputStream
import java.io.File
import java.io.IOException

internal class PreviewTooLargeException : IOException()
internal class UnsupportedPreviewException : IOException()

internal fun renderDocumentPreview(
    source: File,
    crypto: CryptoManager,
): Bitmap {
    val fileSize = source.length()
    if (fileSize > 12L * 1024 * 1024) throw PreviewTooLargeException()
    require(fileSize >= 8 + 12 + 16) { "Incomplete encrypted attachment" }
    val encrypted = DataInputStream(source.inputStream().buffered()).use { input ->
        val magic = ByteArray(8).also(input::readFully)
        require(String(magic, Charsets.US_ASCII) == "WASI_DOC") { "Unknown attachment format" }
        val iv = ByteArray(12).also(input::readFully)
        val ciphertext = ByteArray(fileSize.toInt() - 20).also(input::readFully)
        require(input.read() == -1) { "Attachment size changed" }
        CryptoManager.EncryptedData(iv, ciphertext)
    }
    val bytes = crypto.decrypt(encrypted)
    try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outMimeType !in setOf("image/jpeg", "image/png", "image/webp")) {
            throw UnsupportedPreviewException()
        }
        var sampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / sampleSize > 1600) sampleSize *= 2
        options.inSampleSize = sampleSize
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw UnsupportedPreviewException()
        return bitmap
    } finally {
        bytes.fill(0)
    }
}
