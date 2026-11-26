package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.BeneficiaryDao
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import com.erthi.amanah.data.security.FieldCipher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeneficiaryViewModel @Inject constructor(
    private val beneficiaryDao: BeneficiaryDao,
    private val cipher: FieldCipher,
) : ViewModel() {

    val beneficiaries: StateFlow<List<BeneficiaryEntity>> = beneficiaryDao.getAllBeneficiaries()
        .map { list -> list.map(cipher::decrypt) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addBeneficiary(fullName: String, relation: String, phone: String, email: String, isEmergency: Boolean) {
        if (fullName.isBlank()) return
        viewModelScope.launch {
            beneficiaryDao.insertBeneficiary(cipher.encrypt(
                BeneficiaryEntity(
                    fullName = fullName,
                    relation = relation,
                    phone = phone,
                    email = email,
                    isEmergencyContact = isEmergency,
                )
            ))
        }
    }

    fun updateBeneficiary(beneficiary: BeneficiaryEntity) {
        viewModelScope.launch { beneficiaryDao.updateBeneficiary(cipher.encrypt(beneficiary)) }
    }

    fun delete(beneficiary: BeneficiaryEntity) {
        viewModelScope.launch { beneficiaryDao.deleteBeneficiary(beneficiary) }
    }
}
