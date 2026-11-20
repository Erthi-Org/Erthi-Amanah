package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.BeneficiaryDao
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import com.erthi.amanah.data.security.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WillAccessViewModel @Inject constructor(
    private val beneficiaryDao: BeneficiaryDao,
    private val authManager: AuthManager,
) : ViewModel() {

    val beneficiaries: StateFlow<List<BeneficiaryEntity>> =
        beneficiaryDao.getAllBeneficiaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun verifyPin(pin: String): Boolean = authManager.verifyPin(pin)

    fun saveAccessPolicies(updated: List<BeneficiaryEntity>) {
        viewModelScope.launch {
            updated.forEach { beneficiaryDao.updateBeneficiary(it) }
        }
    }
}
