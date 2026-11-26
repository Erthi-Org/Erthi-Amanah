package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.DebtDao
import com.erthi.amanah.data.db.entity.DebtDirection
import com.erthi.amanah.data.db.entity.DebtEntity
import com.erthi.amanah.data.security.FieldCipher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val debtDao: DebtDao,
    private val cipher: FieldCipher,
) : ViewModel() {

    val debts: StateFlow<List<DebtEntity>> = debtDao.getAllDebts()
        .map { list -> list.map(cipher::decrypt) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addDebt(personName: String, amount: Double, description: String, direction: DebtDirection) {
        if (personName.isBlank() || amount <= 0) return
        viewModelScope.launch {
            debtDao.insertDebt(cipher.encrypt(
                DebtEntity(personName = personName, amount = amount, description = description, direction = direction)
            ))
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch { debtDao.deleteDebt(debt) }
    }
}
