package com.erthi.amanah.ui.screens.entrusted

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erthi.amanah.data.db.entity.EntrustedEntity
import com.erthi.amanah.ui.components.*
import com.erthi.amanah.ui.theme.*
import com.erthi.amanah.ui.viewmodel.EntrustedViewModel

@Composable
fun EntrustedScreen(
    onBack: () -> Unit,
    viewModel: EntrustedViewModel = hiltViewModel(),
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val items by viewModel.items.collectAsState()

    if (showAddDialog) {
        AddEntrustedDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { ownerName, phone, description, instructions ->
                viewModel.addItem(ownerName, phone, description, instructions)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        topBar = { ErthiTopBar(title = "الأمانات", onBack = onBack) },
        containerColor = NavyDeep,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = SilverBlue, contentColor = NavyDeep) {
                Icon(Icons.Default.Add, null)
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState("لا توجد أمانات مسجّلة\nاضغط + لإضافة أمانة")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { item ->
                    EntrustedCard(item, onDelete = { viewModel.delete(item) })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun EntrustedCard(item: EntrustedEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = NavyCard)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.ownerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(item.itemDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
                if (item.ownerPhone.isNotBlank()) Text(item.ownerPhone, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = EmergencyRed, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun AddEntrustedDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = NavyCard,
        title = { Text("إضافة أمانة", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("اسم صاحب الأمانة *", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("وصف الأمانة *", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), maxLines = 3)
                OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("تعليمات الإعادة", color = TextSecondary) }, modifier = Modifier.fillMaxWidth(), colors = dialogFieldColors(), maxLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(ownerName, phone, description, instructions) }, enabled = ownerName.isNotBlank() && description.isNotBlank()) { Text("إضافة", color = SilverBlue, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = TextSecondary) } },
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = SilverBlue, unfocusedBorderColor = DividerColor, focusedLabelColor = SilverBlue, cursorColor = SilverBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
