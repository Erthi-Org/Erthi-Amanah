package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.DigitalAccountDao
import com.erthi.amanah.data.db.entity.DigitalAccountEntity
import com.erthi.amanah.data.security.FieldCipher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DigitalAccountViewModel @Inject constructor(
    private val accountDao: DigitalAccountDao,
    private val cipher: FieldCipher,
) : ViewModel() {

    val accounts: StateFlow<List<DigitalAccountEntity>> = accountDao.getAllAccounts()
        .map { list -> list.map(cipher::decrypt) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAccount(platform: String, identifier: String, instructions: String, notes: String) {
        if (platform.isBlank() || identifier.isBlank()) return
        viewModelScope.launch {
            accountDao.insertAccount(cipher.encrypt(
                DigitalAccountEntity(platform = platform, accountIdentifier = identifier, instructions = instructions, notes = notes)
            ))
        }
    }

    fun delete(account: DigitalAccountEntity) {
        viewModelScope.launch { accountDao.deleteAccount(account) }
    }
}
