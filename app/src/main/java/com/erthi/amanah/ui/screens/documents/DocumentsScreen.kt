package com.erthi.amanah.ui.screens.documents

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.DocumentEntity
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect error events from the ViewModel
    LaunchedEffect(viewModel) {
        viewModel.errorEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Pending file URI from SAF picker; null means no file selected yet
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedFileName by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // uri is null when user cancels — do not show dialog in that case unless already shown
        if (uri != null) {
            pickedUri = uri
            pickedFileName = try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst()) c.getString(0) else "" } ?: ""
            } catch (_: Exception) { "" }
            showAddDialog = true
        } else if (!showAddDialog) {
            // Cancelled with no dialog open — nothing to do
        }
    }

    if (showAddDialog) {
        AddDocumentDialog(
            pickedUri = pickedUri,
            pickedFileName = pickedFileName,
            onPickFile = {
                showAddDialog = false
                filePicker.launch(arrayOf("*/*"))
            },
            onDismiss = {
                showAddDialog = false
                pickedUri = null
                pickedFileName = ""
            },
            onConfirm = { title, category, notes ->
                val uri = pickedUri
                if (uri != null) {
                    viewModel.addDocumentWithFile(uri, title, category, notes)
                } else {
                    viewModel.addDocument(title, category, notes)
                }
                showAddDialog = false
                pickedUri = null
                pickedFileName = ""
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "الوثائق المهمة", onBack = onBack) },
        containerColor = NavyDeep,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                containerColor = DocumentsColor,
                contentColor = NavyDeep,
            ) { Icon(Icons.Default.AttachFile, null) }
        },
    ) { padding ->
        if (documents.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState("لا توجد وثائق مرفقة\nاضغط + لإرفاق ملف أو تسجيل وثيقة")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(documents, key = { it.id }) { doc ->
                    DocumentCard(doc, onDelete = { viewModel.delete(doc) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun DocumentCard(doc: DocumentEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon reflects whether file is attached
            val hasFile = doc.filePath.isNotEmpty()
            Icon(
                if (hasFile) Icons.Default.Description else Icons.Default.TextSnippet,
                null,
                tint = if (hasFile) DocumentsColor else TextMuted,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (doc.category.isNotBlank()) Text(doc.category, style = MaterialTheme.typography.labelSmall, color = DocumentsColor)
                if (hasFile) {
                    val name = java.io.File(doc.filePath).name
                    Text(name, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
                }
                if (doc.notes.isNotBlank()) Text(doc.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = EmergencyRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddDocumentDialog(
    pickedUri: android.net.Uri?,
    pickedFileName: String,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
) {
    var title by remember(pickedFileName) { mutableStateOf(pickedFileName.substringBeforeLast(".").ifBlank { "" }) }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavyCard,
        title = {
            Text(
                if (pickedUri != null) "تسجيل وثيقة مع ملف" else "تسجيل وثيقة",
                color = TextPrimary, fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Show attached file info or offer to pick
                if (pickedUri != null && pickedFileName.isNotBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = DocumentsColor.copy(alpha = 0.12f)), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, null, tint = DocumentsColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(pickedFileName, style = MaterialTheme.typography.labelSmall, color = DocumentsColor, maxLines = 1)
                        }
                    }
                } else {
                    TextButton(onClick = onPickFile) {
                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إرفاق ملف (اختياري)", color = DocumentsColor)
                    }
                }
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("عنوان الوثيقة *", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(), singleLine = true,
                    placeholder = { Text("مثال: بطاقة رقم قومي", color = TextMuted) },
                )
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text("التصنيف", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(), singleLine = true,
                    placeholder = { Text("مثال: هوية، تأمين", color = TextMuted) },
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("ملاحظات", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = dialogFieldColors(), maxLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, category, notes) },
                enabled = title.isNotBlank(),
            ) { Text("إضافة", color = DocumentsColor, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) }
        },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DocumentsColor, unfocusedBorderColor = DividerColor,
    focusedLabelColor = DocumentsColor, cursorColor = DocumentsColor,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
)
