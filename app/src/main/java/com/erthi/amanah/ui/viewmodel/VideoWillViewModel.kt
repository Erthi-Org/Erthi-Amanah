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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** Encrypted video will file format: WASI_VID(8 bytes) + iv(12 bytes) + ciphertext */
private const val VIDEO_MAGIC = "WASI_VID"
private const val VIDEO_DIR = "video_wills"
private const val VIDEO_EXTENSION = ".wasi_video"

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
     * Encrypts the plaintext recording at [sourceFile] into encrypted storage.
     * Deletes the plaintext source after successful encryption.
     * Never deletes any existing encrypted video until the new one is verified.
     */
    fun encryptVideo(sourceFile: File, willId: Long? = null) {
        viewModelScope.launch {
            _state.value = VideoWillState.Encrypting
            try {
                val encryptedPath = encryptVideoFile(sourceFile)
                // Verify the encrypted file is valid before cleaning up
                require(File(encryptedPath).length() > 20) {
                    "ملف الفيديو المشفر تالف — الحجم أصغر من المتوقع"
                }
                // Update the will record if a willId was provided
                if (willId != null) {
                    val existing = willDao.getWillById(willId)
                    if (existing != null) {
                        // Delete the old encrypted video only after the new one is confirmed
                        val oldVideoPath = existing.videoPath
                        willDao.updateWill(existing.copy(videoPath = encryptedPath))
                        oldVideoPath?.let { old ->
                            val oldFile = File(old)
                            if (oldFile.exists() && oldFile.absolutePath != encryptedPath) {
                                oldFile.delete()
                            }
                        }
                    }
                }
                // Delete the plaintext recording only after successful encryption
                if (sourceFile.exists()) sourceFile.delete()
                _state.value = VideoWillState.Ready(encryptedPath)
            } catch (e: Exception) {
                _state.value = VideoWillState.Error("فشل تشفير الفيديو: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun encryptVideoFile(source: File): String = withContext(Dispatchers.IO) {
        require(source.exists() && source.length() > 0) {
            "ملف التسجيل المؤقت غير موجود أو فارغ"
        }
        val videoBytes = source.readBytes()
        val enc = crypto.encrypt(videoBytes)
        val outBytes = VIDEO_MAGIC.toByteArray(Charsets.UTF_8) + enc.iv + enc.ciphertext

        val videoDir = File(context.filesDir, VIDEO_DIR).also { it.mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outFile = File(videoDir, "will_video_$ts$VIDEO_EXTENSION")
        outFile.writeBytes(outBytes)
        outFile.absolutePath
    }

    /**
     * Decrypts [encryptedFile] into a temporary plaintext file for playback.
     * The caller is responsible for deleting the temp file when playback ends.
     */
    suspend fun decryptVideoToTemp(encryptedFile: File, cacheDir: File): File? =
        withContext(Dispatchers.IO) {
            try {
                val encBytes = encryptedFile.readBytes()
                require(encBytes.size >= 20) { "ملف الفيديو المشفر صغير جداً" }
                val magic = String(encBytes.copyOfRange(0, 8), Charsets.UTF_8)
                require(magic == VIDEO_MAGIC) { "تنسيق ملف الفيديو غير صالح ($magic)" }
                val iv = encBytes.copyOfRange(8, 20)
                val ct = encBytes.copyOfRange(20, encBytes.size)
                val plainBytes = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
                val tmpDir = File(cacheDir, "video_playback_tmp").also { it.mkdirs() }
                val tmpFile = File(tmpDir, "playback_${System.currentTimeMillis()}.mp4")
                tmpFile.writeBytes(plainBytes)
                tmpFile
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Saves a new will that has an attached encrypted video.
     */
    fun saveVideoWill(title: String, encryptedVideoPath: String) {
        viewModelScope.launch {
            willDao.insertWill(WillEntity(
                title = title.ifBlank { "وصية مصورة" },
                content = "",
                videoPath = encryptedVideoPath,
                isDraft = false,
                legalDisclaimer = false,
            ))
        }
    }

    /**
     * Attaches an encrypted video to an existing will, or creates a new will record.
     */
    fun attachVideoToWill(willId: Long?, title: String, encryptedVideoPath: String) {
        viewModelScope.launch {
            if (willId != null) {
                val existing = willDao.getWillById(willId)
                if (existing != null) {
                    willDao.updateWill(existing.copy(videoPath = encryptedVideoPath))
                    return@launch
                }
            }
            saveVideoWill(title, encryptedVideoPath)
        }
    }

    fun deleteVideo(encryptedPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            File(encryptedPath).delete()
        }
    }

    fun resetState() { _state.value = VideoWillState.Idle }

    // Clean up any stale temp files from previous sessions
    fun cleanupTempFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            File(context.cacheDir, "video_tmp").deleteRecursively()
            File(context.cacheDir, "video_playback_tmp").deleteRecursively()
        }
    }
}
