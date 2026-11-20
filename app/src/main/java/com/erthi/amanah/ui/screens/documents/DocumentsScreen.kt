package com.erthi.amanah.ui.screens.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.DocumentEntity
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.DocumentViewModel

@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel(),
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val documents by viewModel.documents.collectAsState()

    if (showAddDialog) {
        AddDocumentDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, category, notes ->
                viewModel.addDocument(title, category, notes)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "الوثائق المهمة", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = SilverBlue, contentColor = NavyDeep) {
                Icon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        if (documents.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState("لا توجد وثائق مرفقة\nاضغط + لتسجيل وثيقة")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, null, tint = SilverBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (doc.category.isNotBlank()) Text(doc.category, style = MaterialTheme.typography.labelSmall, color = SilverBlue)
                if (doc.notes.isNotBlank()) Text(doc.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = EmergencyRed, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun AddDocumentDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = NavyCard,
        title = { Text("تسجيل وثيقة", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان الوثيقة *", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true, placeholder = { Text("مثال: بطاقة رقم قومي، عقد تأمين", color = TextMuted) })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("التصنيف", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true, placeholder = { Text("مثال: هوية، تأمين، عقار", color = TextMuted) })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), maxLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(title, category, notes) }, enabled = title.isNotBlank()) { Text("إضافة", color = SilverBlue, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) } },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = SilverBlue, unfocusedBorderColor = DividerColor, focusedLabelColor = SilverBlue, cursorColor = SilverBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
