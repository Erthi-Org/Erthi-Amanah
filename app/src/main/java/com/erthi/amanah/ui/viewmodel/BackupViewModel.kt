package com.erthi.amanah.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.*
import com.erthi.amanah.data.security.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

sealed class BackupState {
    data object Idle : BackupState()
    data object InProgress : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val willDao: WillDao,
    private val debtDao: DebtDao,
    private val assetDao: AssetDao,
    private val entrustedDao: EntrustedDao,
    private val beneficiaryDao: BeneficiaryDao,
    private val digitalAccountDao: DigitalAccountDao,
    private val documentDao: DocumentDao,
    private val vaultItemDao: VaultItemDao,
    private val crypto: CryptoManager,
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val json = buildBackupJson()
                val plainBytes = json.toByteArray(Charsets.UTF_8)
                val encData = crypto.encrypt(plainBytes)
                // Format: magic(8) + iv_len(4) + iv + ciphertext
                val ivBytes = encData.iv
                val ctBytes = encData.ciphertext
                val magic = "WASI_BAK".toByteArray(Charsets.UTF_8)
                val ivLen = ByteArray(4) { i -> ((ivBytes.size shr (24 - i * 8)) and 0xFF).toByte() }
                val output = magic + ivLen + ivBytes + ctBytes

                context.contentResolver.openOutputStream(uri)?.use { it.write(output) }
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
                val raw = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw IllegalStateException("لا يمكن قراءة الملف")

                val magic = "WASI_BAK"
                val header = String(raw.copyOfRange(0, 8), Charsets.UTF_8)
                if (header != magic) throw IllegalArgumentException("الملف غير صالح أو تالف")

                val ivLen = ((raw[8].toInt() and 0xFF) shl 24) or
                        ((raw[9].toInt() and 0xFF) shl 16) or
                        ((raw[10].toInt() and 0xFF) shl 8) or
                        (raw[11].toInt() and 0xFF)
                val iv = raw.copyOfRange(12, 12 + ivLen)
                val ct = raw.copyOfRange(12 + ivLen, raw.size)

                val plainBytes = crypto.decrypt(CryptoManager.EncryptedData(iv, ct))
                val jsonStr = String(plainBytes, Charsets.UTF_8)
                restoreFromJson(jsonStr)

                _state.value = BackupState.Success("✅ تمت الاستعادة بنجاح")
            } catch (e: IllegalArgumentException) {
                _state.value = BackupState.Error("❌ ${e.message}")
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل فك التشفير — تأكد أن الملف من نفس الجهاز: ${e.localizedMessage}")
            }
        }
    }

    fun clearState() { _state.value = BackupState.Idle }

    // ── Portable backup (PBKDF2 + AES-GCM, cross-device) ──────────────────────

    fun exportPortableBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val json = buildBackupJson()
                val plainBytes = json.toByteArray(Charsets.UTF_8)
                val rng = SecureRandom()
                val salt = ByteArray(32).also { rng.nextBytes(it) }
                val iv = ByteArray(12).also { rng.nextBytes(it) }
                val key = pbkdf2Key(passphrase, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
                val ciphertext = cipher.doFinal(plainBytes)
                // Format: WASI_BAK2(8) + salt(32) + iv(12) + ciphertext
                val magic = "WASI_BAK2".toByteArray(Charsets.UTF_8)
                val output = magic + salt + iv + ciphertext
                context.contentResolver.openOutputStream(uri)?.use { it.write(output) }
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
                val raw = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw IllegalStateException("لا يمكن قراءة الملف")
                val magic = "WASI_BAK2"
                val header = String(raw.copyOfRange(0, 9), Charsets.UTF_8)
                if (header != magic) throw IllegalArgumentException("الملف ليس نسخة محمولة صالحة")
                val salt = raw.copyOfRange(9, 41)
                val iv = raw.copyOfRange(41, 53)
                val ct = raw.copyOfRange(53, raw.size)
                val key = pbkdf2Key(passphrase, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                val plainBytes = try {
                    cipher.doFinal(ct)
                } catch (e: Exception) {
                    throw IllegalArgumentException("كلمة المرور خاطئة أو الملف تالف")
                }
                val jsonStr = String(plainBytes, Charsets.UTF_8)
                restoreFromJson(jsonStr)
                _state.value = BackupState.Success("✅ تمت الاستعادة المحمولة بنجاح")
            } catch (e: IllegalArgumentException) {
                _state.value = BackupState.Error("❌ ${e.message}")
            } catch (e: Exception) {
                _state.value = BackupState.Error("❌ فشل الاستعادة المحمولة: ${e.localizedMessage}")
            }
        }
    }

    private fun pbkdf2Key(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 310_000, 256)
        val encoded = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(encoded, "AES")
    }

    private suspend fun buildBackupJson(): String {
        val wills = willDao.getAllWills().first()
        val debts = debtDao.getAllDebts().first()
        val assets = assetDao.getAllAssets().first()
        val entrusted = entrustedDao.getAllEntrusted().first()
        val beneficiaries = beneficiaryDao.getAllBeneficiaries().first()
        val digitalAccounts = digitalAccountDao.getAllAccounts().first()
        val documents = documentDao.getAllDocuments().first()
        val vaultItems = vaultItemDao.getAllItems().first()

        val root = JSONObject().apply {
            put("version", 2)
            put("appName", "وصيتي")
            put("exportDate", System.currentTimeMillis())
            put("wills", JSONArray(wills.map { w ->
                JSONObject().apply {
                    put("id", w.id); put("title", w.title); put("content", w.content)
                    put("isDraft", w.isDraft); put("updatedAt", w.updatedAt)
                }
            }))
            put("debts", JSONArray(debts.map { d ->
                JSONObject().apply {
                    put("id", d.id); put("direction", d.direction.name)
                    put("personName", d.personName); put("amount", d.amount)
                    put("currency", d.currency); put("description", d.description ?: "")
                    put("status", d.status.name)
                }
            }))
            put("assets", JSONArray(assets.map { a ->
                JSONObject().apply {
                    put("id", a.id); put("name", a.name); put("type", a.type.name)
                    put("description", a.description ?: ""); put("estimatedValue", a.estimatedValue ?: 0.0)
                    put("location", a.location ?: "")
                }
            }))
            put("beneficiaries", JSONArray(beneficiaries.map { b ->
                JSONObject().apply {
                    put("id", b.id); put("fullName", b.fullName)
                    put("relation", b.relation ?: ""); put("phone", b.phone ?: "")
                    put("email", b.email ?: ""); put("isEmergency", b.isEmergencyContact)
                }
            }))
            put("vaultItems", JSONArray(vaultItems.map { v ->
                JSONObject().apply {
                    put("id", v.id); put("serviceName", v.serviceName)
                    put("category", v.category.name); put("password", v.password)
                    put("username", v.username ?: ""); put("email", v.email ?: "")
                    put("websiteUrl", v.websiteUrl ?: ""); put("notes", v.notes ?: "")
                    put("updatedAt", v.updatedAt)
                }
            }))
        }
        return root.toString()
    }

    private suspend fun restoreFromJson(json: String) {
        val root = JSONObject(json)
        // Wills
        val willsArr = root.optJSONArray("wills")
        if (willsArr != null) {
            repeat(willsArr.length()) { i ->
                val w = willsArr.getJSONObject(i)
                willDao.insertWill(
                    com.erthi.amanah.data.db.entity.WillEntity(
                        title = w.getString("title"),
                        content = w.getString("content"),
                        isDraft = w.optBoolean("isDraft", true),
                        updatedAt = w.optLong("updatedAt", System.currentTimeMillis()),
                    )
                )
            }
        }
        // Beneficiaries
        val benefArr = root.optJSONArray("beneficiaries")
        if (benefArr != null) {
            repeat(benefArr.length()) { i ->
                val b = benefArr.getJSONObject(i)
                beneficiaryDao.insertBeneficiary(
                    com.erthi.amanah.data.db.entity.BeneficiaryEntity(
                        fullName = b.getString("fullName"),
                        relation = b.optString("relation"),
                        phone = b.optString("phone"),
                        email = b.optString("email"),
                        isEmergencyContact = b.optBoolean("isEmergency", false),
                    )
                )
            }
        }
        // VaultItems — restored as-is (still encrypted with original key)
        val vaultArr = root.optJSONArray("vaultItems")
        if (vaultArr != null) {
            repeat(vaultArr.length()) { i ->
                val v = vaultArr.getJSONObject(i)
                vaultItemDao.insert(
                    com.erthi.amanah.data.db.entity.VaultItemEntity(
                        serviceName = v.getString("serviceName"),
                        category = com.erthi.amanah.data.db.entity.VaultCategory.valueOf(
                            v.optString("category", "OTHER")
                        ),
                        password = v.getString("password"),
                        username = v.optString("username").takeIf { it.isNotEmpty() },
                        email = v.optString("email").takeIf { it.isNotEmpty() },
                        websiteUrl = v.optString("websiteUrl").takeIf { it.isNotEmpty() },
                        notes = v.optString("notes").takeIf { it.isNotEmpty() },
                        updatedAt = v.optLong("updatedAt", System.currentTimeMillis()),
                    )
                )
            }
        }
    }
}
