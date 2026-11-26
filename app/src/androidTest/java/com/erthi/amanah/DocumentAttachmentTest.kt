package com.erthi.amanah

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.erthi.amanah.data.db.ErthiDatabase
import com.erthi.amanah.data.security.CryptoManager
import com.erthi.amanah.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DocumentAttachmentTest {
    @Test
    fun importsUnknownLengthFileAndRejectsMissingOrOversizeWithoutRecords(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File.createTempFile("attachment-fixture-", ".bin", context.cacheDir)
        val oversized = File.createTempFile("attachment-oversize-", ".bin", context.cacheDir)
        var viewModel: DocumentViewModel? = null
        val docsDir = File(context.filesDir, "documents")
        val existing = docsDir.list().orEmpty().toSet()
        val image = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        source.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        val data = source.readBytes()
        java.io.RandomAccessFile(oversized, "rw").use { it.setLength(50L * 1024 * 1024 + 1) }

        val db = Room.inMemoryDatabaseBuilder(context, ErthiDatabase::class.java).build()
        try {
            val dao = db.documentDao()
            val crypto = CryptoManager()
            val vm = DocumentViewModel(context, dao, crypto)
            viewModel = vm
            vm.addDocumentWithFile(Uri.fromFile(source), "Synthetic import", "", "")
            val document = withTimeout(15_000) { dao.getAllDocuments().first { it.isNotEmpty() } }.single()
            val file = File(document.filePath)
            val raw = file.readBytes()
            assertEquals("WASI_DOC", String(raw, 0, 8, Charsets.US_ASCII))
            val encrypted = CryptoManager.EncryptedData.fromByteArray(raw.copyOfRange(8, raw.size))
            assertArrayEquals(data, crypto.decrypt(encrypted))
            val preview = vm.preview(document).getOrThrow()
            assertEquals(4, preview.width)
            assertEquals(4, preview.height)
            preview.recycle()

            for (sourceFile in listOf(File(context.cacheDir, "nonexistent-attachment-fixture"), oversized)) {
                val error = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(25_000) { vm.errorEvent.first() }
                }
                vm.addDocumentWithFile(Uri.fromFile(sourceFile), "Failed attachment", "", "")
                assertTrue(error.await().isNotBlank())
                assertEquals(listOf(document), dao.getAllDocuments().first())
            }
            assertEquals(setOf(file.name), docsDir.list().orEmpty().toSet() - existing)
        } finally {
            viewModel?.viewModelScope?.cancel()
            db.close()
            source.delete()
            oversized.delete()
            docsDir.listFiles().orEmpty().filter { it.name !in existing }.forEach { it.delete() }
        }
    }
}
