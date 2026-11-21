package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.DebtDao
import com.erthi.amanah.data.db.entity.DebtDirection
import com.erthi.amanah.data.db.entity.DebtEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val debtDao: DebtDao,
) : ViewModel() {

    val debts: StateFlow<List<DebtEntity>> = debtDao.getAllDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addDebt(personName: String, amount: Double, description: String, direction: DebtDirection) {
        if (personName.isBlank() || amount <= 0) return
        viewModelScope.launch {
            debtDao.insertDebt(DebtEntity(personName = personName, amount = amount, description = description, direction = direction))
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch { debtDao.deleteDebt(debt) }
    }
}
