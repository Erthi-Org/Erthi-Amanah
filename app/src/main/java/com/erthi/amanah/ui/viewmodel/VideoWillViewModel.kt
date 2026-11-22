package com.erthi.amanah.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.WillDao
import com.erthi.amanah.data.db.entity.WillEntity
import com.erthi.amanah.data.security.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Encrypted video will file format v2 (streaming, bounded memory):
 *   WASI_VD2(8) + chunkCount(4, big-endian) +
 *   for each chunk: iv(12) + ctLen(4) + ciphertext(ctLen)
 *
 * Legacy v1 format (single-pass, still readable for backward compat):
 *   WASI_VID(8) + iv(12) + ciphertext(remaining)
 */
private const val VIDEO_MAGIC_V2 = "WASI_VD2"
private const val VIDEO_MAGIC_V1 = "WASI_VID"
private const val VIDEO_DIR = "video_wills"
private const val VIDEO_EXTENSION = ".wasi_video"
private const val CHUNK_SIZE = 4 * 1024 * 1024  // 4 MB per encryption chunk

sealed class VideoWillState {
    data object Idle : VideoWillState()
    data object Encrypting : VideoWillState()
    data class Ready(val encryptedPath: String) : VideoWillState()
    data class Error(val message: String) : VideoWillState()
}

@HiltViewModel
class VideoWillViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val willDao: WillDao,
    private val crypto: CryptoManager,
) : ViewModel() {

    private val _state = MutableStateFlow<VideoWillState>(VideoWillState.Idle)
    val state: StateFlow<VideoWillState> = _state.asStateFlow()

    // Returns a temp file for CameraX to record into
    fun createTempRecordingFile(): File {
        val tmpDir = File(context.cacheDir, "video_tmp").also { it.mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(tmpDir, "will_video_$ts.mp4")
    }

    /**
     * Fix 1 + Fix 2: Encrypt [sourceFile] with bounded-memory streaming AES-GCM,
     * then atomically insert a new WillEntity with the encrypted path.
     * Room only ever stores the encrypted path — never the plaintext temp path.
     * Deletes [sourceFile] only after the will record is committed to Room.
     */
    fun encryptAndSave(title: String, sourceFile: File) {
        viewModelScope.launch {
            _state.value = VideoWillState.Encrypting
            try {
                val encryptedPath = encryptVideoStream(sourceFile)
                val encFile = File(encryptedPath)
                require(encFile.exists() && encFile.length() > 20) {
                    "ملف الفيديو المشفر تالف أو فارغ بعد التشفير"
                }
                // Insert will with encrypted path — Room record is consistent immediately
                willDao.insertWill(WillEntity(
                    title = title.ifBlank { "وصية مصورة" },
                    content = "",
                    videoPath = encryptedPath,
                    isDraft = false,
                    legalDisclaimer = false,
                ))
                // Delete plaintext only after DB record is committed
                if (sourceFile.exists()) sourceFile.delete()
                _state.value = VideoWillState.Ready(encryptedPath)
            } catch (e: Exception) {
                // Leave plaintext temp intact so user can retry
                _state.value = VideoWillState.Error("فشل حفظ الوصية المصورة: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Re-encrypt [sourceFile] and attach to existing will [willId].
     * Never deletes the old encrypted video until the new one is verified in Room.
     */
    fun encryptAndAttach(willId: Long, sourceFile: File) {
        viewModelScope.launch {
            _state.value = VideoWillState.Encrypting
            try {
                val encryptedPath = encryptVideoStream(sourceFile)
                val encFile = File(encryptedPath)
                require(encFile.exists() && encFile.length() > 20) {
                    "ملف الفيديو المشفر تالف أو فارغ بعد التشفير"
                }
                val existing = willDao.getWillById(willId)
                if (existing != null) {
                    val oldVideoPath = existing.videoPath
                    willDao.updateWill(existing.copy(videoPath = encryptedPath))
                    // Delete old encrypted video only after Room is updated
                    oldVideoPath?.let { old ->
                        val oldFile = File(old)
                        if (oldFile.exists() && oldFile.absolutePath != encryptedPath) {
                            oldFile.delete()
                        }
                    }
                }
                if (sourceFile.exists()) sourceFile.delete()
                _state.value = VideoWillState.Ready(encryptedPath)
            } catch (e: Exception) {
                _state.value = VideoWillState.Error("فشل تشفير الفيديو: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Fix 2: Bounded-memory streaming encryption.
     * Reads [source] in [CHUNK_SIZE]-byte chunks; each chunk is independently AES-GCM encrypted.
     * Peak heap usage ≈ 2 × CHUNK_SIZE (read buffer + ciphertext buffer).
     */
    private suspend fun encryptVideoStream(source: File): String = withContext(Dispatchers.IO) {
        require(source.exists() && source.length() > 0) {
            "ملف التسجيل المؤقت غير موجود أو فارغ"
        }
        val chunkCount = ((source.length() + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        val videoDir = File(context.filesDir, VIDEO_DIR).also { it.mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outFile = File(videoDir, "will_video_$ts$VIDEO_EXTENSION")

        source.inputStream().buffered().use { input ->
            DataOutputStream(outFile.outputStream().buffered()).use { out ->
                out.write(VIDEO_MAGIC_V2.toByteArray(Charsets.UTF_8))  // 8 bytes
                out.writeInt(chunkCount)                                // 4 bytes
                val buf = ByteArray(CHUNK_SIZE)
                var totalChunksWritten = 0
                var bytesRead: Int
                while (input.read(buf).also { bytesRead = it } != -1) {
                    val plain = if (bytesRead == CHUNK_SIZE) buf else buf.copyOf(bytesRead)
                    val enc = crypto.encrypt(plain)
                    out.write(enc.iv)                               // 12 bytes
                    out.writeInt(enc.ciphertext.size)               // 4 bytes
                    out.write(enc.ciphertext)
                    totalChunksWritten++
                }
                require(totalChunksWritten == chunkCount) {
                    "عدد الأجزاء المكتوبة ($totalChunksWritten) لا يطابق المتوقع ($chunkCount)"
                }
            }
        }
        outFile.absolutePath
    }

    /**
     * Fix 2: Bounded-memory streaming decryption.
     * Handles both WASI_VD2 (chunked) and legacy WASI_VID (single-pass, for existing files).
     * Returns a temp plaintext File, or null on failure.
     * The caller must delete the temp file when playback ends.
     */
    suspend fun decryptVideoToTemp(encryptedFile: File, outputDir: File): File? =
        withContext(Dispatchers.IO) {
            if (!encryptedFile.exists() || encryptedFile.length() < 20) return@withContext null
            val tmpDir = File(outputDir, "video_playback_tmp").also { it.mkdirs() }
            val tmpFile = File(tmpDir, "playback_${System.currentTimeMillis()}.mp4")
            try {
                DataInputStream(encryptedFile.inputStream().buffered()).use { input ->
                    val magicBytes = ByteArray(8)
                    input.readFully(magicBytes)
                    when (val magic = String(magicBytes, Charsets.UTF_8)) {
                        VIDEO_MAGIC_V2 -> decryptChunked(input, tmpFile)
                        VIDEO_MAGIC_V1 -> decryptLegacy(input, encryptedFile, tmpFile)
                        else -> throw IllegalStateException("تنسيق ملف الفيديو غير صالح: $magic")
                    }
                }
                tmpFile
            } catch (e: Exception) {
                tmpFile.delete()
                null
            }
        }

    // Fix 2: Chunked (WASI_VD2) decryption — bounded memory
    private fun decryptChunked(input: DataInputStream, outputFile: File) {
        val chunkCount = input.readInt()
        require(chunkCount in 1..100_000) { "عدد أجزاء غير صالح: $chunkCount" }
        outputFile.outputStream().buffered().use { out ->
            repeat(chunkCount) {
                val iv = ByteArray(12); input.readFully(iv)
                val ctLen = input.readInt()
                require(ctLen in 1..(CHUNK_SIZE + 1024)) { "حجم جزء غير صالح: $ctLen" }
                val ct = ByteArray(ctLen); input.readFully(ct)
                val plain = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
                out.write(plain)
            }
        }
    }

    // Fix 2: Legacy (WASI_VID) decryption — loads full file, only used for old recordings
    private fun decryptLegacy(input: DataInputStream, encryptedFile: File, outputFile: File) {
        val remaining = encryptedFile.length() - 8  // already consumed magic
        require(remaining >= 12) { "ملف WASI_VID أصغر من المتوقع" }
        val iv = ByteArray(12); input.readFully(iv)
        val ct = ByteArray((remaining - 12).toInt()); input.readFully(ct)
        val plain = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
        outputFile.writeBytes(plain)
    }

    fun deleteVideo(encryptedPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            File(encryptedPath).delete()
        }
    }

    fun resetState() { _state.value = VideoWillState.Idle }

    fun cleanupTempFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            File(context.cacheDir, "video_tmp").deleteRecursively()
            File(context.cacheDir, "video_playback_tmp").deleteRecursively()
        }
    }
}
