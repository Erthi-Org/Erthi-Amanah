package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.BeneficiaryDao
import com.erthi.amanah.data.db.entity.BeneficiaryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val beneficiaryDao: BeneficiaryDao,
) : ViewModel() {

    val emergencyContacts: StateFlow<List<BeneficiaryEntity>> =
        beneficiaryDao.getEmergencyContacts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addContact(fullName: String, phone: String, priority: Int) {
        if (fullName.isBlank() || phone.isBlank()) return
        viewModelScope.launch {
            beneficiaryDao.insertBeneficiary(
                BeneficiaryEntity(
                    fullName = fullName,
                    relation = "جهة طوارئ",
                    phone = phone,
                    isEmergencyContact = true,
                    emergencyPriority = priority,
                )
            )
        }
    }

    fun deleteContact(contact: BeneficiaryEntity) {
        viewModelScope.launch { beneficiaryDao.deleteBeneficiary(contact) }
    }
}
