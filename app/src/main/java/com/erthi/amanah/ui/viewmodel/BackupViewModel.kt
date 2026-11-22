package com.erthi.amanah.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.erthi.amanah.data.backup.BackupCrypto
import com.erthi.amanah.data.backup.DriveBackupFile
import com.erthi.amanah.data.backup.DriveBackupManager
import com.erthi.amanah.data.backup.RestoreFiles
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.dao.*
import com.erthi.amanah.data.db.entity.*
import com.erthi.amanah.data.db.dao.RestoreJournalDao
import com.erthi.amanah.data.db.entity.RestoreJournalEntity
import com.erthi.amanah.data.security.CryptoManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

sealed class BackupState {
    data object Idle : BackupState()
    data object InProgress : BackupState()
    data class Success(val message: String, val warnings: List<String> = emptyList()) : BackupState()
    data class Error(val message: String) : BackupState()
}

sealed class DriveState {
    data object Idle : DriveState()
    data object InProgress : DriveState()
    data class Success(val message: String) : DriveState()
    data class Error(val message: String) : DriveState()
}

internal const val WASI_VID_MAGIC = "WASI_VID"

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ErthiDatabase,
    private val willDao: WillDao,
    private val debtDao: DebtDao,
    private val assetDao: AssetDao,
    private val entrustedDao: EntrustedDao,
    private val beneficiaryDao: BeneficiaryDao,
    private val digitalAccountDao: DigitalAccountDao,
    private val documentDao: DocumentDao,
    private val vaultItemDao: VaultItemDao,
    private val crypto: CryptoManager,
    private val driveManager: DriveBackupManager,
    private val restoreJournalDao: RestoreJournalDao,
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    private val _driveState = MutableStateFlow<DriveState>(DriveState.Idle)
    val driveState: StateFlow<DriveState> = _driveState.asStateFlow()

    private val _driveAccount = MutableStateFlow<GoogleSignInAccount?>(driveManager.getSignedInAccount())
    val driveAccount: StateFlow<GoogleSignInAccount?> = _driveAccount.asStateFlow()

    private val _driveBackups = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val driveBackups: StateFlow<List<DriveBackupFile>> = _driveBackups.asStateFlow()

    // ── Drive ─────────────────────────────────────────────────────────────

    fun driveSignInIntent(): Intent = driveManager.buildSignInIntent()

    fun handleDriveSignInResult(data: Intent?) {
        val account = driveManager.handleSignInResult(data)
        _driveAccount.value = account
        if (account != null && driveManager.hasRequiredScopes()) {
            loadDriveBackups()
        } else if (account != null) {
            _driveState.value = DriveState.Error("لم يتم منح صلاحية الوصول إلى Drive. أعد المحاولة.")
        } else {
            _driveState.value = DriveState.Error("فشل تسجيل الدخول إلى Google.")
        }
    }

    fun driveSignOut() {
        viewModelScope.launch {
            driveManager.signOut()
            _driveAccount.value = null
            _driveBackups.value = emptyList()
            _driveState.value = DriveState.Idle
        }
    }

    fun loadDriveBackups() {
        viewModelScope.launch {
            _driveState.value = DriveState.InProgress
            try {
                _driveBackups.value = driveManager.listBackups()
                _driveState.value = DriveState.Idle
            } catch (e: Exception) {
                _driveState.value = DriveState.Error("فشل تحميل قائمة النسخ: ${e.localizedMessage}")
            }
        }
    }

    fun clearDriveState() { _driveState.value = DriveState.Idle }
    fun clearState() { _state.value = BackupState.Idle }

    fun deleteDriveBackup(fileId: String) {
        viewModelScope.launch {
            _driveState.value = DriveState.InProgress
            try {
                driveManager.deleteBackup(fileId)
                _driveState.value = DriveState.Success("✅ تم حذف النسخة من Drive")
                loadDriveBackups()
            } catch (e: Exception) {
                _driveState.value = DriveState.Error("❌ فشل الحذف: ${e.localizedMessage}")
            }
        }
    }

    // ── ENC/DEC helpers ───────────────────────────────────────────────────

    private val ENC_PREFIX = "[enc]"

    private fun String.enc(): String {
        val encData = crypto.encrypt(toByteArray(Charsets.UTF_8))
        return ENC_PREFIX + Base64.encodeToString(encData.toByteArray(), Base64.NO_WRAP)
    }

    private fun String.dec(): String {
        if (!startsWith(ENC_PREFIX)) return this
        val bytes = Base64.decode(removePrefix(ENC_PREFIX), Base64.NO_WRAP)
        return String(crypto.decrypt(CryptoManager.EncryptedData.fromByteArray(bytes)), Charsets.UTF_8)
    }

    // ── Export ────────────────────────────────────────────────────────────

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val tmpFile = File(context.cacheDir, "bak4_tmp_${System.currentTimeMillis()}")
                try {
                    withContext(Dispatchers.IO) { buildDeviceStreamingArchive(tmpFile) }
                    context.contentResolver.openOutputStream(uri)
                        ?.use { tmpFile.inputStream().copyTo(it) }
                        ?: throw IllegalStateException("تعذر فتح ملف النسخة للكتابة")
                    _state.value = BackupState.Success("✅ تم تصدير النسخة بنجاح — ${tmpFile.length() / 1024} كيلوبايت")
                } finally {
                    tmpFile.delete()
                }
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل التصدير: ${e.localizedMessage}")
            }
        }
    }

    fun exportPortableBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                val tmpFile = File(context.cacheDir, "bak3_tmp_${System.currentTimeMillis()}")
                try {
                    withContext(Dispatchers.IO) { buildPortableStreamingArchive(key, salt, tmpFile) }
                    context.contentResolver.openOutputStream(uri)
                        ?.use { tmpFile.inputStream().copyTo(it) }
                        ?: throw IllegalStateException("تعذر فتح ملف النسخة للكتابة")
                    _state.value = BackupState.Success("✅ تم تصدير النسخة المحمولة — ${tmpFile.length() / 1024} كيلوبايت")
                } finally {
                    tmpFile.delete()
                }
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل التصدير المحمول: ${e.localizedMessage}")
            }
        }
    }

    fun exportDriveBackup(passphrase: String) {
        viewModelScope.launch {
            _driveState.value = DriveState.InProgress
            try {
                val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                val tmpFile = File(context.cacheDir, "drive_tmp_${System.currentTimeMillis()}")
                try {
                    withContext(Dispatchers.IO) { buildPortableStreamingArchive(key, salt, tmpFile) }
                    val archiveSize = tmpFile.length()
                    require(archiveSize <= MAX_DRIVE_UPLOAD_BYTES) {
                        "حجم النسخة ${archiveSize / 1024 / 1024} ميجابايت يتجاوز حد الرفع (${MAX_DRIVE_UPLOAD_BYTES / 1024 / 1024} ميجابايت)"
                    }
                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    val driveFileId = driveManager.uploadBackup(tmpFile.readBytes(), "wasiyyati_drive_$dateStr.wasi3bak")
                    _driveState.value = DriveState.Success("✅ تم رفع النسخة بنجاح إلى Google Drive ($driveFileId)")
                    loadDriveBackups()
                } finally {
                    tmpFile.delete()
                }
            } catch (e: Exception) {
                _driveState.value = DriveState.Error("❌ فشل الرفع إلى Drive: ${e.localizedMessage}")
            }
        }
    }

    // ── Import ────────────────────────────────────────────────────────────

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                withContext(Dispatchers.IO) {
                    val bis = context.contentResolver.openInputStream(uri)?.buffered()
                        ?: throw IllegalStateException("لا يمكن قراءة الملف")
                    DataInputStream(bis).use { din ->
                        val magic9 = ByteArray(9).also { din.readFully(it) }
                        val magic9Str = String(magic9, Charsets.UTF_8)
                        val magic8Str = String(magic9, 0, 8, Charsets.UTF_8)
                        when {
                            magic9Str == WASI_BAK4_MAGIC -> restoreFromDeviceArchive(din)
                            magic9Str == WASI_BAK3_MAGIC ->
                                throw IllegalArgumentException("هذه نسخة محمولة. استخدم «استعادة نسخة محمولة».")
                            magic8Str == "WASI_BAK" -> {
                                // Last byte of magic9 is the first byte of the 4-byte ivLen
                                val ivLen = ((magic9[8].toInt() and 0xFF) shl 24) or
                                        ((din.read() and 0xFF) shl 16) or
                                        ((din.read() and 0xFF) shl 8) or (din.read() and 0xFF)
                                require(ivLen == 12) { "ترويسة التشفير غير صالحة" }
                                val iv = ByteArray(12).also { din.readFully(it) }
                                val ct = din.readBoundedBytes(MAX_LEGACY_ARCHIVE_BYTES)
                                val plain = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
                                restoreAllStaged(String(plain, Charsets.UTF_8), reEncrypt = false)
                            }
                            else -> throw IllegalArgumentException("الملف غير صالح أو تالف")
                        }
                    }
                }
                _state.value = BackupState.Success("✅ تمت الاستعادة بنجاح")
            } catch (e: IllegalArgumentException) {
                _state.value = BackupState.Error("❌ ${e.message}")
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل فك التشفير — تأكد أن الملف من نفس الجهاز: ${e.localizedMessage}")
            }
        }
    }

    fun importPortableBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                withContext(Dispatchers.IO) {
                    val bis = context.contentResolver.openInputStream(uri)?.buffered()
                        ?: throw IllegalStateException("لا يمكن قراءة الملف")
                    DataInputStream(bis).use { din ->
                        val magic9 = ByteArray(9).also { din.readFully(it) }
                        val magic9Str = String(magic9, Charsets.UTF_8)
                        when (magic9Str) {
                            WASI_BAK3_MAGIC -> restoreFromPortableArchive(din, passphrase)
                            WASI_BAK2_MAGIC -> {
                                val salt = ByteArray(32).also { din.readFully(it) }
                                val iv = ByteArray(12).also { din.readFully(it) }
                                val ct = din.readBoundedBytes(MAX_LEGACY_ARCHIVE_BYTES)
                                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                                val plain = try { BackupCrypto.decrypt(key, iv, ct) }
                                catch (_: Exception) { throw IllegalArgumentException("كلمة المرور خاطئة أو الملف تالف") }
                                restoreAllStaged(String(plain, Charsets.UTF_8), reEncrypt = true)
                            }
                            else -> throw IllegalArgumentException("ملف النسخة ليس نسخة محمولة صالحة")
                        }
                    }
                }
                _state.value = BackupState.Success("✅ تمت الاستعادة المحمولة بنجاح")
            } catch (e: IllegalArgumentException) {
                _state.value = BackupState.Error("❌ ${e.message}")
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل الاستعادة المحمولة: ${e.localizedMessage}")
            }
        }
    }

    fun importDriveBackup(fileId: String, passphrase: String) {
        viewModelScope.launch {
            _driveState.value = DriveState.InProgress
            try {
                val raw = driveManager.downloadBackup(fileId)
                require(raw.size >= 9) { "ملف النسخة تالف أو غير صالح" }
                require(raw.size <= MAX_DRIVE_DOWNLOAD_BYTES) { "حجم ملف Drive يتجاوز الحد المسموح به" }
                withContext(Dispatchers.IO) {
                    DataInputStream(raw.inputStream()).use { din ->
                        val magic9 = ByteArray(9).also { din.readFully(it) }
                        when (String(magic9, Charsets.UTF_8)) {
                            WASI_BAK3_MAGIC -> restoreFromPortableArchive(din, passphrase)
                            WASI_BAK2_MAGIC -> {
                                val salt = ByteArray(32).also { din.readFully(it) }
                                val iv = ByteArray(12).also { din.readFully(it) }
                                val ct = din.readBoundedBytes(MAX_LEGACY_ARCHIVE_BYTES)
                                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                                val plain = try { BackupCrypto.decrypt(key, iv, ct) }
                                catch (_: Exception) { throw IllegalArgumentException("كلمة المرور خاطئة أو الملف تالف") }
                                restoreAllStaged(String(plain, Charsets.UTF_8), reEncrypt = true)
                            }
                            else -> throw IllegalArgumentException("ملف النسخة تالف أو غير صالح")
                        }
                    }
                }
                _driveState.value = DriveState.Success("✅ تمت الاستعادة من Google Drive بنجاح")
            } catch (e: IllegalArgumentException) {
                _driveState.value = DriveState.Error("❌ ${e.message}")
            } catch (e: Exception) {
                _driveState.value = DriveState.Error("❌ فشل الاستعادة من Drive: ${e.localizedMessage}")
            }
        }
    }

    // ── Streaming archive builders ────────────────────────────────────────

    private suspend fun buildDeviceStreamingArchive(tmpFile: File) {
        val wills = willDao.getAllWills().first()
        val debts = debtDao.getAllDebts().first()
        val assets = assetDao.getAllAssets().first()
        val entrusted = entrustedDao.getAllEntrusted().first()
        val beneficiaries = beneficiaryDao.getAllBeneficiaries().first()
        val digitalAccounts = digitalAccountDao.getAllAccounts().first()
        val documents = documentDao.getAllDocuments().first()
        val vaultItems = vaultItemDao.getAllItems().first()
        val mediaFiles = mutableListOf<MediaFile>()
        val metaJson = buildMetadataJsonV3(
            wills, debts, assets, entrusted, beneficiaries,
            digitalAccounts, documents, vaultItems, mediaFiles, portable = false,
        )
        val metaBytes = metaJson.toByteArray(Charsets.UTF_8)
        require(metaBytes.size <= MAX_METADATA_BYTES) { "حجم بيانات التعريف يتجاوز الحد" }
        val encMeta = crypto.encrypt(metaBytes)
        DataOutputStream(tmpFile.outputStream().buffered()).use { out ->
            out.write(WASI_BAK4_MAGIC.toByteArray(Charsets.UTF_8))
            out.write(encMeta.iv)
            out.writeInt(encMeta.ciphertext.size)
            out.write(encMeta.ciphertext)
            out.writeInt(mediaFiles.size)
            for (mf in mediaFiles) {
                val idBytes = mf.fileId.toByteArray(Charsets.UTF_8)
                out.writeShort(idBytes.size)
                out.write(idBytes)
                exportFileToDeviceArchive(mf.file, out)
            }
        }
    }

    private suspend fun buildPortableStreamingArchive(key: SecretKeySpec, salt: ByteArray, tmpFile: File) {
        val wills = willDao.getAllWills().first()
        val debts = debtDao.getAllDebts().first()
        val assets = assetDao.getAllAssets().first()
        val entrusted = entrustedDao.getAllEntrusted().first()
        val beneficiaries = beneficiaryDao.getAllBeneficiaries().first()
        val digitalAccounts = digitalAccountDao.getAllAccounts().first()
        val documents = documentDao.getAllDocuments().first()
        val vaultItems = vaultItemDao.getAllItems().first()
        val mediaFiles = mutableListOf<MediaFile>()
        val metaJson = buildMetadataJsonV3(
            wills, debts, assets, entrusted, beneficiaries,
            digitalAccounts, documents, vaultItems, mediaFiles, portable = true,
        )
        val metaBytes = metaJson.toByteArray(Charsets.UTF_8)
        require(metaBytes.size <= MAX_METADATA_BYTES) { "حجم بيانات التعريف يتجاوز الحد" }
        val (metaIv, metaCt) = BackupCrypto.encrypt(key, metaBytes)
        DataOutputStream(tmpFile.outputStream().buffered()).use { out ->
            out.write(WASI_BAK3_MAGIC.toByteArray(Charsets.UTF_8))
            out.write(salt)
            out.write(metaIv)
            out.writeInt(metaCt.size)
            out.write(metaCt)
            out.writeInt(mediaFiles.size)
            for (mf in mediaFiles) {
                val idBytes = mf.fileId.toByteArray(Charsets.UTF_8)
                out.writeShort(idBytes.size)
                out.write(idBytes)
                when (mf.type) {
                    MediaType.AUDIO -> exportAudioToPortableArchive(mf.file, key, out)
                    MediaType.VIDEO -> exportVideoToPortableArchive(mf.file, key, out)
                    MediaType.DOCUMENT -> exportDocumentToPortableArchive(mf.file, key, out)
                }
            }
        }
    }

    private fun buildMetadataJsonV3(
        wills: List<WillEntity>,
        debts: List<DebtEntity>,
        assets: List<AssetEntity>,
        entrusted: List<EntrustedEntity>,
        beneficiaries: List<BeneficiaryEntity>,
        digitalAccounts: List<DigitalAccountEntity>,
        documents: List<DocumentEntity>,
        vaultItems: List<VaultItemEntity>,
        mediaFiles: MutableList<MediaFile>,
        portable: Boolean,
    ): String {
        val dec: (String?) -> String = { s -> s?.dec() ?: "" }
        return JSONObject().apply {
            put("version", 3)
            put("appName", "وصيتي")
            put("exportDate", System.currentTimeMillis())
            put("wills", JSONArray(wills.mapIndexed { i, w ->
                JSONObject().apply {
                    put("title", w.title); put("content", w.content)
                    put("isDraft", w.isDraft); put("updatedAt", w.updatedAt)
                    put("createdAt", w.createdAt); put("legalDisclaimer", w.legalDisclaimer)
                    w.audioPath?.let { path ->
                        val f = File(path)
                        if (f.exists()) {
                            val fid = "audio_will_$i"
                            put("audioFileId", fid)
                            mediaFiles.add(MediaFile(fid, f, MediaType.AUDIO))
                        }
                    }
                    w.videoPath?.let { path ->
                        val f = File(path)
                        if (f.exists()) {
                            val fid = "video_will_$i"
                            put("videoFileId", fid)
                            mediaFiles.add(MediaFile(fid, f, MediaType.VIDEO))
                        }
                    }
                }
            }))
            put("debts", buildDebtsArray(debts))
            put("assets", buildAssetsArray(assets))
            put("entrusted", buildEntrustedArray(entrusted))
            put("beneficiaries", buildBeneficiariesArray(beneficiaries))
            put("digitalAccounts", buildDigitalAccountsArray(digitalAccounts))
            put("documents", JSONArray(documents.mapIndexed { i, doc ->
                JSONObject().apply {
                    put("title", doc.title); put("category", doc.category)
                    put("mimeType", doc.mimeType); put("notes", doc.notes)
                    if (doc.filePath.isNotEmpty()) {
                        val f = File(doc.filePath)
                        if (f.exists()) {
                            val fid = "doc_$i"
                            put("fileId", fid); put("fileName", f.name)
                            mediaFiles.add(MediaFile(fid, f, MediaType.DOCUMENT))
                        }
                    }
                }
            }))
            put("vaultItems", JSONArray(vaultItems.map { v ->
                JSONObject().apply {
                    put("serviceName", if (portable) dec(v.serviceName) else v.serviceName)
                    put("category", v.category.name)
                    put("password", if (portable) dec(v.password) else v.password)
                    put("username", if (portable) dec(v.username) else v.username ?: "")
                    put("email", if (portable) dec(v.email) else v.email ?: "")
                    put("websiteUrl", if (portable) dec(v.websiteUrl) else v.websiteUrl ?: "")
                    put("accountIdentifier", if (portable) dec(v.accountIdentifier) else v.accountIdentifier ?: "")
                    put("recoveryEmail", if (portable) dec(v.recoveryEmail) else v.recoveryEmail ?: "")
                    put("recoveryPhone", if (portable) dec(v.recoveryPhone) else v.recoveryPhone ?: "")
                    put("recoveryCodes", if (portable) dec(v.recoveryCodes) else v.recoveryCodes ?: "")
                    put("notes", if (portable) dec(v.notes) else v.notes ?: "")
                    put("updatedAt", v.updatedAt)
                }
            }))
        }.toString()
    }

    // ── BAK4 export helpers ───────────────────────────────────────────────

    private fun exportFileToDeviceArchive(file: File, out: DataOutputStream) {
        val fileSize = file.length()
        val chunkCount = ((fileSize + BACKUP_CHUNK_SIZE - 1) / BACKUP_CHUNK_SIZE).toInt().coerceAtLeast(1)
        out.writeInt(chunkCount)
        val buf = ByteArray(BACKUP_CHUNK_SIZE)
        file.inputStream().buffered().use { ins ->
            var remaining = fileSize
            while (remaining > 0) {
                val toRead = minOf(BACKUP_CHUNK_SIZE.toLong(), remaining).toInt()
                var read = 0
                while (read < toRead) {
                    val r = ins.read(buf, read, toRead - read)
                    if (r == -1) break
                    read += r
                }
                out.writeInt(read)
                out.write(buf, 0, read)
                remaining -= read
            }
        }
    }

    // ── BAK3 export helpers ───────────────────────────────────────────────

    private fun exportAudioToPortableArchive(audioFile: File, key: SecretKeySpec, out: DataOutputStream) {
        val fileSize = audioFile.length()
        require(fileSize <= MAX_AUDIO_FILE_BYTES) {
            "ملف الصوت ${audioFile.name} حجمه ${fileSize / 1024 / 1024} ميجابايت — يتجاوز الحد"
        }
        DataInputStream(audioFile.inputStream().buffered()).use { din ->
            val magicBytes = ByteArray(8); din.readFully(magicBytes)
            when (val magic = String(magicBytes, Charsets.UTF_8)) {
                WASI_AUD_MAGIC -> {
                    // Legacy single-block format: iv(12) + ciphertext
                    val ivBytes = ByteArray(12); din.readFully(ivBytes)
                    val ct = din.readBytes()
                    val raw = crypto.decrypt(CryptoManager.EncryptedData(ivBytes, ct))
                    writePortableChunks(raw, key, out)
                }
                WASI_AU2_MAGIC -> {
                    // Chunked format: chunkCount(4) + [iv(12)+ctLen(4)+ct]*N — re-encrypt each chunk
                    val chunkCount = din.readInt()
                    require(chunkCount in 1..MAX_CHUNK_COUNT) { "عدد أجزاء الصوت غير صالح: $chunkCount" }
                    out.writeInt(chunkCount)
                    repeat(chunkCount) {
                        val iv = ByteArray(12); din.readFully(iv)
                        val ctLen = din.readInt()
                        require(ctLen in 1..MAX_CHUNK_BYTES) { "حجم جزء الصوت غير صالح: $ctLen" }
                        val ct = ByteArray(ctLen); din.readFully(ct)
                        val rawChunk = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
                        val (piv, pct) = BackupCrypto.encrypt(key, rawChunk)
                        out.write(piv); out.writeInt(pct.size); out.write(pct)
                    }
                }
                else -> throw IllegalStateException("ملف الصوت بتنسيق غير متوقع: $magic — ${audioFile.name}")
            }
        }
    }

    private fun exportVideoToPortableArchive(videoFile: File, key: SecretKeySpec, out: DataOutputStream) {
        DataInputStream(videoFile.inputStream().buffered()).use { din ->
            val magic = ByteArray(8); din.readFully(magic)
            when (val magicStr = String(magic, Charsets.UTF_8)) {
                "WASI_VD2" -> {
                    val chunkCount = din.readInt()
                    require(chunkCount in 1..MAX_CHUNK_COUNT) { "عدد أجزاء الفيديو غير صالح: $chunkCount" }
                    out.writeInt(chunkCount)
                    repeat(chunkCount) {
                        val iv = ByteArray(12); din.readFully(iv)
                        val ctLen = din.readInt()
                        require(ctLen in 1..MAX_CHUNK_BYTES) { "حجم جزء الفيديو غير صالح: $ctLen" }
                        val ct = ByteArray(ctLen); din.readFully(ct)
                        val rawChunk = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
                        val (piv, pct) = BackupCrypto.encrypt(key, rawChunk)
                        out.write(piv); out.writeInt(pct.size); out.write(pct)
                    }
                }
                WASI_VID_MAGIC -> {
                    // Legacy single-block video — bounded load
                    val fileSize = videoFile.length()
                    require(fileSize <= MAX_LEGACY_SINGLE_BLOCK_BYTES) {
                        "الفيديو القديم حجمه ${fileSize / 1024 / 1024} ميجابايت — يتجاوز حد المعالجة في الذاكرة"
                    }
                    val iv = ByteArray(12); din.readFully(iv)
                    val ctSize = (fileSize - 20).toInt()
                    require(ctSize > 0) { "بيانات الفيديو القديم تالفة" }
                    val ct = ByteArray(ctSize); din.readFully(ct)
                    val raw = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
                    writePortableChunks(raw, key, out)
                }
                else -> throw IllegalStateException("تنسيق فيديو غير متوقع: $magicStr")
            }
        }
    }

    private fun exportDocumentToPortableArchive(docFile: File, key: SecretKeySpec, out: DataOutputStream) {
        val fileSize = docFile.length()
        val chunkCount = ((fileSize + BACKUP_CHUNK_SIZE - 1) / BACKUP_CHUNK_SIZE).toInt().coerceAtLeast(1)
        out.writeInt(chunkCount)
        val buf = ByteArray(BACKUP_CHUNK_SIZE)
        docFile.inputStream().buffered().use { ins ->
            var remaining = fileSize
            while (remaining > 0) {
                val toRead = minOf(BACKUP_CHUNK_SIZE.toLong(), remaining).toInt()
                var read = 0
                while (read < toRead) {
                    val r = ins.read(buf, read, toRead - read)
                    if (r == -1) break
                    read += r
                }
                val chunk = buf.copyOf(read)
                val (iv, ct) = BackupCrypto.encrypt(key, chunk)
                out.write(iv); out.writeInt(ct.size); out.write(ct)
                remaining -= read
            }
        }
    }

    private fun writePortableChunks(raw: ByteArray, key: SecretKeySpec, out: DataOutputStream) {
        val chunkCount = ((raw.size + BACKUP_CHUNK_SIZE - 1) / BACKUP_CHUNK_SIZE).coerceAtLeast(1)
        out.writeInt(chunkCount)
        var offset = 0
        while (offset < raw.size) {
            val end = minOf(offset + BACKUP_CHUNK_SIZE, raw.size)
            val (iv, ct) = BackupCrypto.encrypt(key, raw.copyOfRange(offset, end))
            out.write(iv); out.writeInt(ct.size); out.write(ct)
            offset = end
        }
        if (raw.isEmpty()) {
            val (iv, ct) = BackupCrypto.encrypt(key, ByteArray(0))
            out.write(iv); out.writeInt(ct.size); out.write(ct)
        }
    }

    // ── BAK4 restore (streaming) ───────────────────────────────────────────

    private suspend fun restoreFromDeviceArchive(din: DataInputStream) {
        val metaIv = ByteArray(12); din.readFully(metaIv)
        val metaCtLen = din.readInt()
        require(metaCtLen in 1..MAX_METADATA_BYTES) { "حجم بيانات التعريف غير صالح" }
        val metaCt = ByteArray(metaCtLen); din.readFully(metaCt)
        val metaBytes = crypto.decrypt(CryptoManager.EncryptedData(metaIv, metaCt))
        val metaJson = JSONObject(String(metaBytes, Charsets.UTF_8))
        val stagingDir = Files.createTempDirectory(context.filesDir.toPath(), "restore_staging_").toFile()
        val stagedFiles = mutableListOf<StagedFile>()
        // Once restoreAllStagedWithJournal is called, the Room journal owns the staging dir.
        // The caller must not delete it — the recovery worker will clean up after verified moves.
        var journalOwned = false
        try {
            val fileCount = din.readInt()
            require(fileCount in 0..MAX_ARCHIVE_FILE_COUNT) { "عدد الملفات في النسخة غير صالح: $fileCount" }
            val seenIds = mutableSetOf<String>()
            val stagedByFileId = mutableMapOf<String, File>()
            repeat(fileCount) {
                val idLen = din.readUnsignedShort()
                require(idLen in 1..MAX_FILE_ID_LEN) { "طول معرف الملف غير صالح: $idLen" }
                val idBytes = ByteArray(idLen); din.readFully(idBytes)
                val fileId = String(idBytes, Charsets.UTF_8)
                validateFileId(fileId)
                require(seenIds.add(fileId)) { "معرف ملف مكرر في النسخة: $fileId" }
                val stagingFile = File(stagingDir, fileId)
                stageFileFromDeviceArchive(din, stagingFile)
                stagedByFileId[fileId] = stagingFile
            }
            require(din.read() == -1) { "Trailing archive data" }
            buildStagedFilesFromStreamingMetadata(metaJson, stagedByFileId, stagedFiles)
            journalOwned = true  // DB commit may happen inside; staging dir is now journal's
            restoreAllStagedWithJournal(metaJson, stagingDir, stagedFiles, reEncrypt = false)
        } catch (e: Exception) {
            // Safe to delete only if the journal was never committed (pre-commit failure).
            // restoreAllStagedWithJournal already deleted staging on pre-commit failures.
            // On post-commit failures, staging must survive for the recovery worker.
            if (!journalOwned) stagingDir.deleteRecursively()
            throw e
        }
    }

    private fun stageFileFromDeviceArchive(din: DataInputStream, stagingFile: File) {
        val chunkCount = din.readInt()
        require(chunkCount in 1..MAX_CHUNK_COUNT) { "عدد الأجزاء غير صالح: $chunkCount" }
        stagingFile.outputStream().buffered().use { out ->
            repeat(chunkCount) {
                val rawLen = din.readInt()
                require(rawLen in 0..MAX_CHUNK_BYTES) { "حجم الجزء غير صالح: $rawLen" }
                val raw = ByteArray(rawLen); din.readFully(raw)
                out.write(raw)
            }
        }
    }

    // ── BAK3 restore (streaming) ──────────────────────────────────────────

    private suspend fun restoreFromPortableArchive(din: DataInputStream, passphrase: String) {
        val salt = ByteArray(32); din.readFully(salt)
        val metaIv = ByteArray(12); din.readFully(metaIv)
        val metaCtLen = din.readInt()
        require(metaCtLen in 1..MAX_METADATA_BYTES) { "حجم بيانات التعريف غير صالح" }
        val metaCt = ByteArray(metaCtLen); din.readFully(metaCt)
        val key = BackupCrypto.pbkdf2Key(passphrase, salt)
        val metaBytes = try { BackupCrypto.decrypt(key, metaIv, metaCt) }
        catch (_: Exception) { throw IllegalArgumentException("كلمة المرور خاطئة أو الملف تالف") }
        val metaJson = JSONObject(String(metaBytes, Charsets.UTF_8))
        val stagingDir = Files.createTempDirectory(context.filesDir.toPath(), "restore_staging_").toFile()
        val stagedFiles = mutableListOf<StagedFile>()
        var journalOwned = false
        try {
            val fileCount = din.readInt()
            require(fileCount in 0..MAX_ARCHIVE_FILE_COUNT) { "عدد الملفات غير صالح: $fileCount" }
            val seenIds = mutableSetOf<String>()
            val stagedByFileId = mutableMapOf<String, File>()
            repeat(fileCount) {
                val idLen = din.readUnsignedShort()
                require(idLen in 1..MAX_FILE_ID_LEN) { "طول معرف الملف غير صالح: $idLen" }
                val idBytes = ByteArray(idLen); din.readFully(idBytes)
                val fileId = String(idBytes, Charsets.UTF_8)
                validateFileId(fileId)
                require(seenIds.add(fileId)) { "معرف ملف مكرر: $fileId" }
                val stagingFile = File(stagingDir, fileId)
                when {
                    fileId.startsWith("audio_will_") -> stageAudioFromPortableArchive(din, stagingFile, key)
                    fileId.startsWith("video_will_") -> stageVideoFromPortableArchive(din, stagingFile, key)
                    else -> stageDocumentFromPortableArchive(din, stagingFile, key)
                }
                stagedByFileId[fileId] = stagingFile
            }
            require(din.read() == -1) { "Trailing archive data" }
            buildStagedFilesFromStreamingMetadata(metaJson, stagedByFileId, stagedFiles)
            journalOwned = true
            restoreAllStagedWithJournal(metaJson, stagingDir, stagedFiles, reEncrypt = true)
        } catch (e: Exception) {
            if (!journalOwned) stagingDir.deleteRecursively()
            throw e
        }
    }

    // Writes WASI_AU2: magic(8) + chunkCount(4) + [iv(12)+ctLen(4)+ct]*N
    // Each portable chunk is decrypted then immediately re-encrypted with the device key,
    // keeping memory bounded to one chunk at a time regardless of total audio size.
    private fun stageAudioFromPortableArchive(din: DataInputStream, stagingFile: File, key: SecretKeySpec) {
        val chunkCount = din.readInt()
        require(chunkCount in 1..MAX_CHUNK_COUNT) { "عدد أجزاء الصوت غير صالح" }
        DataOutputStream(stagingFile.outputStream().buffered()).use { out ->
            out.write(WASI_AU2_MAGIC.toByteArray(Charsets.UTF_8))
            out.writeInt(chunkCount)
            repeat(chunkCount) {
                val iv = ByteArray(12); din.readFully(iv)
                val ctLen = din.readInt()
                require(ctLen in 1..MAX_CHUNK_BYTES) { "حجم جزء الصوت غير صالح: $ctLen" }
                val ct = ByteArray(ctLen); din.readFully(ct)
                val rawChunk = BackupCrypto.decrypt(key, iv, ct)
                val enc = crypto.encrypt(rawChunk)
                out.write(enc.iv); out.writeInt(enc.ciphertext.size); out.write(enc.ciphertext)
            }
        }
    }

    private fun stageVideoFromPortableArchive(din: DataInputStream, stagingFile: File, key: SecretKeySpec) {
        val chunkCount = din.readInt()
        require(chunkCount in 1..MAX_CHUNK_COUNT) { "عدد أجزاء الفيديو غير صالح: $chunkCount" }
        DataOutputStream(stagingFile.outputStream().buffered()).use { out ->
            out.write("WASI_VD2".toByteArray(Charsets.UTF_8))
            out.writeInt(chunkCount)
            repeat(chunkCount) {
                val iv = ByteArray(12); din.readFully(iv)
                val ctLen = din.readInt()
                require(ctLen in 1..MAX_CHUNK_BYTES) { "حجم جزء الفيديو غير صالح: $ctLen" }
                val ct = ByteArray(ctLen); din.readFully(ct)
                val rawChunk = BackupCrypto.decrypt(key, iv, ct)
                val enc = crypto.encrypt(rawChunk)
                out.write(enc.iv); out.writeInt(enc.ciphertext.size); out.write(enc.ciphertext)
            }
        }
    }

    private fun stageDocumentFromPortableArchive(din: DataInputStream, stagingFile: File, key: SecretKeySpec) {
        val chunkCount = din.readInt()
        require(chunkCount in 1..MAX_CHUNK_COUNT) { "عدد أجزاء المستند غير صالح: $chunkCount" }
        stagingFile.outputStream().buffered().use { out ->
            repeat(chunkCount) {
                val iv = ByteArray(12); din.readFully(iv)
                val ctLen = din.readInt()
                require(ctLen in 1..MAX_CHUNK_BYTES) { "حجم جزء المستند غير صالح: $ctLen" }
                val ct = ByteArray(ctLen); din.readFully(ct)
                out.write(BackupCrypto.decrypt(key, iv, ct))
            }
        }
    }

    private fun buildStagedFilesFromStreamingMetadata(
        metaJson: JSONObject,
        stagedByFileId: Map<String, File>,
        stagedFiles: MutableList<StagedFile>,
    ) {
        val audioDir = File(context.filesDir, "audio_wills")
        val videoDir = File(context.filesDir, "video_wills")
        val docsDir = File(context.filesDir, "documents")
        val willsArr = metaJson.optJSONArray("wills") ?: JSONArray()
        for (i in 0 until willsArr.length()) {
            val w = willsArr.getJSONObject(i)
            val audioFid = w.optString("audioFileId", "")
            val videoFid = w.optString("videoFileId", "")
            if (audioFid.isNotEmpty()) {
                val stagedAudio = stagedByFileId[audioFid]
                    ?: throw IllegalStateException("ملف الصوت مرجعه في البيانات لكنه غائب من الأرشيف: $audioFid")
                stagedFiles.add(StagedFile(stagedAudio, File(audioDir, "$audioFid.wasi_audio")))
            }
            if (videoFid.isNotEmpty()) {
                val stagedVideo = stagedByFileId[videoFid]
                    ?: throw IllegalStateException("ملف الفيديو مرجعه في البيانات لكنه غائب من الأرشيف: $videoFid")
                stagedFiles.add(StagedFile(stagedVideo, File(videoDir, "$videoFid.wasi_video")))
            }
        }
        val docsArr = metaJson.optJSONArray("documents") ?: JSONArray()
        for (i in 0 until docsArr.length()) {
            val doc = docsArr.getJSONObject(i)
            val fid = doc.optString("fileId", "")
            val fileName = doc.optString("fileName", "")
            if (fid.isNotEmpty() && fileName.isNotEmpty()) {
                validateFileId(fileName)
                val stagedDoc = stagedByFileId[fid]
                    ?: throw IllegalStateException("ملف المستند مرجعه في البيانات لكنه غائب من الأرشيف: $fid")
                stagedFiles.add(StagedFile(stagedDoc, File(docsDir, fileName)))
            }
        }
    }

    // ── Atomic restore with Room journal ─────────────────────────────────

    private suspend fun restoreAllStagedWithJournal(
        metaJson: JSONObject,
        stagingDir: File,
        stagedFiles: List<StagedFile>,
        reEncrypt: Boolean,
    ) = RestoreFiles.mutex.withLock {
        val sessionId = UUID.randomUUID().toString()
        var committed = false
        try {
            check(restoreJournalDao.getPendingJournal() == null) { "An earlier restore is pending" }
            val filesDir = context.filesDir.canonicalFile
            val allowedDirectories = setOf("documents", "audio_wills", "video_wills")
                .map { File(filesDir, it) }.toSet()
            val entries = stagedFiles.map { sf ->
                val dest = sf.final.canonicalFile
                require(dest == sf.final.absoluteFile && dest.parentFile in allowedDirectories) {
                    "Invalid restore destination"
                }
                require(sf.staged.isFile) { "Missing staged restore file" }
                // Stage data durably before transferring ownership to the Room journal.
                java.io.RandomAccessFile(sf.staged, "rw").use { it.fd.sync() }
                RestoreFiles.Entry(sf.staged, dest, sf.staged.length(), RestoreFiles.sha256(sf.staged))
            }
            require(entries.map { it.destination }.toSet().size == entries.size) {
                "Duplicate restore destinations"
            }
            val journalJson = JSONArray(entries.map { it.toJson() }).toString()
            withContext(NonCancellable) {
                db.withTransaction {
                    willDao.deleteAllWills()
                    debtDao.deleteAll()
                    assetDao.deleteAll()
                    entrustedDao.deleteAll()
                    beneficiaryDao.deleteAll()
                    digitalAccountDao.deleteAll()
                    documentDao.deleteAll()
                    vaultItemDao.deleteAll()
                    val willsArr = metaJson.getJSONArray("wills")
                    val docsArr = metaJson.getJSONArray("documents")
                    restoreWillsFromStaged(willsArr, stagedFiles, reEncrypt)
                    restoreDebts(metaJson)
                    restoreAssets(metaJson)
                    restoreEntrusted(metaJson)
                    restoreBeneficiaries(metaJson)
                    restoreDigitalAccounts(metaJson)
                    restoreDocumentsFromStaged(docsArr, stagedFiles)
                    restoreVaultItems(metaJson, reEncrypt)
                    restoreJournalDao.insertJournal(
                        RestoreJournalEntity(sessionId, journalJson, stagingDir.absolutePath)
                    )
                }
                committed = true
            }
            entries.forEach(RestoreFiles::publish)
            restoreJournalDao.deleteJournal(sessionId)
            stagingDir.deleteRecursively()
        } finally {
            if (!committed) stagingDir.deleteRecursively()
        }
    }

    // ── Legacy BAK/BAK2 restore ───────────────────────────────────────────

    private data class StagedFile(val staged: File, val final: File)

    private suspend fun restoreAllStaged(json: String, reEncrypt: Boolean) {
        val root = JSONObject(json)
        val version = root.optInt("version")
        require(version in 1..2) { "إصدار ملف النسخة غير مدعوم: $version" }
        val requiredKeys = listOf("wills", "debts", "assets", "entrusted",
            "beneficiaries", "digitalAccounts", "documents", "vaultItems")
        for (key in requiredKeys) require(root.has(key)) { "بيانات ناقصة: $key" }
        val stagingDir = Files.createTempDirectory(context.filesDir.toPath(), "restore_staging_").toFile()
        val stagedFiles = mutableListOf<StagedFile>()
        var journalOwned = false
        try {
            val willsArr = root.getJSONArray("wills")
            for (i in 0 until willsArr.length()) {
                stageWillMediaFiles(willsArr.getJSONObject(i), i, stagingDir, stagedFiles, reEncrypt)
            }
            val docsArr = root.getJSONArray("documents")
            for (i in 0 until docsArr.length()) {
                stageDocumentFile(docsArr.getJSONObject(i), i, stagingDir, stagedFiles)
            }
            journalOwned = true
            restoreAllStagedWithJournal(root, stagingDir, stagedFiles, reEncrypt)
        } catch (e: Exception) {
            if (!journalOwned) stagingDir.deleteRecursively()
            throw e
        }
    }

    private fun stageWillMediaFiles(
        w: JSONObject, index: Int, stagingDir: File,
        staged: MutableList<StagedFile>, reEncrypt: Boolean,
    ) {
        val audioFileName = w.optString("audioFileName", "")
        val audioDataB64 = w.optString("audioData", "")
        if (audioFileName.isNotEmpty() && audioDataB64.isNotEmpty()) {
            val rawBytes = Base64.decode(audioDataB64, Base64.NO_WRAP)
            require(rawBytes.isNotEmpty()) { "بيانات الصوت للوصية ${index + 1} فارغة" }
            val finalDir = File(context.filesDir, "audio_wills")
            val (stagingBytes, finalFileName) = if (reEncrypt) {
                val enc = crypto.encrypt(rawBytes)
                (WASI_AUD_MAGIC.toByteArray(Charsets.UTF_8) + enc.iv + enc.ciphertext) to
                        (audioFileName.substringBeforeLast(".") + ".wasi_audio")
            } else rawBytes to audioFileName
            val stagingFile = File(stagingDir, "audio_will_$index")
            stagingFile.writeBytes(stagingBytes)
            staged.add(StagedFile(stagingFile, File(finalDir, finalFileName)))
        }
        val videoFileName = w.optString("videoFileName", "")
        val videoDataB64 = w.optString("videoData", "")
        val videoSkipped = w.optBoolean("videoSkipped", false)
        if (videoFileName.isNotEmpty() && videoDataB64.isNotEmpty() && !videoSkipped) {
            val rawBytes = Base64.decode(videoDataB64, Base64.NO_WRAP)
            require(rawBytes.isNotEmpty()) { "بيانات الفيديو للوصية ${index + 1} فارغة" }
            val finalDir = File(context.filesDir, "video_wills")
            val (stagingBytes, finalFileName) = if (reEncrypt) {
                val enc = crypto.encrypt(rawBytes)
                (WASI_VID_MAGIC.toByteArray(Charsets.UTF_8) + enc.iv + enc.ciphertext) to
                        (videoFileName.substringBeforeLast(".") + ".wasi_video")
            } else rawBytes to videoFileName
            val stagingFile = File(stagingDir, "video_will_$index")
            stagingFile.writeBytes(stagingBytes)
            staged.add(StagedFile(stagingFile, File(finalDir, finalFileName)))
        }
    }

    private fun stageDocumentFile(
        doc: JSONObject, index: Int, stagingDir: File, staged: MutableList<StagedFile>,
    ) {
        val fileDataB64 = doc.optString("fileData", "")
        val fileName = doc.optString("fileName", "")
        if (fileName.isNotEmpty() && fileDataB64.isNotEmpty()) {
            val rawBytes = Base64.decode(fileDataB64, Base64.NO_WRAP)
            val finalDir = File(context.filesDir, "documents")
            val stagingFile = File(stagingDir, "doc_$index")
            stagingFile.writeBytes(rawBytes)
            staged.add(StagedFile(stagingFile, File(finalDir, fileName)))
        }
    }

    // ── Restore DB rows ───────────────────────────────────────────────────

    private suspend fun restoreWillsFromStaged(
        arr: JSONArray, staged: List<StagedFile>, reEncrypt: Boolean,
    ) {
        val audioDir = File(context.filesDir, "audio_wills")
        val videoDir = File(context.filesDir, "video_wills")
        for (i in 0 until arr.length()) {
            val w = arr.getJSONObject(i)
            val audioFileId = w.optString("audioFileId", "")
            val videoFileId = w.optString("videoFileId", "")
            val audioFileName = w.optString("audioFileName", "")
            val audioDataB64 = w.optString("audioData", "")
            val videoFileName = w.optString("videoFileName", "")
            val videoDataB64 = w.optString("videoData", "")
            val videoSkipped = w.optBoolean("videoSkipped", false)
            val restoredAudioPath: String? = when {
                audioFileId.isNotEmpty() -> File(audioDir, "$audioFileId.wasi_audio").absolutePath
                audioFileName.isNotEmpty() && audioDataB64.isNotEmpty() -> {
                    val fn = if (reEncrypt) audioFileName.substringBeforeLast(".") + ".wasi_audio" else audioFileName
                    File(audioDir, fn).absolutePath
                }
                else -> null
            }
            val restoredVideoPath: String? = when {
                videoFileId.isNotEmpty() -> File(videoDir, "$videoFileId.wasi_video").absolutePath
                videoFileName.isNotEmpty() && !videoSkipped && videoDataB64.isNotEmpty() -> {
                    val fn = if (reEncrypt) videoFileName.substringBeforeLast(".") + ".wasi_video" else videoFileName
                    File(videoDir, fn).absolutePath
                }
                else -> null
            }
            willDao.insertWill(WillEntity(
                title = w.getString("title"), content = w.getString("content"),
                audioPath = restoredAudioPath, videoPath = restoredVideoPath,
                isDraft = w.optBoolean("isDraft", true),
                legalDisclaimer = w.optBoolean("legalDisclaimer", false),
                createdAt = w.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = w.optLong("updatedAt", System.currentTimeMillis()),
            ))
        }
    }

    private suspend fun restoreDocumentsFromStaged(arr: JSONArray, staged: List<StagedFile>) {
        val finalDir = File(context.filesDir, "documents")
        for (i in 0 until arr.length()) {
            val doc = arr.getJSONObject(i)
            val fileId = doc.optString("fileId", "")
            val fileDataB64 = doc.optString("fileData", "")
            val fileName = doc.optString("fileName", "")
            val legacyFilePath = doc.optString("filePath", "")
            val restoredFilePath = when {
                fileId.isNotEmpty() && fileName.isNotEmpty() -> File(finalDir, fileName).absolutePath
                fileName.isNotEmpty() && fileDataB64.isNotEmpty() -> File(finalDir, fileName).absolutePath
                legacyFilePath.isNotEmpty() -> File(legacyFilePath).canonicalFile.let { existing ->
                    require(existing.parentFile == finalDir.canonicalFile && existing.isFile) {
                        "Missing or invalid legacy attachment"
                    }
                    existing.absolutePath
                }
                else -> ""
            }
            documentDao.insertDocument(DocumentEntity(
                title = doc.getString("title"),
                category = doc.optString("category", ""),
                filePath = restoredFilePath,
                mimeType = doc.optString("mimeType", "application/octet-stream"),
                notes = doc.optString("notes", ""),
            ))
        }
    }

    private suspend fun restoreDebts(root: JSONObject) {
        val arr = root.getJSONArray("debts")
        for (i in 0 until arr.length()) {
            val d = arr.getJSONObject(i)
            debtDao.insertDebt(DebtEntity(
                direction = DebtDirection.valueOf(d.getString("direction")),
                personName = d.getString("personName"),
                amount = d.getDouble("amount"),
                currency = d.optString("currency", "EGP"),
                dueDate = d.optLong("dueDate", 0L).takeIf { it > 0L },
                description = d.optString("description", ""),
                evidence = d.optString("evidence", ""),
                status = RepaymentStatus.valueOf(d.getString("status")),
            ))
        }
    }

    private suspend fun restoreAssets(root: JSONObject) {
        val arr = root.getJSONArray("assets")
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            assetDao.insertAsset(AssetEntity(
                type = AssetType.valueOf(a.getString("type")),
                name = a.getString("name"),
                description = a.optString("description", ""),
                estimatedValue = a.optDouble("estimatedValue", 0.0).takeIf { it > 0.0 },
                currency = a.optString("currency", "EGP"),
                location = a.optString("location", ""),
                accountReference = a.optString("accountReference", ""),
                notes = a.optString("notes", ""),
            ))
        }
    }

    private suspend fun restoreEntrusted(root: JSONObject) {
        val arr = root.getJSONArray("entrusted")
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            entrustedDao.insertEntrusted(EntrustedEntity(
                ownerName = e.getString("ownerName"),
                ownerPhone = e.optString("ownerPhone", ""),
                itemDescription = e.getString("itemDescription"),
                returnInstructions = e.optString("returnInstructions", ""),
                evidence = e.optString("evidence", ""),
            ))
        }
    }

    private suspend fun restoreBeneficiaries(root: JSONObject) {
        val arr = root.getJSONArray("beneficiaries")
        for (i in 0 until arr.length()) {
            val b = arr.getJSONObject(i)
            beneficiaryDao.insertBeneficiary(BeneficiaryEntity(
                fullName = b.getString("fullName"),
                relation = b.optString("relation", ""),
                phone = b.optString("phone", ""),
                email = b.optString("email", ""),
                nationalId = b.optString("nationalId", ""),
                isEmergencyContact = b.optBoolean("isEmergency", false),
                emergencyPriority = b.optInt("emergencyPriority", 0),
                canAccessWill = b.optBoolean("canAccessWill", false),
                notes = b.optString("notes", ""),
            ))
        }
    }

    private suspend fun restoreDigitalAccounts(root: JSONObject) {
        val arr = root.getJSONArray("digitalAccounts")
        for (i in 0 until arr.length()) {
            val da = arr.getJSONObject(i)
            digitalAccountDao.insertAccount(DigitalAccountEntity(
                platform = da.getString("platform"),
                accountIdentifier = da.getString("accountIdentifier"),
                instructions = da.optString("instructions", ""),
                subscriptionCost = da.optDouble("subscriptionCost", 0.0).takeIf { it > 0.0 },
                currency = da.optString("currency", "EGP"),
                notes = da.optString("notes", ""),
            ))
        }
    }

    private suspend fun restoreVaultItems(root: JSONObject, reEncrypt: Boolean) {
        val arr = root.getJSONArray("vaultItems")
        for (i in 0 until arr.length()) {
            val v = arr.getJSONObject(i)
            val category = runCatching { VaultCategory.valueOf(v.optString("category", "OTHER")) }
                .getOrDefault(VaultCategory.OTHER)
            val encOrRaw: (String) -> String? = { raw ->
                raw.takeIf { it.isNotEmpty() }?.let { if (reEncrypt) it.enc() else it }
            }
            vaultItemDao.insert(VaultItemEntity(
                serviceName = if (reEncrypt) v.getString("serviceName").enc() else v.getString("serviceName"),
                category = category,
                password = if (reEncrypt) v.getString("password").enc() else v.getString("password"),
                username = encOrRaw(v.optString("username", "")),
                email = encOrRaw(v.optString("email", "")),
                websiteUrl = encOrRaw(v.optString("websiteUrl", "")),
                accountIdentifier = encOrRaw(v.optString("accountIdentifier", "")),
                recoveryEmail = encOrRaw(v.optString("recoveryEmail", "")),
                recoveryPhone = encOrRaw(v.optString("recoveryPhone", "")),
                recoveryCodes = encOrRaw(v.optString("recoveryCodes", "")),
                notes = encOrRaw(v.optString("notes", "")),
                updatedAt = v.optLong("updatedAt", System.currentTimeMillis()),
            ))
        }
    }

    // ── Shared array builders ─────────────────────────────────────────────

    private fun buildDebtsArray(debts: List<DebtEntity>) = JSONArray(debts.map { d ->
        JSONObject().apply {
            put("direction", d.direction.name); put("personName", d.personName)
            put("amount", d.amount); put("currency", d.currency)
            put("dueDate", d.dueDate ?: 0L); put("description", d.description)
            put("evidence", d.evidence); put("status", d.status.name)
        }
    })

    private fun buildAssetsArray(assets: List<AssetEntity>) = JSONArray(assets.map { a ->
        JSONObject().apply {
            put("type", a.type.name); put("name", a.name)
            put("description", a.description); put("estimatedValue", a.estimatedValue ?: 0.0)
            put("currency", a.currency); put("location", a.location)
            put("accountReference", a.accountReference); put("notes", a.notes)
        }
    })

    private fun buildEntrustedArray(entrusted: List<EntrustedEntity>) = JSONArray(entrusted.map { e ->
        JSONObject().apply {
            put("ownerName", e.ownerName); put("ownerPhone", e.ownerPhone)
            put("itemDescription", e.itemDescription)
            put("returnInstructions", e.returnInstructions); put("evidence", e.evidence)
        }
    })

    private fun buildBeneficiariesArray(beneficiaries: List<BeneficiaryEntity>) = JSONArray(beneficiaries.map { b ->
        JSONObject().apply {
            put("fullName", b.fullName); put("relation", b.relation)
            put("phone", b.phone); put("email", b.email)
            put("nationalId", b.nationalId)
            put("isEmergency", b.isEmergencyContact); put("emergencyPriority", b.emergencyPriority)
            put("canAccessWill", b.canAccessWill); put("notes", b.notes)
        }
    })

    private fun buildDigitalAccountsArray(accounts: List<DigitalAccountEntity>) = JSONArray(accounts.map { da ->
        JSONObject().apply {
            put("platform", da.platform); put("accountIdentifier", da.accountIdentifier)
            put("instructions", da.instructions)
            put("subscriptionCost", da.subscriptionCost ?: 0.0); put("currency", da.currency)
            put("notes", da.notes)
        }
    })

    // ── Validation helpers ────────────────────────────────────────────────

    private fun validateFileId(id: String) {
        require(id.length <= MAX_FILE_ID_LEN) { "معرف الملف طويل جداً" }
        require(!id.contains('/') && !id.contains('\\') && !id.contains("..")) {
            "معرف الملف يحتوي على محارف غير مسموح بها: $id"
        }
        require(id.none { it < ' ' }) { "معرف الملف يحتوي على محارف تحكم" }
    }


    private fun DataInputStream.readBoundedBytes(limit: Int): ByteArray {
        val buf = java.io.ByteArrayOutputStream()
        val tmp = ByteArray(STREAM_COPY_BUF)
        var total = 0
        var n: Int
        while (read(tmp).also { n = it } != -1) {
            total += n
            require(total <= limit) { "الأرشيف أكبر من الحد المسموح به ($limit بايت)" }
            buf.write(tmp, 0, n)
        }
        return buf.toByteArray()
    }

    private data class MediaFile(val fileId: String, val file: File, val type: MediaType)
    private enum class MediaType { AUDIO, VIDEO, DOCUMENT }

    private companion object {
        const val WASI_AUD_MAGIC = "WASI_AUD"
        const val WASI_AU2_MAGIC = "WASI_AU2"  // chunked device-encrypted audio (bounded memory)
        const val WASI_BAK2_MAGIC = "WASI_BAK2"
        const val WASI_BAK3_MAGIC = "WASI_BAK3"
        const val WASI_BAK4_MAGIC = "WASI_BAK4"
        const val BACKUP_CHUNK_SIZE = 4 * 1024 * 1024
        const val STREAM_COPY_BUF = 256 * 1024
        const val MAX_METADATA_BYTES = 10 * 1024 * 1024        // 10 MB metadata
        const val MAX_CHUNK_BYTES = 5 * 1024 * 1024            // 5 MB per encrypted chunk
        const val MAX_CHUNK_COUNT = 500_000                    // ~2 TB at 4MB chunks
        const val MAX_ARCHIVE_FILE_COUNT = 10_000
        const val MAX_FILE_ID_LEN = 128
        const val MAX_LEGACY_ARCHIVE_BYTES = 200 * 1024 * 1024  // 200 MB legacy BAK/BAK2
        const val MAX_LEGACY_SINGLE_BLOCK_BYTES = 200 * 1024 * 1024L
        const val MAX_AUDIO_FILE_BYTES = 100 * 1024 * 1024L    // 100 MB per audio (voice memos)
        const val MAX_DRIVE_UPLOAD_BYTES = 500 * 1024 * 1024L  // 500 MB Drive upload limit
        const val MAX_DRIVE_DOWNLOAD_BYTES = 500 * 1024 * 1024 // 500 MB Drive download limit
    }
}
