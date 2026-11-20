package com.erthi.amanah.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.VaultItemDao
import com.erthi.amanah.data.db.entity.VaultCategory
import com.erthi.amanah.data.db.entity.VaultItemEntity
import com.erthi.amanah.data.security.CryptoManager
import com.erthi.amanah.util.PasswordStrength
import com.erthi.amanah.util.PasswordUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ENC = "[enc]"

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val dao: VaultItemDao,
    private val crypto: CryptoManager,
) : ViewModel() {

    val items: StateFlow<List<VaultItemEntity>> = dao.getAllItems()
        .map { list -> list.map { it.decrypted() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveItem(
        id: Long = 0,
        serviceName: String,
        category: VaultCategory,
        websiteUrl: String?,
        username: String?,
        email: String?,
        password: String,
        accountIdentifier: String?,
        recoveryEmail: String?,
        recoveryPhone: String?,
        recoveryCodes: String?,
        notes: String?,
    ) {
        viewModelScope.launch {
            dao.insert(
                VaultItemEntity(
                    id = id,
                    serviceName = serviceName.enc(),
                    category = category,
                    websiteUrl = websiteUrl?.takeIf { it.isNotBlank() }?.enc(),
                    username = username?.takeIf { it.isNotBlank() }?.enc(),
                    email = email?.takeIf { it.isNotBlank() }?.enc(),
                    password = password.enc(),
                    accountIdentifier = accountIdentifier?.takeIf { it.isNotBlank() }?.enc(),
                    recoveryEmail = recoveryEmail?.takeIf { it.isNotBlank() }?.enc(),
                    recoveryPhone = recoveryPhone?.takeIf { it.isNotBlank() }?.enc(),
                    recoveryCodes = recoveryCodes?.takeIf { it.isNotBlank() }?.enc(),
                    notes = notes?.takeIf { it.isNotBlank() }?.enc(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deleteItem(item: VaultItemEntity) {
        viewModelScope.launch { dao.delete(item) }
    }

    fun generatePassword(
        length: Int = 16,
        upper: Boolean = true,
        lower: Boolean = true,
        digits: Boolean = true,
        symbols: Boolean = true,
    ): String = PasswordUtils.generatePassword(length, upper, lower, digits, symbols)

    fun passwordStrength(password: String): PasswordStrength =
        PasswordUtils.passwordStrength(password)

    private fun String.enc(): String {
        val bytes = toByteArray(Charsets.UTF_8)
        val encData = crypto.encrypt(bytes)
        return ENC + Base64.encodeToString(encData.toByteArray(), Base64.NO_WRAP)
    }

    private fun String.dec(): String {
        if (!startsWith(ENC)) return this
        return try {
            val bytes = Base64.decode(removePrefix(ENC), Base64.NO_WRAP)
            String(crypto.decrypt(CryptoManager.EncryptedData.fromByteArray(bytes)), Charsets.UTF_8)
        } catch (_: Exception) { this }
    }

    private fun VaultItemEntity.decrypted() = copy(
        serviceName = serviceName.dec(),
        websiteUrl = websiteUrl?.dec(),
        username = username?.dec(),
        email = email?.dec(),
        password = password.dec(),
        accountIdentifier = accountIdentifier?.dec(),
        recoveryEmail = recoveryEmail?.dec(),
        recoveryPhone = recoveryPhone?.dec(),
        recoveryCodes = recoveryCodes?.dec(),
        notes = notes?.dec(),
    )
}
