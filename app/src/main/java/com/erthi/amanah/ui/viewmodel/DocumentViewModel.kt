package com.erthi.amanah.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.erthi.amanah.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.DocumentDao
import com.erthi.amanah.data.db.entity.DocumentEntity
import com.erthi.amanah.data.security.CryptoManager
import com.erthi.amanah.data.security.DocumentEncryption
import com.erthi.amanah.data.security.DocumentTooLargeException
import com.erthi.amanah.data.security.PreviewTooLargeException
import com.erthi.amanah.data.security.UnsupportedPreviewException
import com.erthi.amanah.data.security.renderDocumentPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.util.UUID
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

    fun addDocumentWithFile(contentUri: Uri, title: String, category: String, notes: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            var staging: File? = null
            var published: File? = null
            try {
                val reportedSize = queryFileSize(contentUri)
                if (reportedSize != null && reportedSize > DocumentEncryption.MAX_BYTES) {
                    throw DocumentTooLargeException()
                }

                val docsDir = File(context.filesDir, "documents")
                if (!docsDir.isDirectory && !docsDir.mkdirs() && !docsDir.isDirectory) {
                    throw IOException("Cannot create document directory")
                }
                val destFile = File(docsDir, "${UUID.randomUUID()}.erthi_doc")
                val mimeType = context.contentResolver.getType(contentUri) ?: "application/octet-stream"
                val partialFile = File.createTempFile(".attachment-", ".part", docsDir)
                staging = partialFile
                val input = context.contentResolver.openInputStream(contentUri)
                    ?: throw IOException("Document provider returned no stream")
                input.use {
                    partialFile.outputStream().use { output ->
                        crypto.encryptDocument(input, output) { coroutineContext.ensureActive() }
                        output.fd.sync()
                    }
                }
                coroutineContext.ensureActive()
                // Keep the insert and its success bookkeeping together if the screen closes.
                withContext(NonCancellable) {
                    Files.move(partialFile.toPath(), destFile.toPath(), ATOMIC_MOVE)
                    published = destFile
                    documentDao.insertDocument(
                        DocumentEntity(title = title, category = category, filePath = destFile.absolutePath,
                            mimeType = mimeType, notes = notes)
                    )
                    published = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                staging?.delete()
                published?.delete()
                val message = when (e) {
                    is DocumentTooLargeException -> R.string.attachment_too_large
                    is SecurityException -> R.string.attachment_access_expired
                    else -> R.string.attachment_save_failed
                }
                _errorEvent.emit(context.getString(message))
            } finally {
                staging?.delete()
                published?.delete()
            }
        }
    }

    private fun queryFileSize(uri: Uri): Long? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && index >= 0 && !cursor.isNull(index)) cursor.getLong(index)
                else null
            }
    } catch (_: Exception) {
        // Provider metadata is optional. The stream limit is authoritative.
        null
    }

    internal suspend fun preview(doc: DocumentEntity): Result<android.graphics.Bitmap> =
        withContext(Dispatchers.IO) {
            try {
                val directory = File(context.filesDir, "documents").canonicalFile
                val source = File(doc.filePath).canonicalFile
                require(source.isFile && source.parentFile == directory)
                Result.success(renderDocumentPreview(source, crypto))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    internal fun previewError(error: Throwable): Int = when (error) {
        is PreviewTooLargeException -> R.string.preview_too_large
        is UnsupportedPreviewException -> R.string.preview_unsupported
        else -> R.string.preview_failed
    }

    fun delete(doc: DocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(NonCancellable) {
                    documentDao.deleteDocument(doc)
                    if (doc.filePath.isNotEmpty()) {
                        val docsDir = File(context.filesDir, "documents").canonicalFile
                        val file = File(doc.filePath).canonicalFile
                        if (file.parentFile == docsDir) Files.deleteIfExists(file.toPath())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _errorEvent.emit(context.getString(R.string.attachment_delete_failed))
            }
        }
    }

}
