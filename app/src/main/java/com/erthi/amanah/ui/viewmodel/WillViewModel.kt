package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.WillDao
import com.erthi.amanah.data.db.entity.WillEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WillViewModel @Inject constructor(
    private val willDao: WillDao,
) : ViewModel() {

    val wills: StateFlow<List<WillEntity>> = willDao.getAllWills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveWill(id: Long = 0, title: String, content: String, isDraft: Boolean = true) {
        viewModelScope.launch {
            willDao.insertWill(
                WillEntity(
                    id = id,
                    title = title.ifBlank { "وصيتي" },
                    content = content,
                    isDraft = isDraft,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deleteWill(will: WillEntity) {
        viewModelScope.launch { willDao.deleteWill(will) }
    }
}
