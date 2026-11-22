package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.EntrustedDao
import com.erthi.amanah.data.db.entity.EntrustedEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EntrustedViewModel @Inject constructor(
    private val entrustedDao: EntrustedDao,
) : ViewModel() {

    val items: StateFlow<List<EntrustedEntity>> = entrustedDao.getAllEntrusted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(ownerName: String, ownerPhone: String, description: String, returnInstructions: String) {
        if (ownerName.isBlank() || description.isBlank()) return
        viewModelScope.launch {
            entrustedDao.insertEntrusted(EntrustedEntity(ownerName = ownerName, ownerPhone = ownerPhone, itemDescription = description, returnInstructions = returnInstructions))
        }
    }

    fun delete(item: EntrustedEntity) {
        viewModelScope.launch { entrustedDao.deleteEntrusted(item) }
    }
}
