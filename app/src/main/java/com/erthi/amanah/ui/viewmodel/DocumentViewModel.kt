package com.erthi.amanah.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.DocumentDao
import com.erthi.amanah.data.db.entity.DocumentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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

    // Copy file from SAF URI into internal storage, then insert DB record.
    fun addDocumentWithFile(contentUri: Uri, title: String, category: String, notes: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val docsDir = File(context.filesDir, "documents").also { it.mkdirs() }
                val originalName = resolveDisplayName(contentUri) ?: "doc_${System.currentTimeMillis()}"
                val mimeType = context.contentResolver.getType(contentUri) ?: "application/octet-stream"
                // Avoid collisions by prepending a timestamp
                val destFile = File(docsDir, "${System.currentTimeMillis()}_$originalName")
                context.contentResolver.openInputStream(contentUri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                documentDao.insertDocument(
                    DocumentEntity(
                        title = title,
                        category = category,
                        filePath = destFile.absolutePath,
                        mimeType = mimeType,
                        notes = notes,
                    )
                )
            } catch (_: Exception) {
                // Fall back to metadata-only insert if file copy fails
                documentDao.insertDocument(DocumentEntity(title = title, category = category, filePath = "", notes = notes))
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }

    fun delete(doc: DocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (doc.filePath.isNotEmpty()) File(doc.filePath).delete()
            documentDao.deleteDocument(doc)
        }
    }
}
