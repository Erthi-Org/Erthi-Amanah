package com.erthi.amanah.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.erthi.amanah.data.backup.BackupCrypto
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.db.dao.*
import com.erthi.amanah.data.db.entity.*
import com.erthi.amanah.data.security.CryptoManager
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
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    // ── ENC/DEC helpers using device Keystore ─────────────────────────────

    private val ENC_PREFIX = "[enc]"

    private fun String.enc(): String {
        val bytes = toByteArray(Charsets.UTF_8)
        val encData = crypto.encrypt(bytes)
        return ENC_PREFIX + Base64.encodeToString(encData.toByteArray(), Base64.NO_WRAP)
    }

    private fun String.dec(): String {
        if (!startsWith(ENC_PREFIX)) return this
        return try {
            val bytes = Base64.decode(removePrefix(ENC_PREFIX), Base64.NO_WRAP)
            String(crypto.decrypt(CryptoManager.EncryptedData.fromByteArray(bytes)), Charsets.UTF_8)
        } catch (_: Exception) { this }
    }

    // ── Device-bound backup (Android Keystore encrypted, same device only) ─

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val json = buildBackupJson()
                val plainBytes = json.toByteArray(Charsets.UTF_8)
                val encData = crypto.encrypt(plainBytes)
                val magic = "WASI_BAK".toByteArray(Charsets.UTF_8)
                val ivBytes = encData.iv
                val ctBytes = encData.ciphertext
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
                val header = String(raw.copyOfRange(0, 8), Charsets.UTF_8)
                if (header != "WASI_BAK") throw IllegalArgumentException("الملف غير صالح أو تالف")
                val ivLen = ((raw[8].toInt() and 0xFF) shl 24) or
                        ((raw[9].toInt() and 0xFF) shl 16) or
                        ((raw[10].toInt() and 0xFF) shl 8) or
                        (raw[11].toInt() and 0xFF)
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

    // ── Portable backup (PBKDF2 + AES-GCM, cross-device) ─────────────────

    fun exportPortableBackup(uri: Uri, passphrase: String) {
        viewModelScope.launch {
            _state.value = BackupState.InProgress
            try {
                val json = buildPortableBackupJson()
                val plainBytes = json.toByteArray(Charsets.UTF_8)
                val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
                val key = BackupCrypto.pbkdf2Key(passphrase, salt)
                val (iv, ciphertext) = BackupCrypto.encrypt(key, plainBytes)
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
                val header = String(raw.copyOfRange(0, 9), Charsets.UTF_8)
                if (header != "WASI_BAK2") throw IllegalArgumentException("الملف ليس نسخة محمولة صالحة")
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

    // ── Build JSON (device-bound: vault fields kept as [enc] ciphertext) ──

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
                }
            }))
            put("debts", JSONArray(debts.map { d ->
                JSONObject().apply {
                    put("direction", d.direction.name); put("personName", d.personName)
                    put("amount", d.amount); put("currency", d.currency)
                    put("dueDate", d.dueDate ?: 0L); put("description", d.description)
                    put("evidence", d.evidence); put("status", d.status.name)
                }
            }))
            put("assets", JSONArray(assets.map { a ->
                JSONObject().apply {
                    put("type", a.type.name); put("name", a.name)
                    put("description", a.description); put("estimatedValue", a.estimatedValue ?: 0.0)
                    put("currency", a.currency); put("location", a.location)
                    put("accountReference", a.accountReference); put("notes", a.notes)
                }
            }))
            put("entrusted", JSONArray(entrusted.map { e ->
                JSONObject().apply {
                    put("ownerName", e.ownerName); put("ownerPhone", e.ownerPhone)
                    put("itemDescription", e.itemDescription)
                    put("returnInstructions", e.returnInstructions); put("evidence", e.evidence)
                }
            }))
            put("beneficiaries", JSONArray(beneficiaries.map { b ->
                JSONObject().apply {
                    put("fullName", b.fullName); put("relation", b.relation)
                    put("phone", b.phone); put("email", b.email)
                    put("nationalId", b.nationalId)
                    put("isEmergency", b.isEmergencyContact); put("emergencyPriority", b.emergencyPriority)
                    put("canAccessWill", b.canAccessWill); put("notes", b.notes)
                }
            }))
            put("digitalAccounts", JSONArray(digitalAccounts.map { da ->
                JSONObject().apply {
                    put("platform", da.platform); put("accountIdentifier", da.accountIdentifier)
                    put("instructions", da.instructions)
                    put("subscriptionCost", da.subscriptionCost ?: 0.0); put("currency", da.currency)
                    put("notes", da.notes)
                }
            }))
            put("documents", JSONArray(documents.map { doc ->
                JSONObject().apply {
                    put("title", doc.title); put("category", doc.category)
                    put("filePath", doc.filePath); put("mimeType", doc.mimeType); put("notes", doc.notes)
                }
            }))
            // Vault items: raw [enc] values (device-bound, only valid on originating device)
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

    // ── Build JSON (portable: vault fields decrypted to plaintext) ────────

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
                }
            }))
            put("debts", JSONArray(debts.map { d ->
                JSONObject().apply {
                    put("direction", d.direction.name); put("personName", d.personName)
                    put("amount", d.amount); put("currency", d.currency)
                    put("dueDate", d.dueDate ?: 0L); put("description", d.description)
                    put("evidence", d.evidence); put("status", d.status.name)
                }
            }))
            put("assets", JSONArray(assets.map { a ->
                JSONObject().apply {
                    put("type", a.type.name); put("name", a.name)
                    put("description", a.description); put("estimatedValue", a.estimatedValue ?: 0.0)
                    put("currency", a.currency); put("location", a.location)
                    put("accountReference", a.accountReference); put("notes", a.notes)
                }
            }))
            put("entrusted", JSONArray(entrusted.map { e ->
                JSONObject().apply {
                    put("ownerName", e.ownerName); put("ownerPhone", e.ownerPhone)
                    put("itemDescription", e.itemDescription)
                    put("returnInstructions", e.returnInstructions); put("evidence", e.evidence)
                }
            }))
            put("beneficiaries", JSONArray(beneficiaries.map { b ->
                JSONObject().apply {
                    put("fullName", b.fullName); put("relation", b.relation)
                    put("phone", b.phone); put("email", b.email)
                    put("nationalId", b.nationalId)
                    put("isEmergency", b.isEmergencyContact); put("emergencyPriority", b.emergencyPriority)
                    put("canAccessWill", b.canAccessWill); put("notes", b.notes)
                }
            }))
            put("digitalAccounts", JSONArray(digitalAccounts.map { da ->
                JSONObject().apply {
                    put("platform", da.platform); put("accountIdentifier", da.accountIdentifier)
                    put("instructions", da.instructions)
                    put("subscriptionCost", da.subscriptionCost ?: 0.0); put("currency", da.currency)
                    put("notes", da.notes)
                }
            }))
            put("documents", JSONArray(documents.map { doc ->
                JSONObject().apply {
                    put("title", doc.title); put("category", doc.category)
                    put("filePath", doc.filePath); put("mimeType", doc.mimeType); put("notes", doc.notes)
                }
            }))
            // Vault items: plaintext values (decrypted from device Keystore before export)
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

    // ── Restore (device-bound): vault [enc] values stored as-is ──────────

    private suspend fun restoreFromJson(json: String) {
        val root = JSONObject(json)
        db.withTransaction {
            willDao.deleteAll()
            debtDao.deleteAll()
            assetDao.deleteAll()
            entrustedDao.deleteAll()
            beneficiaryDao.deleteAll()
            digitalAccountDao.deleteAll()
            documentDao.deleteAll()
            vaultItemDao.deleteAll()

            restoreWills(root)
            restoreDebts(root)
            restoreAssets(root)
            restoreEntrusted(root)
            restoreBeneficiaries(root)
            restoreDigitalAccounts(root)
            restoreDocuments(root)
            restoreVaultItems(root, reEncrypt = false)
        }
    }

    // ── Restore (portable): vault plaintext values re-encrypted on new device

    private suspend fun restorePortableFromJson(json: String) {
        val root = JSONObject(json)
        db.withTransaction {
            willDao.deleteAll()
            debtDao.deleteAll()
            assetDao.deleteAll()
            entrustedDao.deleteAll()
            beneficiaryDao.deleteAll()
            digitalAccountDao.deleteAll()
            documentDao.deleteAll()
            vaultItemDao.deleteAll()

            restoreWills(root)
            restoreDebts(root)
            restoreAssets(root)
            restoreEntrusted(root)
            restoreBeneficiaries(root)
            restoreDigitalAccounts(root)
            restoreDocuments(root)
            restoreVaultItems(root, reEncrypt = true)
        }
    }

    // ── Individual table restore helpers ──────────────────────────────────

    private suspend fun restoreWills(root: JSONObject) {
        val arr = root.optJSONArray("wills") ?: return
        for (i in 0 until arr.length()) {
            val w = arr.getJSONObject(i)
            willDao.insertWill(WillEntity(
                title = w.getString("title"),
                content = w.getString("content"),
                isDraft = w.optBoolean("isDraft", true),
                updatedAt = w.optLong("updatedAt", System.currentTimeMillis()),
            ))
        }
    }

    private suspend fun restoreDebts(root: JSONObject) {
        val arr = root.optJSONArray("debts") ?: return
        for (i in 0 until arr.length()) {
            val d = arr.getJSONObject(i)
            val direction = runCatching { DebtDirection.valueOf(d.getString("direction")) }.getOrNull() ?: continue
            val status = runCatching { RepaymentStatus.valueOf(d.getString("status")) }.getOrNull() ?: continue
            debtDao.insertDebt(DebtEntity(
                direction = direction,
                personName = d.getString("personName"),
                amount = d.getDouble("amount"),
                currency = d.optString("currency", "EGP"),
                dueDate = d.optLong("dueDate", 0L).takeIf { it > 0L },
                description = d.optString("description", ""),
                evidence = d.optString("evidence", ""),
                status = status,
            ))
        }
    }

    private suspend fun restoreAssets(root: JSONObject) {
        val arr = root.optJSONArray("assets") ?: return
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            val type = runCatching { AssetType.valueOf(a.getString("type")) }.getOrNull() ?: continue
            assetDao.insertAsset(AssetEntity(
                type = type,
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
        val arr = root.optJSONArray("entrusted") ?: return
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
        val arr = root.optJSONArray("beneficiaries") ?: return
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
        val arr = root.optJSONArray("digitalAccounts") ?: return
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

    private suspend fun restoreDocuments(root: JSONObject) {
        val arr = root.optJSONArray("documents") ?: return
        for (i in 0 until arr.length()) {
            val doc = arr.getJSONObject(i)
            documentDao.insertDocument(DocumentEntity(
                title = doc.getString("title"),
                category = doc.optString("category", ""),
                filePath = doc.optString("filePath", ""),
                mimeType = doc.optString("mimeType", "application/octet-stream"),
                notes = doc.optString("notes", ""),
            ))
        }
    }

    private suspend fun restoreVaultItems(root: JSONObject, reEncrypt: Boolean) {
        val arr = root.optJSONArray("vaultItems") ?: return
        for (i in 0 until arr.length()) {
            val v = arr.getJSONObject(i)
            val category = runCatching {
                VaultCategory.valueOf(v.optString("category", "OTHER"))
            }.getOrDefault(VaultCategory.OTHER)
            val encOrRaw: (String) -> String? = { raw ->
                raw.takeIf { it.isNotEmpty() }?.let { if (reEncrypt) it.enc() else it }
            }
            vaultItemDao.insert(VaultItemEntity(
                serviceName = if (reEncrypt) v.getString("serviceName").enc()
                              else v.getString("serviceName"),
                category = category,
                password = if (reEncrypt) v.getString("password").enc()
                           else v.getString("password"),
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
}
