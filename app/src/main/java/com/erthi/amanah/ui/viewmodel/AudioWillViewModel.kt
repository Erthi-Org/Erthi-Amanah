package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.erthi.amanah.data.security.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioWillViewModel @Inject constructor(
    private val crypto: CryptoManager,
) : ViewModel() {

    // Encrypts the recorded .3gp source file, saves as .wasi_audio, deletes source.
    // Returns the encrypted file, or null if encryption fails.
    suspend fun encryptAudio(source: File): File? = withContext(Dispatchers.IO) {
        try {
            val raw = source.readBytes()
            val enc = crypto.encrypt(raw)
            // Format: WASI_AUD(8) + iv(12) + ciphertext
            val magic = "WASI_AUD".toByteArray(Charsets.UTF_8)
            val outFile = File(source.parent, source.nameWithoutExtension + ".wasi_audio")
            outFile.writeBytes(magic + enc.iv + enc.ciphertext)
            source.delete()
            outFile
        } catch (_: Exception) {
            null
        }
    }

    // Decrypts an encrypted .wasi_audio file to a temp .3gp in cacheDir for playback.
    // Handles both WASI_AUD (legacy single-block) and WASI_AU2 (chunked) formats.
    // Caller must delete the temp file after MediaPlayer completes.
    suspend fun decryptAudioToTemp(encFile: File, cacheDir: File): File? = withContext(Dispatchers.IO) {
        val tmp = File(cacheDir, "tmp_playback_${System.currentTimeMillis()}.3gp")
        try {
            java.io.DataInputStream(encFile.inputStream().buffered()).use { din ->
                val magicBytes = ByteArray(8); din.readFully(magicBytes)
                when (String(magicBytes, Charsets.UTF_8)) {
                    "WASI_AUD" -> {
                        val iv = ByteArray(12); din.readFully(iv)
                        val ct = din.readBytes()
                        tmp.writeBytes(crypto.decrypt(CryptoManager.EncryptedData(iv, ct)))
                    }
                    "WASI_AU2" -> {
                        val chunkCount = din.readInt()
                        // Reject clearly malformed chunk counts (P0-D bounds fix)
                        if (chunkCount < 1 || chunkCount > MAX_CHUNK_COUNT) return@withContext null
                        tmp.outputStream().buffered().use { out ->
                            repeat(chunkCount) {
                                val iv = ByteArray(12); din.readFully(iv)
                                val ctLen = din.readInt()
                                if (ctLen < 1 || ctLen > MAX_CHUNK_BYTES) return@withContext null
                                val ct = ByteArray(ctLen); din.readFully(ct)
                                out.write(crypto.decrypt(CryptoManager.EncryptedData(iv, ct)))
                            }
                        }
                        // Ensure no trailing data (would indicate truncated or corrupt file)
                        if (din.available() > 0) return@withContext null
                    }
                    else -> return@withContext null
                }
            }
            tmp
        } catch (_: Exception) {
            tmp.delete()
            null
        }
    }

    companion object {
        private const val MAX_CHUNK_COUNT = 10_000
        // 10 MiB per chunk — well above any reasonable audio segment
        private const val MAX_CHUNK_BYTES = 10 * 1024 * 1024
    }
}
