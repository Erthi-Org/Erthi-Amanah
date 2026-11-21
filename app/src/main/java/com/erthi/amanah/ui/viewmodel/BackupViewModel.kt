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
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.dao.*
import com.erthi.amanah.data.db.entity.*
import com.erthi.amanah.data.security.CryptoManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class BackupState {
    data object Idle : BackupState()
    data object InProgress : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

sealed class DriveState {
    data object Idle : DriveState()
    data object InProgress : DriveState()
    data class Success(val message: String) : DriveState()
    data class Error(val message: String) : DriveState()
}

/** Maximum file size included in a portable archive (video/document). Files larger than this
 *  are excluded with a warning rather than causing an OutOfMemoryError. */
private const val MAX_INLINE_FILE_BYTES = 100 * 1024 * 1024L // 100 MB

/** Magic bytes for video will encrypted files. */
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
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    private val _driveState = MutableStateFlow<DriveState>(DriveState.Idle)
    val driveState: StateFlow<DriveState> = _driveState.asStateFlow()

    private val _driveAccount = MutableStateFlow<GoogleSignInAccount?>(driveManager.getSignedInAccount())
    val driveAccount: StateFlow<GoogleSignInAccount?> = _driveAccount.asStateFlow()

    private val _driveBackups = MutableStateFlow<List<DriveBackupFile>>(emptyList())
    val driveBackups: StateFlow<List<DriveBackupFile>> = _driveBackups.asStateFlow()

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

    fun exportDriveBackup(passphrase: String) {
        viewModelScope.launch {
            _driveState.value = DriveState.InProgress
            try {
                val json = buildPortableBackupJson()
                val plainBytes = json.toByteArray(Charsets.UTF_8)
                val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                val (iv, ciphertext) = BackupCrypto.encrypt(key, plainBytes)
                val encBytes = "WASI_BAK2".toByteArray(Charsets.UTF_8) + salt + iv + ciphertext
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                val fileId = driveManager.uploadBackup(encBytes, "wasiyyati_drive_$dateStr.wasi2bak")
                _driveState.value = DriveState.Success("✅ تم رفع النسخة بنجاح إلى Google Drive ($fileId)")
                loadDriveBackups()
            } catch (e: Exception) {
                _driveState.value = DriveState.Error("❌ فشل الرفع إلى Drive: ${e.localizedMessage}")
            }
        }
    }

    fun importDriveBackup(fileId: String, passphrase: String) {
        viewModelScope.launch {
            _driveState.value = DriveState.InProgress
            try {
                val raw = driveManager.downloadBackup(fileId)
                require(raw.size >= 69) { "ملف النسخة تالف أو غير صالح" }
                val header = String(raw.copyOfRange(0, 9), Charsets.UTF_8)
                if (header != "WASI_BAK2") throw IllegalArgumentException("ملف النسخة تالف أو غير صالح")
                val salt = raw.copyOfRange(9, 41)
                val iv = raw.copyOfRange(41, 53)
                val ct = raw.copyOfRange(53, raw.size)
                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                val plainBytes = try {
                    BackupCrypto.decrypt(key, iv, ct)
                } catch (_: Exception) {
                    throw IllegalArgumentException("كلمة المرور خاطئة أو الملف تالف")
                }
                restorePortableFromJson(String(plainBytes, Charsets.UTF_8))
                _driveState.value = DriveState.Success("✅ تمت الاستعادة من Google Drive بنجاح")
            } catch (e: IllegalArgumentException) {
                _driveState.value = DriveState.Error("❌ ${e.message}")
            } catch (e: Exception) {
                _driveState.value = DriveState.Error("❌ فشل الاستعادة من Drive: ${e.localizedMessage}")
            }
        }
    }

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

    // ── ENC/DEC helpers using device Keystore ─────────────────────────────

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

    // ── Device-bound backup ───────────────────────────────────────────────

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val json = buildBackupJson()
                val plainBytes = json.toByteArray(Charsets.UTF_8)
                val encData = crypto.encrypt(plainBytes)
                val ivBytes = encData.iv
                val ctBytes = encData.ciphertext
                val ivLen = ByteArray(4) { i -> ((ivBytes.size shr (24 - i * 8)) and 0xFF).toByte() }
                val output = "WASI_BAK".toByteArray(Charsets.UTF_8) + ivLen + ivBytes + ctBytes
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("تعذر فتح ملف النسخة للكتابة")
                stream.use { it.write(output) }
                _state.value = BackupState.Success("✅ تم تصدير النسخة الاحتياطية بنجاح — ${plainBytes.size / 1024} كيلوبايت")
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل التصدير: ${e.localizedMessage}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("لا يمكن قراءة الملف")
                require(raw.size >= 40) { "ملف النسخة تالف أو غير صالح" }
                val header = String(raw.copyOfRange(0, 8), Charsets.UTF_8)
                if (header != "WASI_BAK") throw IllegalArgumentException("الملف غير صالح أو تالف")
                val ivLen = ((raw[8].toInt() and 0xFF) shl 24) or
                        ((raw[9].toInt() and 0xFF) shl 16) or
                        ((raw[10].toInt() and 0xFF) shl 8) or
                        (raw[11].toInt() and 0xFF)
                require(ivLen == 12) { "ترويسة التشفير غير صالحة" }
                val iv = raw.copyOfRange(12, 12 + ivLen)
                val ct = raw.copyOfRange(12 + ivLen, raw.size)
                val plainBytes = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
                restoreFromJson(String(plainBytes, Charsets.UTF_8))
                _state.value = BackupState.Success("✅ تمت الاستعادة بنجاح")
            } catch (e: IllegalArgumentException) {
                _state.value = BackupState.Error("❌ ${e.message}")
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل فك التشفير — تأكد أن الملف من نفس الجهاز: ${e.localizedMessage}")
            }
        }
    }

    fun clearState() { _state.value = BackupState.Idle }

    fun exportPortableBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val json = buildPortableBackupJson()
                val plainBytes = json.toByteArray(Charsets.UTF_8)
                val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                val (iv, ciphertext) = BackupCrypto.encrypt(key, plainBytes)
                val output = "WASI_BAK2".toByteArray(Charsets.UTF_8) + salt + iv + ciphertext
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("تعذر فتح ملف النسخة للكتابة")
                stream.use { it.write(output) }
                _state.value = BackupState.Success("✅ تم تصدير النسخة المحمولة — ${plainBytes.size / 1024} كيلوبايت")
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل التصدير المحمول: ${e.localizedMessage}")
            }
        }
    }

    fun importPortableBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("لا يمكن قراءة الملف")
                require(raw.size >= 69) { "ملف النسخة تالف أو غير صالح" }
                val header = String(raw.copyOfRange(0, 9), Charsets.UTF_8)
                if (header != "WASI_BAK2") throw IllegalArgumentException("ملف النسخة ليس نسخة محمولة صالحة")
                val salt = raw.copyOfRange(9, 41)
                val iv = raw.copyOfRange(41, 53)
                val ct = raw.copyOfRange(53, raw.size)
                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                val plainBytes = try {
                    BackupCrypto.decrypt(key, iv, ct)
                } catch (_: Exception) {
                    throw IllegalArgumentException("كلمة المرور خاطئة أو الملف تالف")
                }
                restorePortableFromJson(String(plainBytes, Charsets.UTF_8))
                _state.value = BackupState.Success("✅ تمت الاستعادة المحمولة بنجاح")
            } catch (e: IllegalArgumentException) {
                _state.value = BackupState.Error("❌ ${e.message}")
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل الاستعادة المحمولة: ${e.localizedMessage}")
            }
        }
    }

    // ── Build JSON (device-bound) ─────────────────────────────────────────

    private suspend fun buildBackupJson(): String {
        val wills = willDao.getAllWills().first()
        val debts = debtDao.getAllDebts().first()
        val assets = assetDao.getAllAssets().first()
        val entrusted = entrustedDao.getAllEntrusted().first()
        val beneficiaries = beneficiaryDao.getAllBeneficiaries().first()
        val digitalAccounts = digitalAccountDao.getAllAccounts().first()
        val documents = documentDao.getAllDocuments().first()
        val vaultItems = vaultItemDao.getAllItems().first()

        return JSONObject().apply {
            put("version", 2)
            put("appName", "وصيتي")
            put("exportDate", System.currentTimeMillis())
            put("wills", JSONArray(wills.map { w ->
                JSONObject().apply {
                    put("title", w.title); put("content", w.content)
                    put("isDraft", w.isDraft); put("updatedAt", w.updatedAt)
                    put("createdAt", w.createdAt); put("legalDisclaimer", w.legalDisclaimer)
                    w.audioPath?.let { path ->
                        val f = java.io.File(path)
                        if (f.exists() && f.length() <= MAX_INLINE_FILE_BYTES) {
                            put("audioFileName", f.name)
                            put("audioData", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                        } else if (f.exists()) {
                            put("audioFileName", f.name)
                            // File too large to inline — device backup only stores the path
                        }
                    }
                    w.videoPath?.let { path ->
                        val f = java.io.File(path)
                        if (f.exists() && f.length() <= MAX_INLINE_FILE_BYTES) {
                            put("videoFileName", f.name)
                            put("videoData", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                        } else if (f.exists()) {
                            put("videoFileName", f.name)
                        }
                    }
                }
            }))
            put("debts", buildDebtsArray(debts))
            put("assets", buildAssetsArray(assets))
            put("entrusted", buildEntrustedArray(entrusted))
            put("beneficiaries", buildBeneficiariesArray(beneficiaries))
            put("digitalAccounts", buildDigitalAccountsArray(digitalAccounts))
            put("documents", buildDocumentsArray(documents))
            put("vaultItems", JSONArray(vaultItems.map { v ->
                JSONObject().apply {
                    put("serviceName", v.serviceName); put("category", v.category.name)
                    put("password", v.password); put("username", v.username ?: "")
                    put("email", v.email ?: ""); put("websiteUrl", v.websiteUrl ?: "")
                    put("accountIdentifier", v.accountIdentifier ?: "")
                    put("recoveryEmail", v.recoveryEmail ?: "")
                    put("recoveryPhone", v.recoveryPhone ?: "")
                    put("recoveryCodes", v.recoveryCodes ?: ""); put("notes", v.notes ?: "")
                    put("updatedAt", v.updatedAt)
                }
            }))
        }.toString()
    }

    // ── Build JSON (portable: Keystore-decrypt audio/video before archiving) ─

    private suspend fun buildPortableBackupJson(): String {
        val wills = willDao.getAllWills().first()
        val debts = debtDao.getAllDebts().first()
        val assets = assetDao.getAllAssets().first()
        val entrusted = entrustedDao.getAllEntrusted().first()
        val beneficiaries = beneficiaryDao.getAllBeneficiaries().first()
        val digitalAccounts = digitalAccountDao.getAllAccounts().first()
        val documents = documentDao.getAllDocuments().first()
        val vaultItems = vaultItemDao.getAllItems().first()

        return JSONObject().apply {
            put("version", 2)
            put("appName", "وصيتي")
            put("exportDate", System.currentTimeMillis())
            put("wills", JSONArray(wills.map { w ->
                JSONObject().apply {
                    put("title", w.title); put("content", w.content)
                    put("isDraft", w.isDraft); put("updatedAt", w.updatedAt)
                    put("createdAt", w.createdAt); put("legalDisclaimer", w.legalDisclaimer)

                    // Audio: decrypt WASI_AUD from Keystore and embed raw bytes.
                    // FAIL the entire backup if an existing audio file cannot be read/decrypted —
                    // never produce a silent partial backup.
                    w.audioPath?.let { path ->
                        val f = java.io.File(path)
                        if (f.exists()) {
                            val encBytes = f.readBytes()
                            if (encBytes.size >= 20 &&
                                String(encBytes.copyOfRange(0, 8), Charsets.UTF_8) == "WASI_AUD") {
                                val rawAudio = try {
                                    crypto.decrypt(CryptoManager.EncryptedData(
                                        encBytes.copyOfRange(8, 20),
                                        encBytes.copyOfRange(20, encBytes.size)))
                                } catch (e: Exception) {
                                    throw IllegalStateException(
                                        "فشل فك تشفير الصوت للوصية «${w.title}» — " +
                                        "لا يمكن إنشاء نسخة محمولة كاملة. " +
                                        "تأكد أن هذا الجهاز هو جهاز التسجيل الأصلي.")
                                }
                                put("audioFileName", f.nameWithoutExtension + ".3gp")
                                put("audioData", Base64.encodeToString(rawAudio, Base64.NO_WRAP))
                            } else {
                                throw IllegalStateException(
                                    "ملف الصوت للوصية «${w.title}» تالف أو بتنسيق غير متوقع.")
                            }
                        }
                        // If file doesn't exist at the path, the DB record is stale — log but skip.
                    }

                    // Video: decrypt WASI_VID from Keystore and embed raw bytes.
                    // Skip (with annotation) if file exceeds MAX_INLINE_FILE_BYTES.
                    w.videoPath?.let { path ->
                        val f = java.io.File(path)
                        if (f.exists()) {
                            if (f.length() > MAX_INLINE_FILE_BYTES) {
                                put("videoFileName", f.name)
                                put("videoSkipped", true)
                                put("videoSkipReason", "الحجم أكبر من ${MAX_INLINE_FILE_BYTES / 1024 / 1024} ميجابايت")
                            } else {
                                val encBytes = f.readBytes()
                                if (encBytes.size >= 20 &&
                                    String(encBytes.copyOfRange(0, 8), Charsets.UTF_8) == WASI_VID_MAGIC) {
                                    val rawVideo = try {
                                        crypto.decrypt(CryptoManager.EncryptedData(
                                            encBytes.copyOfRange(8, 20),
                                            encBytes.copyOfRange(20, encBytes.size)))
                                    } catch (e: Exception) {
                                        throw IllegalStateException(
                                            "فشل فك تشفير الفيديو للوصية «${w.title}» — " +
                                            "لا يمكن إنشاء نسخة محمولة كاملة.")
                                    }
                                    put("videoFileName", f.nameWithoutExtension + ".mp4")
                                    put("videoData", Base64.encodeToString(rawVideo, Base64.NO_WRAP))
                                } else {
                                    throw IllegalStateException(
                                        "ملف الفيديو للوصية «${w.title}» تالف أو بتنسيق غير متوقع.")
                                }
                            }
                        }
                    }
                }
            }))
            put("debts", buildDebtsArray(debts))
            put("assets", buildAssetsArray(assets))
            put("entrusted", buildEntrustedArray(entrusted))
            put("beneficiaries", buildBeneficiariesArray(beneficiaries))
            put("digitalAccounts", buildDigitalAccountsArray(digitalAccounts))
            put("documents", buildDocumentsArray(documents))
            put("vaultItems", JSONArray(vaultItems.map { v ->
                JSONObject().apply {
                    put("serviceName", v.serviceName.dec()); put("category", v.category.name)
                    put("password", v.password.dec()); put("username", v.username?.dec() ?: "")
                    put("email", v.email?.dec() ?: ""); put("websiteUrl", v.websiteUrl?.dec() ?: "")
                    put("accountIdentifier", v.accountIdentifier?.dec() ?: "")
                    put("recoveryEmail", v.recoveryEmail?.dec() ?: "")
                    put("recoveryPhone", v.recoveryPhone?.dec() ?: "")
                    put("recoveryCodes", v.recoveryCodes?.dec() ?: ""); put("notes", v.notes?.dec() ?: "")
                    put("updatedAt", v.updatedAt)
                }
            }))
        }.toString()
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

    private fun buildDocumentsArray(documents: List<DocumentEntity>) = JSONArray(documents.map { doc ->
        JSONObject().apply {
            put("title", doc.title); put("category", doc.category)
            put("mimeType", doc.mimeType); put("notes", doc.notes)
            if (doc.filePath.isNotEmpty()) {
                val f = java.io.File(doc.filePath)
                put("fileName", f.name)
                if (f.exists() && f.length() <= MAX_INLINE_FILE_BYTES) {
                    put("fileData", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                }
            }
        }
    })

    // ── Restore entry points ──────────────────────────────────────────────

    private suspend fun restoreFromJson(json: String) = restoreAllStaged(json, reEncrypt = false)
    private suspend fun restorePortableFromJson(json: String) = restoreAllStaged(json, reEncrypt = true)

    // ── Staged restore ────────────────────────────────────────────────────

    private data class StagedFile(val staged: java.io.File, val final: java.io.File)

    private suspend fun restoreAllStaged(json: String, reEncrypt: Boolean) {
        // Phase 0: pre-validate structure before touching any existing data
        val root = JSONObject(json)
        require(root.optInt("version") == 2) { "إصدار ملف النسخة غير مدعوم" }
        val requiredKeys = listOf("wills", "debts", "assets", "entrusted",
            "beneficiaries", "digitalAccounts", "documents", "vaultItems")
        for (key in requiredKeys) {
            require(root.has(key)) { "بيانات النسخة ناقصة: قسم «$key» مفقود" }
        }

        // Phase 1: stage all media files (do not touch existing data yet)
        val stagingDir = java.io.File(context.cacheDir, "restore_staging_${System.currentTimeMillis()}")
            .also { it.mkdirs() }
        val stagedFiles = mutableListOf<StagedFile>()
        try {
            // Pre-stage files so we know the filesystem writes will succeed
            val willsArr = root.getJSONArray("wills")
            for (i in 0 until willsArr.length()) {
                val w = willsArr.getJSONObject(i)
                stageWillMediaFiles(w, i, stagingDir, stagedFiles, reEncrypt)
            }
            val docsArr = root.getJSONArray("documents")
            for (i in 0 until docsArr.length()) {
                val doc = docsArr.getJSONObject(i)
                stageDocumentFile(doc, i, stagingDir, stagedFiles)
            }

            // Phase 2: DB transaction — delete old data and insert new records
            db.withTransaction {
                willDao.deleteAllWills()
                debtDao.deleteAll()
                assetDao.deleteAll()
                entrustedDao.deleteAll()
                beneficiaryDao.deleteAll()
                digitalAccountDao.deleteAll()
                documentDao.deleteAll()
                vaultItemDao.deleteAll()
                restoreWillsFromStaged(willsArr, stagedFiles, reEncrypt)
                restoreDebts(root)
                restoreAssets(root)
                restoreEntrusted(root)
                restoreBeneficiaries(root)
                restoreDigitalAccounts(root)
                restoreDocumentsFromStaged(docsArr, stagedFiles)
                restoreVaultItems(root, reEncrypt)
            }

            // Phase 3: DB committed — move staged files to final locations
            val failedMoves = mutableListOf<String>()
            for (sf in stagedFiles) {
                sf.final.parentFile?.mkdirs()
                val moved = sf.staged.renameTo(sf.final)
                if (!moved) {
                    try {
                        sf.final.writeBytes(sf.staged.readBytes())
                        sf.staged.delete()
                    } catch (e: Exception) {
                        failedMoves += "${sf.final.name}: ${e.message}"
                    }
                }
            }
            if (failedMoves.isNotEmpty()) {
                // DB is committed, but some files could not be moved to final location.
                // Report this clearly — do NOT report as success.
                throw IllegalStateException(
                    "تمت الاستعادة جزئياً — فشل نقل ${failedMoves.size} ملف. " +
                    "أعد تثبيت النسخة الاحتياطية: ${failedMoves.take(2).joinToString()}")
            }
        } catch (e: Exception) {
            // Clean up staging directory to leave no orphans
            stagingDir.deleteRecursively()
            throw e
        }
        // Success: clean up any leftover staging files
        stagingDir.deleteRecursively()
    }

    // ── Stage media files (called BEFORE DB transaction) ──────────────────

    private fun stageWillMediaFiles(
        w: JSONObject,
        index: Int,
        stagingDir: java.io.File,
        staged: MutableList<StagedFile>,
        reEncrypt: Boolean,
    ) {
        val audioFileName = w.optString("audioFileName", "")
        val audioDataB64 = w.optString("audioData", "")
        if (audioFileName.isNotEmpty() && audioDataB64.isNotEmpty()) {
            val rawBytes = Base64.decode(audioDataB64, Base64.NO_WRAP)
            require(rawBytes.isNotEmpty()) { "بيانات الصوت للوصية ${index + 1} فارغة" }
            val finalDir = java.io.File(context.filesDir, "audio_wills")
            val (stagingBytes, finalFileName) = if (reEncrypt) {
                val enc = crypto.encrypt(rawBytes)
                (WASI_AUD_MAGIC.toByteArray(Charsets.UTF_8) + enc.iv + enc.ciphertext) to
                        (audioFileName.substringBeforeLast(".") + ".wasi_audio")
            } else {
                rawBytes to audioFileName
            }
            val stagingFile = java.io.File(stagingDir, "audio_will_$index")
            stagingFile.writeBytes(stagingBytes)
            staged.add(StagedFile(stagingFile, java.io.File(finalDir, finalFileName)))
        }

        val videoFileName = w.optString("videoFileName", "")
        val videoDataB64 = w.optString("videoData", "")
        val videoSkipped = w.optBoolean("videoSkipped", false)
        if (videoFileName.isNotEmpty() && videoDataB64.isNotEmpty() && !videoSkipped) {
            val rawBytes = Base64.decode(videoDataB64, Base64.NO_WRAP)
            require(rawBytes.isNotEmpty()) { "بيانات الفيديو للوصية ${index + 1} فارغة" }
            val finalDir = java.io.File(context.filesDir, "video_wills")
            val (stagingBytes, finalFileName) = if (reEncrypt) {
                val enc = crypto.encrypt(rawBytes)
                (WASI_VID_MAGIC.toByteArray(Charsets.UTF_8) + enc.iv + enc.ciphertext) to
                        (videoFileName.substringBeforeLast(".") + ".wasi_video")
            } else {
                rawBytes to videoFileName
            }
            val stagingFile = java.io.File(stagingDir, "video_will_$index")
            stagingFile.writeBytes(stagingBytes)
            staged.add(StagedFile(stagingFile, java.io.File(finalDir, finalFileName)))
        }
    }

    private fun stageDocumentFile(
        doc: JSONObject,
        index: Int,
        stagingDir: java.io.File,
        staged: MutableList<StagedFile>,
    ) {
        val fileDataB64 = doc.optString("fileData", "")
        val fileName = doc.optString("fileName", "")
        if (fileName.isNotEmpty() && fileDataB64.isNotEmpty()) {
            val rawBytes = Base64.decode(fileDataB64, Base64.NO_WRAP)
            val finalDir = java.io.File(context.filesDir, "documents")
            val stagingFile = java.io.File(stagingDir, "doc_$index")
            stagingFile.writeBytes(rawBytes)
            staged.add(StagedFile(stagingFile, java.io.File(finalDir, fileName)))
        }
    }

    // ── Restore DB rows using pre-staged file locations ───────────────────

    private suspend fun restoreWillsFromStaged(
        arr: JSONArray,
        staged: List<StagedFile>,
        reEncrypt: Boolean,
    ) {
        // Build maps from staged index → final paths
        val audioDir = java.io.File(context.filesDir, "audio_wills")
        val videoDir = java.io.File(context.filesDir, "video_wills")

        for (i in 0 until arr.length()) {
            val w = arr.getJSONObject(i)
            val audioFileName = w.optString("audioFileName", "")
            val audioDataB64 = w.optString("audioData", "")
            val videoFileName = w.optString("videoFileName", "")
            val videoDataB64 = w.optString("videoData", "")
            val videoSkipped = w.optBoolean("videoSkipped", false)

            var restoredAudioPath: String? = null
            if (audioFileName.isNotEmpty() && audioDataB64.isNotEmpty()) {
                val finalFileName = if (reEncrypt)
                    audioFileName.substringBeforeLast(".") + ".wasi_audio"
                else audioFileName
                restoredAudioPath = java.io.File(audioDir, finalFileName).absolutePath
            }

            var restoredVideoPath: String? = null
            if (videoFileName.isNotEmpty() && !videoSkipped) {
                if (videoDataB64.isNotEmpty()) {
                    val finalFileName = if (reEncrypt)
                        videoFileName.substringBeforeLast(".") + ".wasi_video"
                    else videoFileName
                    restoredVideoPath = java.io.File(videoDir, finalFileName).absolutePath
                }
                // If videoSkipped=true, leave videoPath null (file not restored)
            }

            willDao.insertWill(WillEntity(
                title = w.getString("title"),
                content = w.getString("content"),
                audioPath = restoredAudioPath,
                videoPath = restoredVideoPath,
                isDraft = w.optBoolean("isDraft", true),
                legalDisclaimer = w.optBoolean("legalDisclaimer", false),
                createdAt = w.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = w.optLong("updatedAt", System.currentTimeMillis()),
            ))
        }
    }

    private suspend fun restoreDocumentsFromStaged(
        arr: JSONArray,
        staged: List<StagedFile>,
    ) {
        val finalDir = java.io.File(context.filesDir, "documents")
        for (i in 0 until arr.length()) {
            val doc = arr.getJSONObject(i)
            val fileDataB64 = doc.optString("fileData", "")
            val fileName = doc.optString("fileName", "")
            val legacyFilePath = doc.optString("filePath", "")
            val restoredFilePath = when {
                fileName.isNotEmpty() && fileDataB64.isNotEmpty() ->
                    java.io.File(finalDir, fileName).absolutePath
                legacyFilePath.isNotEmpty() -> legacyFilePath
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

    private companion object {
        const val WASI_AUD_MAGIC = "WASI_AUD"
    }
}
