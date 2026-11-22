package com.erthi.amanah.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.DocumentDao
import com.erthi.amanah.data.db.entity.DocumentEntity
import com.erthi.amanah.data.security.CryptoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentDao: DocumentDao,
    private val crypto: CryptoManager,
) : ViewModel() {

    val documents: StateFlow<List<DocumentEntity>> = documentDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    fun addDocument(title: String, category: String, notes: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            documentDao.insertDocument(DocumentEntity(title = title, category = category, filePath = "", notes = notes))
        }
    }

    // Copy and encrypt file from SAF URI into internal encrypted storage, then insert DB record.
    fun addDocumentWithFile(contentUri: Uri, title: String, category: String, notes: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check file size before reading anything into memory
                val fileSize = queryFileSize(contentUri)
                if (fileSize != null && fileSize > MAX_DOC_BYTES) {
                    _errorEvent.emit("حجم الملف يتجاوز الحد المسموح (${MAX_DOC_BYTES / 1024 / 1024} ميجابايت)")
                    documentDao.insertDocument(DocumentEntity(title = title, category = category, filePath = "", notes = notes))
                    return@launch
                }

                val docsDir = File(context.filesDir, "documents").also { it.mkdirs() }
                val originalName = resolveDisplayName(contentUri) ?: "doc_${System.currentTimeMillis()}"
                val mimeType = context.contentResolver.getType(contentUri) ?: "application/octet-stream"
                // Encrypt and store with .erthi_doc extension
                val baseName = originalName.substringBeforeLast(".")
                val destFile = File(docsDir, "${System.currentTimeMillis()}_${baseName}.erthi_doc")

                val inputStream = context.contentResolver.openInputStream(contentUri)
                    ?: run {
                        _errorEvent.emit("تعذر فتح الملف. قد تكون الصلاحيات منتهية الصلاحية — جرب إعادة تحديد الملف.")
                        documentDao.insertDocument(DocumentEntity(title = title, category = category, filePath = "", notes = notes))
                        return@launch
                    }

                inputStream.use { input ->
                    val raw = input.readBytes()
                    if (raw.size > MAX_DOC_BYTES) {
                        _errorEvent.emit("حجم الملف يتجاوز الحد المسموح (${MAX_DOC_BYTES / 1024 / 1024} ميجابايت)")
                        documentDao.insertDocument(DocumentEntity(title = title, category = category, filePath = "", notes = notes))
                        return@launch
                    }
                    val enc = crypto.encrypt(raw)
                    destFile.outputStream().use { out ->
                        out.write(DOC_MAGIC.toByteArray(Charsets.UTF_8))
                        out.write(enc.iv)
                        out.write(enc.ciphertext)
                    }
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
            } catch (e: SecurityException) {
                // URI permission was revoked (e.g. app was killed while picker was open)
                _errorEvent.emit("انتهت صلاحية الوصول للملف. يرجى تحديد الملف مرة أخرى.")
                documentDao.insertDocument(DocumentEntity(title = title, category = category, filePath = "", notes = notes))
            } catch (_: Exception) {
                // Fall back to metadata-only insert if file copy fails
                documentDao.insertDocument(DocumentEntity(title = title, category = category, filePath = "", notes = notes))
            }
        }
    }

    private fun queryFileSize(uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (idx >= 0) cursor.getLong(idx) else null
                    } else null
                }
        } catch (_: Exception) { null }
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

    companion object {
        const val MAX_DOC_BYTES = 50 * 1024 * 1024  // 50 MB
        const val DOC_MAGIC = "WASI_DOC"
    }
}
