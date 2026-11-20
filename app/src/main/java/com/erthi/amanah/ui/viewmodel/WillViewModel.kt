package com.erthi.amanah.ui.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.WillDao
import com.erthi.amanah.data.db.entity.WillEntity
import com.erthi.amanah.data.security.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ENC_PREFIX = "[enc]"

@HiltViewModel
class WillViewModel @Inject constructor(
    private val willDao: WillDao,
    private val cryptoManager: CryptoManager,
) : ViewModel() {

    val wills: StateFlow<List<WillEntity>> = willDao.getAllWills()
        .map { list -> list.map { it.withDecryptedContent() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveWill(id: Long = 0, title: String, content: String, isDraft: Boolean = true) {
        viewModelScope.launch {
            val encData = cryptoManager.encrypt(content.toByteArray(Charsets.UTF_8))
            val encContent = ENC_PREFIX + Base64.encodeToString(encData.toByteArray(), Base64.NO_WRAP)
            willDao.insertWill(
                WillEntity(
                    id = id,
                    title = title.ifBlank { "وصيتي" },
                    content = encContent,
                    isDraft = isDraft,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deleteWill(will: WillEntity) {
        viewModelScope.launch { willDao.deleteWill(will) }
    }

    private fun WillEntity.withDecryptedContent(): WillEntity {
        if (!content.startsWith(ENC_PREFIX)) return this
        return try {
            val encBytes = Base64.decode(content.removePrefix(ENC_PREFIX), Base64.NO_WRAP)
            val plaintext = cryptoManager.decrypt(CryptoManager.EncryptedData.fromByteArray(encBytes))
            copy(content = String(plaintext, Charsets.UTF_8))
        } catch (_: Exception) {
            this
        }
    }
}
