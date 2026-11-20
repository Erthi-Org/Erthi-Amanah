package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.DocumentDao
import com.erthi.amanah.data.db.entity.DocumentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentDao: DocumentDao,
) : ViewModel() {

    val documents: StateFlow<List<DocumentEntity>> = documentDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addDocument(title: String, category: String, notes: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            documentDao.insertDocument(DocumentEntity(title = title, category = category, filePath = "", notes = notes))
        }
    }

    fun delete(doc: DocumentEntity) {
        viewModelScope.launch { documentDao.deleteDocument(doc) }
    }
}
