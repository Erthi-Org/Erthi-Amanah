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
    // Caller must delete the temp file after MediaPlayer completes.
    suspend fun decryptAudioToTemp(encFile: File, cacheDir: File): File? = withContext(Dispatchers.IO) {
        try {
            val raw = encFile.readBytes()
            if (raw.size < 20) return@withContext null
            val magic = "WASI_AUD"
            if (String(raw.copyOfRange(0, 8), Charsets.UTF_8) != magic) return@withContext null
            val iv = raw.copyOfRange(8, 20)
            val ct = raw.copyOfRange(20, raw.size)
            val plainBytes = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
            val tmp = File(cacheDir, "tmp_playback_${System.currentTimeMillis()}.3gp")
            tmp.writeBytes(plainBytes)
            tmp
        } catch (_: Exception) {
            null
        }
    }
}
